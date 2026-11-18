package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureInitialSeedData_authenticatedOwner_populatesAllCollectionsAndStreamsViaFlow() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureInitialSeedData(uid)
    }

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeProfile(uid).first { it != null }
    }
    assertNotNull(profile)
    assertEquals(uid, profile?.userId)

    val tasks = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks(uid).first { it.isNotEmpty() }
    }
    assertTrue(tasks.isNotEmpty())

    val appointments = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeAppointments(uid).first { it.isNotEmpty() }
    }
    assertTrue(appointments.isNotEmpty())

    val checklists = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeChecklists(uid).first { it.isNotEmpty() }
    }
    assertTrue(checklists.isNotEmpty())

    val protocolSteps = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeProtocolSteps(uid).first { it.isNotEmpty() }
    }
    assertTrue(protocolSteps.isNotEmpty())
  }

  @Test
  fun crossDeviceSimulation_ownerMutationsAreObservedAndPersist() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureInitialSeedData(uid)
    }

    val taskId = "task_${UUID.randomUUID()}"
    val newTask = TaskItem(
      id = taskId,
      title = "Sync Verification Task",
      date = "10 nov. 2026",
      category = "Médication",
      details = "Vérification multi-appareils",
      isCompleted = false,
      hasAlarm = true,
      alarmTimeMinutesBefore = 30
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveTask(newTask)
    }

    val emittedAfterCreate = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks(uid).first { list -> list.any { it.id == taskId && !it.isCompleted } }
    }
    assertTrue(emittedAfterCreate.any { it.id == taskId })

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.toggleTask(newTask)
      repository.updateWaterGlasses(8)
      repository.updatePregnancyProfile("Fatou", "29", 12, "20 Juin 2027")
    }

    val emittedAfterToggle = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks(uid).first { list -> list.any { it.id == taskId && it.isCompleted } }
    }
    assertTrue(emittedAfterToggle.first { it.id == taskId }.isCompleted)

    val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeProfile(uid).first { it?.waterGlasses == 8L && it.currentWeek == 12L }
    }
    assertEquals("29", updatedProfile?.patientAge)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.deleteTask(taskId)
    }
  }

  @Test
  fun crossUserIsolation_bobCannotReadOrWriteAliceCollections() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestoreRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureInitialSeedData(aliceUid)
    }

    signInTestUser(BOB_EMAIL)

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("profile").document("main").get().await()
      }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's profile")
    } catch (e: Exception) {
      val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("tasks").document("t1")
          .update("isCompleted", true).await()
      }
      fail("Expected PERMISSION_DENIED when Bob updates Alice's task")
    } catch (e: Exception) {
      val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }
  }

  @Test
  fun crossUserIsolation_bobCannotReadOrWriteAliceMedReminders() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestoreRepository(firestore, auth)
    val remId = "mr_test_${UUID.randomUUID()}"
    val rem = SyncedMedReminder(id = remId, userId = aliceUid, title = "Test Med", medication = "Estrace", dosage = "2mg", hour = 8, minute = 0)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.saveMedReminder(rem)
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("med_reminders").document(remId).get().await()
      }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's med reminder")
    } catch (e: Exception) {
      val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }
  }

  @Test
  fun forceFetchFromCloud_authenticatedOwner_returnsVerifiedTimestampAndCounts() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureInitialSeedData(uid)
    }

    val summary = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.forceFetchFromCloud(uid)
    }

    assertNotNull(summary.lastSyncedEpochMillis)
    assertEquals("MANUAL", summary.lastSyncTrigger)
    assertEquals(1, summary.profileCount)
    assertTrue(summary.tasksCount > 0)
    assertTrue(summary.appointmentsCount > 0)
    assertTrue(summary.checklistsCount > 0)
    assertTrue(summary.walletCardsCount > 0)
    assertTrue(summary.totalSyncedRecords >= 10)
  }

  @Test
  fun walletCards_ownerCrudAndCrossUserIsolation_succeedsAndEnforcesSecurity() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestoreRepository(firestore, auth)

    val cardId = "wc_${UUID.randomUUID()}"
    val card = SyncedWalletCard(
      id = cardId,
      cardTitle = "Assurance Maternité",
      cardCategory = "Assurance",
      cardNumber = "POL-1234-5678",
      holderName = "Fatou",
      expiryDate = "12/2029",
      notes = "Carte test",
      gradientKey = "magenta_blue",
      frontImageBase64 = "ZmFrZV9mcm9udA==",
      backImageBase64 = "ZmFrZV9iYWNr",
      sortOrder = 5L
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.saveWalletCard(card)
    }

    val snapshotAfterCreate = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeWalletCards(aliceUid).first { snap -> snap.cards.any { it.id == cardId } }
    }
    val saved = snapshotAfterCreate.cards.first { it.id == cardId }
    assertEquals("Assurance Maternité", saved.cardTitle)
    assertEquals("magenta_blue", saved.gradientKey)
    assertEquals("ZmFrZV9mcm9udA==", saved.frontImageBase64)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.saveWalletCard(saved.copy(cardTitle = "Assurance Mise à Jour", gradientKey = "deep_purple"))
    }
    val snapshotAfterUpdate = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeWalletCards(aliceUid).first { snap ->
        snap.cards.any { it.id == cardId && it.cardTitle == "Assurance Mise à Jour" }
      }
    }
    assertEquals("deep_purple", snapshotAfterUpdate.cards.first { it.id == cardId }.gradientKey)

    // Bob cannot read or delete Alice's wallet card
    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("wallet_cards").document(cardId).get().await()
      }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's wallet card")
    } catch (e: Exception) {
      val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    // Re-authenticate Alice and clean up
    signInTestUser(ALICE_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.deleteWalletCard(cardId)
    }
  }

  @Test
  fun resetProtocolToDefault_restoresSection1AndAllFourPhases() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureInitialSeedData(uid)
    }

    // Mutate Section 1 and Section 2 with custom changes
    val customStepId = "p_custom_${UUID.randomUUID()}"
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveProtocolStep(
        SyncedProtocolStep(
          id = customStepId,
          phaseId = 2L,
          dateStr = "14 nov.",
          stepName = "Étape Test Personnalisée",
          medication = "Estrace",
          dosage = "2 mg",
          status = "En cours",
          details = "Note test",
          isCompleted = false,
          sortOrder = 99L
        )
      )
      repository.deleteTask("t5")
    }

    val mutatedSteps = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeProtocolSteps(uid).first { list -> list.any { it.id == customStepId } }
    }
    assertTrue(mutatedSteps.any { it.id == customStepId })

    // Reset to default RMA East Side protocol
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.resetProtocolToDefault(uid)
    }

    val restoredSteps = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeProtocolSteps(uid).first { list ->
        list.size == SeedDefaults.initialProtocolSteps.size && list.none { it.id == customStepId }
      }
    }
    assertEquals(33, restoredSteps.size)
    val todayStep = restoredSteps.firstOrNull { it.id == "p2_10_today" }
    assertNotNull(todayStep)
    assertEquals("Aujourd'hui", todayStep?.status)
    assertEquals(2L, todayStep?.phaseId)

    val restoredTasks = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks(uid).first { list -> list.any { it.id == "t5" } }
    }
    assertEquals(5, restoredTasks.size)
    assertTrue(restoredTasks.any { it.id == "t5" && !it.isCompleted })
  }

  private companion object {
    const val ALICE_EMAIL = "alice_sync@test.com"
    const val BOB_EMAIL = "bob_sync@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
