package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

class FirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth
  )

  fun currentUserId(): String? = auth.currentUser?.uid

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  private fun <T> Flow<T>.handleListenerErrors(uid: String, path: String): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && auth.currentUser?.uid == uid) {
        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
      }
      throw error
    }

  // -----------------------------------------------------------------
  // 1. REAL-TIME SNAPSHOT OBSERVERS (Strictly Auth-Scoped)
  // -----------------------------------------------------------------
  fun observeProfile(userId: String): Flow<SyncedUserProfile?> = flow {
    val path = "users/$userId/profile/main"
    emitAll(
      userDoc(userId).collection("profile").document("main")
        .snapshots()
        .map { snapshot ->
          if (!snapshot.exists()) null
          else snapshot.toObject(SyncedUserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeTasks(userId: String): Flow<List<SyncedTaskItem>> = flow {
    val path = "users/$userId/tasks"
    emitAll(
      userDoc(userId).collection("tasks")
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            try {
              SyncedTaskItem(
                id = doc.id,
                userId = doc.getString("userId") ?: userId,
                title = doc.getString("title") ?: "",
                date = doc.getString("date") ?: "",
                category = doc.getString("category") ?: "",
                details = doc.getString("details") ?: "",
                isCompleted = doc.getBoolean("isCompleted") ?: false,
                hasAlarm = doc.getBoolean("hasAlarm") ?: false,
                alarmTimeMinutesBefore = doc.getLong("alarmTimeMinutesBefore") ?: 15L,
                addedBy = doc.getString("addedBy") ?: "Fatou",
                createdAt = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)
              )
            } catch (e: Exception) {
              Log.w(TAG, "Skipping malformed task ${doc.id}", e)
              null
            }
          }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeNotes(userId: String): Flow<List<SyncedNoteItem>> = flow {
    val path = "users/$userId/notes"
    emitAll(
      userDoc(userId).collection("notes")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedNoteItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeAppointments(userId: String): Flow<List<SyncedAppointmentItem>> = flow {
    val path = "users/$userId/appointments"
    emitAll(
      userDoc(userId).collection("appointments")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedAppointmentItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeDocuments(userId: String): Flow<List<SyncedMedicalDocItem>> = flow {
    val path = "users/$userId/documents"
    emitAll(
      userDoc(userId).collection("documents")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedMedicalDocItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeChecklists(userId: String): Flow<List<SyncedChecklistItem>> = flow {
    val path = "users/$userId/checklists"
    emitAll(
      userDoc(userId).collection("checklists")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedChecklistItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.sortOrder }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeProtocolSteps(userId: String): Flow<List<SyncedProtocolStep>> = flow {
    val path = "users/$userId/protocol_steps"
    emitAll(
      userDoc(userId).collection("protocol_steps")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedProtocolStep::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.sortOrder }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeCareProviders(userId: String): Flow<List<SyncedCareProvider>> = flow {
    val path = "users/$userId/care_providers"
    emitAll(
      userDoc(userId).collection("care_providers")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedCareProvider::class.java, ServerTimestampBehavior.ESTIMATE)
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeVoiceMemos(userId: String): Flow<List<SyncedVoiceMemo>> = flow {
    val path = "users/$userId/voice_memos"
    emitAll(
      userDoc(userId).collection("voice_memos")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedVoiceMemo::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeWalletCards(userId: String): Flow<WalletSyncSnapshot> = flow {
    val path = "users/$userId/wallet_cards"
    emitAll(
      userDoc(userId).collection("wallet_cards")
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          val cards = snapshot.documents.mapNotNull { doc ->
            try {
              SyncedWalletCard(
                id = doc.id,
                userId = doc.getString("userId") ?: userId,
                cardTitle = doc.getString("cardTitle") ?: "",
                cardCategory = doc.getString("cardCategory") ?: "Assurance",
                cardNumber = doc.getString("cardNumber") ?: "",
                holderName = doc.getString("holderName") ?: "",
                expiryDate = doc.getString("expiryDate") ?: "",
                notes = doc.getString("notes") ?: "",
                gradientKey = normalizeGradientKey(doc.getString("gradientKey")),
                frontImageBase64 = doc.getString("frontImageBase64") ?: "",
                backImageBase64 = doc.getString("backImageBase64") ?: "",
                sortOrder = doc.getLong("sortOrder") ?: 0L,
                createdAt = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)
              )
            } catch (e: Exception) {
              Log.w(TAG, "Skipping malformed wallet card ${doc.id}", e)
              null
            }
          }.sortedWith(compareBy<SyncedWalletCard> { it.sortOrder }.thenBy { it.cardTitle })

          val syncStatus = when {
            snapshot.metadata.hasPendingWrites() && snapshot.metadata.isFromCache -> WalletSyncStatus.OFFLINE_LOCAL
            snapshot.metadata.hasPendingWrites() -> WalletSyncStatus.SYNCING
            snapshot.metadata.isFromCache -> WalletSyncStatus.OFFLINE_LOCAL
            else -> WalletSyncStatus.SYNCED
          }
          WalletSyncSnapshot(cards = cards, syncStatus = syncStatus)
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeMedReminders(userId: String): Flow<List<SyncedMedReminder>> = flow {
    val path = "users/$userId/med_reminders"
    emitAll(
      userDoc(userId).collection("med_reminders")
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            try {
              SyncedMedReminder(
                id = doc.id,
                userId = doc.getString("userId") ?: userId,
                title = doc.getString("title") ?: "",
                medication = doc.getString("medication") ?: "",
                dosage = doc.getString("dosage") ?: "",
                hour = doc.getLong("hour") ?: 8L,
                minute = doc.getLong("minute") ?: 0L,
                repeatDaily = doc.getBoolean("repeatDaily") ?: true,
                isEnabled = doc.getBoolean("isEnabled") ?: true,
                protocolPhase = doc.getString("protocolPhase") ?: "",
                notes = doc.getString("notes") ?: "",
                pillsRemaining = doc.getLong("pillsRemaining") ?: 60L,
                dailyAmTaken = doc.getBoolean("dailyAmTaken") ?: false,
                dailyPmTaken = doc.getBoolean("dailyPmTaken") ?: false,
                lastDoseDateStr = doc.getString("lastDoseDateStr") ?: "",
                soundEnabled = doc.getBoolean("soundEnabled") ?: true,
                createdAt = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)
              )
            } catch (e: Exception) {
              Log.w(TAG, "Skipping malformed med_reminder ${doc.id}", e)
              null
            }
          }.sortedWith(compareBy<SyncedMedReminder> { it.hour * 60L + it.minute }.thenBy { it.title })
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observePregnancyMeds(userId: String): Flow<List<SyncedPregnancyMed>> = flow {
    val path = "users/$userId/preg_medications"
    emitAll(
      userDoc(userId).collection("preg_medications")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedPregnancyMed::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedWith(
              compareBy<SyncedPregnancyMed> {
                when (it.safetyLevel) {
                  "SAFE" -> 0
                  "CAUTION" -> 1
                  else -> 2
                }
              }.thenBy { it.name }
            )
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeNutritionFoods(userId: String): Flow<List<SyncedNutritionFood>> = flow {
    val path = "users/$userId/nutrition_foods"
    emitAll(
      userDoc(userId).collection("nutrition_foods")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedNutritionFood::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedWith(
              compareBy<SyncedNutritionFood> {
                when (it.safetyStatus) {
                  "EAT" -> 0
                  "LIMIT" -> 1
                  else -> 2
                }
              }.thenBy { it.name }
            )
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeGroceryItems(userId: String): Flow<List<SyncedGroceryItem>> = flow {
    val path = "users/$userId/grocery_items"
    emitAll(
      userDoc(userId).collection("grocery_items")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedGroceryItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareBy<SyncedGroceryItem> { it.isChecked }.thenBy { it.category }.thenBy { it.label })
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeHealthIssues(userId: String): Flow<List<SyncedHealthIssue>> = flow {
    val path = "users/$userId/health_issues"
    emitAll(
      userDoc(userId).collection("health_issues")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedHealthIssue::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedWith(
              compareBy<SyncedHealthIssue> { it.isResolved }
                .thenByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            )
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeExpenses(userId: String): Flow<List<SyncedExpenseRecord>> = flow {
    val path = "users/$userId/expenses"
    emitAll(
      userDoc(userId).collection("expenses")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedExpenseRecord::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
        }
        .handleListenerErrors(userId, path)
    )
  }

  fun observeBackups(userId: String): Flow<List<SyncedBackupSnapshot>> = flow {
    val path = "users/$userId/backups"
    emitAll(
      userDoc(userId).collection("backups")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SyncedBackupSnapshot::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.timestampEpochMillis }
        }
        .handleListenerErrors(userId, path)
    )
  }

  // -----------------------------------------------------------------
  // 2. IDEMPOTENT FIRST-LOGIN SEEDING (Never overwrites existing docs)
  // -----------------------------------------------------------------
  suspend fun ensureInitialSeedData(userId: String = requireUserId()) {
    val uDoc = userDoc(userId)
    val profileRef = uDoc.collection("profile").document("main")
    try {
      val profileSnap = profileRef.get().await()
      if (profileSnap.exists()) {
        // Existing account: ensure wallet_cards and Health Hub collections have starter data if missing from prior schema versions
        val backfillBatch = db.batch()
        var hasBackfill = false
        if (uDoc.collection("wallet_cards").limit(1).get().await().isEmpty) {
          SeedDefaults.initialWalletCards.forEach { card ->
            backfillBatch.set(uDoc.collection("wallet_cards").document(card.id), walletCardToCreateMap(userId, card))
          }
          hasBackfill = true
        }
        if (uDoc.collection("med_reminders").limit(1).get().await().isEmpty) {
          HealthHubDefaults.initialMedReminders.forEach { reminder ->
            backfillBatch.set(uDoc.collection("med_reminders").document(reminder.id), medReminderToCreateMap(userId, reminder))
          }
          hasBackfill = true
        }
        if (uDoc.collection("preg_medications").limit(1).get().await().isEmpty) {
          HealthHubDefaults.initialPregnancyMeds.forEach { med ->
            backfillBatch.set(uDoc.collection("preg_medications").document(med.id), pregnancyMedToCreateMap(userId, med))
          }
          hasBackfill = true
        }
        if (uDoc.collection("nutrition_foods").limit(1).get().await().isEmpty) {
          HealthHubDefaults.initialNutritionFoods.forEach { food ->
            backfillBatch.set(uDoc.collection("nutrition_foods").document(food.id), nutritionFoodToCreateMap(userId, food))
          }
          hasBackfill = true
        }
        if (uDoc.collection("grocery_items").limit(1).get().await().isEmpty) {
          HealthHubDefaults.initialGroceryItems.forEach { item ->
            backfillBatch.set(uDoc.collection("grocery_items").document(item.id), groceryItemToCreateMap(userId, item))
          }
          hasBackfill = true
        }
        if (uDoc.collection("health_issues").limit(1).get().await().isEmpty) {
          HealthHubDefaults.initialHealthIssues.forEach { issue ->
            backfillBatch.set(uDoc.collection("health_issues").document(issue.id), healthIssueToCreateMap(userId, issue))
          }
          hasBackfill = true
        }
        if (uDoc.collection("expenses").limit(1).get().await().isEmpty) {
          HealthHubDefaults.initialExpenses.forEach { expense ->
            backfillBatch.set(uDoc.collection("expenses").document(expense.id), expenseToCreateMap(userId, expense))
          }
          hasBackfill = true
        }
        // Replace legacy [Doctor's Name] placeholder providers with RMA East Side care providers
        val existingProviders = uDoc.collection("care_providers").get().await()
        if (existingProviders.isEmpty || existingProviders.documents.any { it.getString("name")?.contains("[Doctor") == true }) {
          existingProviders.documents.forEach { backfillBatch.delete(it.reference) }
          HealthHubDefaults.initialCareProviders.forEach { provider ->
            backfillBatch.set(uDoc.collection("care_providers").document(provider.id), providerToCreateMap(userId, provider))
          }
          hasBackfill = true
        }
        if (uDoc.collection("checklists").whereEqualTo("listKey", "partner_todo").limit(1).get().await().isEmpty) {
          HealthHubDefaults.initialPartnerTodoItems.forEach { item ->
            backfillBatch.set(uDoc.collection("checklists").document(item.id), checklistToCreateMap(userId, item))
          }
          hasBackfill = true
        }
        if (hasBackfill) {
          backfillBatch.commit().await()
        }
        // Auto-upgrade if account has pre-Nov-18 protocol seed or missing Nov 18 transfer day step
        val stepNov18Snap = uDoc.collection("protocol_steps").document("p3_1").get().await()
        val stepNov17Snap = uDoc.collection("protocol_steps").document("p2_12").get().await()
        val taskT6Snap = uDoc.collection("tasks").document("t6").get().await()
        if (!stepNov18Snap.exists() || !stepNov17Snap.exists() || !taskT6Snap.exists()) {
          resetProtocolToDefault(userId)
        }
        return
      }

      val batch = db.batch()
      batch.set(profileRef, SyncedUserProfile().toCreateMap(userId))

      if (uDoc.collection("tasks").limit(1).get().await().isEmpty) {
        PmaRepository.immediateTasks.forEach { task ->
          val docRef = uDoc.collection("tasks").document(task.id)
          batch.set(docRef, taskToCreateMap(userId, task))
        }
      }

      if (uDoc.collection("notes").limit(1).get().await().isEmpty) {
        SeedDefaults.initialNotes.forEach { note ->
          val docRef = uDoc.collection("notes").document(note.id)
          batch.set(docRef, noteToCreateMap(userId, note))
        }
      }

      if (uDoc.collection("appointments").limit(1).get().await().isEmpty) {
        SeedDefaults.initialAppointments.forEach { apt ->
          val docRef = uDoc.collection("appointments").document(apt.id)
          batch.set(docRef, appointmentToCreateMap(userId, apt))
        }
      }

      if (uDoc.collection("documents").limit(1).get().await().isEmpty) {
        SeedDefaults.initialDocuments.forEach { doc ->
          val docRef = uDoc.collection("documents").document(doc.id)
          batch.set(docRef, documentToCreateMap(userId, doc))
        }
      }

      if (uDoc.collection("checklists").limit(1).get().await().isEmpty) {
        (SeedDefaults.initialChecklistItems + HealthHubDefaults.initialPartnerTodoItems).forEach { item ->
          val docRef = uDoc.collection("checklists").document(item.id)
          batch.set(docRef, checklistToCreateMap(userId, item))
        }
      }

      if (uDoc.collection("protocol_steps").limit(1).get().await().isEmpty) {
        SeedDefaults.initialProtocolSteps.forEach { step ->
          val docRef = uDoc.collection("protocol_steps").document(step.id)
          batch.set(docRef, protocolStepToCreateMap(userId, step))
        }
      }

      if (uDoc.collection("care_providers").limit(1).get().await().isEmpty) {
        SeedDefaults.initialProviders.forEach { provider ->
          val docRef = uDoc.collection("care_providers").document(provider.id)
          batch.set(docRef, providerToCreateMap(userId, provider))
        }
      }

      if (uDoc.collection("voice_memos").limit(1).get().await().isEmpty) {
        SeedDefaults.initialVoiceMemos.forEach { memo ->
          val docRef = uDoc.collection("voice_memos").document(memo.id)
          batch.set(docRef, voiceMemoToCreateMap(userId, memo))
        }
      }

      if (uDoc.collection("wallet_cards").limit(1).get().await().isEmpty) {
        SeedDefaults.initialWalletCards.forEach { card ->
          val docRef = uDoc.collection("wallet_cards").document(card.id)
          batch.set(docRef, walletCardToCreateMap(userId, card))
        }
      }

      if (uDoc.collection("med_reminders").limit(1).get().await().isEmpty) {
        HealthHubDefaults.initialMedReminders.forEach { reminder ->
          batch.set(uDoc.collection("med_reminders").document(reminder.id), medReminderToCreateMap(userId, reminder))
        }
      }

      if (uDoc.collection("preg_medications").limit(1).get().await().isEmpty) {
        HealthHubDefaults.initialPregnancyMeds.forEach { med ->
          batch.set(uDoc.collection("preg_medications").document(med.id), pregnancyMedToCreateMap(userId, med))
        }
      }

      if (uDoc.collection("nutrition_foods").limit(1).get().await().isEmpty) {
        HealthHubDefaults.initialNutritionFoods.forEach { food ->
          batch.set(uDoc.collection("nutrition_foods").document(food.id), nutritionFoodToCreateMap(userId, food))
        }
      }

      if (uDoc.collection("grocery_items").limit(1).get().await().isEmpty) {
        HealthHubDefaults.initialGroceryItems.forEach { item ->
          batch.set(uDoc.collection("grocery_items").document(item.id), groceryItemToCreateMap(userId, item))
        }
      }

      if (uDoc.collection("health_issues").limit(1).get().await().isEmpty) {
        HealthHubDefaults.initialHealthIssues.forEach { issue ->
          batch.set(uDoc.collection("health_issues").document(issue.id), healthIssueToCreateMap(userId, issue))
        }
      }

      if (uDoc.collection("expenses").limit(1).get().await().isEmpty) {
        HealthHubDefaults.initialExpenses.forEach { expense ->
          batch.set(uDoc.collection("expenses").document(expense.id), expenseToCreateMap(userId, expense))
        }
      }

      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, profileRef.path)
      throw e
    }
  }

  /**
   * Manually triggers a direct fetch from Cloud Firestore servers (bypassing cache via Source.SERVER)
   * across all user collections, returning a verified summary with server completion timestamp.
   */
  suspend fun forceFetchFromCloud(userId: String = requireUserId()): CloudSyncSummary {
    val uDoc = userDoc(userId)
    val profileRef = uDoc.collection("profile").document("main")
    return try {
      val initialProfileSnap = profileRef.get(Source.SERVER).await()
      if (!initialProfileSnap.exists()) {
        ensureInitialSeedData(userId)
      }

      coroutineScope {
        val profileDeferred = async { profileRef.get(Source.SERVER).await() }
        val tasksDeferred = async { uDoc.collection("tasks").get(Source.SERVER).await() }
        val notesDeferred = async { uDoc.collection("notes").get(Source.SERVER).await() }
        val appointmentsDeferred = async { uDoc.collection("appointments").get(Source.SERVER).await() }
        val documentsDeferred = async { uDoc.collection("documents").get(Source.SERVER).await() }
        val checklistsDeferred = async { uDoc.collection("checklists").get(Source.SERVER).await() }
        val protocolDeferred = async { uDoc.collection("protocol_steps").get(Source.SERVER).await() }
        val providersDeferred = async { uDoc.collection("care_providers").get(Source.SERVER).await() }
        val voiceMemosDeferred = async { uDoc.collection("voice_memos").get(Source.SERVER).await() }
        val walletCardsDeferred = async { uDoc.collection("wallet_cards").get(Source.SERVER).await() }
        val medRemindersDeferred = async { uDoc.collection("med_reminders").get(Source.SERVER).await() }
        val pregMedsDeferred = async { uDoc.collection("preg_medications").get(Source.SERVER).await() }
        val nutritionDeferred = async { uDoc.collection("nutrition_foods").get(Source.SERVER).await() }
        val groceryDeferred = async { uDoc.collection("grocery_items").get(Source.SERVER).await() }
        val healthIssuesDeferred = async { uDoc.collection("health_issues").get(Source.SERVER).await() }
        val expensesDeferred = async { uDoc.collection("expenses").get(Source.SERVER).await() }

        val pSnap = profileDeferred.await()
        val tSnap = tasksDeferred.await()
        val nSnap = notesDeferred.await()
        val aSnap = appointmentsDeferred.await()
        val dSnap = documentsDeferred.await()
        val cSnap = checklistsDeferred.await()
        val psSnap = protocolDeferred.await()
        val cpSnap = providersDeferred.await()
        val vmSnap = voiceMemosDeferred.await()
        val wcSnap = walletCardsDeferred.await()
        val mrSnap = medRemindersDeferred.await()
        val pmSnap = pregMedsDeferred.await()
        val nfSnap = nutritionDeferred.await()
        val grSnap = groceryDeferred.await()
        val hiSnap = healthIssuesDeferred.await()
        val expSnap = expensesDeferred.await()

        CloudSyncSummary(
          lastSyncedEpochMillis = System.currentTimeMillis(),
          isSyncing = false,
          syncError = null,
          lastSyncTrigger = "MANUAL",
          profileCount = if (pSnap.exists()) 1 else 0,
          tasksCount = tSnap.size(),
          notesCount = nSnap.size(),
          appointmentsCount = aSnap.size(),
          documentsCount = dSnap.size(),
          checklistsCount = cSnap.size(),
          protocolStepsCount = psSnap.size(),
          careProvidersCount = cpSnap.size(),
          voiceMemosCount = vmSnap.size(),
          walletCardsCount = wcSnap.size(),
          medRemindersCount = mrSnap.size(),
          healthHubRecordsCount = pmSnap.size() + nfSnap.size() + grSnap.size() + hiSnap.size() + expSnap.size()
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, profileRef.path)
      throw e
    }
  }

  // -----------------------------------------------------------------
  // 3. PROFILE & SETTINGS UPDATES
  // -----------------------------------------------------------------
  suspend fun updatePregnancyProfile(
    patientName: String,
    patientAge: String,
    currentWeek: Int,
    dueDate: String
  ) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    val updates = mapOf(
      "patientName" to patientName.take(100),
      "patientAge" to patientAge.take(20),
      "currentWeek" to currentWeek.toLong().coerceIn(1L, 45L),
      "dueDate" to dueDate.take(100),
      "daysRemaining" to ((40 - currentWeek).coerceAtLeast(0) * 7L).coerceIn(0L, 400L),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    executeUpdate(docRef.path) { docRef.update(updates).await() }
  }

  suspend fun updateCurrentWeek(week: Int) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "currentWeek" to week.toLong().coerceIn(1L, 45L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun updateDashboardProgress(
    dueDateText: String,
    gestationalWeeks: Int,
    gestationalDaysRemainder: Int,
    remainingCountdownDays: Int,
    currentTrimester: String,
    progressPercent: Float
  ) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    val updates = mapOf(
      "dashboardDueDateText" to dueDateText.take(100),
      "gestationalWeeks" to gestationalWeeks.toLong().coerceIn(1L, 45L),
      "gestationalDaysRemainder" to gestationalDaysRemainder.toLong().coerceIn(0L, 6L),
      "remainingCountdownDays" to remainingCountdownDays.toLong().coerceIn(0L, 400L),
      "currentTrimester" to currentTrimester.take(60),
      "progressPercent" to progressPercent.toDouble().coerceIn(0.0, 1.0),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    executeUpdate(docRef.path) { docRef.update(updates).await() }
  }

  suspend fun updateWaterGlasses(glasses: Int) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "waterGlasses" to glasses.toLong().coerceIn(0L, 50L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun updateHealthVitals(
    healthMood: String,
    healthWeight: String,
    waterGlasses: Int,
    bloodPressure: String,
    personalNotesText: String
  ) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    val updates = mapOf(
      "healthMood" to healthMood.take(100),
      "healthWeight" to healthWeight.take(30),
      "waterGlasses" to waterGlasses.toLong().coerceIn(0L, 50L),
      "bloodPressure" to bloodPressure.take(30),
      "personalNotesText" to personalNotesText.take(5000),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    executeUpdate(docRef.path) { docRef.update(updates).await() }
  }

  suspend fun updatePreferences(
    momAvatar: String,
    partnerAvatar: String,
    babyAvatar: String,
    languageCode: String,
    themePaletteName: String
  ) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    val updates = mapOf(
      "momAvatar" to momAvatar.take(20),
      "partnerAvatar" to partnerAvatar.take(20),
      "babyAvatar" to babyAvatar.take(20),
      "languageCode" to if (languageCode == "EN") "EN" else "FR",
      "themePaletteName" to themePaletteName.take(50),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    executeUpdate(docRef.path) { docRef.update(updates).await() }
  }

  suspend fun updatePartnerProfile(activePartner: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "activePartnerProfile" to if (activePartner == "Partner") "Partner" else "Fatou",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun updateCoupleAvatar(avatarBase64: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "coupleAvatarBase64" to avatarBase64.take(250000),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun updateWalletPin(newPin: String) {
    val uid = requireUserId()
    val cleanPin = newPin.filter { it.isDigit() }.take(4).padStart(4, '8')
    val docRef = userDoc(uid).collection("profile").document("main")
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "walletPin" to cleanPin,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun updateStageViewMode(mode: String, betaHcgConfirmed: Boolean? = null) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    val updates = mutableMapOf<String, Any>(
      "stageViewMode" to if (mode == "PREGNANCY") "PREGNANCY" else "IVF",
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (betaHcgConfirmed != null) {
      updates["isBetaHcgConfirmed"] = betaHcgConfirmed
    }
    executeUpdate(docRef.path) { docRef.update(updates).await() }
  }

  suspend fun updateAutoBackupEnabled(enabled: Boolean) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("profile").document("main")
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "autoCloudBackupEnabled" to enabled,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  // -----------------------------------------------------------------
  // 4. TASKS CRUD
  // -----------------------------------------------------------------
  suspend fun saveTask(task: TaskItem) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(task.id)
    val docRef = userDoc(uid).collection("tasks").document(docId)
    val payload = taskToCreateMap(uid, task)
    executeCreateOrUpdate(docRef.path) {
      val existing = docRef.get().await()
      if (existing.exists()) {
        val updatePayload = mutableMapOf<String, Any>(
          "title" to task.title.take(200),
          "date" to task.date.take(100),
          "category" to task.category.take(100),
          "details" to task.details.take(2000),
          "isCompleted" to task.isCompleted,
          "hasAlarm" to task.hasAlarm,
          "alarmTimeMinutesBefore" to task.alarmTimeMinutesBefore.toLong().coerceIn(0L, 10080L),
          "addedBy" to task.addedBy.take(40),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updatePayload).await()
      } else {
        docRef.set(payload).await()
      }
    }
  }

  suspend fun toggleTask(task: TaskItem) {
    val uid = requireUserId()
    require(task.id.isNotBlank()) { "Task ID must not be blank" }
    val docRef = userDoc(uid).collection("tasks").document(task.id)
    val updatedCompleted = !task.isCompleted
    executeUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists() && snap.get("createdAt") != null) {
        docRef.update(
          mapOf(
            "isCompleted" to updatedCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else if (!snap.exists()) {
        docRef.set(taskToCreateMap(uid, task.copy(isCompleted = updatedCompleted))).await()
      } else {
        // Legacy doc pre-existing without createdAt/updatedAt: recreate cleanly so future updates succeed
        val batch = db.batch()
        batch.delete(docRef)
        batch.set(docRef, taskToCreateMap(uid, task.copy(isCompleted = updatedCompleted)))
        batch.commit().await()
      }
    }
  }

  suspend fun deleteTask(taskId: String) {
    val uid = requireUserId()
    require(taskId.isNotBlank()) { "Task ID must not be blank" }
    val docRef = userDoc(uid).collection("tasks").document(taskId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  // -----------------------------------------------------------------
  // 5. NOTES CRUD (Daily Notes & Clinical Notepad)
  // -----------------------------------------------------------------
  suspend fun saveNote(note: SyncedNoteItem) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(note.id)
    val docRef = userDoc(uid).collection("notes").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "title" to note.title.take(200),
            "content" to note.content.take(5000),
            "category" to note.category.take(100),
            "timestampText" to note.timestampText.take(100),
            "mood" to note.mood.take(100),
            "symptoms" to note.symptoms.take(20),
            "addedBy" to note.addedBy.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(noteToCreateMap(uid, note)).await()
      }
    }
  }

  suspend fun deleteNote(noteId: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("notes").document(noteId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  // -----------------------------------------------------------------
  // 6. PRENATAL APPOINTMENTS & CALENDAR EVENTS CRUD
  // -----------------------------------------------------------------
  suspend fun saveAppointment(apt: SyncedAppointmentItem) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(apt.id)
    val docRef = userDoc(uid).collection("appointments").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "title" to apt.title.take(200),
            "dateStr" to apt.dateStr.take(100),
            "timeStr" to apt.timeStr.take(50),
            "doctor" to apt.doctor.take(150),
            "location" to apt.location.take(200),
            "clinicalNotes" to apt.clinicalNotes.take(2000),
            "eventType" to apt.eventType.take(50),
            "dayOfMonth" to apt.dayOfMonth.coerceIn(0L, 31L),
            "monthIndex" to apt.monthIndex.coerceIn(0L, 11L),
            "yearNum" to apt.yearNum.coerceIn(2020L, 2035L),
            "isCompleted" to apt.isCompleted,
            "hasAlarm" to apt.hasAlarm,
            "alarmOffsetMinutes" to apt.alarmOffsetMinutes.coerceIn(0L, 10080L),
            "addedBy" to apt.addedBy.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(appointmentToCreateMap(uid, apt)).await()
      }
    }
  }

  suspend fun toggleAppointment(aptId: String, currentCompleted: Boolean) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("appointments").document(aptId)
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "isCompleted" to !currentCompleted,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun deleteAppointment(aptId: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("appointments").document(aptId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  // -----------------------------------------------------------------
  // 7. MEDICAL DOCUMENTS CRUD
  // -----------------------------------------------------------------
  suspend fun saveDocument(doc: SyncedMedicalDocItem) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(doc.id)
    val docRef = userDoc(uid).collection("documents").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "title" to doc.title.take(200),
            "category" to doc.category.take(100),
            "dateStr" to doc.dateStr.take(100),
            "summary" to doc.summary.take(2000),
            "source" to doc.source.take(50),
            "addedBy" to doc.addedBy.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(documentToCreateMap(uid, doc)).await()
      }
    }
  }

  suspend fun deleteDocument(docId: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("documents").document(docId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  // -----------------------------------------------------------------
  // 8. CHECKLISTS CRUD
  // -----------------------------------------------------------------
  suspend fun saveChecklistItem(item: SyncedChecklistItem) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(item.id)
    val docRef = userDoc(uid).collection("checklists").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "listKey" to item.listKey.take(50),
            "label" to item.label.take(300),
            "isChecked" to item.isChecked,
            "sortOrder" to item.sortOrder.coerceIn(0L, 1000L),
            "addedBy" to item.addedBy.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(checklistToCreateMap(uid, item)).await()
      }
    }
  }

  suspend fun toggleChecklistItem(itemId: String, currentChecked: Boolean) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("checklists").document(itemId)
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "isChecked" to !currentChecked,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun deleteChecklistItem(itemId: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("checklists").document(itemId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  // -----------------------------------------------------------------
  // 9. PROTOCOL STEPS CRUD
  // -----------------------------------------------------------------
  suspend fun saveProtocolStep(step: SyncedProtocolStep) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(step.id)
    val docRef = userDoc(uid).collection("protocol_steps").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "phaseId" to step.phaseId.coerceIn(1L, 4L),
            "dateStr" to step.dateStr.take(100),
            "stepName" to step.stepName.take(200),
            "medication" to step.medication.take(150),
            "dosage" to step.dosage.take(100),
            "status" to step.status.take(50),
            "details" to step.details.take(2000),
            "isCompleted" to step.isCompleted,
            "isCriticalWindow" to step.isCriticalWindow,
            "targetEpochMillis" to step.targetEpochMillis.coerceAtLeast(0L),
            "sortOrder" to step.sortOrder.coerceIn(0L, 1000L),
            "hasAlarm" to step.hasAlarm,
            "alarmOffsetMinutes" to step.alarmOffsetMinutes.coerceIn(0L, 10080L),
            "addedBy" to step.addedBy.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(protocolStepToCreateMap(uid, step)).await()
      }
    }
  }

  suspend fun toggleProtocolStep(step: SyncedProtocolStep) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("protocol_steps").document(step.id)
    val nextCompleted = !step.isCompleted
    val nextStatus = if (nextCompleted) "Complété" else "En cours"
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "isCompleted" to nextCompleted,
          "status" to nextStatus,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun deleteProtocolStep(stepId: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("protocol_steps").document(stepId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  /**
   * Restores Section 1 (Tableau de Bord Immédiat) and Section 2 (Chronologie Complète)
   * back to the default RMA East Side medical protocol & pregnancy timeline.
   */
  suspend fun resetProtocolToDefault(userId: String = requireUserId()) {
    val uDoc = userDoc(userId)
    val stepsCol = uDoc.collection("protocol_steps")
    val tasksCol = uDoc.collection("tasks")
    executeCreateOrUpdate(stepsCol.path) {
      val existingSteps = stepsCol.get().await()
      if (!existingSteps.isEmpty) {
        val deleteStepsBatch = db.batch()
        existingSteps.documents.forEach { deleteStepsBatch.delete(it.reference) }
        deleteStepsBatch.commit().await()
      }
      val seedStepsBatch = db.batch()
      SeedDefaults.initialProtocolSteps.forEach { step ->
        seedStepsBatch.set(stepsCol.document(step.id), protocolStepToCreateMap(userId, step))
      }
      seedStepsBatch.commit().await()

      val existingTasks = tasksCol.get().await()
      if (!existingTasks.isEmpty) {
        val deleteTasksBatch = db.batch()
        existingTasks.documents.forEach { deleteTasksBatch.delete(it.reference) }
        deleteTasksBatch.commit().await()
      }
      val seedTasksBatch = db.batch()
      PmaRepository.immediateTasks.forEach { task ->
        seedTasksBatch.set(tasksCol.document(task.id), taskToCreateMap(userId, task))
      }
      seedTasksBatch.commit().await()
    }
  }

  // -----------------------------------------------------------------
  // 10. CARE PROVIDERS & VOICE MEMOS CRUD
  // -----------------------------------------------------------------
  suspend fun saveCareProvider(provider: SyncedCareProvider) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(provider.id)
    val docRef = userDoc(uid).collection("care_providers").document(docId)
    executeCreateOrUpdate(docRef.path) {
      docRef.set(providerToCreateMap(uid, provider)).await()
    }
  }

  suspend fun deleteCareProvider(providerId: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("care_providers").document(providerId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun saveVoiceMemo(memo: SyncedVoiceMemo) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(memo.id)
    val docRef = userDoc(uid).collection("voice_memos").document(docId)
    executeCreateOrUpdate(docRef.path) {
      docRef.set(voiceMemoToCreateMap(uid, memo)).await()
    }
  }

  suspend fun deleteVoiceMemo(memoId: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("voice_memos").document(memoId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  // -----------------------------------------------------------------
  // 11. DIGITAL WALLET CARDS CRUD (Offline-Queued & Auto-Synced)
  // -----------------------------------------------------------------
  suspend fun saveWalletCard(card: SyncedWalletCard) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(card.id)
    val docRef = userDoc(uid).collection("wallet_cards").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val cachedSnap = runCatching { docRef.get(Source.CACHE).await() }.getOrNull()
      val existsInCacheWithTimestamp = cachedSnap?.exists() == true && cachedSnap.get("createdAt") != null
      if (card.createdAt != null || existsInCacheWithTimestamp) {
        docRef.update(
          mapOf(
            "cardTitle" to card.cardTitle.ifBlank { "Carte" }.take(120),
            "cardCategory" to card.cardCategory.ifBlank { "Assurance" }.take(60),
            "cardNumber" to card.cardNumber.take(80),
            "holderName" to card.holderName.take(100),
            "expiryDate" to card.expiryDate.take(30),
            "notes" to card.notes.take(500),
            "gradientKey" to normalizeGradientKey(card.gradientKey),
            "frontImageBase64" to card.frontImageBase64.take(420000),
            "backImageBase64" to card.backImageBase64.take(420000),
            "sortOrder" to card.sortOrder.coerceIn(0L, 1000L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(walletCardToCreateMap(uid, card)).await()
      }
    }
  }

  suspend fun deleteWalletCard(cardId: String) {
    val uid = requireUserId()
    require(cardId.isNotBlank()) { "Wallet card ID must not be blank" }
    val docRef = userDoc(uid).collection("wallet_cards").document(cardId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun seedSampleWalletCards(userId: String = requireUserId()) {
    val uDoc = userDoc(userId)
    val col = uDoc.collection("wallet_cards")
    executeCreateOrUpdate(col.path) {
      if (col.limit(1).get().await().isEmpty) {
        val batch = db.batch()
        SeedDefaults.initialWalletCards.forEach { card ->
          batch.set(col.document(card.id), walletCardToCreateMap(userId, card))
        }
        batch.commit().await()
      }
    }
  }

  // -----------------------------------------------------------------
  // 12. MEDICATION REMINDERS & HEALTH HUB CRUD
  // -----------------------------------------------------------------
  suspend fun saveMedReminder(reminder: SyncedMedReminder) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(reminder.id)
    val docRef = userDoc(uid).collection("med_reminders").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "title" to reminder.title.ifBlank { "Rappel Médicament" }.take(150),
            "medication" to reminder.medication.take(150),
            "dosage" to reminder.dosage.take(100),
            "hour" to reminder.hour.coerceIn(0L, 23L),
            "minute" to reminder.minute.coerceIn(0L, 59L),
            "repeatDaily" to reminder.repeatDaily,
            "isEnabled" to reminder.isEnabled,
            "protocolPhase" to reminder.protocolPhase.take(100),
            "notes" to reminder.notes.take(500),
            "pillsRemaining" to reminder.pillsRemaining.coerceIn(0L, 999L),
            "dailyAmTaken" to reminder.dailyAmTaken,
            "dailyPmTaken" to reminder.dailyPmTaken,
            "lastDoseDateStr" to reminder.lastDoseDateStr.take(40),
            "soundEnabled" to reminder.soundEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(medReminderToCreateMap(uid, reminder)).await()
      }
    }
  }

  suspend fun toggleMedDose(reminder: SyncedMedReminder, period: String, todayKey: String) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("med_reminders").document(reminder.id)
    val resetToday = reminder.lastDoseDateStr != todayKey
    val currentAm = if (resetToday) false else reminder.dailyAmTaken
    val currentPm = if (resetToday) false else reminder.dailyPmTaken
    val nextAm = if (period == "AM") !currentAm else currentAm
    val nextPm = if (period == "PM") !currentPm else currentPm
    val wasTaken = if (period == "AM") currentAm else currentPm
    val nextPills = if (!wasTaken) (reminder.pillsRemaining - 1L).coerceAtLeast(0L) else (reminder.pillsRemaining + 1L).coerceAtMost(999L)
    executeUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "dailyAmTaken" to nextAm,
            "dailyPmTaken" to nextPm,
            "lastDoseDateStr" to todayKey,
            "pillsRemaining" to nextPills,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(
          medReminderToCreateMap(
            uid,
            reminder.copy(
              dailyAmTaken = nextAm,
              dailyPmTaken = nextPm,
              lastDoseDateStr = todayKey,
              pillsRemaining = nextPills
            )
          )
        ).await()
      }
    }
  }

  suspend fun refillMedPills(reminderId: String, newCount: Long) {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("med_reminders").document(reminderId)
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "pillsRemaining" to newCount.coerceIn(0L, 999L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun toggleMedReminder(reminderId: String, currentEnabled: Boolean) {
    val uid = requireUserId()
    require(reminderId.isNotBlank()) { "Reminder ID must not be blank" }
    val docRef = userDoc(uid).collection("med_reminders").document(reminderId)
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "isEnabled" to !currentEnabled,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun deleteMedReminder(reminderId: String) {
    val uid = requireUserId()
    require(reminderId.isNotBlank()) { "Reminder ID must not be blank" }
    val docRef = userDoc(uid).collection("med_reminders").document(reminderId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun savePregnancyMed(med: SyncedPregnancyMed) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(med.id)
    val docRef = userDoc(uid).collection("preg_medications").document(docId)
    val safeLevel = normalizeMedSafetyLevel(med.safetyLevel)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "name" to med.name.ifBlank { "Médicament" }.take(150),
            "activeIngredient" to med.activeIngredient.take(150),
            "category" to med.category.take(100),
            "safetyLevel" to safeLevel,
            "trimester1" to med.trimester1.take(100),
            "trimester2" to med.trimester2.take(100),
            "trimester3" to med.trimester3.take(100),
            "clinicalNotes" to med.clinicalNotes.take(2000),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(pregnancyMedToCreateMap(uid, med)).await()
      }
    }
  }

  suspend fun deletePregnancyMed(medId: String) {
    val uid = requireUserId()
    require(medId.isNotBlank()) { "Pregnancy med ID must not be blank" }
    val docRef = userDoc(uid).collection("preg_medications").document(medId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun saveNutritionFood(food: SyncedNutritionFood) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(food.id)
    val docRef = userDoc(uid).collection("nutrition_foods").document(docId)
    val status = normalizeFoodSafetyStatus(food.safetyStatus)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "name" to food.name.ifBlank { "Aliment" }.take(150),
            "category" to food.category.take(100),
            "safetyStatus" to status,
            "highlights" to food.highlights.take(1000),
            "nutrients" to food.nutrients.take(300),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(nutritionFoodToCreateMap(uid, food)).await()
      }
    }
  }

  suspend fun deleteNutritionFood(foodId: String) {
    val uid = requireUserId()
    require(foodId.isNotBlank()) { "Nutrition food ID must not be blank" }
    val docRef = userDoc(uid).collection("nutrition_foods").document(foodId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun saveGroceryItem(item: SyncedGroceryItem) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(item.id)
    val docRef = userDoc(uid).collection("grocery_items").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "label" to item.label.ifBlank { "Article" }.take(200),
            "category" to item.category.take(100),
            "quantity" to item.quantity.take(80),
            "isChecked" to item.isChecked,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(groceryItemToCreateMap(uid, item)).await()
      }
    }
  }

  suspend fun toggleGroceryItem(itemId: String, currentChecked: Boolean) {
    val uid = requireUserId()
    require(itemId.isNotBlank()) { "Grocery item ID must not be blank" }
    val docRef = userDoc(uid).collection("grocery_items").document(itemId)
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "isChecked" to !currentChecked,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun deleteGroceryItem(itemId: String) {
    val uid = requireUserId()
    require(itemId.isNotBlank()) { "Grocery item ID must not be blank" }
    val docRef = userDoc(uid).collection("grocery_items").document(itemId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun saveHealthIssue(issue: SyncedHealthIssue) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(issue.id)
    val docRef = userDoc(uid).collection("health_issues").document(docId)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "title" to issue.title.ifBlank { "Suivi Santé" }.take(200),
            "trimester" to issue.trimester.take(50),
            "severity" to issue.severity.take(50),
            "dateStr" to issue.dateStr.take(80),
            "symptoms" to issue.symptoms.take(1500),
            "doctorNotes" to issue.doctorNotes.take(2000),
            "prescribedTreatment" to issue.prescribedTreatment.take(1000),
            "isResolved" to issue.isResolved,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(healthIssueToCreateMap(uid, issue)).await()
      }
    }
  }

  suspend fun toggleHealthIssue(issueId: String, currentResolved: Boolean) {
    val uid = requireUserId()
    require(issueId.isNotBlank()) { "Health issue ID must not be blank" }
    val docRef = userDoc(uid).collection("health_issues").document(issueId)
    executeUpdate(docRef.path) {
      docRef.update(
        mapOf(
          "isResolved" to !currentResolved,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun deleteHealthIssue(issueId: String) {
    val uid = requireUserId()
    require(issueId.isNotBlank()) { "Health issue ID must not be blank" }
    val docRef = userDoc(uid).collection("health_issues").document(issueId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun saveExpense(expense: SyncedExpenseRecord) {
    val uid = requireUserId()
    val docId = sanitizeOrNewId(expense.id)
    val docRef = userDoc(uid).collection("expenses").document(docId)
    val validCurrency = normalizeCurrency(expense.currency)
    executeCreateOrUpdate(docRef.path) {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef.update(
          mapOf(
            "title" to expense.title.ifBlank { "Dépense" }.take(200),
            "amount" to expense.amount.coerceIn(0.0, 1_000_000_000.0),
            "currency" to validCurrency,
            "category" to expense.category.take(100),
            "phase" to expense.phase.take(60),
            "paymentStatus" to expense.paymentStatus.take(60),
            "dateStr" to expense.dateStr.take(80),
            "notes" to expense.notes.take(1000),
            "receiptBase64" to expense.receiptBase64.take(250000),
            "attachmentTitle" to expense.attachmentTitle.take(120),
            "addedBy" to expense.addedBy.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(expenseToCreateMap(uid, expense)).await()
      }
    }
  }

  suspend fun deleteExpense(expenseId: String) {
    val uid = requireUserId()
    require(expenseId.isNotBlank()) { "Expense ID must not be blank" }
    val docRef = userDoc(uid).collection("expenses").document(expenseId)
    executeDelete(docRef.path) { docRef.delete().await() }
  }

  suspend fun createBackupSnapshot(
    triggerType: String,
    addedBy: String,
    recordCount: Long,
    payloadJson: String
  ) {
    val uid = requireUserId()
    val now = System.currentTimeMillis()
    val docId = "bkp_$now"
    val docRef = userDoc(uid).collection("backups").document(docId)
    executeCreateOrUpdate(docRef.path) {
      docRef.set(
        mapOf(
          "userId" to uid,
          "timestampEpochMillis" to now,
          "triggerType" to triggerType.take(20),
          "addedBy" to addedBy.take(40),
          "recordCount" to recordCount.coerceAtLeast(0L),
          "payloadJson" to payloadJson.take(750000),
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      userDoc(uid).collection("profile").document("main").update(
        mapOf(
          "lastAutoBackupEpochMillis" to now,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun restoreBackupFromJson(payloadJson: String) {
    val uid = requireUserId()
    val uDoc = userDoc(uid)
    val root = org.json.JSONObject(payloadJson)
    executeCreateOrUpdate("users/$uid/restore") {
      root.optJSONObject("profile")?.let { p ->
        val updates = mutableMapOf<String, Any>(
          "updatedAt" to FieldValue.serverTimestamp()
        )
        if (p.has("patientName")) updates["patientName"] = p.optString("patientName", "Fatou")
        if (p.has("dueDate")) updates["dueDate"] = p.optString("dueDate")
        if (p.has("gestationalWeeks")) updates["gestationalWeeks"] = p.optLong("gestationalWeeks", 16L)
        if (p.has("gestationalDaysRemainder")) updates["gestationalDaysRemainder"] = p.optLong("gestationalDaysRemainder", 4L)
        if (p.has("languageCode")) updates["languageCode"] = p.optString("languageCode", "FR")
        if (p.has("themePaletteName")) updates["themePaletteName"] = p.optString("themePaletteName", "CARAMEL")
        if (p.has("activePartnerProfile")) updates["activePartnerProfile"] = p.optString("activePartnerProfile", "Fatou")
        if (p.has("walletPin")) updates["walletPin"] = p.optString("walletPin", "8888")
        if (p.has("isBetaHcgConfirmed")) updates["isBetaHcgConfirmed"] = p.optBoolean("isBetaHcgConfirmed", false)
        if (p.has("stageViewMode")) updates["stageViewMode"] = p.optString("stageViewMode", "IVF")
        uDoc.collection("profile").document("main").update(updates).await()
      }

      suspend fun restoreArray(arrName: String, buildWrite: (org.json.JSONObject, com.google.firebase.firestore.WriteBatch) -> Unit) {
        val arr = root.optJSONArray(arrName) ?: return
        var batch = db.batch()
        var ops = 0
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          buildWrite(obj, batch)
          ops++
          if (ops >= 400) {
            batch.commit().await()
            batch = db.batch()
            ops = 0
          }
        }
        if (ops > 0) batch.commit().await()
      }

      restoreArray("expenses") { obj, batch ->
        val id = sanitizeOrNewId(obj.optString("id"))
        val rec = SyncedExpenseRecord(
          id = id,
          title = obj.optString("title"),
          amount = obj.optDouble("amount", 0.0),
          currency = obj.optString("currency", "USD"),
          category = obj.optString("category", "Others"),
          phase = obj.optString("phase", "IVF / Clinic"),
          paymentStatus = obj.optString("paymentStatus", "Out-of-Pocket"),
          dateStr = obj.optString("dateStr"),
          notes = obj.optString("notes"),
          addedBy = obj.optString("addedBy", "Fatou")
        )
        batch.set(uDoc.collection("expenses").document(id), expenseToCreateMap(uid, rec))
      }

      restoreArray("protocolSteps") { obj, batch ->
        val id = sanitizeOrNewId(obj.optString("id"))
        val step = SyncedProtocolStep(
          id = id,
          phaseId = obj.optLong("phaseId", 2L),
          dateStr = obj.optString("dateStr"),
          stepName = obj.optString("stepName"),
          medication = obj.optString("medication"),
          dosage = obj.optString("dosage"),
          status = obj.optString("status"),
          details = obj.optString("details"),
          isCompleted = obj.optBoolean("isCompleted", false),
          isCriticalWindow = obj.optBoolean("isCriticalWindow", false),
          targetEpochMillis = obj.optLong("targetEpochMillis", 0L),
          sortOrder = obj.optLong("sortOrder", 0L),
          addedBy = obj.optString("addedBy", "Fatou")
        )
        batch.set(uDoc.collection("protocol_steps").document(id), protocolStepToCreateMap(uid, step))
      }

      restoreArray("appointments") { obj, batch ->
        val id = sanitizeOrNewId(obj.optString("id"))
        val apt = SyncedAppointmentItem(
          id = id,
          title = obj.optString("title"),
          dateStr = obj.optString("dateStr"),
          timeStr = obj.optString("timeStr"),
          doctor = obj.optString("doctor"),
          location = obj.optString("location"),
          clinicalNotes = obj.optString("clinicalNotes"),
          eventType = obj.optString("eventType", "Rendez-vous"),
          dayOfMonth = obj.optLong("dayOfMonth", 13L),
          monthIndex = obj.optLong("monthIndex", 10L),
          yearNum = obj.optLong("yearNum", 2026L),
          isCompleted = obj.optBoolean("isCompleted", false),
          hasAlarm = obj.optBoolean("hasAlarm", false),
          alarmOffsetMinutes = obj.optLong("alarmOffsetMinutes", 15L),
          addedBy = obj.optString("addedBy", "Fatou")
        )
        batch.set(uDoc.collection("appointments").document(id), appointmentToCreateMap(uid, apt))
      }

      restoreArray("tasks") { obj, batch ->
        val id = sanitizeOrNewId(obj.optString("id"))
        val task = TaskItem(
          id = id,
          title = obj.optString("title"),
          date = obj.optString("date"),
          category = obj.optString("category"),
          details = obj.optString("details"),
          isCompleted = obj.optBoolean("isCompleted", false),
          hasAlarm = obj.optBoolean("hasAlarm", false),
          alarmTimeMinutesBefore = obj.optInt("alarmTimeMinutesBefore", 15),
          addedBy = obj.optString("addedBy", "Fatou")
        )
        batch.set(uDoc.collection("tasks").document(id), taskToCreateMap(uid, task))
      }

      restoreArray("notes") { obj, batch ->
        val id = sanitizeOrNewId(obj.optString("id"))
        val note = SyncedNoteItem(
          id = id,
          title = obj.optString("title"),
          content = obj.optString("content"),
          category = obj.optString("category", "clinical"),
          timestampText = obj.optString("timestampText"),
          mood = obj.optString("mood"),
          addedBy = obj.optString("addedBy", "Fatou")
        )
        batch.set(uDoc.collection("notes").document(id), noteToCreateMap(uid, note))
      }

      restoreArray("checklists") { obj, batch ->
        val id = sanitizeOrNewId(obj.optString("id"))
        val item = SyncedChecklistItem(
          id = id,
          listKey = obj.optString("listKey", "mom_bag"),
          label = obj.optString("label"),
          isChecked = obj.optBoolean("isChecked", false),
          sortOrder = obj.optLong("sortOrder", 0L),
          addedBy = obj.optString("addedBy", "Fatou")
        )
        batch.set(uDoc.collection("checklists").document(id), checklistToCreateMap(uid, item))
      }
    }
  }

  // -----------------------------------------------------------------
  // HELPERS & PAYLOAD MAPPERS
  // -----------------------------------------------------------------
  private suspend fun executeCreateOrUpdate(path: String, block: suspend () -> Unit) {
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      throw e
    }
  }

  private suspend fun executeUpdate(path: String, block: suspend () -> Unit) {
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      throw e
    }
  }

  private suspend fun executeDelete(path: String, block: suspend () -> Unit) {
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      throw e
    }
  }

  private fun sanitizeOrNewId(rawId: String): String {
    val trimmed = rawId.trim()
    return if (trimmed.isNotEmpty() && ID_REGEX.matches(trimmed)) trimmed
    else UUID.randomUUID().toString()
  }

  private fun taskToCreateMap(uid: String, task: TaskItem): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to task.title.take(200),
    "date" to task.date.take(100),
    "category" to task.category.take(100),
    "details" to task.details.take(2000),
    "isCompleted" to task.isCompleted,
    "hasAlarm" to task.hasAlarm,
    "alarmTimeMinutesBefore" to task.alarmTimeMinutesBefore.toLong().coerceIn(0L, 10080L),
    "addedBy" to task.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun noteToCreateMap(uid: String, note: SyncedNoteItem): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to note.title.take(200),
    "content" to note.content.take(5000),
    "category" to note.category.take(100),
    "timestampText" to note.timestampText.take(100),
    "mood" to note.mood.take(100),
    "symptoms" to note.symptoms.take(20),
    "addedBy" to note.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun appointmentToCreateMap(uid: String, apt: SyncedAppointmentItem): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to apt.title.take(200),
    "dateStr" to apt.dateStr.take(100),
    "timeStr" to apt.timeStr.take(50),
    "doctor" to apt.doctor.take(150),
    "location" to apt.location.take(200),
    "clinicalNotes" to apt.clinicalNotes.take(2000),
    "eventType" to apt.eventType.take(50),
    "dayOfMonth" to apt.dayOfMonth.coerceIn(0L, 31L),
    "monthIndex" to apt.monthIndex.coerceIn(0L, 11L),
    "yearNum" to apt.yearNum.coerceIn(2020L, 2035L),
    "isCompleted" to apt.isCompleted,
    "hasAlarm" to apt.hasAlarm,
    "alarmOffsetMinutes" to apt.alarmOffsetMinutes.coerceIn(0L, 10080L),
    "addedBy" to apt.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun documentToCreateMap(uid: String, doc: SyncedMedicalDocItem): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to doc.title.take(200),
    "category" to doc.category.take(100),
    "dateStr" to doc.dateStr.take(100),
    "summary" to doc.summary.take(2000),
    "source" to doc.source.take(50),
    "addedBy" to doc.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun checklistToCreateMap(uid: String, item: SyncedChecklistItem): Map<String, Any> = mapOf(
    "userId" to uid,
    "listKey" to item.listKey.take(50),
    "label" to item.label.take(300),
    "isChecked" to item.isChecked,
    "sortOrder" to item.sortOrder.coerceIn(0L, 1000L),
    "addedBy" to item.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun protocolStepToCreateMap(uid: String, step: SyncedProtocolStep): Map<String, Any> = mapOf(
    "userId" to uid,
    "phaseId" to step.phaseId.coerceIn(1L, 4L),
    "dateStr" to step.dateStr.take(100),
    "stepName" to step.stepName.take(200),
    "medication" to step.medication.take(150),
    "dosage" to step.dosage.take(100),
    "status" to step.status.take(50),
    "details" to step.details.take(2000),
    "isCompleted" to step.isCompleted,
    "isCriticalWindow" to step.isCriticalWindow,
    "targetEpochMillis" to step.targetEpochMillis.coerceAtLeast(0L),
    "sortOrder" to step.sortOrder.coerceIn(0L, 1000L),
    "hasAlarm" to step.hasAlarm,
    "alarmOffsetMinutes" to step.alarmOffsetMinutes.coerceIn(0L, 10080L),
    "addedBy" to step.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun providerToCreateMap(uid: String, provider: SyncedCareProvider): Map<String, Any> = mapOf(
    "userId" to uid,
    "providerType" to provider.providerType.take(60),
    "name" to provider.name.take(150),
    "affiliation" to provider.affiliation.take(200),
    "phone" to provider.phone.take(50),
    "emergencyContact" to provider.emergencyContact.take(50),
    "address" to provider.address.take(250),
    "portalLink" to provider.portalLink.take(300),
    "email" to provider.email.take(150),
    "photoUri" to provider.photoUri.take(500),
    "notes" to provider.notes.take(500),
    "isEmergency" to provider.isEmergency,
    "category" to provider.category.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun voiceMemoToCreateMap(uid: String, memo: SyncedVoiceMemo): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to memo.title.take(200),
    "durationSec" to memo.durationSec.coerceIn(0L, 300L),
    "timestampText" to memo.timestampText.take(100),
    "audioBase64" to memo.audioBase64.take(280000),
    "addedBy" to memo.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun normalizeGradientKey(raw: String?): String = when (raw) {
    "teal", "sunset_orange", "magenta_blue", "deep_purple" -> raw
    else -> "teal"
  }

  private fun walletCardToCreateMap(uid: String, card: SyncedWalletCard): Map<String, Any> = mapOf(
    "userId" to uid,
    "cardTitle" to card.cardTitle.ifBlank { "Carte" }.take(120),
    "cardCategory" to card.cardCategory.ifBlank { "Assurance" }.take(60),
    "cardNumber" to card.cardNumber.take(80),
    "holderName" to card.holderName.take(100),
    "expiryDate" to card.expiryDate.take(30),
    "notes" to card.notes.take(500),
    "gradientKey" to normalizeGradientKey(card.gradientKey),
    "frontImageBase64" to card.frontImageBase64.take(420000),
    "backImageBase64" to card.backImageBase64.take(420000),
    "sortOrder" to card.sortOrder.coerceIn(0L, 1000L),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun normalizeMedSafetyLevel(raw: String): String = when (raw.uppercase()) {
    "SAFE", "CAUTION", "AVOID" -> raw.uppercase()
    else -> "CAUTION"
  }

  private fun normalizeFoodSafetyStatus(raw: String): String = when (raw.uppercase()) {
    "EAT", "LIMIT", "AVOID" -> raw.uppercase()
    else -> "LIMIT"
  }

  private fun normalizeCurrency(raw: String): String = when (raw.uppercase()) {
    "USD", "EUR", "XOF" -> raw.uppercase()
    else -> "USD"
  }

  private fun medReminderToCreateMap(uid: String, reminder: SyncedMedReminder): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to reminder.title.ifBlank { "Rappel Médicament" }.take(150),
    "medication" to reminder.medication.take(150),
    "dosage" to reminder.dosage.take(100),
    "hour" to reminder.hour.coerceIn(0L, 23L),
    "minute" to reminder.minute.coerceIn(0L, 59L),
    "repeatDaily" to reminder.repeatDaily,
    "isEnabled" to reminder.isEnabled,
    "protocolPhase" to reminder.protocolPhase.take(100),
    "notes" to reminder.notes.take(500),
    "pillsRemaining" to reminder.pillsRemaining.coerceIn(0L, 999L),
    "dailyAmTaken" to reminder.dailyAmTaken,
    "dailyPmTaken" to reminder.dailyPmTaken,
    "lastDoseDateStr" to reminder.lastDoseDateStr.take(40),
    "soundEnabled" to reminder.soundEnabled,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun pregnancyMedToCreateMap(uid: String, med: SyncedPregnancyMed): Map<String, Any> = mapOf(
    "userId" to uid,
    "name" to med.name.ifBlank { "Médicament" }.take(150),
    "activeIngredient" to med.activeIngredient.take(150),
    "category" to med.category.take(100),
    "safetyLevel" to normalizeMedSafetyLevel(med.safetyLevel),
    "trimester1" to med.trimester1.take(100),
    "trimester2" to med.trimester2.take(100),
    "trimester3" to med.trimester3.take(100),
    "clinicalNotes" to med.clinicalNotes.take(2000),
    "addedBy" to med.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun nutritionFoodToCreateMap(uid: String, food: SyncedNutritionFood): Map<String, Any> = mapOf(
    "userId" to uid,
    "name" to food.name.ifBlank { "Aliment" }.take(150),
    "category" to food.category.take(100),
    "safetyStatus" to normalizeFoodSafetyStatus(food.safetyStatus),
    "highlights" to food.highlights.take(1000),
    "nutrients" to food.nutrients.take(300),
    "addedBy" to food.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun groceryItemToCreateMap(uid: String, item: SyncedGroceryItem): Map<String, Any> = mapOf(
    "userId" to uid,
    "label" to item.label.ifBlank { "Article" }.take(200),
    "category" to item.category.take(100),
    "quantity" to item.quantity.take(80),
    "isChecked" to item.isChecked,
    "addedBy" to item.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun healthIssueToCreateMap(uid: String, issue: SyncedHealthIssue): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to issue.title.ifBlank { "Suivi Santé" }.take(200),
    "trimester" to issue.trimester.take(50),
    "severity" to issue.severity.take(50),
    "dateStr" to issue.dateStr.take(80),
    "symptoms" to issue.symptoms.take(1500),
    "doctorNotes" to issue.doctorNotes.take(2000),
    "prescribedTreatment" to issue.prescribedTreatment.take(1000),
    "isResolved" to issue.isResolved,
    "addedBy" to issue.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun expenseToCreateMap(uid: String, expense: SyncedExpenseRecord): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to expense.title.ifBlank { "Dépense" }.take(200),
    "amount" to expense.amount.coerceIn(0.0, 1_000_000_000.0),
    "insurancePaid" to expense.insurancePaid.coerceIn(0.0, 1_000_000_000.0),
    "totalBill" to expense.totalBill.coerceIn(0.0, 1_000_000_000.0),
    "currency" to normalizeCurrency(expense.currency),
    "category" to expense.category.take(100),
    "phase" to expense.phase.take(60),
    "paymentStatus" to expense.paymentStatus.take(60),
    "dateStr" to expense.dateStr.take(80),
    "notes" to expense.notes.take(1000),
    "receiptBase64" to expense.receiptBase64.take(250000),
    "attachmentTitle" to expense.attachmentTitle.take(120),
    "addedBy" to expense.addedBy.take(40),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private companion object {
    const val TAG = "FirestoreRepository"
    val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
  }
}

object SeedDefaults {
  val initialNotes = listOf(
    SyncedNoteItem(
      id = "cn_1",
      title = "Rappel Protocole RMA Dr. Brauer",
      content = "Estrace 2 mg PO 2 fois par jour (08h00 et 20h00). Bien s'hydrater (2L eau/jour). Prochaine prise de sang LH/E2 le 10 nov. 2026.",
      category = "clinical",
      timestampText = "06 nov. 2026 à 10:15"
    ),
    SyncedNoteItem(
      id = "cn_2",
      title = "Symptômes & Conseils Nutrition",
      content = "Prendre les vitamines prénatales avec le repas du midi. Éviter thé/café 1h avant/après le fer.",
      category = "clinical",
      timestampText = "05 nov. 2026 à 18:00"
    )
  )

  val initialVoiceMemos = listOf(
    SyncedVoiceMemo(id = "vm_1", title = "Questions Dr. Brauer (Écho T1)", durationSec = 24L, timestampText = "06 nov. 2026 à 09:15"),
    SyncedVoiceMemo(id = "vm_2", title = "Ressenti prise Estrace AM", durationSec = 18L, timestampText = "05 nov. 2026 à 20:30")
  )

  val initialAppointments = listOf(
    SyncedAppointmentItem(id = "apt_nov05", title = "Monitorages & Bilans (Cycle Start)", dateStr = "05 Nov 2026", timeStr = "08:00", doctor = "Dr. Anate Brauer", location = "Cabinet East Side (635 Madison Ave)", clinicalNotes = "Monitorage initial et début Estrace 2 mg PO AM & PM (2x/j).", eventType = "Rendez-vous", dayOfMonth = 5L, monthIndex = 10L, yearNum = 2026L),
    SyncedAppointmentItem(id = "apt_nov12", title = "Monitorage de contrôle endomètre", dateStr = "12 Nov 2026", timeStr = "08:30", doctor = "Dr. Anate Brauer", location = "Cabinet East Side", clinicalNotes = "Échographie endomètre & bilan hormonal complétés.", eventType = "Rendez-vous", dayOfMonth = 12L, monthIndex = 10L, yearNum = 2026L),
    SyncedAppointmentItem(id = "apt_nov17", title = "Monitoring pré-transfert", dateStr = "17 Nov 2026", timeStr = "09:30", doctor = "Dr. Anate Brauer", location = "Cabinet East Side", clinicalNotes = "Contrôle endomètre ≥ 7-8 mm & bilan hormonal complétés. Poursuite Estrace 2 mg TID + PIO 1 mL IM.", eventType = "Rendez-vous", dayOfMonth = 17L, monthIndex = 10L, yearNum = 2026L),
    SyncedAppointmentItem(id = "apt_nov18", title = "🎯 Transfert d'Embryon Congelé (TEC 🥳🎉)", dateStr = "18 Nov 2026", timeStr = "14:00", doctor = "Dr. Anate Brauer / Alex", location = "Cabinet East Side / RMA Bloc", clinicalNotes = "Transfert d'Embryon Congelé (TEC) à 14h00 (02:00 PM EST) du blastocyste euploïde masculin. Vessie pleine, sans parfum.", eventType = "Rendez-vous", dayOfMonth = 18L, monthIndex = 10L, yearNum = 2026L),
    SyncedAppointmentItem(id = "apt_nov20", title = "🩸 Progesterone Check (J2 post-TEC)", dateStr = "20 Nov 2026", timeStr = "09:30", doctor = "Laboratoire RMA", location = "Cabinet East Side", clinicalNotes = "Prise de sang de contrôle @ 9:30 AM (poursuite Estrace 2 mg TID + PIO 1.5 mL IM Daily).", eventType = "Rendez-vous", dayOfMonth = 20L, monthIndex = 10L, yearNum = 2026L),
    SyncedAppointmentItem(id = "apt_nov27", title = "🩸 Prise de sang Bêta-hCG sérique #1 🤞", dateStr = "27 Nov 2026", timeStr = "09:30", doctor = "Laboratoire RMA", location = "Cabinet East Side", clinicalNotes = "Confirmation biologique de nidation @ 9:30 AM 🤞 (déblocage Maternité 2027).", eventType = "Rendez-vous", dayOfMonth = 27L, monthIndex = 10L, yearNum = 2026L),
    SyncedAppointmentItem(id = "apt_ob1", title = "Échographie du 1er trimestre (T1) & DPNI", dateStr = "15 Jan 2027", timeStr = "09:30", doctor = "Dr. Laurent (OB/GYN)", location = "Centre de Santé Maternelle", clinicalNotes = "Échographie T1, clarté nucale et dépistage prénatal non invasif.", eventType = "Rendez-vous", dayOfMonth = 15L, monthIndex = 0L, yearNum = 2027L),
    SyncedAppointmentItem(id = "apt_ob2", title = "Échographie morphologique T2", dateStr = "31 Mar 2027", timeStr = "10:00", doctor = "Dr. Laurent (OB/GYN)", location = "Centre de Santé Maternelle", clinicalNotes = "Échographie morphologique du 2e trimestre détaillée.", eventType = "Rendez-vous", dayOfMonth = 31L, monthIndex = 2L, yearNum = 2027L),
    SyncedAppointmentItem(id = "apt_ob3", title = "Échographie du 3e trimestre (T3)", dateStr = "09 Juin 2027", timeStr = "10:30", doctor = "Dr. Laurent (OB/GYN)", location = "Centre de Santé Maternelle", clinicalNotes = "Biométrie fœtale et présentation.", eventType = "Rendez-vous", dayOfMonth = 9L, monthIndex = 5L, yearNum = 2027L),
    SyncedAppointmentItem(id = "apt_ob4", title = "Terme & Accouchement (DPA : 6 août 2027)", dateStr = "06 Août 2027", timeStr = "08:00", doctor = "Équipe Maternité", location = "Maternité Hôpital", clinicalNotes = "Date prévue d'accouchement calculée à 40 SA (TEC J5 du 18 nov. 2026).", eventType = "Rendez-vous", dayOfMonth = 6L, monthIndex = 7L, yearNum = 2027L)
  )

  val initialDocuments = listOf(
    SyncedMedicalDocItem(id = "doc_1", title = "Échographie T1 — Clarté nucale (1.2 mm)", category = "Échographie", dateStr = "05 Nov 2026", summary = "Mesure normale du pli nucal, embryon unique bien implanté.", source = "hub"),
    SyncedMedicalDocItem(id = "doc_2", title = "Ordonnance Estrace 2 mg PO (AM & PM)", category = "Ordonnance", dateStr = "06 Nov 2026", summary = "1 comprimé matin (08h) et soir (20h) prescrit par Dr. Laurent.", source = "hub"),
    SyncedMedicalDocItem(id = "doc_3", title = "Bilan Hormonal — Progestérone & HCG", category = "Biologie", dateStr = "07 Nov 2026", summary = "Taux HCG et progestérone parfaitement conformes à 4 SA.", source = "hub"),
    SyncedMedicalDocItem(id = "doc_4", title = "Carte de Groupe Sanguin & Dossier Prénatal", category = "Administratif", dateStr = "01 Nov 2026", summary = "Groupe O+, anticorps réguliers négatifs, dossier archivé.", source = "hub"),
    SyncedMedicalDocItem(id = "vault_1", title = "Ordonnance Estrace 2 mg PO (AM & PM) — Dr. Brauer RMA", category = "Ordonnance", dateStr = "06 nov. 2026", summary = "06 nov. 2026 • 2x/jour (08h & 20h) • Validé", source = "vault"),
    SyncedMedicalDocItem(id = "vault_2", title = "Échographie T1 — Clarté nucale (1.2 mm)", category = "Échographie", dateStr = "05 nov. 2026", summary = "05 nov. 2026 • Dr. Brauer • Validé", source = "vault"),
    SyncedMedicalDocItem(id = "vault_3", title = "Bilan Hormonal — Progestérone & LH", category = "Biologie", dateStr = "05 nov. 2026", summary = "05 nov. 2026 • Laboratoire RMA • Conforme", source = "vault"),
    SyncedMedicalDocItem(id = "vault_4", title = "Carte d'Assurance Médicale & Dossier RMA307254", category = "Administratif", dateStr = "Actif 2026", summary = "Actif 2026 • Stockage Chiffré", source = "vault")
  )

  val initialChecklistItems = listOf(
    SyncedChecklistItem(id = "mb_1", listKey = "mom_bag", label = "Dossier médical complet, carte Vitale, carte mutuelle, livret de famille", isChecked = true, sortOrder = 0L),
    SyncedChecklistItem(id = "mb_2", listKey = "mom_bag", label = "Chemises de nuit ouvertes pour allaitement ou pyjamas amples", isChecked = true, sortOrder = 1L),
    SyncedChecklistItem(id = "mb_3", listKey = "mom_bag", label = "Chaussons confortables antidérapants et chaussettes chaudes", isChecked = false, sortOrder = 2L),
    SyncedChecklistItem(id = "mb_4", listKey = "mom_bag", label = "Culottes jetables en filet ou coton et serviettes post-partum", isChecked = true, sortOrder = 3L),
    SyncedChecklistItem(id = "mb_5", listKey = "mom_bag", label = "Soutiens-gorge d'allaitement et coussinets absorbants", isChecked = false, sortOrder = 4L),
    SyncedChecklistItem(id = "mb_6", listKey = "mom_bag", label = "Crème apaisante à la lanoline pour mamelons et brumisateur d'eau", isChecked = false, sortOrder = 5L),
    SyncedChecklistItem(id = "mb_7", listKey = "mom_bag", label = "Trousse de toilette personnelle, élastiques, baume à lèvres", isChecked = false, sortOrder = 6L),
    SyncedChecklistItem(id = "mb_8", listKey = "mom_bag", label = "Coussin d'allaitement et chargeur de téléphone extra-long", isChecked = false, sortOrder = 7L),

    SyncedChecklistItem(id = "bb_1", listKey = "baby_bag", label = "Tenue de naissance salle d'accouchement (body croisé, pyjama velours, bonnet, chaussons)", isChecked = true, sortOrder = 10L),
    SyncedChecklistItem(id = "bb_2", listKey = "baby_bag", label = "6 bodies en coton (ouverture croisée / cache-cœur, taille 1 mois)", isChecked = true, sortOrder = 11L),
    SyncedChecklistItem(id = "bb_3", listKey = "baby_bag", label = "6 pyjamas doux et confortables en velours", isChecked = false, sortOrder = 12L),
    SyncedChecklistItem(id = "bb_4", listKey = "baby_bag", label = "2 brassières ou gilets en laine / tricot chaud", isChecked = true, sortOrder = 13L),
    SyncedChecklistItem(id = "bb_5", listKey = "baby_bag", label = "2 bonnets en coton et 3 paires de chaussettes", isChecked = true, sortOrder = 14L),
    SyncedChecklistItem(id = "bb_6", listKey = "baby_bag", label = "4 langes doux en mousseline de coton et 3 bavoirs naissance", isChecked = false, sortOrder = 15L),
    SyncedChecklistItem(id = "bb_7", listKey = "baby_bag", label = "2 capes de bain éponge et gigoteuse / turbulette (0-6M)", isChecked = false, sortOrder = 16L),
    SyncedChecklistItem(id = "bb_8", listKey = "baby_bag", label = "Siège auto homologué groupe 0+ installé pour la sortie", isChecked = false, sortOrder = 17L),

    SyncedChecklistItem(id = "pb_1", listKey = "partner_bag", label = "Pièce d'identité & reconnaissance anticipée", isChecked = true, sortOrder = 20L),
    SyncedChecklistItem(id = "pb_2", listKey = "partner_bag", label = "T-shirt propre pour le peau-à-peau en salle de naissance", isChecked = true, sortOrder = 21L),
    SyncedChecklistItem(id = "pb_3", listKey = "partner_bag", label = "En-cas énergétiques, gourde d'eau et monnaie pour machine à café", isChecked = false, sortOrder = 22L),
    SyncedChecklistItem(id = "pb_4", listKey = "partner_bag", label = "Smartphone & chargeur chargé pour immortaliser les premiers instants", isChecked = true, sortOrder = 23L),

    SyncedChecklistItem(id = "bf_1", listKey = "before_birth", label = "Lit bébé ou berceau cododo aux normes de sécurité", isChecked = true, sortOrder = 30L),
    SyncedChecklistItem(id = "bf_2", listKey = "before_birth", label = "Matelas ferme adapté au lit et alèse imperméable respirante", isChecked = false, sortOrder = 31L),
    SyncedChecklistItem(id = "bf_3", listKey = "before_birth", label = "Poussette adaptée dès la naissance avec nacelle / cosy", isChecked = false, sortOrder = 32L),
    SyncedChecklistItem(id = "bf_4", listKey = "before_birth", label = "Table ou matelas à langer avec rebords sécurisés", isChecked = false, sortOrder = 33L),
    SyncedChecklistItem(id = "bf_5", listKey = "before_birth", label = "Thermomètre médical rectal souple et thermomètre de bain", isChecked = true, sortOrder = 34L),
    SyncedChecklistItem(id = "bf_6", listKey = "before_birth", label = "Baignoire pour bébé ergonomique et gel lavant corps & cheveux surgras", isChecked = false, sortOrder = 35L),
    SyncedChecklistItem(id = "bf_7", listKey = "before_birth", label = "Paquet de couches taille 1 (2-5 kg) et liniment oléo-calcaire / coton bio", isChecked = false, sortOrder = 36L),
    SyncedChecklistItem(id = "bf_8", listKey = "before_birth", label = "Sérum physiologique en unidoses et compresses stériles", isChecked = true, sortOrder = 37L),

    SyncedChecklistItem(id = "af_1", listKey = "after_birth", label = "Tire-lait électrique ou manuel (selon choix et confort)", isChecked = false, sortOrder = 40L),
    SyncedChecklistItem(id = "af_2", listKey = "after_birth", label = "Biberons anti-coliques supplémentaires et goupillon de nettoyage", isChecked = false, sortOrder = 41L),
    SyncedChecklistItem(id = "af_3", listKey = "after_birth", label = "Vêtements taille 3 mois selon le rythme de croissance réel", isChecked = false, sortOrder = 42L),
    SyncedChecklistItem(id = "af_4", listKey = "after_birth", label = "Écharpe de portage ou porte-bébé physiologique testé avec bébé", isChecked = false, sortOrder = 43L),
    SyncedChecklistItem(id = "af_5", listKey = "after_birth", label = "Tapis d'éveil d'activité pour les temps sur le ventre (tummy time)", isChecked = false, sortOrder = 44L),
    SyncedChecklistItem(id = "af_6", listKey = "after_birth", label = "Veilleuse douce et babyphone caméra", isChecked = false, sortOrder = 45L)
  )

  private val defaultEstraceMillis: Long = Calendar.getInstance().apply {
    set(Calendar.YEAR, 2026)
    set(Calendar.MONTH, Calendar.NOVEMBER)
    set(Calendar.DAY_OF_MONTH, 13)
    set(Calendar.HOUR_OF_DAY, 20)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis

  val initialProtocolSteps = listOf(
    // Phase 1 : Stimulation Ovarienne & Ponction (Terminée)
    SyncedProtocolStep(
      id = "p1_1", phaseId = 1L, dateStr = "19 sept.",
      stepName = "🛑 Blocage ovarien", medication = "Nortrel", dosage = "Quotidien",
      status = "Complété", details = "Blocage ovarien (Nortrel quotidien).",
      isCompleted = true, sortOrder = 0L
    ),
    SyncedProtocolStep(
      id = "p1_2", phaseId = 1L, dateStr = "21 sept.",
      stepName = "💉 Début stimulation", medication = "Gonal-F + Menopur", dosage = "150 UI + 150 UI / jour",
      status = "Complété", details = "Début stimulation (Gonal-F 150 UI + Menopur 150 UI / jour).",
      isCompleted = true, sortOrder = 1L
    ),
    SyncedProtocolStep(
      id = "p1_3", phaseId = 1L, dateStr = "23 sept.",
      stepName = "🩺 Monitorage 1", medication = "Échographie + Sang", dosage = "7h00",
      status = "Complété", details = "Échographie + bilan sanguin à 7h00.",
      isCompleted = true, sortOrder = 2L
    ),
    SyncedProtocolStep(
      id = "p1_4", phaseId = 1L, dateStr = "25 sept.",
      stepName = "🛑 Blocage LH", medication = "Cétrotide", dosage = "0,25 mg / jour",
      status = "Complété", details = "Blocage LH (Cétrotide 0,25 mg / jour).",
      isCompleted = true, sortOrder = 3L
    ),
    SyncedProtocolStep(
      id = "p1_5", phaseId = 1L, dateStr = "27 sept.",
      stepName = "🩺 Monitorage 2", medication = "Échographie + Sang", dosage = "7h30",
      status = "Complété", details = "Échographie + bilan sanguin à 7h30.",
      isCompleted = true, sortOrder = 4L
    ),
    SyncedProtocolStep(
      id = "p1_6", phaseId = 1L, dateStr = "29 sept.",
      stepName = "⚡ Déclenchement", medication = "Trigger Shot", dosage = "23h00",
      status = "Complété", details = "Déclenchement (23h00).",
      isCompleted = true, isCriticalWindow = true, sortOrder = 5L
    ),
    SyncedProtocolStep(
      id = "p1_7", phaseId = 1L, dateStr = "30 sept.",
      stepName = "📋 Bilan pré-opératoire", medication = "Consultation", dosage = "8h45",
      status = "Complété", details = "Bilan pré-opératoire (8h45).",
      isCompleted = true, sortOrder = 6L
    ),
    SyncedProtocolStep(
      id = "p1_8", phaseId = 1L, dateStr = "01 oct.",
      stepName = "🥚 Ponction ovocytaire", medication = "OPU", dosage = "42 ovocytes",
      status = "Complété", details = "42 ovocytes prélevés ➔ 34 matures ➔ 11 fécondés.",
      isCompleted = true, sortOrder = 7L
    ),

    // Phase 2 : Développement Embryonnaire & Préparation au TEC (En cours)
    SyncedProtocolStep(
      id = "p2_1", phaseId = 2L, dateStr = "02 oct.",
      stepName = "🔬 Fécondation ICSI", medication = "Laboratoire", dosage = "11 fécondés",
      status = "Complété", details = "Fécondation ICSI en laboratoire.",
      isCompleted = true, sortOrder = 8L
    ),
    SyncedProtocolStep(
      id = "p2_2", phaseId = 2L, dateStr = "06–08 oct.",
      stepName = "🧬 Culture prolongée & PGT-A", medication = "Génétique", dosage = "J5/J6",
      status = "Complété", details = "Culture prolongée & analyse génétique PGT-A.",
      isCompleted = true, sortOrder = 9L
    ),
    SyncedProtocolStep(
      id = "p2_3", phaseId = 2L, dateStr = "09 oct.",
      stepName = "❄️ Cryoconservation", medication = "Vitrification", dosage = "8 blastocystes",
      status = "Complété", details = "8 blastocystes vitrifiés, dont 5 euploïdes confirmés.",
      isCompleted = true, sortOrder = 10L
    ),
    SyncedProtocolStep(
      id = "p2_4", phaseId = 2L, dateStr = "11 oct.",
      stepName = "🩺 Échographie basale pré-TEC", medication = "Nortrel", dosage = "Basale",
      status = "Complété", details = "Échographie basale pré-TEC (Nortrel).",
      isCompleted = true, sortOrder = 11L
    ),
    SyncedProtocolStep(
      id = "p2_5", phaseId = 2L, dateStr = "13 oct.",
      stepName = "📝 Sélection de l'embryon", medication = "Portail Patient", dosage = "Euploïde M",
      status = "Complété", details = "Sélection d'un blastocyste euploïde masculin (score morphologique optimal).",
      isCompleted = true, sortOrder = 12L
    ),
    SyncedProtocolStep(
      id = "p2_6", phaseId = 2L, dateStr = "15 oct.",
      stepName = "🩺 Sonohystérographie", medication = "Imagerie RMA", dosage = "8h00",
      status = "Complété", details = "Réalisée à 8h00 au cabinet RMA East Side.",
      isCompleted = true, sortOrder = 13L
    ),
    SyncedProtocolStep(
      id = "p2_7", phaseId = 2L, dateStr = "Fin oct.",
      stepName = "🏥 Hystéroscopie opératoire", medication = "Gramercy Surgery", dosage = "Chirurgie",
      status = "Complété", details = "Gramercy Surgery Center (380 2nd Avenue, Suite 1000) — résection et révision de la cavité endométriale terminées.",
      isCompleted = true, sortOrder = 14L
    ),
    SyncedProtocolStep(
      id = "p2_8", phaseId = 2L, dateStr = "05–08 nov. (Cycle Start)",
      stepName = "💊 Estrace 2 mg PO AM & PM", medication = "Estrace", dosage = "2 mg PO × 2/j (Matin & Soir)",
      status = "Complété", details = "Contrôle de phase (Baseline monitoring & écho) et début de Estrace 2 mg PO AM & PM (2 fois par jour : Matin & Soir) du 05 au 08 nov.",
      isCompleted = true, sortOrder = 15L
    ),
    SyncedProtocolStep(
      id = "p2_9", phaseId = 2L, dateStr = "09 nov.",
      stepName = "💊 Augmentation Estrace (TID)", medication = "Estrace", dosage = "2 mg PO × 3/j (TID)",
      status = "Complété", details = "Augmentation posologie — Estrace 2 mg PO TID (3 fois par jour : Matin, Midi & Soir) à compter du 09 nov.",
      isCompleted = true, sortOrder = 16L
    ),
    SyncedProtocolStep(
      id = "p2_10", phaseId = 2L, dateStr = "12 nov.",
      stepName = "🩺 Monitorage de contrôle", medication = "Échographie + Bilan", dosage = "Cabinet East Side",
      status = "Complété", details = "Monitorage de contrôle complété (Échographie endomètre & bilan hormonal).",
      isCompleted = true, sortOrder = 17L
    ),
    SyncedProtocolStep(
      id = "p2_11", phaseId = 2L, dateStr = "13 nov.",
      stepName = "💉💊 Début PIO (Progestérone IM) & Azithromycine", medication = "Progestérone IM + Azithromycine", dosage = "1 mL IM + 1 000 mg PO",
      status = "Complété", details = "Début des injections intramusculaires de Progestérone (PIO) 1 mL IM Daily chaque soir à heure fixe + prise unique de 1 000 mg d'Azithromycine PO avec de la nourriture.",
      isCompleted = true, sortOrder = 18L
    ),
    SyncedProtocolStep(
      id = "p2_12", phaseId = 2L, dateStr = "17 nov.",
      stepName = "🩺 Monitoring pré-transfert (@ 9:30 AM)", medication = "Cabinet East Side", dosage = "9:30 AM",
      status = "Complété", details = "Monitoring à 9:30 AM complété (Échographie de contrôle de l'endomètre ≥ 7–8 mm & bilan sanguin ; poursuite Estrace 2 mg TID + PIO 1 mL IM Daily).",
      isCompleted = true, sortOrder = 19L
    ),

    // Phase 3 : Transfert (TEC) & Suivi Précoce (En cours)
    SyncedProtocolStep(
      id = "p3_1", phaseId = 3L, dateStr = "18 nov. (Aujourd'hui — Transfer Day 🥳🎉)",
      stepName = "🎯 Transfert d'Embryon Congelé (TEC @ 14:00)", medication = "Blastocyste euploïde masculin", dosage = "02:00 PM EST",
      status = "Aujourd'hui", details = "Embryo Transfer Surgery at 02:00 PM EST du blastocyste euploïde masculin (Vessie pleine, sans parfum ; poursuite Estrace 2 mg TID + PIO 1 mL IM Daily).",
      isCompleted = false, isCriticalWindow = true, sortOrder = 20L
    ),
    SyncedProtocolStep(
      id = "p3_2", phaseId = 3L, dateStr = "19 nov.",
      stepName = "💉 Augmentation Progestérone (PIO 1.5 mL)", medication = "Progestérone IM", dosage = "1.5 mL Daily",
      status = "Planifié", details = "Augmentation Progestérone — passage à PIO 1.5 mL IM Daily chaque soir (poursuite Estrace 2 mg TID).",
      isCompleted = false, sortOrder = 21L
    ),
    SyncedProtocolStep(
      id = "p3_3", phaseId = 3L, dateStr = "20 nov. (J2 post-TEC)",
      stepName = "🩸 Progesterone Check (@ 9:30 AM)", medication = "Laboratoire RMA", dosage = "9:30 AM",
      status = "Planifié", details = "Progesterone Check @ 9:30 AM (prise de sang de contrôle ; dosage susceptible d'être ajusté selon les résultats, poursuite Estrace 2 mg TID + PIO 1.5 mL IM Daily).",
      isCompleted = false, sortOrder = 22L
    ),
    SyncedProtocolStep(
      id = "p3_4", phaseId = 3L, dateStr = "27 nov. (J9 post-TEC)",
      stepName = "🩸 Pregnancy bloodwork (Bêta-hCG @ 9:30 AM 🤞)", medication = "Bêta-hCG sérique #1", dosage = "9:30 AM",
      status = "Planifié", details = "Prise de sang Bêta-hCG sérique #1 @ 9:30 AM 🤞 (confirmation biologique de nidation et déblocage de la Phase 4 Maternité).",
      isCompleted = false, isCriticalWindow = true, sortOrder = 23L
    ),
    SyncedProtocolStep(
      id = "p3_5", phaseId = 3L, dateStr = "Semaines 5–6 (30 nov. – 13 déc. 2026)",
      stepName = "🩺 Échographie précoce 1", medication = "Échographie RMA", dosage = "Sac gestationnel",
      status = "Planifié", details = "Visualisation du sac gestationnel intra-utérin.",
      isCompleted = false, sortOrder = 24L
    ),
    SyncedProtocolStep(
      id = "p3_6", phaseId = 3L, dateStr = "Semaines 7–8 (14 – 27 déc. 2026)",
      stepName = "🩺 Échographie précoce 2", medication = "Échographie RMA", dosage = "Vitalité & Battement",
      status = "Planifié", details = "Vitalité fœtale et rythme cardiaque.",
      isCompleted = false, sortOrder = 25L
    ),
    SyncedProtocolStep(
      id = "p3_7", phaseId = 3L, dateStr = "Semaines 8–10 (Fin déc. 2026 – Début janv. 2027)",
      stepName = "🎉 Sevrage hormonal & Diplôme de sortie RMA", medication = "Transition OB/GYN", dosage = "Sortie RMA",
      status = "Planifié", details = "Sevrage des traitements hormonaux & Transition / Diplôme de sortie RMA.",
      isCompleted = false, sortOrder = 26L
    ),

    // Phase 4 : Suivi Obstétrical & Maternité (En attente de confirmation le 27 nov. — Calendrier Prévisionnel 2027)
    SyncedProtocolStep(
      id = "p4_1", phaseId = 4L, dateStr = "Semaines 11–13 (11 – 31 janv. 2027)",
      stepName = "🩺 Échographie du 1er trimestre (T1) & DPNI", medication = "Dépistage T1", dosage = "Clarté nucale + DPNI",
      status = "Planifié", details = "Échographie du 1er trimestre (T1) & DPNI (dépistage prénatal non invasif / clarté nucale).",
      isCompleted = false, sortOrder = 27L
    ),
    SyncedProtocolStep(
      id = "p4_2", phaseId = 4L, dateStr = "Semaine 12 (18 – 24 janv. 2027)",
      stepName = "👩‍⚕️ Consultation prénatale mensuelle", medication = "Obstétrique", dosage = "Suivi T1",
      status = "Planifié", details = "Consultation prénatale mensuelle.",
      isCompleted = false, sortOrder = 28L
    ),
    SyncedProtocolStep(
      id = "p4_3", phaseId = 4L, dateStr = "Semaines 18–20 (1er – 21 mars 2027)",
      stepName = "👣 Premiers mouvements fœtaux ressentis", medication = "Clinique", dosage = "T2",
      status = "Planifié", details = "Premiers mouvements fœtaux ressentis.",
      isCompleted = false, sortOrder = 29L
    ),
    SyncedProtocolStep(
      id = "p4_4", phaseId = 4L, dateStr = "Semaine 22 (29 mars – 4 avr. 2027)",
      stepName = "🩺 Échographie morphologique du 2e trimestre (T2)", medication = "Échographie", dosage = "Morphologie T2",
      status = "Planifié", details = "Échographie morphologique du 2e trimestre (T2).",
      isCompleted = false, sortOrder = 30L
    ),
    SyncedProtocolStep(
      id = "p4_5", phaseId = 4L, dateStr = "Semaines 24–28 (12 avr. – 16 mai 2027)",
      stepName = "🩸 Test HGPO (dépistage diabète gestationnel)", medication = "Laboratoire", dosage = "HGPO",
      status = "Planifié", details = "Test HGPO (dépistage du diabète gestationnel).",
      isCompleted = false, sortOrder = 31L
    ),
    SyncedProtocolStep(
      id = "p4_6", phaseId = 4L, dateStr = "Semaine 32 (7 – 13 juin 2027)",
      stepName = "🩺 Échographie du 3e trimestre (T3)", medication = "Échographie", dosage = "Biométrie T3",
      status = "Planifié", details = "Échographie du 3e trimestre (T3).",
      isCompleted = false, sortOrder = 32L
    ),
    SyncedProtocolStep(
      id = "p4_7", phaseId = 4L, dateStr = "Semaine 36 (5 – 11 juil. 2027)",
      stepName = "🏥 Bilan pré-accouchement & consultation d'anesthésie", medication = "Maternité", dosage = "Anesthésie + SGB",
      status = "Planifié", details = "Bilan pré-accouchement & consultation d'anesthésie (+ dépistage Streptocoque B).",
      isCompleted = false, sortOrder = 33L
    ),
    SyncedProtocolStep(
      id = "p4_8", phaseId = 4L, dateStr = "Semaines 37–41 (16 juil. – 13 août 2027)",
      stepName = "👶 Terme & Accouchement (DPA : 6 août 2027)", medication = "Maternité", dosage = "40 SA : 6 août 2027",
      status = "Planifié", details = "Terme & Accouchement (Date prévue d'accouchement à 40 SA : 6 août 2027).",
      isCompleted = false, isCriticalWindow = true, sortOrder = 34L
    )
  )

  val initialProviders = HealthHubDefaults.initialCareProviders

  val initialWalletCards = listOf(
    SyncedWalletCard(
      id = "wc_insurance",
      cardTitle = "Assurance Santé & Mutuelle",
      cardCategory = "Assurance",
      cardNumber = "POL-9482-7731-4829",
      holderName = "Fatou",
      expiryDate = "12/2027",
      notes = "Couverture maternité & PMA 100%",
      gradientKey = "teal",
      sortOrder = 0L
    ),
    SyncedWalletCard(
      id = "wc_driver_id",
      cardTitle = "Permis de Conduire / ID",
      cardCategory = "Identité",
      cardNumber = "ID-FR-4920-5819-9041",
      holderName = "Fatou",
      expiryDate = "11/2035",
      notes = "Pièce d'identité officielle",
      gradientKey = "sunset_orange",
      sortOrder = 1L
    ),
    SyncedWalletCard(
      id = "wc_membership",
      cardTitle = "Carte Membre & Clinique",
      cardCategory = "Membre",
      cardNumber = "MBR-RMA-3072-8410",
      holderName = "Fatou",
      expiryDate = "09/2028",
      notes = "Accès prioritaire consultations",
      gradientKey = "deep_purple",
      sortOrder = 2L
    ),
    SyncedWalletCard(
      id = "wc_benefit",
      cardTitle = "Carte Pharmacie & Tiers Payant",
      cardCategory = "Santé",
      cardNumber = "RX-6640-1120-5592",
      holderName = "Fatou",
      expiryDate = "06/2028",
      notes = "Remboursement direct pharmacie",
      gradientKey = "magenta_blue",
      sortOrder = 3L
    )
  )
}
