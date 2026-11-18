package com.example.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.net.Uri
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.AudioMemoController
import com.example.data.CloudSyncSummary
import com.example.data.DateSortUtils
import com.example.data.PmaRepository
import com.example.data.SyncedAppointmentItem
import com.example.data.SyncedCareProvider
import com.example.data.SyncedChecklistItem
import com.example.data.SyncedMedReminder
import com.example.data.SyncedMedicalDocItem
import com.example.data.SyncedNoteItem
import com.example.data.SyncedProtocolStep
import com.example.data.SyncedUserProfile
import com.example.data.SyncedVoiceMemo
import com.example.data.SyncedWalletCard
import com.example.data.TaskItem
import com.example.data.WalletSyncStatus
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.theme.ThemeColors
import com.example.ui.theme.getCategoryBadgeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun formatNowFr(): String {
  val fmt = SimpleDateFormat("dd MMM yyyy 'à' HH:mm", Locale.FRENCH)
  return fmt.format(Date())
}

private fun formatShortDateFr(): String {
  val fmt = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
  return fmt.format(Date())
}

private fun shareContent(context: android.content.Context, title: String, text: String) {
  try {
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TITLE, title)
      putExtra(Intent.EXTRA_TEXT, text)
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, title)
    context.startActivity(shareIntent)
  } catch (_: Exception) {
    Toast.makeText(context, "Impossible de partager", Toast.LENGTH_SHORT).show()
  }
}

private fun formatLiveCountdown(diffMillis: Long): String {
  if (diffMillis <= 0) return "Prise due maintenant !"
  val seconds = (diffMillis / 1000) % 60
  val minutes = (diffMillis / (1000 * 60)) % 60
  val hours = (diffMillis / (1000 * 60 * 60)) % 24
  val days = diffMillis / (1000 * 60 * 60 * 24)
  return if (days > 0) {
    "${days}j ${hours}h ${minutes}m ${seconds}s"
  } else {
    "${hours}h ${minutes}m ${seconds}s"
  }
}

@Composable
fun DashboardScreen(
  userId: String,
  userEmail: String,
  syncSummary: CloudSyncSummary,
  tasks: List<TaskItem>,
  protocolSteps: List<SyncedProtocolStep>,
  appointments: List<SyncedAppointmentItem> = emptyList(),
  checklists: List<SyncedChecklistItem> = emptyList(),
  medReminders: List<SyncedMedReminder>,
  profile: SyncedUserProfile,
  notes: List<SyncedNoteItem>,
  documents: List<SyncedMedicalDocItem>,
  voiceMemos: List<SyncedVoiceMemo>,
  walletCards: List<SyncedWalletCard>,
  walletSyncStatus: WalletSyncStatus,
  externalOpenWalletCardId: String? = null,
  onClearExternalWalletCard: () -> Unit = {},
  onUpdateWalletPin: (String) -> Unit = {},
  onLogExpenseFromCard: (SyncedWalletCard) -> Unit = {},
  onUpdateStageViewMode: (String, Boolean?) -> Unit = { _, _ -> },
  onToggleMedDose: (SyncedMedReminder, String, String) -> Unit = { _, _, _ -> },
  onRefillMedPills: (String, Long) -> Unit = { _, _ -> },
  onSaveChecklistItem: (SyncedChecklistItem) -> Unit = {},
  onToggleChecklistItem: (String, Boolean) -> Unit = { _, _ -> },
  onDeleteChecklistItem: (String) -> Unit = {},
  onInitiateSync: () -> Unit,
  onTaskToggled: (TaskItem) -> Unit,
  onTaskAdded: (TaskItem) -> Unit,
  onTaskDeleted: (String) -> Unit,
  onSaveMedReminder: (SyncedMedReminder) -> Unit,
  onToggleMedReminder: (String, Boolean) -> Unit,
  onDeleteMedReminder: (String) -> Unit,
  onUpdateDashboardProgress: (dueDateText: String, weeks: Int, daysRem: Int, countdownDays: Int, trimester: String, progress: Float) -> Unit,
  onSaveNote: (SyncedNoteItem) -> Unit,
  onDeleteNote: (String) -> Unit,
  onSaveDocument: (SyncedMedicalDocItem) -> Unit,
  onSaveVoiceMemo: (SyncedVoiceMemo) -> Unit,
  onDeleteVoiceMemo: (String) -> Unit,
  onSaveWalletCard: (SyncedWalletCard) -> Unit,
  onDeleteWalletCard: (String) -> Unit,
  onSeedSampleWalletCards: () -> Unit,
  onNavigateToProtocol: () -> Unit,
  onNavigateToPregnancy: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToPhoneBook: () -> Unit = {},
  careProviders: List<SyncedCareProvider> = emptyList(),
  onSaveCareProvider: (SyncedCareProvider) -> Unit = {},
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val activePartner = profile.activePartnerProfile
  val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
  val audioController = remember(context) { AudioMemoController(context) }

  DisposableEffect(audioController) {
    onDispose {
      audioController.releaseAll()
    }
  }

  var showAddDialog by remember { mutableStateOf(false) }
  var showAddContactDialog by remember { mutableStateOf(false) }
  var editingTaskId by remember { mutableStateOf<String?>(null) }
  var showVaultDialog by remember { mutableStateOf(false) }
  var showEditDueDateDialog by remember { mutableStateOf(false) }
  var showArchivedPastItems by remember { mutableStateOf(false) }
  var isWalletExpanded by remember { mutableStateOf(false) }
  var showMedReminderDialog by remember { mutableStateOf(false) }
  var editingMedReminder by remember { mutableStateOf<SyncedMedReminder?>(null) }
  var takenRefreshTick by remember { mutableIntStateOf(0) }

  val notifPermLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      AlarmScheduler.triggerTestNotification(context)
      Toast.makeText(
        context,
        if (language == AppLanguage.FR) "Notification de test envoyée !" else "Test notification sent!",
        Toast.LENGTH_SHORT
      ).show()
    } else {
      Toast.makeText(
        context,
        if (language == AppLanguage.FR) "Autorisez les notifications dans les réglages Android." else "Enable notifications in Android Settings.",
        Toast.LENGTH_LONG
      ).show()
    }
  }

  // Live second-by-second countdown ticker
  var currentEpochTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      currentEpochTime = System.currentTimeMillis()
    }
  }

  // Next Estrace 2 mg PO dose countdown (08:00 AM & 20:00 PM)
  val nextEstraceMillis = remember(currentEpochTime) {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val targetCal = Calendar.getInstance().apply {
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (hour < 8) {
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
      } else if (hour < 20) {
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 0)
      } else {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
      }
    }
    targetCal.timeInMillis
  }

  // Synced Due Date & Gestational Progress state
  val dueDateText = profile.dashboardDueDateText
  val gestationalWeeks = profile.gestationalWeeks.toInt()
  val gestationalDaysRemainder = profile.gestationalDaysRemainder.toInt()
  val remainingCountdownDays = profile.remainingCountdownDays.toInt()
  val currentTrimester = profile.currentTrimester
  val progressPercent = profile.progressPercent.toFloat()

  // 30-Sec Voice Memo & Notepad state
  var memoTab by remember { mutableIntStateOf(0) }
  var isRecordingVoice by remember { mutableStateOf(false) }
  var recordingSeconds by remember { mutableIntStateOf(0) }
  var activeRecordingMemoId by remember { mutableStateOf("") }
  var playingMemoId by remember { mutableStateOf<String?>(null) }

  val recordAudioLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      val id = "vm_${System.currentTimeMillis()}"
      if (audioController.startRecording(id)) {
        activeRecordingMemoId = id
        isRecordingVoice = true
        Toast.makeText(
          context,
          if (language == AppLanguage.FR) "🎤 Enregistrement audio démarré (max 30s)" else "🎤 Audio recording started (max 30s)",
          Toast.LENGTH_SHORT
        ).show()
      } else {
        Toast.makeText(
          context,
          if (language == AppLanguage.FR) "Impossible d'accéder au microphone" else "Unable to access microphone",
          Toast.LENGTH_SHORT
        ).show()
      }
    } else {
      Toast.makeText(
        context,
        if (language == AppLanguage.FR) "Permission microphone refusée" else "Microphone permission denied",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  var showEditNoteDialog by remember { mutableStateOf(false) }
  var editingNoteId by remember { mutableStateOf<String?>(null) }
  var editingNoteCategory by remember { mutableStateOf("clinical") }
  var editingNoteMood by remember { mutableStateOf("") }
  var editingNoteSymptoms by remember { mutableStateOf<List<String>>(emptyList()) }
  var noteTitleInput by remember { mutableStateOf("") }
  var noteContentInput by remember { mutableStateOf("") }

  // Voice memo auto-stop at 30 seconds
  LaunchedEffect(isRecordingVoice) {
    if (isRecordingVoice) {
      recordingSeconds = 0
      while (isRecordingVoice && recordingSeconds < 30) {
        delay(1000L)
        recordingSeconds++
      }
      if (isRecordingVoice && recordingSeconds >= 30) {
        isRecordingVoice = false
        val (duration, b64) = audioController.stopRecording()
        val id = activeRecordingMemoId.ifBlank { "vm_${System.currentTimeMillis()}" }
        onSaveVoiceMemo(
          SyncedVoiceMemo(
            id = id,
            title = if (language == AppLanguage.FR) "Mémo Consultation (${duration}s)" else "Consultation Memo (${duration}s)",
            durationSec = duration,
            timestampText = formatNowFr(),
            audioBase64 = b64,
            addedBy = activePartner
          )
        )
        Toast.makeText(
          context,
          if (language == AppLanguage.FR) "🎤 Mémo vocal de ${duration}s enregistré !" else "🎤 ${duration}s voice memo saved!",
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  // Automatic suggestions popup
  var autoSuggestionPopup by remember { mutableStateOf<String?>(null) }

  var newTitle by remember { mutableStateOf("") }
  var newDate by remember { mutableStateOf("") }
  var newCategory by remember { mutableStateOf("") }
  var newDetails by remember { mutableStateOf("") }
  var enableAlarm by remember { mutableStateOf(true) }
  var alarmOffsetMinutes by remember { mutableIntStateOf(15) }

  val partitionedTasks = remember(tasks, currentEpochTime) {
    DateSortUtils.partitionUpcomingFirst(
      items = tasks,
      nowMillis = currentEpochTime,
      epochProvider = { DateSortUtils.taskEpochMillis(it) },
      completedProvider = { it.isCompleted }
    )
  }
  val upcomingTasks = partitionedTasks.upcoming
  val pastTasks = partitionedTasks.past

  // Date & Time Picker launcher for due date and gestational progress
  val openCalendarTimePicker: () -> Unit = {
    val now = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
      context,
      { _, selectedYear, selectedMonth, selectedDay ->
        val timePickerDialog = TimePickerDialog(
          context,
          { _, selectedHour, selectedMinute ->
            val selectedCal = Calendar.getInstance().apply {
              set(Calendar.YEAR, selectedYear)
              set(Calendar.MONTH, selectedMonth)
              set(Calendar.DAY_OF_MONTH, selectedDay)
              set(Calendar.HOUR_OF_DAY, selectedHour)
              set(Calendar.MINUTE, selectedMinute)
            }

            val frenchMonths = arrayOf(
              "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
              "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
            )
            val monthName = frenchMonths.getOrElse(selectedMonth) { "Mois ${selectedMonth + 1}" }
            val minuteFormatted = if (selectedMinute < 10) "0$selectedMinute" else "$selectedMinute"
            val hourFormatted = if (selectedHour < 10) "0$selectedHour" else "$selectedHour"

            val newDueDateText = "$selectedDay $monthName $selectedYear à ${hourFormatted}h$minuteFormatted"

            val diffMillis = selectedCal.timeInMillis - System.currentTimeMillis()
            val diffDays = (diffMillis / (1000L * 60 * 60 * 24)).toInt()
            val newRemainingDays = diffDays.coerceAtLeast(0)
            val newWeeks: Int
            val newDaysRem: Int
            val newProgress: Float
            val newTrimester: String
            if (diffDays > 0) {
              val elapsedDays = (280 - diffDays).coerceIn(14, 280)
              newWeeks = (elapsedDays / 7).coerceIn(2, 40)
              newDaysRem = elapsedDays % 7
              newProgress = (elapsedDays.toFloat() / 280f).coerceIn(0.05f, 1.0f)
              newTrimester = when {
                newWeeks < 13 -> "Trimestre 1"
                newWeeks < 28 -> "Trimestre 2"
                else -> "Trimestre 3"
              }
            } else {
              newWeeks = 40
              newDaysRem = 0
              newProgress = 1.0f
              newTrimester = "Terme atteint"
            }

            onUpdateDashboardProgress(
              newDueDateText,
              newWeeks,
              newDaysRem,
              newRemainingDays,
              newTrimester,
              newProgress
            )
            Toast.makeText(context, "Terme & Calendrier mis à jour : $newDueDateText (Semaine $newWeeks + $newDaysRem j)", Toast.LENGTH_LONG).show()
          },
          now.get(Calendar.HOUR_OF_DAY),
          now.get(Calendar.MINUTE),
          true
        )
        timePickerDialog.setTitle("Sélectionner l'heure du terme")
        timePickerDialog.show()
      },
      now.get(Calendar.YEAR),
      now.get(Calendar.MONTH),
      now.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.setTitle("Sélectionner la Date du Terme")
    datePickerDialog.show()
  }

  // Auto Date & Time picker for Quick Add Task / Appointment
  val openDatePickerForQuickAdd: () -> Unit = {
    val now = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
      context,
      { _, selectedYear, selectedMonth, selectedDay ->
        val timePickerDialog = TimePickerDialog(
          context,
          { _, selectedHour, selectedMinute ->
            val frenchMonths = arrayOf(
              "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
              "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
            )
            val monthName = frenchMonths.getOrElse(selectedMonth) { "Mois ${selectedMonth + 1}" }
            val minuteFormatted = if (selectedMinute < 10) "0$selectedMinute" else "$selectedMinute"
            val hourFormatted = if (selectedHour < 10) "0$selectedHour" else "$selectedHour"
            newDate = "$selectedDay $monthName $selectedYear à ${hourFormatted}h$minuteFormatted"
            showAddDialog = true
            Toast.makeText(context, if (language == AppLanguage.FR) "Date choisie : $newDate" else "Selected date: $newDate", Toast.LENGTH_SHORT).show()
          },
          now.get(Calendar.HOUR_OF_DAY),
          now.get(Calendar.MINUTE),
          true
        )
        timePickerDialog.setTitle(if (language == AppLanguage.FR) "Sélectionner l'heure" else "Select time")
        timePickerDialog.show()
      },
      now.get(Calendar.YEAR),
      now.get(Calendar.MONTH),
      now.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.setTitle(if (language == AppLanguage.FR) "Sélectionner la Date (2026)" else "Select Date (2026)")
    datePickerDialog.show()
  }



  Box(modifier = Modifier.fillMaxSize()) {
    SelectionContainer {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .background(theme.background)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
      ) {
        // 0. Digital Wallet Stack at the Top (Above RMA of New York Cabinet East Side)
        item {
          DigitalWalletStackSection(
            userId = userId,
            cards = walletCards,
            syncStatus = walletSyncStatus,
            onSaveCard = onSaveWalletCard,
            onDeleteCard = onDeleteWalletCard,
            onSeedSampleCards = onSeedSampleWalletCards,
            theme = theme,
            language = language,
            isAnchoredBottomDock = false,
            isExpanded = isWalletExpanded,
            onToggleExpanded = { isWalletExpanded = !isWalletExpanded },
            walletPin = profile.walletPin,
            userEmail = userEmail,
            externalOpenCardId = externalOpenWalletCardId,
            onClearExternalOpenCard = onClearExternalWalletCard,
            onUpdateWalletPin = onUpdateWalletPin,
            onLogExpenseFromCard = onLogExpenseFromCard,
            modifier = Modifier.fillMaxWidth()
          )
        }

        // 0B. Rolling 3-Slot Live Countdown Queue directly below the Digital Wallet
        item {
          RollingCountdownQueueSection(
            lang = language,
            protocolSteps = protocolSteps,
            appointments = appointments,
            tasks = tasks,
            onSlotClick = { item ->
              if (item.sourceType == "PROTOCOL") onNavigateToProtocol() else onNavigateToPregnancy()
            }
          )
        }

        // 1. Patient Profile Header
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                  painter = painterResource(id = R.drawable.white_leather_bf),
                  contentDescription = "B&F Logo",
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp)),
                  contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "${if (language == AppLanguage.FR) "Dossier" else "File"} : ${PmaRepository.dossierId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.mutedText,
                    letterSpacing = 0.5.sp
                  )
                  Text(
                    text = PmaRepository.patientName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryText
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFDCFCE7))
              ) {
                Text(
                  text = Strings.t("active_fet", language),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF16A34A),
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(theme.border)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = PmaRepository.clinicName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = theme.primaryText
                )
                Text(
                  text = PmaRepository.clinicAddress,
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText
                )
                Text(
                  text = if (language == AppLanguage.FR) {
                    "Médecin Référent : ${PmaRepository.doctorName} • ${PmaRepository.embryologistInfo}"
                  } else {
                    "Referring Physician: ${PmaRepository.doctorName} • ${PmaRepository.embryologistInfo}"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { isWalletExpanded = !isWalletExpanded },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("dashboard_jump_wallet_button")
                ) {
                  Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", tint = theme.accent, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("${walletCards.size}", style = MaterialTheme.typography.labelSmall, color = theme.primaryText)
                }
                OutlinedButton(
                  onClick = { showVaultDialog = true },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("dashboard_vault_button")
                ) {
                  Icon(Icons.Default.Shield, contentDescription = "Coffre", tint = theme.accent, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("${documents.size}", style = MaterialTheme.typography.labelSmall, color = theme.primaryText)
                }
                OutlinedButton(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(PmaRepository.dossierId))
                    Toast.makeText(context, if (language == AppLanguage.FR) "Numéro de dossier copié !" else "File ID copied!", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("dashboard_copy_dossier_button")
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = theme.accent, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (language == AppLanguage.FR) "Copier" else "Copy", style = MaterialTheme.typography.labelSmall, color = theme.primaryText)
                }
              }
            }
          }
        }
      }

      // 1A-2. FERTILITY PROTOCOL / PREGNANCY STAGE SWITCHER & PROGRESS CARD
      item {
        val isPregMode = profile.stageViewMode == "PREGNANCY"
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                FilterChip(
                  selected = !isPregMode,
                  onClick = { onUpdateStageViewMode("IVF", null) },
                  label = {
                    Text(
                      text = Strings.t("stage_mode_ivf", language),
                      maxLines = 1,
                      softWrap = false,
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
                )
                FilterChip(
                  selected = isPregMode,
                  onClick = { onUpdateStageViewMode("PREGNANCY", null) },
                  label = {
                    Text(
                      text = Strings.t("stage_mode_preg", language),
                      maxLines = 1,
                      softWrap = false,
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
                )
              }
              FilterChip(
                selected = profile.isBetaHcgConfirmed,
                onClick = { onUpdateStageViewMode(profile.stageViewMode, !profile.isBetaHcgConfirmed) },
                label = {
                  Text(
                    text = if (profile.isBetaHcgConfirmed) {
                      if (language == AppLanguage.FR) "Bêta-hCG +" else "Beta-hCG +"
                    } else {
                      if (language == AppLanguage.FR) "Attente Bêta" else "Awaiting Beta"
                    },
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              )
              FilterChip(
                selected = false,
                onClick = onNavigateToPhoneBook,
                leadingIcon = {
                  Text("📞", fontSize = 12.sp)
                },
                label = {
                  Text(
                    text = if (language == AppLanguage.FR) "Répertoire" else "Contacts",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              )
            }
          }

          if (!isPregMode) {
            ProtocolCycleProgressCard(
              protocolSteps = protocolSteps,
              onNavigateToProtocol = onNavigateToProtocol,
              theme = theme,
              language = language
            )
          } else {
            PregnancyWeekProgressStageCard(
              gestationalWeeks = gestationalWeeks,
              gestationalDaysRemainder = gestationalDaysRemainder,
              remainingCountdownDays = remainingCountdownDays,
              dueDateText = dueDateText,
              progressPercent = progressPercent,
              isBetaHcgConfirmed = profile.isBetaHcgConfirmed,
              onNavigateToPregnancy = onNavigateToPregnancy,
              theme = theme,
              language = language
            )
          }
        }
      }

      // 1B. PROMINENT ESTRACE 2 MG PO (AM & PM) CLINICAL DIRECTIVE CARD WITH DAILY CHECKBOXES & REFILL
      item {
        val estraceReminder = remember(medReminders) {
          medReminders.firstOrNull {
            it.medication.contains("estrace", ignoreCase = true) ||
              it.title.contains("estrace", ignoreCase = true)
          }
        }
        val amChecked = estraceReminder != null && estraceReminder.lastDoseDateStr == todayKey && estraceReminder.dailyAmTaken
        val pmChecked = estraceReminder != null && estraceReminder.lastDoseDateStr == todayKey && estraceReminder.dailyPmTaken
        val pillsLeft = estraceReminder?.pillsRemaining ?: 60L

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.5.dp, theme.accent)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = theme.accent.copy(alpha = 0.16f),
                  modifier = Modifier.size(34.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(text = "💊", fontSize = 18.sp)
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = if (language == AppLanguage.FR) "Protocole Actif : Estrace 2 mg PO" else "Active Protocol: Estrace 2 mg PO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                  Text(
                    text = if (language == AppLanguage.FR) "Dr. Brauer • RMA of New York • Matin & Soir" else "Dr. Brauer • RMA of New York • AM & PM",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
              ) {
                Text(
                  text = if (language == AppLanguage.FR) "EN COURS" else "ACTIVE",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = Color(0xFF2563EB),
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = theme.background,
              border = BorderStroke(1.dp, theme.border)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = if (language == AppLanguage.FR) "📋 Consigne Clinique Directe :" else "📋 Direct Clinical Directive:",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = theme.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "« Begin Estrace 2 mg PO twice daily (AM and PM) »",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = if (language == AppLanguage.FR)
                    "Prendre 1 comprimé de 2 mg par voie orale le matin (08:00) et 1 comprimé le soir (20:00). Indispensable pour la préparation endométriale optimale."
                  else
                    "Take 1 tablet of 2 mg orally in the morning (08:00 AM) and 1 tablet in the evening (08:00 PM). Essential for endometrial preparation.",
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText,
                  lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Daily AM & PM Checkboxes + Pill Supply Counter + Refill
                if (estraceReminder != null) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    FilterChip(
                      selected = amChecked,
                      onClick = { onToggleMedDose(estraceReminder, "AM", todayKey) },
                      leadingIcon = if (amChecked) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                      } else null,
                      label = {
                        Text(
                          text = if (language == AppLanguage.FR) "Matin (AM)" else "Morning (AM)",
                          maxLines = 1,
                          softWrap = false,
                          style = MaterialTheme.typography.labelSmall
                        )
                      }
                    )
                    FilterChip(
                      selected = pmChecked,
                      onClick = { onToggleMedDose(estraceReminder, "PM", todayKey) },
                      leadingIcon = if (pmChecked) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                      } else null,
                      label = {
                        Text(
                          text = if (language == AppLanguage.FR) "Soir (PM)" else "Evening (PM)",
                          maxLines = 1,
                          softWrap = false,
                          style = MaterialTheme.typography.labelSmall
                        )
                      }
                    )
                    Spacer(Modifier.weight(1f))
                    AssistChip(
                      onClick = {
                        onRefillMedPills(estraceReminder.id, pillsLeft + 30L)
                        Toast.makeText(
                          context,
                          if (language == AppLanguage.FR) "+30 comprimés ajoutés au stock" else "+30 pills refilled",
                          Toast.LENGTH_SHORT
                        ).show()
                      },
                      label = {
                        Text(
                          text = "$pillsLeft ${if (language == AppLanguage.FR) "rest. +30" else "left +30"}",
                          maxLines = 1,
                          softWrap = false,
                          style = MaterialTheme.typography.labelSmall
                        )
                      }
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = theme.accent.copy(alpha = 0.12f),
                  border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.3f))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (language == AppLanguage.FR)
                        "Prochaine prise dans : ${formatLiveCountdown(nextEstraceMillis - currentEpochTime)}"
                      else
                        "Next dose in: ${formatLiveCountdown(nextEstraceMillis - currentEpochTime)}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = theme.accent
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  AlarmScheduler.scheduleReminder(
                    context = context,
                    id = 8802,
                    title = "Prise Estrace 2 mg PO",
                    message = "Dr. Brauer RMA : Prendre 1 comprimé Estrace 2 mg PO (Matin 08:00 / Soir 20:00)",
                    triggerAtMillis = System.currentTimeMillis() + (60 * 1000L)
                  )
                  Toast.makeText(context, if (language == AppLanguage.FR) "⏰ Alarmes Estrace programmées pour 08:00 et 20:00 !" else "⏰ Estrace alarms set for 08:00 AM and 08:00 PM!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (language == AppLanguage.FR) "Alarme 08h & 20h" else "Alarm 8AM & 8PM", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
              }

              OutlinedButton(
                onClick = {
                  clipboardManager.setText(AnnotatedString("Protocole RMA Dr. Brauer : Begin Estrace 2 mg PO twice daily (AM and PM)"))
                  Toast.makeText(context, if (language == AppLanguage.FR) "Consigne Estrace copiée !" else "Estrace directive copied!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.border),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (language == AppLanguage.FR) "Copier" else "Copy", style = MaterialTheme.typography.labelSmall, color = theme.primaryText)
              }
            }
          }
        }
      }

      // 1C. LOCAL MEDICATION NOTIFICATIONS & REMINDERS SCHEDULE
      item {
        MedicationRemindersCard(
          reminders = medReminders,
          todayKey = todayKey,
          takenRefreshTick = takenRefreshTick,
          onToggleMedDose = onToggleMedDose,
          onRefillMedPills = onRefillMedPills,
          onMarkTakenNow = { reminder ->
            AlarmScheduler.markMedicationTakenNow(context, reminder.id)
            takenRefreshTick++
            Toast.makeText(
              context,
              if (language == AppLanguage.FR) "✓ ${reminder.title} marqué comme pris !" else "✓ ${reminder.title} marked taken!",
              Toast.LENGTH_SHORT
            ).show()
          },
          onToggleReminder = { reminder ->
            onToggleMedReminder(reminder.id, reminder.isEnabled)
          },
          onEditReminder = { reminder ->
            editingMedReminder = reminder
            showMedReminderDialog = true
          },
          onDeleteReminder = { reminder ->
            onDeleteMedReminder(reminder.id)
          },
          onAddReminder = {
            editingMedReminder = null
            showMedReminderDialog = true
          },
          onTestNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
              ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
              notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              val sent = AlarmScheduler.triggerTestNotification(context)
              Toast.makeText(
                context,
                if (sent) {
                  if (language == AppLanguage.FR) "🔔 Notification de rappel envoyée !" else "🔔 Test medication notification sent!"
                } else {
                  if (language == AppLanguage.FR) "Activez les notifications pour tester." else "Enable notifications to test."
                },
                Toast.LENGTH_SHORT
              ).show()
            }
          },
          theme = theme,
          language = language
        )
      }

      // 1D. COMPACT PARTNER TO-DO CHECKLIST WIDGET
      item {
        PartnerTodoChecklistCard(
          items = checklists.filter { it.listKey == "partner_todo" },
          activePartner = activePartner,
          onToggleItem = onToggleChecklistItem,
          onAddItem = { label ->
            onSaveChecklistItem(
              SyncedChecklistItem(
                id = "pt_${System.currentTimeMillis()}",
                listKey = "partner_todo",
                label = label,
                isChecked = false,
                sortOrder = checklists.size.toLong(),
                addedBy = activePartner
              )
            )
          },
          onDeleteItem = onDeleteChecklistItem,
          theme = theme,
          language = language
        )
      }

      // 1E. PROMINENT MEDICAL PHONE BOOK & EMERGENCY SPEED DIAL CARD (Requirement 2 & 3)
      item {
        val emergencyOrTopContacts = remember(careProviders) {
          val emergencies = careProviders.filter { it.isEmergency }
          if (emergencies.isNotEmpty()) emergencies.take(4)
          else careProviders.take(4)
        }

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_phonebook_card"),
          shape = RoundedCornerShape(20.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Header Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = theme.accent.copy(alpha = 0.14f),
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Phone,
                      contentDescription = null,
                      tint = theme.accent,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Column {
                  Text(
                    text = if (language == AppLanguage.FR) "Répertoire Médical & Urgences" else "Medical Team & Emergency Contacts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                  Text(
                    text = if (language == AppLanguage.FR) "Appels rapides 24/7 • Scan de cartes OCR" else "24/7 direct dial • OCR Card scanner",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = theme.accent.copy(alpha = 0.1f),
                border = BorderStroke(0.5.dp, theme.accent.copy(alpha = 0.4f))
              ) {
                Text(
                  text = "${careProviders.size} ${if (language == AppLanguage.FR) "contacts" else "saved"}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = theme.accent,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            // Quick Speed Dial Chips for Emergency / Key Contacts
            if (emergencyOrTopContacts.isNotEmpty()) {
              Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                emergencyOrTopContacts.forEach { provider ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = theme.background,
                    border = BorderStroke(0.5.dp, theme.border),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Text(
                          text = when (provider.category.uppercase()) {
                            "MEDICAL" -> "🩺"
                            "CLINIC" -> "🏥"
                            "PHARMACY" -> "💊"
                            "FAMILY" -> "👨‍👩‍👦"
                            else -> "👤"
                          },
                          fontSize = 18.sp
                        )
                        Column {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                              text = provider.name,
                              style = MaterialTheme.typography.bodySmall,
                              fontWeight = FontWeight.Bold,
                              color = theme.primaryText,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                            )
                            if (provider.isEmergency) {
                              Spacer(modifier = Modifier.width(4.dp))
                              Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEE2E2)
                              ) {
                                Text(
                                  text = "URG",
                                  style = MaterialTheme.typography.labelSmall,
                                  fontSize = 9.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = Color(0xFFDC2626),
                                  modifier = Modifier.padding(horizontal = 4.dp)
                                )
                              }
                            }
                          }
                          Text(
                            text = "${provider.providerType}${if (provider.affiliation.isNotBlank()) " • ${provider.affiliation}" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }

                      // Direct 1-Click Dial Button
                      Button(
                        onClick = {
                          try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                          } catch (_: Exception) {
                            Toast.makeText(context, "Numéroteur indisponible", Toast.LENGTH_SHORT).show()
                          }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                          .height(32.dp)
                          .testTag("dashboard_call_${provider.id}")
                      ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == AppLanguage.FR) "Appeler" else "Call", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }

            // Action Row: + Nouveau Contact (with OCR scanner) & Consulter tout le répertoire
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { showAddContactDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 40.dp)
                  .testTag("dashboard_card_add_contact_button")
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (language == AppLanguage.FR) "+ Contact / Scan" else "+ Contact / Scan",
                  color = Color.White,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold
                )
              }

              OutlinedButton(
                onClick = onNavigateToPhoneBook,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.5f)),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 40.dp)
                  .testTag("dashboard_card_open_phonebook_button")
              ) {
                Text(
                  text = if (language == AppLanguage.FR) "Tout voir →" else "View All →",
                  color = theme.accent,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // 2. CENTRAL MILESTONE TRACKER: Développement du Bébé & Progression & Countdown (Editable)
      item {
        val (fetalFruit, fetalSize, fetalDesc) = when (gestationalWeeks) {
          in 2..4 -> Triple("🌱 Graine de pavot", "0.2 cm • < 1 g", "Nidation & premières divisions cellulaires")
          in 5..7 -> Triple("🫐 Myrtille", "1.2 cm • 1 g", "Cœur qui bat & formation du tube neural")
          in 8..10 -> Triple("🍓 Fraise", "3.1 cm • 4 g", "Doigts et orteils formés, début des réflexes")
          in 11..13 -> Triple("🍋 Citron vert", "7.4 cm • 23 g", "Déglutition du liquide amniotique, profil visible")
          in 14..15 -> Triple("🍎 Pomme", "9.3 cm • 70 g", "Mouvements spontanés & perception lumineuse")
          in 16..17 -> Triple("🥑 Bel Avocat", "11.6 cm • 100 g", "Oreille interne formée & perception des voix des parents")
          in 18..20 -> Triple("🥭 Mangue", "15.3 cm • 240 g", "Premiers coups de pied ressentis (quickening)")
          in 21..24 -> Triple("🌽 Épi de maïs", "28.5 cm • 500 g", "Empreintes digitales et sommeil rythmé")
          in 25..28 -> Triple("🍆 Aubergine", "35.6 cm • 1000 g", "Ouverture des yeux & surfactant pulmonaire")
          in 29..32 -> Triple("🥥 Noix de coco", "42.4 cm • 1700 g", "Développement cérébral rapide & gain de poids")
          in 33..36 -> Triple("🍍 Ananas", "47.4 cm • 2600 g", "Position tête en bas & système immunitaire actif")
          else -> Triple("🍉 Pastèque", "51.2 cm • 3400 g", "Bébé à terme prêt pour la rencontre magique !")
        }

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { openCalendarTimePicker() }
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = theme.accent.copy(alpha = 0.16f),
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(text = "👶", fontSize = 20.sp)
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = Strings.t("baby_dev_title", language),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = theme.primaryText
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                      Icons.Default.CalendarMonth,
                      contentDescription = "Changer la date",
                      tint = theme.accent,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Text(
                    text = "Semaine $gestationalWeeks + $gestationalDaysRemainder j • $currentTrimester • $remainingCountdownDays j restants",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                }
              }

              // Direct "Changer Date & Heure" Button
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = theme.accent.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, theme.accent),
                onClick = { openCalendarTimePicker() }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.EditCalendar,
                    contentDescription = "Edit due date",
                    tint = theme.accent,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Calendrier",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.accent
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Size comparison highlight
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = theme.background,
              border = BorderStroke(1.dp, theme.border)
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                val emoji = fetalFruit.takeWhile { !it.isLetter() }.trim()
                val fruitName = fetalFruit.dropWhile { !it.isLetter() }.trim()
                Text(text = if (emoji.isNotBlank()) emoji else "🥑", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Taille d'un $fruitName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryText
                  )
                  Text(
                    text = fetalSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                  Text(
                    text = "👂 $fetalDesc",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.accent,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Countdown & Progress Bar (Clickable to open calendar and time picker)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { openCalendarTimePicker() }
                .padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Progression : ${(progressPercent * 100).toInt()}% • $remainingCountdownDays j restants",
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Terme : $dueDateText",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = theme.accent
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
              progress = { progressPercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = theme.accent,
              trackColor = theme.border
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
              onClick = onNavigateToPregnancy,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(vertical = 6.dp),
              border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.5f)),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primaryText)
            ) {
              Icon(Icons.Default.Visibility, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Explorer le Modèle 3D du Bébé 👶",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 3. DAILY NOTES & SYMPTOMS / EMOTIONAL STATE LOG (JOURNAL DU TRAITEMENT)
      item {
        var dailyMood by remember { mutableStateOf("🧘‍♀️ Calme") }
        var dailyNoteText by remember { mutableStateOf("") }
        val selectedSymptoms = remember { mutableStateListOf<String>() }
        val symptomsOptions = listOf("Fatigue", "Tiraillements", "Nausées", "Insomnie", "Maux de tête", "Sereine")
        val moods = listOf("🧘‍♀️ Calme", "🌸 Sereine", "😊 Confiante", "😴 Fatiguée", "🥺 Émotive", "💪 Motivée")

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = theme.accent.copy(alpha = 0.15f),
                  modifier = Modifier.size(34.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text("📝", fontSize = 18.sp)
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = if (language == AppLanguage.FR) "Journal & Notes Quotidiennes" else "Daily Notes & Symptoms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                  Text(
                    text = if (language == AppLanguage.FR) "Symptômes & état émotionnel durant le traitement" else "Log symptoms & emotional state during treatment",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(100.dp),
                color = theme.accent.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, theme.accent)
              ) {
                Text(
                  text = "08 Nov 2026",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = theme.accent,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Emotional state selector
            Text(
              text = if (language == AppLanguage.FR) "État émotionnel du jour :" else "Today's Emotional State:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              moods.take(4).forEach { mood ->
                val isSelected = dailyMood == mood
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) theme.accent else theme.background,
                  border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { dailyMood = mood }
                ) {
                  Text(
                    text = mood,
                    modifier = Modifier.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else theme.primaryText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Symptoms multi-select chips
            Text(
              text = if (language == AppLanguage.FR) "Symptômes observés :" else "Observed Symptoms:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              symptomsOptions.take(4).forEach { sym ->
                val isChecked = selectedSymptoms.contains(sym)
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isChecked) theme.accent.copy(alpha = 0.18f) else theme.background,
                  border = BorderStroke(1.dp, if (isChecked) theme.accent else theme.border),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      if (isChecked) selectedSymptoms.remove(sym) else selectedSymptoms.add(sym)
                    }
                ) {
                  Text(
                    text = sym,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isChecked) theme.accent else theme.primaryText,
                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Note text field
            OutlinedTextField(
              value = dailyNoteText,
              onValueChange = { dailyNoteText = it },
              label = { Text(if (language == AppLanguage.FR) "Note du jour & ressentis" else "Daily reflection & feelings", style = MaterialTheme.typography.bodySmall) },
              placeholder = { Text(if (language == AppLanguage.FR) "ex: Prise Estrace effectuée, sensation de calme..." else "e.g. Took Estrace, feeling peaceful...", style = MaterialTheme.typography.bodySmall) },
              modifier = Modifier.fillMaxWidth(),
              minLines = 2,
              maxLines = 4,
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = {
                val symptomStr = if (selectedSymptoms.isNotEmpty()) selectedSymptoms.joinToString(", ") else "Aucun symptôme majeur"
                val fullContent = "Humeur: $dailyMood • Symptômes: $symptomStr\n${if (dailyNoteText.isNotBlank()) dailyNoteText else "R.A.S."}"
                onSaveNote(
                  SyncedNoteItem(
                    id = "daily_${System.currentTimeMillis()}",
                    title = if (language == AppLanguage.FR) "Note Quotidienne ($dailyMood)" else "Daily Note ($dailyMood)",
                    content = fullContent,
                    category = "daily",
                    timestampText = formatNowFr(),
                    mood = dailyMood,
                    symptoms = selectedSymptoms.toList(),
                    addedBy = activePartner
                  )
                )
                dailyNoteText = ""
                selectedSymptoms.clear()
                Toast.makeText(context, if (language == AppLanguage.FR) "Note quotidienne enregistrée & synchronisée !" else "Daily note saved & synced!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_save_daily_note_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              contentPadding = PaddingValues(vertical = 10.dp)
            ) {
              Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (language == AppLanguage.FR) "Enregistrer la Note Quotidienne" else "Save Daily Note", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 4B. 30-SEC VOICE MEMO & CLINICAL NOTEPAD MODULE
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            // Header with Tab Switcher
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = theme.accent.copy(alpha = 0.15f),
                  modifier = Modifier.size(34.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      if (memoTab == 0) Icons.Default.Mic else Icons.Default.EditNote,
                      contentDescription = null,
                      tint = theme.accent,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = if (language == AppLanguage.FR) "Mémos Vocaux (30s) & Bloc-Notes" else "Voice Memos (30s) & Notepad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryText
                  )
                  Text(
                    text = if (language == AppLanguage.FR) "Enregistreur plafonné 30s & Notes médicales" else "30s audio recorder & clinical notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two tabs: Voice Memo vs Clinical Notepad
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { memoTab = 0 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (memoTab == 0) theme.accent else theme.background
                ),
                border = BorderStroke(1.dp, if (memoTab == 0) theme.accent else theme.border),
                contentPadding = PaddingValues(vertical = 8.dp)
              ) {
                Icon(
                  Icons.Default.Mic,
                  contentDescription = null,
                  tint = if (memoTab == 0) Color.White else theme.primaryText,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  if (language == AppLanguage.FR) "Mémo Vocal" else "Voice Memo",
                  color = if (memoTab == 0) Color.White else theme.primaryText,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                )
              }

              Button(
                onClick = { memoTab = 1 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (memoTab == 1) theme.accent else theme.background
                ),
                border = BorderStroke(1.dp, if (memoTab == 1) theme.accent else theme.border),
                contentPadding = PaddingValues(vertical = 8.dp)
              ) {
                Icon(
                  Icons.Default.EditNote,
                  contentDescription = null,
                  tint = if (memoTab == 1) Color.White else theme.primaryText,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  if (language == AppLanguage.FR) "Bloc-Notes" else "Notepad",
                  color = if (memoTab == 1) Color.White else theme.primaryText,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (memoTab == 0) {
              // VOICE RECORDER SECTION (Capped at 30 seconds)
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = theme.background,
                border = BorderStroke(1.dp, if (isRecordingVoice) theme.accent else theme.border)
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  // Timer and status
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .background(
                            if (isRecordingVoice) Color(0xFFEF4444) else theme.mutedText,
                            CircleShape
                          )
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = if (isRecordingVoice) "Enregistrement en cours..." else "Prêt à enregistrer (max 30s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRecordingVoice) Color(0xFFEF4444) else theme.mutedText,
                        fontWeight = FontWeight.Medium
                      )
                    }

                    Text(
                      text = "${if (recordingSeconds < 10) "00:0$recordingSeconds" else "00:$recordingSeconds"} / 00:30",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = if (isRecordingVoice) theme.accent else theme.primaryText
                    )
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // Live animated audio waveform simulation
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(36.dp)
                      .background(theme.cardSurface, RoundedCornerShape(8.dp))
                      .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    val barCount = 18
                    for (i in 0 until barCount) {
                      val barHeight = if (isRecordingVoice) {
                        val factor = ((i + recordingSeconds) % 5 + 1) * 6
                        factor.dp
                      } else {
                        6.dp
                      }
                      Box(
                        modifier = Modifier
                          .width(3.dp)
                          .height(barHeight)
                          .background(
                            if (isRecordingVoice) theme.accent else theme.border,
                            RoundedCornerShape(2.dp)
                          )
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // Action Buttons (Start / Stop with real microphone capture)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                  ) {
                    if (!isRecordingVoice) {
                      Button(
                        onClick = {
                          if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            val id = "vm_${System.currentTimeMillis()}"
                            if (audioController.startRecording(id)) {
                              activeRecordingMemoId = id
                              isRecordingVoice = true
                              Toast.makeText(
                                context,
                                if (language == AppLanguage.FR) "🎤 Enregistrement audio démarré (max 30s)" else "🎤 Recording started (max 30s)",
                                Toast.LENGTH_SHORT
                              ).show()
                            } else {
                              Toast.makeText(
                                context,
                                if (language == AppLanguage.FR) "Impossible d'ouvrir le microphone" else "Could not open microphone",
                                Toast.LENGTH_SHORT
                              ).show()
                            }
                          } else {
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                          }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                      ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          if (language == AppLanguage.FR) "Démarrer Mémo (30s)" else "Record Memo (30s)",
                          color = Color.White,
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    } else {
                      Button(
                        onClick = {
                          isRecordingVoice = false
                          val (duration, b64) = audioController.stopRecording()
                          val id = activeRecordingMemoId.ifBlank { "vm_${System.currentTimeMillis()}" }
                          onSaveVoiceMemo(
                            SyncedVoiceMemo(
                              id = id,
                              title = if (language == AppLanguage.FR) "Mémo Consultation (${duration}s)" else "Consultation Memo (${duration}s)",
                              durationSec = duration,
                              timestampText = formatNowFr(),
                              audioBase64 = b64,
                              addedBy = activePartner
                            )
                          )
                          Toast.makeText(
                            context,
                            if (language == AppLanguage.FR) "✅ Mémo audio sauvegardé & synchronisé !" else "✅ Audio memo saved & synced!",
                            Toast.LENGTH_SHORT
                          ).show()
                        },
                        modifier = Modifier
                          .heightIn(min = 48.dp)
                          .testTag("dashboard_stop_voice_memo_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                      ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          if (language == AppLanguage.FR) "Arrêter & Sauvegarder" else "Stop & Save",
                          color = Color.White,
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // List of saved voice memos
              Text(
                if (language == AppLanguage.FR) "Mémos Enregistrés (${voiceMemos.size}) :" else "Recorded Memos (${voiceMemos.size}):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
              Spacer(modifier = Modifier.height(6.dp))

              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                voiceMemos.forEach { memo ->
                  val isPlaying = playingMemoId == memo.id
                  Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.background,
                    border = BorderStroke(0.5.dp, theme.border)
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Surface(
                          shape = CircleShape,
                          color = theme.accent.copy(alpha = 0.15f),
                          modifier = Modifier
                            .size(40.dp)
                            .clickable {
                              if (isPlaying) {
                                audioController.stopPlayback()
                                playingMemoId = null
                              } else {
                                val started = audioController.playMemo(
                                  memo = memo,
                                  onCompletion = { playingMemoId = null },
                                  onError = {
                                    playingMemoId = null
                                    Toast.makeText(
                                      context,
                                      if (language == AppLanguage.FR) "Aucun audio enregistré pour ce mémo" else "No audio stream stored for this memo",
                                      Toast.LENGTH_SHORT
                                    ).show()
                                  }
                                )
                                if (started) {
                                  playingMemoId = memo.id
                                }
                              }
                            }
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                              contentDescription = if (language == AppLanguage.FR) "Lire" else "Play",
                              tint = theme.accent,
                              modifier = Modifier.size(20.dp)
                            )
                          }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                          Text(text = memo.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = theme.primaryText)
                          Text(
                            text = "${memo.timestampText} • 00:${if (memo.durationSec < 10) "0${memo.durationSec}" else "${memo.durationSec}"} • ${memo.addedBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.mutedText
                          )
                        }
                      }

                      Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                          onClick = {
                            shareContent(
                              context = context,
                              title = memo.title,
                              text = "🎙️ Mémo Vocal : ${memo.title}\nDate : ${memo.timestampText}\nDurée : ${memo.durationSec}s"
                            )
                          },
                          modifier = Modifier.size(36.dp)
                        ) {
                          Icon(Icons.Default.Share, contentDescription = if (language == AppLanguage.FR) "Partager" else "Share", tint = theme.accent, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                          onClick = {
                            if (isPlaying) {
                              audioController.stopPlayback()
                              playingMemoId = null
                            }
                            onDeleteVoiceMemo(memo.id)
                            Toast.makeText(context, if (language == AppLanguage.FR) "Mémo supprimé" else "Memo deleted", Toast.LENGTH_SHORT).show()
                          },
                          modifier = Modifier.size(36.dp)
                        ) {
                          Icon(Icons.Default.Delete, contentDescription = if (language == AppLanguage.FR) "Supprimer" else "Delete", tint = theme.mutedText, modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }
                }
              }
            } else {
              // CLINICAL NOTEPAD SECTION (Rich Text Editor + Edit + Delete + Share)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Notes Médicales (${notes.size}) :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
                Button(
                  onClick = {
                    editingNoteId = null
                    editingNoteCategory = "clinical"
                    editingNoteMood = ""
                    editingNoteSymptoms = emptyList()
                    noteTitleInput = ""
                    noteContentInput = ""
                    showEditNoteDialog = true
                  },
                  modifier = Modifier.testTag("dashboard_add_note_button"),
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Nouvelle Note", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                notes.forEach { note ->
                  Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.background,
                    border = BorderStroke(0.5.dp, theme.border)
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = note.title,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = theme.primaryText,
                          modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          IconButton(
                            onClick = {
                              editingNoteId = note.id
                              editingNoteCategory = note.category
                              editingNoteMood = note.mood
                              editingNoteSymptoms = note.symptoms
                              noteTitleInput = note.title
                              noteContentInput = note.content
                              showEditNoteDialog = true
                            },
                            modifier = Modifier.size(28.dp)
                          ) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = theme.accent, modifier = Modifier.size(16.dp))
                          }
                          IconButton(
                            onClick = {
                              shareContent(
                                context = context,
                                title = note.title,
                                text = "📝 Note Médicale : ${note.title}\nDate : ${note.timestampText}\n\n${note.content}\n\n— Carnet Grossesse & PMA 2026"
                              )
                            },
                            modifier = Modifier.size(28.dp)
                          ) {
                            Icon(Icons.Default.Share, contentDescription = "Partager", tint = theme.accent, modifier = Modifier.size(16.dp))
                          }
                          IconButton(
                            onClick = {
                              onDeleteNote(note.id)
                              Toast.makeText(context, "Note supprimée", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                          ) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = theme.mutedText, modifier = Modifier.size(16.dp))
                          }
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.primaryText,
                        lineHeight = 18.sp
                      )
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(
                        text = "${note.timestampText} • ${if (language == AppLanguage.FR) "Par" else "By"} ${note.addedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.mutedText,
                        fontSize = 11.sp
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 5. Prochaines Actions & Rendez-vous
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (language == AppLanguage.FR) "Prochaines Actions & Rendez-vous" else "Upcoming Actions & Appointments",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
            Text(
              text = "${upcomingTasks.size} action(s) active(s)",
              style = MaterialTheme.typography.labelSmall,
              color = theme.mutedText
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          FilledTonalButton(
            onClick = {
              editingTaskId = null
              newTitle = ""
              if (newDate.isBlank()) newDate = "13 Novembre 2026 à 14:00"
              newCategory = ""
              newDetails = ""
              showAddDialog = true
            },
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("dashboard_add_upcoming_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = theme.accent.copy(alpha = 0.14f),
              contentColor = theme.accent
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (language == AppLanguage.FR) "Ajouter" else "Add",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      if (upcomingTasks.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Toutes les étapes en cours sont à jour !" else "All current steps are up to date!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = theme.primaryText
              )
            }
          }
        }
      } else {
        items(upcomingTasks) { task ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.Top
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, theme.border),
                modifier = Modifier
                  .size(24.dp)
                  .padding(top = 2.dp),
                onClick = { onTaskToggled(task) }
              ) {
                Box(contentAlignment = Alignment.Center) {}
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryText
                  )
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = theme.background,
                    border = BorderStroke(0.5.dp, theme.border)
                  ) {
                    Text(
                      text = task.date,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.mutedText
                    )
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = task.details,
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText,
                  lineHeight = 18.sp
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    val toggled = !task.hasAlarm
                    onTaskAdded(task.copy(hasAlarm = toggled))
                    val ts = DateSortUtils.taskEpochMillis(task)
                    if (toggled && ts != Long.MAX_VALUE) {
                      AlarmScheduler.scheduleAppointmentAlarm(
                        context = context,
                        itemId = "task_${task.id}",
                        title = task.title,
                        details = "${task.date} • ${task.details}".trim(),
                        eventEpochMillis = ts,
                        offsetMinutes = task.alarmTimeMinutesBefore.toLong()
                      )
                    } else {
                      AlarmScheduler.cancelAppointmentAlarm(context, "task_${task.id}")
                    }
                    Toast.makeText(
                      context,
                      if (toggled) {
                        if (language == AppLanguage.FR) "🔔 Rappel activé" else "🔔 Alarm enabled"
                      } else {
                        if (language == AppLanguage.FR) "🔕 Rappel désactivé" else "🔕 Alarm disabled"
                      },
                      Toast.LENGTH_SHORT
                    ).show()
                  },
                  modifier = Modifier.size(40.dp)
                ) {
                  Icon(
                    imageVector = if (task.hasAlarm) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                    contentDescription = if (language == AppLanguage.FR) "Alarme" else "Alarm",
                    tint = if (task.hasAlarm) theme.accent else theme.mutedText,
                    modifier = Modifier.size(17.dp)
                  )
                }
                IconButton(
                  onClick = {
                    editingTaskId = task.id
                    newTitle = task.title
                    newDate = task.date
                    newCategory = task.category
                    newDetails = task.details
                    showAddDialog = true
                  },
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("dashboard_edit_task_${task.id}")
                ) {
                  Icon(Icons.Default.Edit, contentDescription = if (language == AppLanguage.FR) "Modifier" else "Edit", tint = theme.accent, modifier = Modifier.size(17.dp))
                }
                IconButton(
                  onClick = { onTaskDeleted(task.id) },
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("dashboard_delete_task_${task.id}")
                ) {
                  Icon(Icons.Default.Close, contentDescription = if (language == AppLanguage.FR) "Supprimer" else "Delete", tint = theme.mutedText, modifier = Modifier.size(17.dp))
                }
              }
            }
          }
        }
      }

      // 6. Collapsible "Passé" / "Past" Section
      if (pastTasks.isNotEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = theme.background,
            border = BorderStroke(0.5.dp, theme.border),
            onClick = { showArchivedPastItems = !showArchivedPastItems }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (language == AppLanguage.FR)
                  "Passé (${pastTasks.size} éléments terminés ou passés)"
                else
                  "Past (${pastTasks.size} completed or past items)",
                style = MaterialTheme.typography.bodySmall,
                color = theme.mutedText
              )
              Icon(
                imageVector = if (showArchivedPastItems) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = theme.mutedText
              )
            }
          }
        }

        if (showArchivedPastItems) {
          items(pastTasks, key = { "past_${it.id}" }) { pastTask ->
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = theme.cardSurface.copy(alpha = 0.65f),
              border = BorderStroke(0.5.dp, theme.border)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  if (pastTask.isCompleted) Icons.Default.CheckCircle else Icons.Default.History,
                  contentDescription = null,
                  tint = if (pastTask.isCompleted) Color(0xFF16A34A) else theme.mutedText,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = pastTask.title, style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
                  Text(text = "${pastTask.date} • ${pastTask.addedBy}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = theme.mutedText)
                }
              }
            }
          }
        }
      }
    }
    }

    // Floating Action Buttons: Add Contact (Requirement 2) & Quick Add Event
    AnimatedVisibility(
      visible = !listState.isScrollInProgress && !isWalletExpanded,
      enter = fadeIn() + slideInVertically { it },
      exit = fadeOut() + slideOutVertically { it },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 16.dp, bottom = 16.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Quick Add Contact FAB (Requirement 2)
        FloatingActionButton(
          onClick = { showAddContactDialog = true },
          modifier = Modifier
            .border(1.5.dp, theme.accent, RoundedCornerShape(16.dp))
            .testTag("dashboard_add_contact_fab"),
          containerColor = theme.cardSurface,
          contentColor = theme.accent,
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.PersonAdd,
              contentDescription = if (language == AppLanguage.FR) "Ajouter Contact" else "Add Contact",
              tint = theme.accent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (language == AppLanguage.FR) "+ Contact" else "+ Contact",
              fontWeight = FontWeight.Bold,
              color = theme.accent
            )
          }
        }

        // Quick Add Event FAB
        FloatingActionButton(
          onClick = {
            editingTaskId = null
            newTitle = ""
            if (newDate.isBlank()) newDate = "13 Novembre 2026 à 14:00"
            newCategory = ""
            newDetails = ""
            showAddDialog = true
          },
          modifier = Modifier.testTag("dashboard_quick_add_fab"),
          containerColor = theme.accent,
          contentColor = Color.White,
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.AddAlarm, contentDescription = Strings.t("quick_add", language))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = Strings.t("quick_add", language), fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }


  }

  // Automatic Suggestion Popup
  autoSuggestionPopup?.let { suggestionMessage ->
    AlertDialog(
      onDismissRequest = { autoSuggestionPopup = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Suggestion Intelligente", color = theme.primaryText)
        }
      },
      text = {
        SelectionContainer {
          Text(suggestionMessage, style = MaterialTheme.typography.bodyMedium, color = theme.primaryText, lineHeight = 20.sp)
        }
      },
      confirmButton = {
        Button(
          onClick = { autoSuggestionPopup = null },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Compris", color = Color.White)
        }
      }
    )
  }

  // Edit Due Date Dialog
  if (showEditDueDateDialog) {
    var tempDueDate by remember(dueDateText) { mutableStateOf(dueDateText) }
    AlertDialog(
      onDismissRequest = { showEditDueDateDialog = false },
      title = { Text("Modifier la Date de Terme", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = {
              showEditDueDateDialog = false
              openCalendarTimePicker()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ouvrir le Calendrier & Horloge", color = Color.White)
          }
          OutlinedTextField(
            value = tempDueDate,
            onValueChange = { tempDueDate = it },
            label = { Text("Ou saisie directe de la date") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (tempDueDate.isNotBlank()) {
              onUpdateDashboardProgress(
                tempDueDate,
                gestationalWeeks,
                gestationalDaysRemainder,
                remainingCountdownDays,
                currentTrimester,
                progressPercent
              )
              showEditDueDateDialog = false
              Toast.makeText(context, "Date de terme mise à jour !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Enregistrer", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDueDateDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Coffre & Documents Dialog (Synced to Firestore)
  if (showVaultDialog) {
    var newDocTitle by remember { mutableStateOf("") }
    var newDocNote by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showVaultDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Shield, contentDescription = null, tint = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text(Strings.t("vault_tile_title", language), color = theme.primaryText)
        }
      },
      text = {
        Column(
          modifier = Modifier.heightIn(max = 480.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Enregistrez vos cartes d'assurance, ordonnances ou résultats médicaux pour les retrouver sur tous vos appareils.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.mutedText
          )

          Text("Catégories Recommandées :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("💊 Ordonnance Estrace", "💳 Carte RMA", "👶 Échographie").forEach { cat ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = theme.accent.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, theme.accent),
                modifier = Modifier.clickable {
                  newDocTitle = cat
                  newDocNote = "Ajouté le ${formatShortDateFr()} • Synchronisé"
                }
              ) {
                Text(cat, style = MaterialTheme.typography.labelSmall, color = theme.accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
            }
          }

          OutlinedTextField(
            value = newDocTitle,
            onValueChange = { newDocTitle = it },
            label = { Text("Nom du document ou de la carte") },
            placeholder = { Text("ex: Carte d'Assurance Médicale RMA") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newDocNote,
            onValueChange = { newDocNote = it },
            label = { Text("Notes / Détails (ex: Ordonnance Dr. Brauer)") },
            modifier = Modifier.fillMaxWidth()
          )

          Button(
            onClick = {
              val titleToSave = if (newDocTitle.isNotBlank()) newDocTitle else "Carte d'Assurance Médicale RMA"
              val noteToSave = if (newDocNote.isNotBlank()) newDocNote else "${formatShortDateFr()} • Synchronisé"
              onSaveDocument(
                SyncedMedicalDocItem(
                  id = "vault_${System.currentTimeMillis()}",
                  title = titleToSave,
                  category = "Coffre",
                  dateStr = formatShortDateFr(),
                  summary = noteToSave,
                  source = "vault"
                )
              )
              newDocTitle = ""
              newDocNote = ""
              Toast.makeText(context, "✅ Document enregistré & synchronisé dans le coffre !", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sauvegarder dans le Coffre", color = Color.White, style = MaterialTheme.typography.bodySmall)
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text("Documents enregistrés (${documents.size}) :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)

          SelectionContainer {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              documents.forEach { doc ->
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(10.dp),
                  color = theme.background,
                  border = BorderStroke(0.5.dp, theme.border)
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(doc.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = theme.primaryText)
                      Text(doc.summary, style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                    }
                    IconButton(
                      onClick = {
                        clipboardManager.setText(AnnotatedString("${doc.title} - ${doc.summary}"))
                        Toast.makeText(context, "Copié !", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(36.dp)
                    ) {
                      Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = theme.accent, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showVaultDialog = false }) {
          Text("Fermer", color = theme.accent)
        }
      }
    )
  }

  // Edit or Add Clinical Note Dialog
  if (showEditNoteDialog) {
    AlertDialog(
      onDismissRequest = { showEditNoteDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.EditNote, contentDescription = null, tint = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (editingNoteId == null) "Nouvelle Note Médicale" else "Modifier la Note",
            color = theme.primaryText
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = noteTitleInput,
            onValueChange = { noteTitleInput = it },
            label = { Text("Titre de la note") },
            placeholder = { Text("ex: Questions consultation Dr. Brauer") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = noteContentInput,
            onValueChange = { noteContentInput = it },
            label = { Text("Contenu de la note / symptômes") },
            placeholder = { Text("Saisissez vos observations, dosages ou questions...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (noteTitleInput.isNotBlank()) {
              val currentNoteId = editingNoteId
              onSaveNote(
                SyncedNoteItem(
                  id = currentNoteId ?: "cn_${System.currentTimeMillis()}",
                  title = noteTitleInput,
                  content = noteContentInput,
                  category = editingNoteCategory,
                  timestampText = if (currentNoteId == null) formatNowFr() else "${formatNowFr()} • Modifié",
                  mood = editingNoteMood,
                  symptoms = editingNoteSymptoms,
                  addedBy = activePartner
                )
              )
              Toast.makeText(
                context,
                if (currentNoteId == null) "Note ajoutée & synchronisée !" else "Note mise à jour !",
                Toast.LENGTH_SHORT
              ).show()
              showEditNoteDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Enregistrer", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditNoteDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Dialog for Adding Task/Appointment with 7 Specialized Categories & Presets
  if (showAddDialog) {
    val specializedCategories = listOf(
      "💊 Médicaments" to "Estrace 2 mg PO (AM & PM)",
      "🩺 Rendez-vous" to "Consultation Dr. Brauer RMA",
      "📊 Monitorage" to "Monitorage folliculaire & prise de sang",
      "✨ Vitamines" to "Vitamines prénatales & Acide folique",
      "⏰ Déclenchement" to "Injection Ovitrelle 250 µg",
      "🩸 Bilan Hormonal" to "Bilan Estradiol & Progestérone",
      "👶 Échographie" to "Échographie T1 & Clarté nucale"
    )

    AlertDialog(
      onDismissRequest = {
        showAddDialog = false
        editingTaskId = null
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.EventNote, contentDescription = null, tint = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (editingTaskId != null) {
              if (language == AppLanguage.FR) "Modifier le Rendez-vous / Action" else "Edit Appointment / Action"
            } else {
              Strings.t("add_task", language)
            },
            color = theme.primaryText
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = { openDatePickerForQuickAdd() },
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.5f))
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (language == AppLanguage.FR) "Choisir Date & Heure (Calendrier)" else "Pick Date & Time (Calendar)",
              color = theme.accent,
              fontWeight = FontWeight.SemiBold
            )
          }

          Text(
            text = "7 Catégories Spécialisées (Préréglages 1-Clic) :",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )

          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(specializedCategories) { (catName, defaultTitle) ->
              val isSelected = newCategory == catName
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) theme.accent.copy(alpha = 0.15f) else theme.background,
                border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
                modifier = Modifier.clickable {
                  newCategory = catName
                  if (newTitle.isBlank()) newTitle = defaultTitle
                  if (newDate.isBlank()) {
                    newDate = "13 Novembre 2026 à 14:00"
                  }
                  Toast.makeText(context, "$catName sélectionné !", Toast.LENGTH_SHORT).show()
                }
              ) {
                Text(
                  text = catName,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) theme.accent else theme.primaryText,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text(Strings.t("title_hint", language)) },
            placeholder = { Text("ex: Estrace 2 mg PO (AM & PM)") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dashboard_add_dialog_title_input")
          )
          OutlinedTextField(
            value = newDate,
            onValueChange = { newDate = it },
            label = { Text(Strings.t("date_hint", language)) },
            placeholder = { Text("13 Novembre 2026 à 14:00") },
            trailingIcon = {
              IconButton(onClick = { openDatePickerForQuickAdd() }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Choisir la date", tint = theme.accent)
              }
            },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newCategory,
            onValueChange = { newCategory = it },
            label = { Text(Strings.t("category_hint", language)) },
            placeholder = { Text("ex: 💊 Médicaments") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newDetails,
            onValueChange = { newDetails = it },
            label = { Text(Strings.t("details_hint", language)) },
            placeholder = { Text("ex: Dr. Anate Brauer RMA, consignes...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newTitle.isNotBlank()) {
              val taskId = editingTaskId ?: "custom_${System.currentTimeMillis()}"
              val existingTask = tasks.firstOrNull { it.id == editingTaskId }
              val newTask = TaskItem(
                id = taskId,
                title = newTitle.trim(),
                date = if (newDate.isBlank()) "13 Novembre 2026 à 14:00" else newDate.trim(),
                category = if (newCategory.isBlank()) "💊 Médicaments" else newCategory.trim(),
                details = newDetails.trim(),
                isCompleted = existingTask?.isCompleted ?: false,
                hasAlarm = existingTask?.hasAlarm ?: enableAlarm,
                alarmTimeMinutesBefore = existingTask?.alarmTimeMinutesBefore ?: alarmOffsetMinutes,
                addedBy = existingTask?.addedBy ?: activePartner
              )
              onTaskAdded(newTask)
              val targetTs = DateSortUtils.taskEpochMillis(newTask)
              if (newTask.hasAlarm && targetTs != Long.MAX_VALUE) {
                AlarmScheduler.scheduleAppointmentAlarm(
                  context = context,
                  itemId = "task_${newTask.id}",
                  title = newTask.title,
                  details = "${newTask.date} • ${newTask.details}".trim(),
                  eventEpochMillis = targetTs,
                  offsetMinutes = newTask.alarmTimeMinutesBefore.toLong()
                )
              }
              newTitle = ""
              newDate = ""
              newCategory = ""
              newDetails = ""
              editingTaskId = null
              showAddDialog = false
              Toast.makeText(
                context,
                if (existingTask != null) "Événement mis à jour !" else "Événement ajouté avec succès !",
                Toast.LENGTH_SHORT
              ).show()
            } else {
              Toast.makeText(
                context,
                if (language == AppLanguage.FR) "Veuillez saisir un titre" else "Please enter a title",
                Toast.LENGTH_SHORT
              ).show()
            }
          },
          modifier = Modifier.testTag("dashboard_add_dialog_save_button"),
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(Strings.t("save", language), color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = {
          showAddDialog = false
          editingTaskId = null
        }) {
          Text(Strings.t("cancel", language), color = theme.mutedText)
        }
      }
    )
  }

  if (showMedReminderDialog) {
    MedReminderAddEditDialog(
      existing = editingMedReminder,
      language = language,
      theme = theme,
      onSave = { reminder ->
        onSaveMedReminder(reminder)
        showMedReminderDialog = false
        editingMedReminder = null
        Toast.makeText(
          context,
          if (language == AppLanguage.FR) "Rappel médicament enregistré !" else "Medication reminder saved!",
          Toast.LENGTH_SHORT
        ).show()
      },
      onDismiss = {
        showMedReminderDialog = false
        editingMedReminder = null
      }
    )
  }

  if (showAddContactDialog) {
    EditCareProviderDialog(
      provider = null,
      existingProviders = careProviders,
      theme = theme,
      isFr = language == AppLanguage.FR,
      onDismiss = { showAddContactDialog = false },
      onSave = { newProvider ->
        onSaveCareProvider(newProvider)
        showAddContactDialog = false
        Toast.makeText(
          context,
          if (language == AppLanguage.FR) "Contact '${newProvider.name}' enregistré & synchronisé !" else "Contact '${newProvider.name}' saved & synced!",
          Toast.LENGTH_SHORT
        ).show()
      }
    )
  }
}

private data class ProtocolCycleStageInfo(
  val key: String,
  val labelFr: String,
  val labelEn: String,
  val emoji: String,
  val isDone: Boolean,
  val isActive: Boolean
)

@Composable
private fun ProtocolCycleProgressCard(
  protocolSteps: List<SyncedProtocolStep>,
  onNavigateToProtocol: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val isFr = language == AppLanguage.FR
  val totalSteps = protocolSteps.size.coerceAtLeast(1)
  val completedSteps = protocolSteps.count { it.isCompleted }
  val progressFraction = (completedSteps.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

  val phase1Stim = protocolSteps.filter { it.phaseId == 1L && it.id != "p1_8" }
  val phaseRetrieval = protocolSteps.filter { it.id == "p1_8" || it.id in listOf("p2_1", "p2_2", "p2_3") }
  val phaseTecPrep = protocolSteps.filter { it.phaseId == 2L && it.id !in listOf("p2_1", "p2_2", "p2_3") }
  val phaseTransfer = protocolSteps.filter { it.id == "p3_1" }
  val phaseBeta = protocolSteps.filter { it.id in listOf("p3_2", "p3_3") }
  val phaseOb = protocolSteps.filter { it.phaseId == 4L || it.id in listOf("p3_4", "p3_5", "p3_6") }

  fun isGroupDone(items: List<SyncedProtocolStep>, defaultDone: Boolean): Boolean =
    if (items.isEmpty()) defaultDone else items.all { it.isCompleted }

  val stimDone = isGroupDone(phase1Stim, true)
  val retrievalDone = isGroupDone(phaseRetrieval, true)
  val prepDone = isGroupDone(phaseTecPrep, false)
  val transferDone = isGroupDone(phaseTransfer, false)
  val betaDone = isGroupDone(phaseBeta, false)
  val obDone = isGroupDone(phaseOb, false)

  val activeStageIndex = when {
    !stimDone -> 0
    !retrievalDone -> 1
    !prepDone -> 2
    !transferDone -> 3
    !betaDone -> 4
    !obDone -> 5
    else -> 5
  }

  val stages = listOf(
    ProtocolCycleStageInfo("stim", "Stim.", "Stim.", "💉", stimDone, activeStageIndex == 0),
    ProtocolCycleStageInfo("opu", "Ponction", "Retrieval", "🥚", retrievalDone, activeStageIndex == 1),
    ProtocolCycleStageInfo("prep", "Prépa TEC", "FET Prep", "💊", prepDone, activeStageIndex == 2),
    ProtocolCycleStageInfo("tec", "Transfert", "Transfer", "🎯", transferDone, activeStageIndex == 3),
    ProtocolCycleStageInfo("beta", "Bêta-hCG", "Beta-hCG", "🩸", betaDone, activeStageIndex == 4),
    ProtocolCycleStageInfo("ob", "Suivi OB", "OB Care", "👶", obDone, activeStageIndex == 5)
  )

  val currentActiveStep = protocolSteps.firstOrNull { !it.isCompleted } ?: protocolSteps.lastOrNull()

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dashboard_protocol_progress_card"),
    shape = RoundedCornerShape(20.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isFr) "Cycle FIV / Transfert TEC • Progression" else "IVF / FET Cycle • Protocol Progress",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
          Text(
            text = if (isFr) {
              "$completedSteps sur ${protocolSteps.size} étapes complétées (${(progressFraction * 100).toInt()}%)"
            } else {
              "$completedSteps of ${protocolSteps.size} milestones completed (${(progressFraction * 100).toInt()}%)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedText
          )
        }

        Surface(
          shape = RoundedCornerShape(100.dp),
          color = theme.accent.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.3f))
        ) {
          Text(
            text = "${(progressFraction * 100).toInt()}%",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = theme.accent
          )
        }
      }

      LinearProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = theme.accent,
        trackColor = theme.border
      )

      // 6-Stage Visual Fertility & Embryo Transfer Pipeline
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        stages.forEach { stage ->
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              modifier = Modifier.size(34.dp),
              shape = CircleShape,
              color = when {
                stage.isDone -> Color(0xFF16A34A)
                stage.isActive -> theme.accent
                else -> theme.background
              },
              border = BorderStroke(
                width = if (stage.isActive) 2.dp else 1.dp,
                color = when {
                  stage.isDone -> Color(0xFF16A34A)
                  stage.isActive -> theme.accent
                  else -> theme.border
                }
              )
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (stage.isDone) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                } else {
                  Text(text = stage.emoji, fontSize = 14.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isFr) stage.labelFr else stage.labelEn,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              fontWeight = if (stage.isActive || stage.isDone) FontWeight.Bold else FontWeight.Normal,
              color = when {
                stage.isDone -> Color(0xFF15803D)
                stage.isActive -> theme.accent
                else -> theme.mutedText
              },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      if (currentActiveStep != null) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = theme.background,
          border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isFr) "Étape Actuelle • ${currentActiveStep.dateStr}" else "Current Stage • ${currentActiveStep.dateStr}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = theme.accent
              )
              Text(
                text = currentActiveStep.stepName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
              onClick = onNavigateToProtocol,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("dashboard_open_protocol_button")
            ) {
              Text(
                text = if (isFr) "Protocole" else "Protocol",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = theme.accent
              )
              Icon(Icons.Default.ChevronRight, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PregnancyWeekProgressStageCard(
  gestationalWeeks: Int,
  gestationalDaysRemainder: Int,
  remainingCountdownDays: Int,
  dueDateText: String,
  progressPercent: Float,
  isBetaHcgConfirmed: Boolean,
  onNavigateToPregnancy: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val isFr = language == AppLanguage.FR
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dashboard_pregnancy_progress_card"),
    shape = RoundedCornerShape(20.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isFr) "Suivi Grossesse • Semaines & Trimestres" else "Pregnancy Tracker • Weeks & Trimesters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
          Text(
            text = if (isFr) {
              "Semaine $gestationalWeeks + ${gestationalDaysRemainder}j • $remainingCountdownDays j restants ($dueDateText)"
            } else {
              "Week $gestationalWeeks + ${gestationalDaysRemainder}d • $remainingCountdownDays d left ($dueDateText)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedText
          )
        }
        Surface(
          shape = RoundedCornerShape(100.dp),
          color = if (isBetaHcgConfirmed) Color(0xFFDCFCE7) else theme.accent.copy(alpha = 0.14f)
        ) {
          Text(
            text = "${(progressPercent * 100).toInt()}%",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (isBetaHcgConfirmed) Color(0xFF15803D) else theme.accent
          )
        }
      }

      LinearProgressIndicator(
        progress = { progressPercent.coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = if (isBetaHcgConfirmed) Color(0xFF16A34A) else theme.accent,
        trackColor = theme.border
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf("T1 (1-13s)" to (gestationalWeeks < 14), "T2 (14-27s)" to (gestationalWeeks in 14..27), "T3 (28-40s)" to (gestationalWeeks >= 28)).forEach { (label, active) ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (active) theme.accent.copy(alpha = 0.14f) else theme.background,
            border = BorderStroke(1.dp, if (active) theme.accent else theme.border)
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
              color = if (active) theme.accent else theme.mutedText,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
        TextButton(onClick = onNavigateToPregnancy) {
          Text(if (isFr) "Détails" else "Details", color = theme.accent, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun PartnerTodoChecklistCard(
  items: List<SyncedChecklistItem>,
  activePartner: String,
  onToggleItem: (String, Boolean) -> Unit,
  onAddItem: (String) -> Unit,
  onDeleteItem: (String) -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val isFr = language == AppLanguage.FR
  var newTodoText by remember { mutableStateOf("") }
  val completed = items.count { it.isChecked }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("partner_todo_widget"),
    shape = RoundedCornerShape(20.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = theme.accent.copy(alpha = 0.14f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.Group, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = Strings.t("partner_todo_title", language),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Text(
              text = if (isFr) "$completed/${items.size} complétés • Mode : $activePartner" else "$completed/${items.size} done • Mode: $activePartner",
              style = MaterialTheme.typography.labelSmall,
              color = theme.mutedText
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = newTodoText,
          onValueChange = { newTodoText = it },
          placeholder = {
            Text(
              if (isFr) "Ajouter une tâche partagée..." else "Add shared couple task...",
              style = MaterialTheme.typography.bodySmall
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        )
        FilledIconButton(
          onClick = {
            val clean = newTodoText.trim()
            if (clean.isNotEmpty()) {
              onAddItem(clean)
              newTodoText = ""
            }
          },
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = theme.accent),
          modifier = Modifier.size(48.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = if (isFr) "Ajouter" else "Add", tint = Color.White)
        }
      }

      if (items.isEmpty()) {
        Text(
          text = if (isFr) "Aucune tâche couple. Ajoutez votre première action partagée !" else "No shared items yet. Add your first partner task!",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items.forEach { item ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = theme.background,
              border = BorderStroke(1.dp, theme.border),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleItem(item.id, item.isChecked) }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = item.isChecked,
                  onCheckedChange = { onToggleItem(item.id, item.isChecked) }
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                      textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isChecked) theme.mutedText else theme.primaryText,
                    fontWeight = FontWeight.Medium
                  )
                  Text(
                    text = if (isFr) "Ajouté par ${item.addedBy}" else "Added by ${item.addedBy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText,
                    fontSize = 10.sp
                  )
                }
                IconButton(
                  onClick = { onDeleteItem(item.id) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = if (isFr) "Supprimer" else "Delete", tint = theme.mutedText, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MedicationRemindersCard(
  reminders: List<SyncedMedReminder>,
  todayKey: String,
  takenRefreshTick: Int,
  onToggleMedDose: (SyncedMedReminder, String, String) -> Unit,
  onRefillMedPills: (String, Long) -> Unit,
  onMarkTakenNow: (SyncedMedReminder) -> Unit,
  onToggleReminder: (SyncedMedReminder) -> Unit,
  onEditReminder: (SyncedMedReminder) -> Unit,
  onDeleteReminder: (SyncedMedReminder) -> Unit,
  onAddReminder: () -> Unit,
  onTestNotification: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val isFr = language == AppLanguage.FR
  val activeCount = reminders.count { it.isEnabled }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dashboard_med_reminders_card"),
    shape = RoundedCornerShape(20.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = theme.accent.copy(alpha = 0.14f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (isFr) "Rappels Médicaments (Alarmes)" else "Medication Reminders (Local Alarms)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Text(
              text = if (isFr) {
                "$activeCount alarme(s) quotidienne(s) programmée(s)"
              } else {
                "$activeCount active daily reminder(s)"
              },
              style = MaterialTheme.typography.labelSmall,
              color = theme.mutedText
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onAddReminder,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("dashboard_add_med_reminder_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Default.AddAlarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFr) "Ajouter Rappel" else "Add Reminder",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
          )
        }

        OutlinedButton(
          onClick = onTestNotification,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("dashboard_test_notification_button"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.45f)),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Default.Notifications, contentDescription = null, tint = theme.accent, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFr) "Tester Notif." else "Test Notif.",
            style = MaterialTheme.typography.labelMedium,
            color = theme.accent,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
          )
        }
      }

      if (reminders.isEmpty()) {
        Text(
          text = if (isFr) "Aucun rappel configuré." else "No medication reminders configured yet.",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          reminders.forEach { reminder ->
            val lastTakenMillis = remember(reminder.id, takenRefreshTick) {
              AlarmScheduler.getLastTakenMillis(context, reminder.id)
            }
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", reminder.hour, reminder.minute)

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("med_reminder_item_${reminder.id}"),
              shape = RoundedCornerShape(14.dp),
              color = theme.background,
              border = BorderStroke(1.dp, if (reminder.isEnabled) theme.accent.copy(alpha = 0.25f) else theme.border)
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = if (reminder.isEnabled) theme.accent else theme.mutedText.copy(alpha = 0.25f)
                    ) {
                      Text(
                        text = timeStr,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (reminder.isEnabled) Color.White else theme.mutedText
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryText
                      )
                      if (reminder.dosage.isNotBlank() || reminder.protocolPhase.isNotBlank()) {
                        Text(
                          text = listOfNotNull(
                            reminder.dosage.takeIf { it.isNotBlank() },
                            reminder.protocolPhase.takeIf { it.isNotBlank() }
                          ).joinToString(" • "),
                          style = MaterialTheme.typography.labelSmall,
                          color = theme.mutedText
                        )
                      }
                    }
                  }

                  Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = { onToggleReminder(reminder) },
                    modifier = Modifier.testTag("med_reminder_switch_${reminder.id}")
                  )
                }

                if (reminder.notes.isNotBlank()) {
                  Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.mutedText,
                    fontSize = 11.sp
                  )
                }

                // Daily AM/PM dose adherence checkboxes + pill supply refill
                val isToday = reminder.lastDoseDateStr == todayKey
                val amDone = isToday && reminder.dailyAmTaken
                val pmDone = isToday && reminder.dailyPmTaken
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  FilterChip(
                    selected = amDone,
                    onClick = { onToggleMedDose(reminder, "AM", todayKey) },
                    label = { Text("AM", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) }
                  )
                  FilterChip(
                    selected = pmDone,
                    onClick = { onToggleMedDose(reminder, "PM", todayKey) },
                    label = { Text("PM", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) }
                  )
                  Spacer(Modifier.weight(1f))
                  AssistChip(
                    onClick = { onRefillMedPills(reminder.id, reminder.pillsRemaining + 30L) },
                    label = {
                      Text(
                        text = "${reminder.pillsRemaining} ${if (isFr) "comp. +30" else "left +30"}",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelSmall
                      )
                    }
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                      onClick = { onMarkTakenNow(reminder) },
                      modifier = Modifier.heightIn(min = 40.dp),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.5f))
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = if (isFr) "Pris" else "Taken",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.SemiBold
                      )
                    }

                    if (lastTakenMillis != null) {
                      val takenTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastTakenMillis))
                      Text(
                        text = if (isFr) "Pris à $takenTime" else "Taken $takenTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }

                  Row {
                    IconButton(
                      onClick = { onEditReminder(reminder) },
                      modifier = Modifier.size(40.dp)
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = if (isFr) "Modifier" else "Edit", tint = theme.accent, modifier = Modifier.size(17.dp))
                    }
                    IconButton(
                      onClick = { onDeleteReminder(reminder) },
                      modifier = Modifier.size(40.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = if (isFr) "Supprimer" else "Delete", tint = theme.mutedText, modifier = Modifier.size(17.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MedReminderAddEditDialog(
  existing: SyncedMedReminder?,
  language: AppLanguage,
  theme: ThemeColors,
  onSave: (SyncedMedReminder) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isFr = language == AppLanguage.FR
  var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
  var medication by remember(existing) { mutableStateOf(existing?.medication ?: "") }
  var dosage by remember(existing) { mutableStateOf(existing?.dosage ?: "") }
  var hour by remember(existing) { mutableIntStateOf(existing?.hour?.toInt() ?: 8) }
  var minute by remember(existing) { mutableIntStateOf(existing?.minute?.toInt() ?: 0) }
  var repeatDaily by remember(existing) { mutableStateOf(existing?.repeatDaily ?: true) }
  var protocolPhase by remember(existing) { mutableStateOf(existing?.protocolPhase ?: "Phase 2 • Prépa TEC") }
  var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = theme.cardSurface,
    title = {
      Text(
        text = if (existing != null) {
          if (isFr) "Modifier le Rappel" else "Edit Reminder"
        } else {
          if (isFr) "Nouveau Rappel Médicament" else "New Medication Reminder"
        },
        fontWeight = FontWeight.Bold,
        color = theme.primaryText
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = {
            TimePickerDialog(
              context,
              { _, h, m ->
                hour = h
                minute = m
              },
              hour,
              minute,
              true
            ).show()
          },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, theme.accent)
        ) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          val timeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
          Text(
            text = if (isFr) "Heure du rappel : $timeText (Modifier)" else "Reminder Time: $timeText (Change)",
            color = theme.accent,
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(if (isFr) "Titre du rappel *" else "Reminder Title *") },
          placeholder = { Text(if (isFr) "ex: Progestérone IM (Soir)" else "e.g. Progesterone IM (Evening)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = medication,
          onValueChange = { medication = it },
          label = { Text(if (isFr) "Médicament" else "Medication") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = dosage,
          onValueChange = { dosage = it },
          label = { Text(if (isFr) "Dosage / Posologie" else "Dosage") },
          placeholder = { Text("ex: 2 mg PO / 50 mg IM") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = protocolPhase,
          onValueChange = { protocolPhase = it },
          label = { Text(if (isFr) "Phase du protocole" else "Protocol Phase") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isFr) "Répéter chaque jour" else "Repeat daily",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.primaryText
          )
          Switch(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalTitle = title.trim().ifBlank { medication.trim().ifBlank { "Rappel Médicament" } }
          onSave(
            SyncedMedReminder(
              id = existing?.id?.ifBlank { null } ?: "mr_${System.currentTimeMillis()}",
              title = finalTitle,
              medication = medication.trim(),
              dosage = dosage.trim(),
              hour = hour.toLong(),
              minute = minute.toLong(),
              repeatDaily = repeatDaily,
              isEnabled = existing?.isEnabled ?: true,
              protocolPhase = protocolPhase.trim(),
              notes = notes.trim(),
              createdAt = existing?.createdAt
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
      ) {
        Text(if (isFr) "Enregistrer" else "Save", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
      }
    }
  )
}
