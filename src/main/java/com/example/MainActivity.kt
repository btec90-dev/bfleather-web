package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FirestoreRepository
import com.example.data.SyncedAppointmentItem
import com.example.data.SyncedExpenseRecord
import com.example.data.SyncedNoteItem
import com.example.data.configureAppCheck
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.*
import com.example.ui.theme.AppTheme
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.AppThemeProvider
import com.example.ui.theme.ThemeColors
import com.example.ui.theme.ThemeManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private var isFirestoreOfflineConfigured = false

private fun getOfflineEnabledFirestore(dbId: String): FirebaseFirestore {
  val db = FirebaseFirestore.getInstance(dbId)
  if (!isFirestoreOfflineConfigured) {
    isFirestoreOfflineConfigured = true
    runCatching {
      db.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(
          PersistentCacheSettings.newBuilder()
            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        )
        .build()
    }
  }
  return db
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    setContent {
      AppThemeProvider(themeManager = ThemeManager) {
        var showSplash by remember { mutableStateOf(true) }
        var isCheckingAutoSignIn by remember { mutableStateOf(true) }
        var currentLanguage by remember { mutableStateOf(AppLanguage.FR) }

        val context = LocalContext.current
        val activeTheme = AppTheme.colors
        val currentUser by Firebase.auth.authStateFlow()
          .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val notifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { }

          LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
              notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
        }

        // Attempt silent returning-user sign-in with Credential Manager on cold start
        LaunchedEffect(Unit) {
          attemptAutoSignIn(this@MainActivity)
          isCheckingAutoSignIn = false
        }

        if (showSplash) {
          SplashScreen(onTimeout = { showSplash = false })
        } else if (currentUser == null) {
          AuthScreen(
            theme = activeTheme,
            language = currentLanguage,
            isCheckingSession = isCheckingAutoSignIn
          )
        } else {
          val user = currentUser!!
          key(user.uid) {
            AuthenticatedAppContent(
              uid = user.uid,
              userEmail = user.email ?: user.uid,
              currentLanguage = currentLanguage,
              onLanguageChange = { currentLanguage = it },
              activeTheme = activeTheme
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthenticatedAppContent(
  uid: String,
  userEmail: String,
  currentLanguage: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  activeTheme: ThemeColors
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var currentTab by remember { mutableIntStateOf(0) }
  var showSettings by remember { mutableStateOf(false) }
  var globalSearchQuery by remember { mutableStateOf("") }
  var showVoiceAssistantModal by remember { mutableStateOf(false) }
  var prefilledExpense by remember { mutableStateOf<ExpensePrefill?>(null) }
  var externalOpenWalletCardId by remember { mutableStateOf<String?>(null) }
  var autoBackupAttemptedThisSession by remember { mutableStateOf(false) }
  val activePalette = AppTheme.palette

  val syncViewModel: PmaSyncViewModel = viewModel(
    key = uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val dbId = app.getString(R.string.firestore_database_id)
        val db = getOfflineEnabledFirestore(dbId)
        PmaSyncViewModel(FirestoreRepository(db, Firebase.auth), uid)
      }
    }
  )

  val profile by syncViewModel.profile.collectAsStateWithLifecycle()
  val tasksState by syncViewModel.tasks.collectAsStateWithLifecycle()
  val notesState by syncViewModel.notes.collectAsStateWithLifecycle()
  val appointmentsState by syncViewModel.appointments.collectAsStateWithLifecycle()
  val documentsState by syncViewModel.documents.collectAsStateWithLifecycle()
  val checklistsState by syncViewModel.checklists.collectAsStateWithLifecycle()
  val protocolStepsState by syncViewModel.protocolSteps.collectAsStateWithLifecycle()
  val careProvidersState by syncViewModel.careProviders.collectAsStateWithLifecycle()
  val voiceMemosState by syncViewModel.voiceMemos.collectAsStateWithLifecycle()
  val walletCardsState by syncViewModel.walletCards.collectAsStateWithLifecycle()
  val walletSyncStatus by syncViewModel.walletSyncStatus.collectAsStateWithLifecycle()
  val medRemindersState by syncViewModel.medReminders.collectAsStateWithLifecycle()
  val pregnancyMedsState by syncViewModel.pregnancyMeds.collectAsStateWithLifecycle()
  val nutritionFoodsState by syncViewModel.nutritionFoods.collectAsStateWithLifecycle()
  val groceryItemsState by syncViewModel.groceryItems.collectAsStateWithLifecycle()
  val healthIssuesState by syncViewModel.healthIssues.collectAsStateWithLifecycle()
  val expensesState by syncViewModel.expenses.collectAsStateWithLifecycle()
  val backupsState by syncViewModel.backups.collectAsStateWithLifecycle()
  val syncSummary by syncViewModel.syncSummary.collectAsStateWithLifecycle()
  val galleryViewModel: GalleryViewModel = viewModel()

  BackHandler(enabled = showSettings) {
    showSettings = false
  }

  LaunchedEffect(medRemindersState) {
    com.example.alarm.AlarmScheduler.syncDailyMedicationAlarms(
      context,
      medRemindersState.map { it.toLocalMedAlarm() }
    )
  }

  // Trigger silent daily auto-backup if enabled and >=24h since last backup
  LaunchedEffect(profile.autoCloudBackupEnabled, profile.lastAutoBackupEpochMillis, syncSummary.totalSyncedRecords) {
    val oneDayMs = 24L * 60L * 60L * 1000L
    if (!autoBackupAttemptedThisSession &&
      profile.autoCloudBackupEnabled &&
      syncSummary.totalSyncedRecords > 0 &&
      (System.currentTimeMillis() - profile.lastAutoBackupEpochMillis) >= oneDayMs
    ) {
      autoBackupAttemptedThisSession = true
      syncViewModel.createCloudBackup("AUTO")
    }
  }

  // Apply synced theme & language preferences whenever remote profile updates
  LaunchedEffect(profile.themePaletteName, profile.languageCode) {
    val parsedPalette = runCatching { AppThemePalette.valueOf(profile.themePaletteName) }
      .getOrDefault(AppThemePalette.CARAMEL)
    if (parsedPalette != activePalette) {
      ThemeManager.setPalette(parsedPalette)
    }
    val syncedLang = if (profile.languageCode == "EN") AppLanguage.EN else AppLanguage.FR
    if (syncedLang != currentLanguage) {
      onLanguageChange(syncedLang)
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = activeTheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      NavigationBar(
        containerColor = activeTheme.headerFooterBg,
        contentColor = activeTheme.primaryText,
        tonalElevation = 0.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(BorderStroke(1.dp, activeTheme.border))
      ) {
        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.Home,
              contentDescription = Strings.t("dashboard_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("dashboard_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1,
              softWrap = false
            )
          },
          alwaysShowLabel = true,
          selected = !showSettings && currentTab == 0,
          onClick = { showSettings = false; currentTab = 0 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.Timeline,
              contentDescription = Strings.t("protocol_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("protocol_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1,
              softWrap = false
            )
          },
          alwaysShowLabel = true,
          selected = !showSettings && currentTab == 1,
          onClick = { showSettings = false; currentTab = 1 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.PregnantWoman,
              contentDescription = Strings.t("pregnancy_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("pregnancy_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1,
              softWrap = false
            )
          },
          alwaysShowLabel = true,
          selected = !showSettings && currentTab == 2,
          onClick = { showSettings = false; currentTab = 2 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.Default.AutoAwesome,
              contentDescription = Strings.t("medical_db_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("medical_db_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1,
              softWrap = false
            )
          },
          alwaysShowLabel = true,
          selected = !showSettings && currentTab == 3,
          onClick = { showSettings = false; currentTab = 3 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )

        NavigationBarItem(
          icon = {
            Icon(
              Icons.AutoMirrored.Filled.ReceiptLong,
              contentDescription = Strings.t("expenses_tab", currentLanguage),
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = Strings.t("expenses_tab", currentLanguage),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
              maxLines = 1,
              softWrap = false
            )
          },
          alwaysShowLabel = true,
          selected = !showSettings && currentTab == 4,
          onClick = { showSettings = false; currentTab = 4 },
          modifier = Modifier.testTag("nav_tab_expenses"),
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeTheme.primaryText,
            selectedTextColor = activeTheme.primaryText,
            indicatorColor = activeTheme.accent.copy(alpha = 0.16f),
            unselectedIconColor = activeTheme.mutedText,
            unselectedTextColor = activeTheme.mutedText
          )
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      GlobalHeaderBar(
        lang = currentLanguage,
        coupleAvatarBase64 = profile.coupleAvatarBase64,
        activePartnerProfile = profile.activePartnerProfile,
        onTogglePartnerProfile = { partner -> syncViewModel.updatePartnerProfile(partner) },
        onUpdateCoupleAvatar = { b64 -> syncViewModel.updateCoupleAvatar(b64) },
        onOpenSettings = { showSettings = !showSettings },
        onOpenPhoneBook = {
          showSettings = false
          currentTab = 3
        }
      )

      if (!showSettings) {
        GlobalSearchAndVoiceBar(
          lang = currentLanguage,
          query = globalSearchQuery,
          onQueryChange = { globalSearchQuery = it },
          onOpenVoiceAssistant = { showVoiceAssistantModal = true },
          pregnancyMeds = pregnancyMedsState,
          medReminders = medRemindersState,
          appointments = appointmentsState,
          expenses = expensesState,
          walletCards = walletCardsState,
          notes = notesState,
          voiceMemos = voiceMemosState,
          protocolSteps = protocolStepsState,
          nutritionFoods = nutritionFoodsState,
          careProviders = careProvidersState,
          showDropdown = currentTab != 3,
          onResultClick = { item ->
            globalSearchQuery = ""
            if (item.requiresWalletPin) {
              currentTab = 0
              externalOpenWalletCardId = item.id.removePrefix("wc_")
            } else {
              currentTab = item.targetTab
            }
          }
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        if (showSettings) {
          SettingsScreen(
            userEmail = userEmail,
            syncSummary = syncSummary,
            profile = profile,
            protocolSteps = protocolStepsState,
            medReminders = medRemindersState,
            pregnancyMeds = pregnancyMedsState,
            careProviders = careProvidersState,
            expenses = expensesState,
            appointments = appointmentsState,
            tasks = tasksState,
            notes = notesState,
            voiceMemos = voiceMemosState,
            walletCards = walletCardsState,
            backups = backupsState,
            onAvatarsChanged = { mom, partner, baby ->
              syncViewModel.updatePreferences(
                momAvatar = mom,
                partnerAvatar = partner,
                babyAvatar = baby,
                languageCode = currentLanguage.name,
                themePaletteName = activePalette.name
              )
            },
            onPartnerProfileChanged = { syncViewModel.updatePartnerProfile(it) },
            onCoupleAvatarChanged = { syncViewModel.updateCoupleAvatar(it) },
            onUpdateWalletPin = { syncViewModel.updateWalletPin(it) },
            onCreateCloudBackup = { syncViewModel.createCloudBackup("MANUAL") },
            onRestoreBackupJson = { json -> syncViewModel.restoreBackupFromJson(json) },
            onToggleAutoBackup = { enabled -> syncViewModel.updateAutoBackupEnabled(enabled) },
            currentPalette = activePalette,
            onPaletteChanged = { newPalette ->
              ThemeManager.setPalette(newPalette)
              syncViewModel.updatePreferences(
                momAvatar = profile.momAvatar,
                partnerAvatar = profile.partnerAvatar,
                babyAvatar = profile.babyAvatar,
                languageCode = currentLanguage.name,
                themePaletteName = newPalette.name
              )
            },
            currentLanguage = currentLanguage,
            onLanguageChanged = { newLang ->
              onLanguageChange(newLang)
              syncViewModel.updatePreferences(
                momAvatar = profile.momAvatar,
                partnerAvatar = profile.partnerAvatar,
                babyAvatar = profile.babyAvatar,
                languageCode = newLang.name,
                themePaletteName = activePalette.name
              )
            },
            onInitiateSync = { syncViewModel.initiateSync() },
            onSignOut = {
              coroutineScope.launch {
                signOutWithGoogle(context)
              }
            },
            onCloseSettings = { showSettings = false },
            theme = activeTheme
          )
        } else {
          when (currentTab) {
            0 -> DashboardScreen(
              userId = uid,
              userEmail = userEmail,
              syncSummary = syncSummary,
              tasks = tasksState,
              protocolSteps = protocolStepsState,
              appointments = appointmentsState,
              checklists = checklistsState,
              medReminders = medRemindersState,
              profile = profile,
              notes = notesState,
              documents = documentsState.filter { it.source == "vault" },
              voiceMemos = voiceMemosState,
              walletCards = walletCardsState,
              walletSyncStatus = walletSyncStatus,
              externalOpenWalletCardId = externalOpenWalletCardId,
              onClearExternalWalletCard = { externalOpenWalletCardId = null },
              onUpdateWalletPin = { syncViewModel.updateWalletPin(it) },
              onLogExpenseFromCard = { card ->
                prefilledExpense = ExpensePrefill(
                  title = card.cardTitle,
                  category = if (card.cardCategory.contains("pharm", true) || card.cardCategory.contains("assur", true)) "Prescriptions" else "Others",
                  notes = "${card.cardCategory} •••• ${card.cardNumber.takeLast(4)}",
                  attachmentTitle = card.cardTitle
                )
                currentTab = 4
              },
              onUpdateStageViewMode = { mode, beta -> syncViewModel.updateStageViewMode(mode, beta) },
              onToggleMedDose = { rem, period, dayKey -> syncViewModel.toggleMedDose(rem, period, dayKey) },
              onRefillMedPills = { id, newCount -> syncViewModel.refillMedPills(id, newCount) },
              onSaveChecklistItem = { syncViewModel.saveChecklistItem(it) },
              onToggleChecklistItem = { id, checked -> syncViewModel.toggleChecklistItem(id, checked) },
              onDeleteChecklistItem = { syncViewModel.deleteChecklistItem(it) },
              onInitiateSync = { syncViewModel.initiateSync() },
              onTaskToggled = { syncViewModel.toggleTask(it) },
              onTaskAdded = { syncViewModel.saveTask(it) },
              onTaskDeleted = { syncViewModel.deleteTask(it) },
              onSaveMedReminder = { syncViewModel.saveMedReminder(it) },
              onToggleMedReminder = { id, enabled -> syncViewModel.toggleMedReminder(id, enabled) },
              onDeleteMedReminder = { syncViewModel.deleteMedReminder(it) },
              onUpdateDashboardProgress = { dueDateText, weeks, daysRem, countdownDays, trimester, progress ->
                syncViewModel.updateDashboardProgress(dueDateText, weeks, daysRem, countdownDays, trimester, progress)
              },
              onSaveNote = { syncViewModel.saveNote(it) },
              onDeleteNote = { syncViewModel.deleteNote(it) },
              onSaveDocument = { syncViewModel.saveDocument(it) },
              onSaveVoiceMemo = { syncViewModel.saveVoiceMemo(it) },
              onDeleteVoiceMemo = { syncViewModel.deleteVoiceMemo(it) },
              onSaveWalletCard = { syncViewModel.saveWalletCard(it) },
              onDeleteWalletCard = { syncViewModel.deleteWalletCard(it) },
              onSeedSampleWalletCards = { syncViewModel.seedSampleWalletCards() },
              onNavigateToProtocol = { currentTab = 1 },
              onNavigateToPregnancy = { currentTab = 2 },
              onNavigateToSettings = { showSettings = true },
              onNavigateToPhoneBook = { currentTab = 3 },
              careProviders = careProvidersState,
              onSaveCareProvider = { syncViewModel.saveCareProvider(it) },
              theme = activeTheme,
              language = currentLanguage
            )
            1 -> ProtocolScreen(
              protocolSteps = protocolStepsState,
              immediateTasks = tasksState,
              activePartnerProfile = profile.activePartnerProfile,
              isBetaHcgConfirmed = profile.isBetaHcgConfirmed,
              onToggleBetaHcg = { confirmed ->
                syncViewModel.updateStageViewMode(if (confirmed) "PREGNANCY" else "IVF", confirmed)
              },
              onSaveStep = { syncViewModel.saveProtocolStep(it) },
              onToggleStep = { syncViewModel.toggleProtocolStep(it) },
              onDeleteStep = { syncViewModel.deleteProtocolStep(it) },
              onSaveImmediateTask = { syncViewModel.saveTask(it) },
              onToggleImmediateTask = { syncViewModel.toggleTask(it) },
              onDeleteImmediateTask = { syncViewModel.deleteTask(it) },
              onResetProtocol = { syncViewModel.resetProtocolToDefault() },
              theme = activeTheme,
              language = currentLanguage
            )
            2 -> PregnancyHubScreen(
              userEmail = userEmail,
              profile = profile,
              appointments = appointmentsState,
              documents = documentsState.filter { it.source == "hub" },
              checklists = checklistsState,
              galleryViewModel = galleryViewModel,
              activePartnerProfile = profile.activePartnerProfile,
              onConfirmPregnancy = {
                syncViewModel.updateStageViewMode("PREGNANCY", true)
                syncViewModel.updatePregnancyProfile("Fatou", "28", 3, "06 Août 2027")
              },
              onUpdateProfile = { name, age, week, dueDate ->
                syncViewModel.updatePregnancyProfile(name, age, week, dueDate)
              },
              onUpdateWeek = { week -> syncViewModel.updateCurrentWeek(week) },
              onUpdateWater = { glasses -> syncViewModel.updateWaterGlasses(glasses) },
              onSaveHealthVitals = { mood, weight, water, bp, notesText ->
                syncViewModel.updateHealthVitals(mood, weight, water, bp, notesText)
              },
              onSaveAppointment = { syncViewModel.saveAppointment(it) },
              onToggleAppointment = { id, checked -> syncViewModel.toggleAppointment(id, checked) },
              onSaveDocument = { syncViewModel.saveDocument(it) },
              onSaveChecklistItem = { syncViewModel.saveChecklistItem(it) },
              onToggleChecklistItem = { id, checked -> syncViewModel.toggleChecklistItem(id, checked) },
              onNavigateToExpensePrefill = { prefill ->
                prefilledExpense = prefill
                currentTab = 4
              },
              theme = activeTheme,
              language = currentLanguage
            )
            3 -> MedicalDatabaseScreen(
              pregnancyMeds = pregnancyMedsState,
              nutritionFoods = nutritionFoodsState,
              groceryItems = groceryItemsState,
              healthIssues = healthIssuesState,
              expenses = expensesState,
              careProviders = careProvidersState,
              searchQuery = globalSearchQuery,
              onClearSearch = { globalSearchQuery = "" },
              activePartnerProfile = profile.activePartnerProfile,
              onSavePregnancyMed = { syncViewModel.savePregnancyMed(it) },
              onDeletePregnancyMed = { syncViewModel.deletePregnancyMed(it) },
              onSaveNutritionFood = { syncViewModel.saveNutritionFood(it) },
              onDeleteNutritionFood = { syncViewModel.deleteNutritionFood(it) },
              onSaveGroceryItem = { syncViewModel.saveGroceryItem(it) },
              onToggleGroceryItem = { id, checked -> syncViewModel.toggleGroceryItem(id, checked) },
              onDeleteGroceryItem = { syncViewModel.deleteGroceryItem(it) },
              onSaveHealthIssue = { syncViewModel.saveHealthIssue(it) },
              onToggleHealthIssue = { id, resolved -> syncViewModel.toggleHealthIssue(id, resolved) },
              onDeleteHealthIssue = { syncViewModel.deleteHealthIssue(it) },
              onSaveExpense = { syncViewModel.saveExpense(it) },
              onDeleteExpense = { syncViewModel.deleteExpense(it) },
              onSaveCareProvider = { syncViewModel.saveCareProvider(it) },
              onDeleteCareProvider = { syncViewModel.deleteCareProvider(it) },
              theme = activeTheme,
              language = currentLanguage
            )
            4 -> ExpenseTrackerScreen(
              expenses = expensesState,
              profile = profile,
              activePartnerProfile = profile.activePartnerProfile,
              prefilledExpense = prefilledExpense,
              onClearPrefill = { prefilledExpense = null },
              onSaveExpense = { syncViewModel.saveExpense(it) },
              onDeleteExpense = { syncViewModel.deleteExpense(it) },
              theme = activeTheme,
              language = currentLanguage
            )
          }
        }
      }
    }
  }

  if (showVoiceAssistantModal) {
    val sysCtx = "Patient: ${profile.patientName} | Active Partner: ${profile.activePartnerProfile} | Week: ${profile.gestationalWeeks} | Mode: ${profile.stageViewMode}"
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val nowStamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
    AiVoiceAssistantDialog(
      lang = currentLanguage,
      systemContext = sysCtx,
      onExecuteExpense = { act ->
        syncViewModel.saveExpense(
          SyncedExpenseRecord(
            id = "exp_${System.currentTimeMillis()}",
            title = act.title,
            amount = act.amount,
            currency = act.currency,
            category = act.category,
            phase = if (profile.stageViewMode == "PREGNANCY") "Trimestre ${((profile.gestationalWeeks / 13) + 1).coerceIn(1, 3)}" else "Phase 2 TEC",
            paymentStatus = "Paid",
            dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
            notes = act.notes,
            addedBy = profile.activePartnerProfile
          )
        )
      },
      onExecuteNote = { act ->
        syncViewModel.saveNote(
          SyncedNoteItem(
            id = "vn_${System.currentTimeMillis()}",
            title = act.title,
            content = act.content,
            category = act.category,
            timestampText = nowStamp,
            addedBy = profile.activePartnerProfile
          )
        )
      },
      onExecuteAppointment = { act ->
        syncViewModel.saveAppointment(
          SyncedAppointmentItem(
            id = "va_${System.currentTimeMillis()}",
            title = act.title,
            dateStr = act.dateStr,
            timeStr = act.timeStr,
            location = act.location,
            hasAlarm = true,
            alarmOffsetMinutes = 15L,
            addedBy = profile.activePartnerProfile
          )
        )
      },
      onExecuteMedDose = { act ->
        val match = medRemindersState.firstOrNull {
          it.medication.contains(act.medicationHint, ignoreCase = true) ||
            it.title.contains(act.medicationHint, ignoreCase = true)
        } ?: medRemindersState.firstOrNull()
        if (match != null) {
          syncViewModel.toggleMedDose(match, act.period, todayKey)
        } else {
          Toast.makeText(
            context,
            if (currentLanguage == AppLanguage.FR) "Aucun rappel médicament actif trouvé" else "No active medication reminder found",
            Toast.LENGTH_SHORT
          ).show()
        }
      },
      onDismiss = { showVoiceAssistantModal = false }
    )
  }
}
