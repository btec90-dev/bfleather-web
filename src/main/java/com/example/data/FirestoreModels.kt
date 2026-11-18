package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull { it !is CancellationException }
    ?: exception

  val currentUser = try {
    Firebase.auth.currentUser
  } catch (_: Exception) {
    null
  }

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

@IgnoreExtraProperties
data class SyncedUserProfile(
  @DocumentId val id: String = "main",
  val userId: String = "",
  val patientName: String = "Fatou",
  val patientAge: String = "28",
  val currentWeek: Long = 4L,
  val dueDate: String = "15 Juillet 2027",
  val daysRemaining: Long = 249L,
  val dashboardDueDateText: String = "24 Juillet 2026 à 09:30",
  val gestationalWeeks: Long = 16L,
  val gestationalDaysRemainder: Long = 4L,
  val remainingCountdownDays: Long = 168L,
  val currentTrimester: String = "Trimestre 2",
  val progressPercent: Double = 0.55,
  val healthMood: String = "Calme",
  val healthWeight: String = "64.5",
  val waterGlasses: Long = 4L,
  val bloodPressure: String = "118/76",
  val personalNotesText: String = "",
  val momAvatar: String = "🌸",
  val partnerAvatar: String = "💙",
  val babyAvatar: String = "👶",
  val languageCode: String = "FR",
  val themePaletteName: String = "CARAMEL",
  val activePartnerProfile: String = "Fatou",
  val coupleAvatarBase64: String = "",
  val walletPin: String = "8888",
  val isBetaHcgConfirmed: Boolean = false,
  val stageViewMode: String = "IVF",
  val autoCloudBackupEnabled: Boolean = true,
  val lastAutoBackupEpochMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "patientName" to patientName.take(100),
    "patientAge" to patientAge.take(20),
    "currentWeek" to currentWeek.coerceIn(1L, 45L),
    "dueDate" to dueDate.take(100),
    "daysRemaining" to daysRemaining.coerceIn(0L, 400L),
    "dashboardDueDateText" to dashboardDueDateText.take(100),
    "gestationalWeeks" to gestationalWeeks.coerceIn(1L, 45L),
    "gestationalDaysRemainder" to gestationalDaysRemainder.coerceIn(0L, 6L),
    "remainingCountdownDays" to remainingCountdownDays.coerceIn(0L, 400L),
    "currentTrimester" to currentTrimester.take(60),
    "progressPercent" to progressPercent.coerceIn(0.0, 1.0),
    "healthMood" to healthMood.take(100),
    "healthWeight" to healthWeight.take(30),
    "waterGlasses" to waterGlasses.coerceIn(0L, 50L),
    "bloodPressure" to bloodPressure.take(30),
    "personalNotesText" to personalNotesText.take(5000),
    "momAvatar" to momAvatar.take(20),
    "partnerAvatar" to partnerAvatar.take(20),
    "babyAvatar" to babyAvatar.take(20),
    "languageCode" to if (languageCode == "EN") "EN" else "FR",
    "themePaletteName" to themePaletteName.take(50),
    "activePartnerProfile" to if (activePartnerProfile == "Partner") "Partner" else "Fatou",
    "coupleAvatarBase64" to coupleAvatarBase64.take(250000),
    "walletPin" to walletPin.filter { it.isDigit() }.take(4).padStart(4, '8'),
    "isBetaHcgConfirmed" to isBetaHcgConfirmed,
    "stageViewMode" to if (stageViewMode == "PREGNANCY") "PREGNANCY" else "IVF",
    "autoCloudBackupEnabled" to autoCloudBackupEnabled,
    "lastAutoBackupEpochMillis" to lastAutoBackupEpochMillis,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )
}

@IgnoreExtraProperties
data class SyncedTaskItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val date: String = "",
  val category: String = "",
  val details: String = "",
  val isCompleted: Boolean = false,
  val hasAlarm: Boolean = false,
  val alarmTimeMinutesBefore: Long = 15L,
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toTaskItem(): TaskItem = TaskItem(
    id = id,
    title = title,
    date = date,
    category = category,
    details = details,
    isCompleted = isCompleted,
    hasAlarm = hasAlarm,
    alarmTimeMinutesBefore = alarmTimeMinutesBefore.toInt(),
    addedBy = addedBy
  )
}

@IgnoreExtraProperties
data class SyncedNoteItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val content: String = "",
  val category: String = "clinical",
  val timestampText: String = "",
  val mood: String = "",
  val symptoms: List<String> = emptyList(),
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedAppointmentItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val dateStr: String = "",
  val timeStr: String = "",
  val doctor: String = "",
  val location: String = "",
  val clinicalNotes: String = "",
  val eventType: String = "Rendez-vous",
  val dayOfMonth: Long = 13L,
  val monthIndex: Long = 10L,
  val yearNum: Long = 2026L,
  val isCompleted: Boolean = false,
  val hasAlarm: Boolean = false,
  val alarmOffsetMinutes: Long = 15L,
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedMedicalDocItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val category: String = "",
  val dateStr: String = "",
  val summary: String = "",
  val source: String = "hub",
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedChecklistItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val listKey: String = "mom_bag",
  val label: String = "",
  val isChecked: Boolean = false,
  val sortOrder: Long = 0L,
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedProtocolStep(
  @DocumentId val id: String = "",
  val userId: String = "",
  val phaseId: Long = 2L,
  val dateStr: String = "",
  val stepName: String = "",
  val medication: String = "",
  val dosage: String = "",
  val status: String = "En cours",
  val details: String = "",
  val isCompleted: Boolean = false,
  val isCriticalWindow: Boolean = false,
  val targetEpochMillis: Long = 0L,
  val sortOrder: Long = 0L,
  val hasAlarm: Boolean = false,
  val alarmOffsetMinutes: Long = 15L,
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedCareProvider(
  @DocumentId val id: String = "",
  val userId: String = "",
  val providerType: String = "OB/GYN",
  val name: String = "",
  val affiliation: String = "",
  val phone: String = "",
  val emergencyContact: String = "",
  val address: String = "",
  val portalLink: String = "",
  val email: String = "",
  val photoUri: String = "",
  val notes: String = "",
  val isEmergency: Boolean = false,
  val category: String = "MEDICAL",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedVoiceMemo(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val durationSec: Long = 0L,
  val timestampText: String = "",
  val audioBase64: String = "",
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class WalletSyncStatus {
  SYNCED,
  OFFLINE_LOCAL,
  SYNCING,
  ERROR
}

@IgnoreExtraProperties
data class SyncedWalletCard(
  @DocumentId val id: String = "",
  val userId: String = "",
  val cardTitle: String = "",
  val cardCategory: String = "Assurance",
  val cardNumber: String = "",
  val holderName: String = "",
  val expiryDate: String = "",
  val notes: String = "",
  val gradientKey: String = "teal",
  val frontImageBase64: String = "",
  val backImageBase64: String = "",
  val sortOrder: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class WalletSyncSnapshot(
  val cards: List<SyncedWalletCard> = emptyList(),
  val syncStatus: WalletSyncStatus = WalletSyncStatus.SYNCED
)

@IgnoreExtraProperties
data class SyncedMedReminder(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val medication: String = "",
  val dosage: String = "",
  val hour: Long = 8L,
  val minute: Long = 0L,
  val repeatDaily: Boolean = true,
  val isEnabled: Boolean = true,
  val protocolPhase: String = "Phase 2 TEC",
  val notes: String = "",
  val pillsRemaining: Long = 60L,
  val dailyAmTaken: Boolean = false,
  val dailyPmTaken: Boolean = false,
  val lastDoseDateStr: String = "",
  val soundEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toLocalMedAlarm(): com.example.alarm.LocalMedAlarm = com.example.alarm.LocalMedAlarm(
    id = id,
    title = title,
    medication = medication,
    dosage = dosage,
    hour = hour.toInt().coerceIn(0, 23),
    minute = minute.toInt().coerceIn(0, 59),
    repeatDaily = repeatDaily,
    isEnabled = isEnabled,
    protocolPhase = protocolPhase,
    notes = notes
  )
}

@IgnoreExtraProperties
data class SyncedPregnancyMed(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val activeIngredient: String = "",
  val category: String = "",
  val safetyLevel: String = "SAFE",
  val trimester1: String = "Sûr",
  val trimester2: String = "Sûr",
  val trimester3: String = "Sûr",
  val clinicalNotes: String = "",
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedNutritionFood(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val category: String = "",
  val safetyStatus: String = "EAT",
  val highlights: String = "",
  val nutrients: String = "",
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedGroceryItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val label: String = "",
  val category: String = "Fruits & Légumes",
  val quantity: String = "1",
  val isChecked: Boolean = false,
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedHealthIssue(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val trimester: String = "T1",
  val severity: String = "Léger",
  val dateStr: String = "",
  val symptoms: String = "",
  val doctorNotes: String = "",
  val prescribedTreatment: String = "",
  val isResolved: Boolean = false,
  val addedBy: String = "Fatou",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedExpenseRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val amount: Double = 0.0,
  val currency: String = "USD",
  val category: String = "Others",
  val phase: String = "IVF / Clinic",
  val paymentStatus: String = "Out-of-Pocket",
  val dateStr: String = "",
  val notes: String = "",
  val receiptBase64: String = "",
  val attachmentTitle: String = "",
  val addedBy: String = "Fatou",
  val insurancePaid: Double = 0.0,
  val totalBill: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncedBackupSnapshot(
  @DocumentId val id: String = "",
  val userId: String = "",
  val timestampEpochMillis: Long = 0L,
  val triggerType: String = "MANUAL",
  val addedBy: String = "Fatou",
  val recordCount: Long = 0L,
  val payloadJson: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

data class CloudSyncSummary(
  val lastSyncedEpochMillis: Long? = null,
  val isSyncing: Boolean = false,
  val syncError: String? = null,
  val lastSyncTrigger: String = "LIVE",
  val profileCount: Int = 0,
  val tasksCount: Int = 0,
  val notesCount: Int = 0,
  val appointmentsCount: Int = 0,
  val documentsCount: Int = 0,
  val checklistsCount: Int = 0,
  val protocolStepsCount: Int = 0,
  val careProvidersCount: Int = 0,
  val voiceMemosCount: Int = 0,
  val walletCardsCount: Int = 0,
  val medRemindersCount: Int = 0,
  val healthHubRecordsCount: Int = 0
) {
  val totalSyncedRecords: Int
    get() = profileCount + tasksCount + notesCount + appointmentsCount +
      documentsCount + checklistsCount + protocolStepsCount +
      careProvidersCount + voiceMemosCount + walletCardsCount +
      medRemindersCount + healthHubRecordsCount
}



