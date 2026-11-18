package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PmaRepository
import com.example.data.SyncedProtocolStep
import com.example.data.TaskItem
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun ProtocolScreen(
  protocolSteps: List<SyncedProtocolStep>,
  immediateTasks: List<TaskItem>,
  activePartnerProfile: String = "Fatou",
  isBetaHcgConfirmed: Boolean = false,
  onToggleBetaHcg: (Boolean) -> Unit = {},
  onSaveStep: (SyncedProtocolStep) -> Unit,
  onToggleStep: (SyncedProtocolStep) -> Unit,
  onDeleteStep: (String) -> Unit,
  onSaveImmediateTask: (TaskItem) -> Unit,
  onToggleImmediateTask: (TaskItem) -> Unit,
  onDeleteImmediateTask: (String) -> Unit,
  onResetProtocol: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isFr = language == AppLanguage.FR

  // Modals state
  var showStepModal by remember { mutableStateOf(false) }
  var editingStep by remember { mutableStateOf<SyncedProtocolStep?>(null) }
  var modalInitialPhaseId by remember { mutableIntStateOf(2) }
  var modalInitialDate by remember { mutableStateOf("13 nov.") }

  var showImmediateTaskModal by remember { mutableStateOf(false) }
  var editingImmediateTask by remember { mutableStateOf<TaskItem?>(null) }

  var showResetConfirmDialog by remember { mutableStateOf(false) }

  // Phase tabs & collapsible state
  var selectedPhaseTab by remember { mutableIntStateOf(0) } // 0 = All Phases
  val expandedPhases = remember {
    mutableStateMapOf(
      1 to false,
      2 to true,
      3 to true,
      4 to false
    )
  }

  // Optional November 2026 quick calendar drawer (collapsed by default)
  var isCalendarExpanded by remember { mutableStateOf(false) }
  var selectedDayNumber by remember { mutableIntStateOf(13) }

  // Live countdown ticker
  var currentEpochTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      currentEpochTime = System.currentTimeMillis()
    }
  }

  fun formatLiveCountdown(targetMillis: Long): String {
    val diff = targetMillis - currentEpochTime
    if (diff <= 0) return if (isFr) "À prendre maintenant" else "Due now"
    val days = diff / (1000L * 60 * 60 * 24)
    val hours = (diff / (1000L * 60 * 60)) % 24
    val minutes = (diff / (1000L * 60)) % 60
    val seconds = (diff / 1000L) % 60
    return if (days > 0) "${days}j ${hours}h ${minutes}m ${seconds}s" else "${hours}h ${minutes}m ${seconds}s"
  }

  val nextEstraceMillis = remember(currentEpochTime) {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    Calendar.getInstance().apply {
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      when {
        hour < 8 -> {
          set(Calendar.HOUR_OF_DAY, 8)
          set(Calendar.MINUTE, 0)
        }
        hour < 14 -> {
          set(Calendar.HOUR_OF_DAY, 14)
          set(Calendar.MINUTE, 0)
        }
        hour < 20 -> {
          set(Calendar.HOUR_OF_DAY, 20)
          set(Calendar.MINUTE, 0)
        }
        else -> {
          add(Calendar.DAY_OF_MONTH, 1)
          set(Calendar.HOUR_OF_DAY, 8)
          set(Calendar.MINUTE, 0)
        }
      }
    }.timeInMillis
  }

  val completedStepsCount = protocolSteps.count { it.isCompleted }
  val totalStepsCount = protocolSteps.size.coerceAtLeast(1)
  val progressFraction = (completedStepsCount.toFloat() / totalStepsCount.toFloat()).coerceIn(0f, 1f)

  val openAddStepModal: (Int, String) -> Unit = { phaseId, dateStr ->
    editingStep = null
    modalInitialPhaseId = phaseId
    modalInitialDate = dateStr
    showStepModal = true
  }

  SelectionContainer {
    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .background(theme.background)
          .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp)
      ) {
        // -------------------------------------------------------------
        // HEADER CARD: Clinic, Doctor, Embryologist, Patient, Objective & Reset
        // -------------------------------------------------------------
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("protocol_header_card"),
            shape = RoundedCornerShape(20.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.2.dp, theme.accent.copy(alpha = 0.35f)),
            shadowElevation = 3.dp
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(
                      theme.accent.copy(alpha = 0.10f),
                      theme.cardSurface
                    )
                  )
                )
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = if (isFr) {
                  "📋 Protocole PMA & Étapes de Grossesse : Calendrier Directeur"
                } else {
                  "📋 IVF / FET Protocol & Pregnancy Stages: Master Calendar"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = theme.primaryText,
                lineHeight = 22.sp
              )

              HorizontalDivider(color = theme.border)

              ProtocolHeaderLine(if (isFr) "🏢 Centre :" else "🏢 Clinic:", "${PmaRepository.clinicName} (${PmaRepository.clinicAddress})", theme)
              ProtocolHeaderLine(if (isFr) "🩺 Médecin référent :" else "🩺 Referring Doctor:", PmaRepository.doctorName, theme)
              ProtocolHeaderLine(if (isFr) "👩‍⚕️ Équipe & Coordonnées :" else "👩‍⚕️ Team & Contact:", "${PmaRepository.embryologistInfo}.", theme)
              ProtocolHeaderLine(if (isFr) "👤 Patiente :" else "👤 Patient:", PmaRepository.patientName, theme)
              ProtocolHeaderLine(if (isFr) "🎯 Objectif :" else "🎯 Objective:", PmaRepository.objective, theme, highlightValue = true)
              ProtocolHeaderLine(
                label = if (isFr) "🗓️ Terme Théorique Calculé (DPA) :" else "🗓️ Calculated Due Date (EDD):",
                value = if (isFr) {
                  if (isBetaHcgConfirmed) "Mercredi 6 août 2027 (Débloqué ✓ — 40 SA)"
                  else "Mercredi 6 août 2027 (40 SA, à débloquer après confirmation le 27 nov.)"
                } else {
                  if (isBetaHcgConfirmed) "Wednesday, August 6, 2027 (Unlocked ✓ — 40 weeks)"
                  else "Wednesday, August 6, 2027 (40 weeks, unlocks upon Nov 27 Beta-hCG confirmation)"
                },
                theme = theme,
                highlightValue = isBetaHcgConfirmed
              )

              // Progress Bar
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = if (isFr) {
                      "Progression : $completedStepsCount / ${protocolSteps.size} étapes complétées"
                    } else {
                      "Progress: $completedStepsCount / ${protocolSteps.size} steps completed"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.mutedText
                  )
                  Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent
                  )
                }
                LinearProgressIndicator(
                  progress = { progressFraction },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp)),
                  color = theme.accent,
                  trackColor = theme.border
                )
              }

              // Reset & Quick Add Buttons
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { showResetConfirmDialog = true },
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("protocol_reset_button"),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, theme.border),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(Icons.Default.RestartAlt, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFr) "Réinitialiser" else "Reset",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryText,
                    maxLines = 1
                  )
                }

                Button(
                  onClick = { openAddStepModal(2, "13 nov.") },
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("protocol_header_add_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFr) "Ajouter Étape" else "Add Step",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                  )
                }
              }
            }
          }
        }

        // -------------------------------------------------------------
        // COMPACT LIVE ESTRACE COUNTDOWN + COLLAPSIBLE NOV 2026 CALENDAR
        // -------------------------------------------------------------
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.28f))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = "⏱️", fontSize = 18.sp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = if (isFr) "Prochaine prise Estrace 2 mg PO (TID : 08h, 14h, 20h)" else "Next Dose Estrace 2 mg PO (TID: 8 AM, 2 PM, 8 PM)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText
                    )
                    Text(
                      text = formatLiveCountdown(nextEstraceMillis),
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.ExtraBold,
                      color = theme.accent
                    )
                  }
                }

                TextButton(
                  onClick = { isCalendarExpanded = !isCalendarExpanded },
                  modifier = Modifier.heightIn(min = 48.dp)
                ) {
                  Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isFr) "Nov. 2026" else "Nov 2026",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent
                  )
                  Icon(
                    imageVector = if (isCalendarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              AnimatedVisibility(visible = isCalendarExpanded) {
                NovemberCalendarGrid(
                  selectedDayNumber = selectedDayNumber,
                  theme = theme,
                  language = language,
                  onDayClick = { day ->
                    selectedDayNumber = day
                    openAddStepModal(2, "$day nov. 2026")
                  }
                )
              }
            }
          }
        }

        // -------------------------------------------------------------
        // SECTION 1: 📊 1. Tableau de Bord Immédiat (📌 Actions & Statuts Cliniques)
        // -------------------------------------------------------------
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("protocol_section_1_card"),
            shape = RoundedCornerShape(18.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.2.dp, theme.accent.copy(alpha = 0.35f)),
            shadowElevation = 2.dp
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isFr) "📊 1. Tableau de Bord Immédiat" else "📊 1. Immediate Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.primaryText
                  )
                  Text(
                    text = if (isFr) "📌 Actions & Statuts Cliniques" else "📌 Immediate Clinical Actions & Statuses",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.accent
                  )
                }

                FilledTonalButton(
                  onClick = {
                    editingImmediateTask = null
                    showImmediateTaskModal = true
                  },
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("protocol_add_immediate_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = theme.accent.copy(alpha = 0.14f),
                    contentColor = theme.accent
                  ),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isFr) "Ajouter" else "Add",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }

              if (immediateTasks.isEmpty()) {
                Text(
                  text = if (isFr) "Aucune action clinique enregistrée." else "No immediate clinical actions.",
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText
                )
              } else {
                val partitionedTasks = remember(immediateTasks, currentEpochTime) {
                  com.example.data.DateSortUtils.partitionUpcomingFirst(
                    items = immediateTasks,
                    nowMillis = currentEpochTime,
                    epochProvider = { com.example.data.DateSortUtils.taskEpochMillis(it) },
                    completedProvider = { it.isCompleted }
                  )
                }
                partitionedTasks.upcoming.forEach { task ->
                  ImmediateClinicalActionCard(
                    task = task,
                    isPastItem = false,
                    theme = theme,
                    language = language,
                    onToggle = { onToggleImmediateTask(task) },
                    onToggleAlarm = {
                      val nextAlarm = !task.hasAlarm
                      val updated = task.copy(hasAlarm = nextAlarm)
                      val epoch = com.example.data.DateSortUtils.taskEpochMillis(updated)
                      if (nextAlarm && epoch != Long.MAX_VALUE) {
                        com.example.alarm.AlarmScheduler.scheduleAppointmentAlarm(
                          context = context,
                          itemId = updated.id,
                          title = updated.title,
                          details = updated.details.ifBlank { updated.category },
                          eventEpochMillis = epoch,
                          offsetMinutes = updated.alarmTimeMinutesBefore.toLong()
                        )
                        Toast.makeText(context, com.example.i18n.Strings.t("alarm_scheduled_msg", language), Toast.LENGTH_SHORT).show()
                      } else {
                        com.example.alarm.AlarmScheduler.cancelAppointmentAlarm(context, updated.id)
                      }
                      onSaveImmediateTask(updated)
                    },
                    onEdit = {
                      editingImmediateTask = task
                      showImmediateTaskModal = true
                    },
                    onDelete = {
                      onDeleteImmediateTask(task.id)
                      Toast.makeText(context, if (isFr) "Action supprimée" else "Action deleted", Toast.LENGTH_SHORT).show()
                    }
                  )
                }
                if (partitionedTasks.past.isNotEmpty()) {
                  var showPastTasks by remember { mutableStateOf(false) }
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = theme.background,
                    border = BorderStroke(0.8.dp, theme.border),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { showPastTasks = !showPastTasks }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "${com.example.i18n.Strings.t("past_divider", language)} (${partitionedTasks.past.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.mutedText
                      )
                      Icon(
                        if (showPastTasks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = theme.mutedText
                      )
                    }
                  }
                  if (showPastTasks) {
                    partitionedTasks.past.forEach { task ->
                      ImmediateClinicalActionCard(
                        task = task,
                        isPastItem = true,
                        theme = theme,
                        language = language,
                        onToggle = { onToggleImmediateTask(task) },
                        onToggleAlarm = {},
                        onEdit = {
                          editingImmediateTask = task
                          showImmediateTaskModal = true
                        },
                        onDelete = {
                          onDeleteImmediateTask(task.id)
                          Toast.makeText(context, if (isFr) "Action supprimée" else "Action deleted", Toast.LENGTH_SHORT).show()
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // -------------------------------------------------------------
        // SECTION 2 HEADER & PHASE TABS: 🗓️ 2. Chronologie Complète du Parcours Médical
        // -------------------------------------------------------------
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isFr) {
                  "🗓️ 2. Calendrier Directeur PMA & Grossesse (Phases 1 à 4)"
                } else {
                  "🗓️ 2. Complete IVF & Pregnancy Director Timeline (Phases 1 to 4)"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = theme.primaryText,
                modifier = Modifier.weight(1f)
              )
              FilledTonalButton(
                onClick = { openAddStepModal(if (selectedPhaseTab in 1..4) selectedPhaseTab else 2, "13 nov.") },
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("protocol_add_step_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = theme.accent.copy(alpha = 0.14f),
                  contentColor = theme.accent
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isFr) "Étape" else "Step", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
              }
            }

            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              val tabs = listOf(0 to (if (isFr) "Toutes (4 Phases)" else "All Phases")) +
                protocolPhasesMeta.map { it.phaseId to "${it.emoji} Phase ${it.phaseId}" }
              items(tabs) { (phaseId, label) ->
                val isSel = selectedPhaseTab == phaseId
                FilterChip(
                  selected = isSel,
                  onClick = {
                    selectedPhaseTab = phaseId
                    if (phaseId in 1..4) {
                      expandedPhases[phaseId] = true
                    }
                  },
                  modifier = Modifier.testTag("protocol_phase_tab_$phaseId"),
                  label = {
                    Text(
                      text = label,
                      fontSize = 11.5.sp,
                      fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1,
                      softWrap = false
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = theme.accent,
                    selectedLabelColor = Color.White
                  ),
                  border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSel,
                    borderColor = theme.border,
                    selectedBorderColor = theme.accent
                  )
                )
              }
            }
          }
        }

        // -------------------------------------------------------------
        // 4 COLLAPSIBLE PHASES
        // -------------------------------------------------------------
        val visiblePhases = if (selectedPhaseTab == 0) {
          protocolPhasesMeta
        } else {
          protocolPhasesMeta.filter { it.phaseId == selectedPhaseTab }
        }

        items(visiblePhases, key = { "phase_${it.phaseId}" }) { phaseMeta ->
          val isPhase4Locked = phaseMeta.phaseId == 4 && !isBetaHcgConfirmed

          if (isPhase4Locked) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("protocol_phase_card_4_locked"),
              shape = RoundedCornerShape(18.dp),
              color = theme.cardSurface,
              border = BorderStroke(1.2.dp, theme.accent.copy(alpha = 0.35f))
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = "🔒", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = if (isFr) "Phase 4 : Suivi Obstétrical & Maternité" else "Phase 4: Obstetrics & Maternity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryText
                      )
                      Text(
                        text = if (isFr) "Mode Veille (En attente confirmation 27 nov.)" else "Standby Mode (Awaiting Nov 27 confirmation)",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.accent,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF3C7)
                  ) {
                    Text(
                      text = if (isFr) "EN VEILLE" else "STANDBY",
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFB45309)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = theme.background,
                  border = BorderStroke(0.8.dp, theme.border),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = if (isFr) "🗓️ Terme Théorique Calculé (DPA — 40 SA)" else "🗓️ Theoretical Due Date (EDD — 40 weeks)",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = theme.accent
                    )
                    Text(
                      text = if (isFr)
                        "Mercredi 6 août 2027 (basé sur un transfert de blastocyste J5 le 18 novembre 2026). Révélé et activé dès confirmation biologique."
                      else
                        "Wednesday, August 6, 2027 (based on Day 5 blastocyst transfer on November 18, 2026). Fully unlocked upon biological confirmation.",
                      style = MaterialTheme.typography.bodySmall,
                      color = theme.primaryText
                    )
                  }
                }

                Text(
                  text = if (isFr)
                    "Verrouillé en attente des résultats sanguins du 27 novembre 2026 (Prise de sang Bêta-hCG sérique #1 à 9h30 🤞). Utilisez le commutateur ou le bouton pour débloquer le suivi de grossesse jusqu'à l'accouchement."
                  else
                    "Locked awaiting bloodwork results on November 27, 2026 (Serum Beta-hCG blood test #1 at 9:30 AM 🤞). Toggle the switch or tap the button to unlock full pregnancy care through delivery.",
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText,
                  lineHeight = 18.sp
                )

                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = theme.background,
                  border = BorderStroke(1.dp, theme.border),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = if (isFr) "Confirmation Bêta-hCG (27 nov.)" else "Beta-hCG Confirmation (Nov 27)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryText
                      )
                      Text(
                        text = if (isFr) "Basculer pour débloquer la phase 4" else "Toggle to unlock phase 4",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.mutedText
                      )
                    }
                    Switch(
                      checked = isBetaHcgConfirmed,
                      onCheckedChange = { onToggleBetaHcg(it) },
                      modifier = Modifier.testTag("switch_beta_hcg_phase_4")
                    )
                  }
                }

                Button(
                  onClick = { onToggleBetaHcg(true) },
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("unlock_phase_4_button")
                ) {
                  Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFr) "🔓 Confirmer Bêta-hCG (27 nov.) & Débloquer" else "🔓 Confirm Beta-hCG (Nov 27) & Unlock",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          } else {
            val phaseSteps = protocolSteps
              .filter { it.phaseId.toInt() == phaseMeta.phaseId }
              .sortedBy { it.sortOrder }
            val phaseDone = phaseSteps.count { it.isCompleted }
            val isExpanded = expandedPhases[phaseMeta.phaseId] ?: phaseMeta.defaultExpanded

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("protocol_phase_card_${phaseMeta.phaseId}"),
              shape = RoundedCornerShape(18.dp),
              color = theme.cardSurface,
              border = BorderStroke(
                width = if (phaseMeta.phaseId == 2) 1.3.dp else 1.dp,
                color = if (phaseMeta.phaseId == 2) theme.accent.copy(alpha = 0.45f) else theme.border
              )
            ) {
            Column(modifier = Modifier.padding(12.dp)) {
              // Phase Collapsible Header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { expandedPhases[phaseMeta.phaseId] = !isExpanded }
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = phaseMeta.emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (isFr) phaseMeta.titleFr else phaseMeta.titleEn,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Spacer(modifier = Modifier.height(3.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = if (isFr) phaseMeta.dateRangeFr else phaseMeta.dateRangeEn,
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.mutedText
                    )
                    Surface(
                      shape = RoundedCornerShape(100.dp),
                      color = if (phaseDone == phaseSteps.size && phaseSteps.isNotEmpty()) Color(0xFFF0FDF4) else theme.accent.copy(alpha = 0.12f)
                    ) {
                      Text(
                        text = "$phaseDone/${phaseSteps.size}",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (phaseDone == phaseSteps.size && phaseSteps.isNotEmpty()) Color(0xFF15803D) else theme.accent
                      )
                    }
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { openAddStepModal(phaseMeta.phaseId, "13 nov.") },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("protocol_add_phase_${phaseMeta.phaseId}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.AddCircleOutline,
                      contentDescription = if (isFr) "Ajouter à la phase ${phaseMeta.phaseId}" else "Add to phase ${phaseMeta.phaseId}",
                      tint = theme.accent
                    )
                  }
                  Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = theme.mutedText,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              AnimatedVisibility(visible = isExpanded) {
                Column(
                  modifier = Modifier.padding(top = 8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  if (phaseMeta.phaseId == 4) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color(0xFFF0FDF4),
                      border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("🟢", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = if (isFr) "Bêta-hCG Confirmé (27 nov.) — Maternité Débloquée 🎉" else "Beta-hCG Confirmed (Nov 27) — Maternity Unlocked 🎉",
                              style = MaterialTheme.typography.labelMedium,
                              fontWeight = FontWeight.Bold,
                              color = Color(0xFF15803D)
                            )
                          }
                          Switch(
                            checked = isBetaHcgConfirmed,
                            onCheckedChange = { onToggleBetaHcg(it) },
                            modifier = Modifier.testTag("switch_beta_hcg_unlocked")
                          )
                        }
                        Text(
                          text = if (isFr)
                            "🗓️ Terme Théorique Calculé (DPA — 40 SA) : Mercredi 6 août 2027 (Transfert J5 le 18 nov. 2026)"
                          else
                            "🗓️ Calculated Due Date (EDD — 40 weeks): Wednesday, August 6, 2027 (D5 transfer Nov 18, 2026)",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF15803D)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    PregnancyMilestonesSection(
                      currentWeek = 4,
                      theme = theme,
                      language = language
                    )
                  }
                  if (phaseSteps.isEmpty()) {
                    Text(
                      text = if (isFr) "Aucune étape dans cette phase." else "No steps in this phase.",
                      style = MaterialTheme.typography.bodySmall,
                      color = theme.mutedText
                    )
                  } else {
                    val partitionedSteps = remember(phaseSteps, currentEpochTime) {
                      com.example.data.DateSortUtils.partitionUpcomingFirst(
                        items = phaseSteps,
                        nowMillis = currentEpochTime,
                        epochProvider = { com.example.data.DateSortUtils.protocolStepEpochMillis(it) },
                        completedProvider = { it.isCompleted }
                      )
                    }
                    val firstUncompletedId = partitionedSteps.upcoming.firstOrNull { !it.isCompleted }?.id
                    partitionedSteps.upcoming.forEach { step ->
                      val isNextActive = step.id == firstUncompletedId
                      ProtocolTimelineStepCard(
                        step = step,
                        isNextActive = isNextActive,
                        isPastItem = false,
                        theme = theme,
                        language = language,
                        countdownText = if (step.targetEpochMillis > 0L && !step.isCompleted) {
                          formatLiveCountdown(step.targetEpochMillis)
                        } else null,
                        onToggle = { onToggleStep(step) },
                        onToggleAlarm = {
                          val nextAlarm = !step.hasAlarm
                          val updated = step.copy(hasAlarm = nextAlarm)
                          val epoch = com.example.data.DateSortUtils.protocolStepEpochMillis(updated)
                          if (nextAlarm && epoch != Long.MAX_VALUE) {
                            com.example.alarm.AlarmScheduler.scheduleAppointmentAlarm(
                              context = context,
                              itemId = updated.id,
                              title = updated.stepName,
                              details = updated.details.ifBlank { updated.medication },
                              eventEpochMillis = epoch,
                              offsetMinutes = updated.alarmOffsetMinutes
                            )
                            Toast.makeText(context, com.example.i18n.Strings.t("alarm_scheduled_msg", language), Toast.LENGTH_SHORT).show()
                          } else {
                            com.example.alarm.AlarmScheduler.cancelAppointmentAlarm(context, updated.id)
                          }
                          onSaveStep(updated)
                        },
                        onEdit = {
                          editingStep = step
                          modalInitialPhaseId = step.phaseId.toInt()
                          modalInitialDate = step.dateStr
                          showStepModal = true
                        },
                        onCopy = {
                          val copyText = "${step.dateStr} • ${step.stepName}\n${step.details}"
                          clipboardManager.setText(AnnotatedString(copyText))
                          Toast.makeText(context, if (isFr) "Étape copiée !" else "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                          onDeleteStep(step.id)
                          Toast.makeText(context, if (isFr) "Étape supprimée" else "Deleted", Toast.LENGTH_SHORT).show()
                        }
                      )
                    }
                    if (partitionedSteps.past.isNotEmpty()) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = theme.border)
                        Surface(
                          shape = RoundedCornerShape(100.dp),
                          color = theme.background,
                          border = BorderStroke(0.6.dp, theme.border)
                        ) {
                          Text(
                            text = "${com.example.i18n.Strings.t("past_divider", language)} (${partitionedSteps.past.size})",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.mutedText
                          )
                        }
                        HorizontalDivider(modifier = Modifier.weight(1f), color = theme.border)
                      }
                      partitionedSteps.past.forEach { step ->
                        ProtocolTimelineStepCard(
                          step = step,
                          isPastItem = true,
                          theme = theme,
                          language = language,
                          countdownText = null,
                          onToggle = { onToggleStep(step) },
                          onToggleAlarm = {},
                          onEdit = {
                            editingStep = step
                            modalInitialPhaseId = step.phaseId.toInt()
                            modalInitialDate = step.dateStr
                            showStepModal = true
                          },
                          onCopy = {
                            val copyText = "${step.dateStr} • ${step.stepName}\n${step.details}"
                            clipboardManager.setText(AnnotatedString(copyText))
                            Toast.makeText(context, if (isFr) "Étape copiée !" else "Copied!", Toast.LENGTH_SHORT).show()
                          },
                          onDelete = {
                            onDeleteStep(step.id)
                            Toast.makeText(context, if (isFr) "Étape supprimée" else "Deleted", Toast.LENGTH_SHORT).show()
                          }
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // SECTION 3: 💊 3. Mémo Récapitulatif des Posologies Actuelles (17–20 Nov.)
      // -------------------------------------------------------------
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("protocol_section_3_memo"),
            shape = RoundedCornerShape(18.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.2.dp, theme.accent.copy(alpha = 0.35f))
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(theme.accent.copy(alpha = 0.15f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text("💊", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isFr) "3. Mémo Récapitulatif des Posologies Actuelles" else "3. Current Medication Dosages Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.primaryText
                  )
                  Text(
                    text = if (isFr) "Fenêtre critique du 17 au 20 nov. 2026 • Soutien de la phase lutéale" else "Critical window Nov 17–20, 2026 • Luteal Phase Support",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                }
              }

              // Card 1: Estrace TID
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = theme.background,
                border = BorderStroke(0.6.dp, theme.border)
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "💊 Estrace 2 mg PO (TID)",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = theme.accent
                    )
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = theme.accent.copy(alpha = 0.12f)
                    ) {
                      Text(
                        text = "3x / jour",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = theme.accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = "• Posologie active : 1 comprimé de 2 mg par voie orale 3 fois par jour (Matin 08h00, Midi 14h00, Soir 20h00 en continu).\n• Historique : Débuté à 2 mg PO 2x/j (AM & PM) du 05 au 08 nov. (Cycle Start), augmenté à 3x/j (TID) à partir du 09 nov.\n• Continuité : Maintenir sans aucune interruption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.primaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                  )
                }
              }

              // Card 2: Progestérone IM (PIO)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = theme.background,
                border = BorderStroke(0.6.dp, theme.border)
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "💉 Progestérone IM (PIO)",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFD97706)
                    )
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color(0xFFD97706).copy(alpha = 0.12f)
                    ) {
                      Text(
                        text = "IM Soir",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = "• 17 & 18 nov. : 1.0 mL IM une fois par jour le soir à heure régulière (20h00).\n• À partir du 19 nov. : Augmentation à 1.5 mL IM Daily chaque soir.\n• 20 nov. : Progesterone Check @ 9:30 AM au laboratoire RMA (poursuite 1.5 mL IM ; posologie susceptible d'être ajustée selon le taux sérique).",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.primaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                  )
                }
              }

              // Card 3: Azithromycine & Consignes Clés
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = theme.background,
                border = BorderStroke(0.6.dp, theme.border)
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "🩺 Antibioprophylaxie & Étapes Clés",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                  Text(
                    text = "• Azithromycine 1 000 mg PO : Prise unique complétée le 13 nov. avec nourriture.\n• 18 nov. (Aujourd'hui) : TEC à 14h00 EST (Vessie pleine, sans parfum). Continuer Estrace 2 mg TID + PIO 1 mL IM.\n• 27 nov. : Pregnancy bloodwork (Bêta-hCG) @ 9:30 AM 🤞 (débloque le suivi complet Phase 4 Maternité).",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.mutedText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                  )
                }
              }
            }
          }
        }
      }

      // Bottom FAB "+ Ajouter Étape"
      FloatingActionButton(
        onClick = { openAddStepModal(if (selectedPhaseTab in 1..4) selectedPhaseTab else 2, "13 nov.") },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 16.dp)
          .testTag("protocol_fab_add_step"),
        containerColor = theme.accent,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = if (isFr) "Ajouter Étape" else "Add Step", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFr) "Ajouter Étape" else "Add Step",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }
    }
  }

  // Edit / Add Immediate Action Dialog (Section 1)
  if (showImmediateTaskModal) {
    ProtocolImmediateTaskDialog(
      existingTask = editingImmediateTask,
      activePartnerProfile = activePartnerProfile,
      theme = theme,
      language = language,
      onSave = { savedTask ->
        onSaveImmediateTask(savedTask)
        showImmediateTaskModal = false
        editingImmediateTask = null
        Toast.makeText(context, if (isFr) "Action enregistrée !" else "Action saved!", Toast.LENGTH_SHORT).show()
      },
      onDismiss = {
        showImmediateTaskModal = false
        editingImmediateTask = null
      }
    )
  }

  // Edit / Add Timeline Step Dialog (Section 2)
  if (showStepModal) {
    ProtocolStepEditDialog(
      existingStep = editingStep,
      initialPhaseId = modalInitialPhaseId,
      initialDateStr = modalInitialDate,
      nextSortOrder = (protocolSteps.maxOfOrNull { it.sortOrder } ?: 32L) + 1L,
      activePartnerProfile = activePartnerProfile,
      theme = theme,
      language = language,
      onSave = { savedStep ->
        onSaveStep(savedStep)
        expandedPhases[savedStep.phaseId.toInt()] = true
        showStepModal = false
        editingStep = null
        Toast.makeText(context, if (isFr) "Étape enregistrée !" else "Step saved!", Toast.LENGTH_SHORT).show()
      },
      onDismiss = {
        showStepModal = false
        editingStep = null
      }
    )
  }

  // Reset Confirmation Dialog
  if (showResetConfirmDialog) {
    ProtocolResetConfirmDialog(
      theme = theme,
      language = language,
      onConfirmReset = {
        onResetProtocol()
        showResetConfirmDialog = false
        Toast.makeText(
          context,
          if (isFr) "Protocole réinitialisé par défaut !" else "Protocol reset to default!",
          Toast.LENGTH_SHORT
        ).show()
      },
      onDismiss = { showResetConfirmDialog = false }
    )
  }
}

@Composable
private fun ProtocolHeaderLine(
  label: String,
  value: String,
  theme: ThemeColors,
  highlightValue: Boolean = false
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "$label ",
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Bold,
      color = theme.primaryText
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (highlightValue) FontWeight.Bold else FontWeight.Normal,
      color = if (highlightValue) theme.accent else theme.mutedText,
      modifier = Modifier.weight(1f),
      lineHeight = 18.sp
    )
  }
}

@Composable
private fun ImmediateClinicalActionCard(
  task: TaskItem,
  isPastItem: Boolean = false,
  theme: ThemeColors,
  language: AppLanguage,
  onToggle: () -> Unit,
  onToggleAlarm: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val isFr = language == AppLanguage.FR
  val bgColor = if (task.isCompleted || isPastItem) theme.background else theme.accent.copy(alpha = 0.08f)
  val borderColor = if (task.isCompleted || isPastItem) theme.border else theme.accent

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("protocol_immediate_item_${task.id}"),
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = BorderStroke(if (task.isCompleted || isPastItem) 0.9.dp else 1.5.dp, borderColor)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onToggle,
          modifier = Modifier
            .size(48.dp)
            .testTag("protocol_toggle_immediate_${task.id}")
        ) {
          Surface(
            shape = RoundedCornerShape(7.dp),
            color = if (task.isCompleted) Color(0xFFF0FDF4) else theme.cardSurface,
            border = BorderStroke(1.2.dp, if (task.isCompleted) Color(0xFF16A34A) else theme.accent),
            modifier = Modifier.size(22.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (task.isCompleted) {
                Icon(Icons.Default.Check, contentDescription = "Validé", tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (task.isCompleted) Color(0xFFF0FDF4) else theme.accent.copy(alpha = 0.14f)
            ) {
              Text(
                text = task.date,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (task.isCompleted) Color(0xFF15803D) else theme.accent
              )
            }
            if (isPastItem) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = theme.border.copy(alpha = 0.45f)
              ) {
                Text(
                  text = com.example.i18n.Strings.t("past_badge", language),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = theme.mutedText
                )
              }
            }
            if (task.category.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = theme.cardSurface,
                border = BorderStroke(0.5.dp, theme.border)
              ) {
                Text(
                  text = task.category,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = theme.mutedText
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
        }
      }

      if (task.details.isNotBlank()) {
        Text(
          text = task.details,
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText,
          lineHeight = 18.sp,
          modifier = Modifier.padding(start = 12.dp)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${if (isFr) "Par" else "By"} ${task.addedBy}",
          style = MaterialTheme.typography.labelSmall,
          color = theme.mutedText,
          modifier = Modifier.padding(start = 12.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (!isPastItem) {
            IconButton(
              onClick = onToggleAlarm,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = if (task.hasAlarm) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                contentDescription = if (isFr) "Alarme" else "Alarm",
                tint = if (task.hasAlarm) theme.accent else theme.mutedText,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          TextButton(
            onClick = onEdit,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("protocol_edit_immediate_${task.id}")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isFr) "Modifier" else "Edit", style = MaterialTheme.typography.labelMedium, color = theme.accent, fontWeight = FontWeight.SemiBold)
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(48.dp)
              .testTag("protocol_delete_immediate_${task.id}")
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = if (isFr) "Supprimer" else "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun ProtocolTimelineStepCard(
  step: SyncedProtocolStep,
  isNextActive: Boolean = false,
  isPastItem: Boolean = false,
  theme: ThemeColors,
  language: AppLanguage,
  countdownText: String?,
  onToggle: () -> Unit,
  onToggleAlarm: () -> Unit,
  onEdit: () -> Unit,
  onCopy: () -> Unit,
  onDelete: () -> Unit
) {
  val isFr = language == AppLanguage.FR

  val cardBg: Color
  val cardBorder: Color
  val statusBg: Color
  val statusFg: Color
  val statusLabel: String

  when {
    step.isCompleted -> {
      cardBg = Color(0xFFF0FDF4)
      cardBorder = Color(0xFFBBF7D0)
      statusBg = Color(0xFFDCFCE7)
      statusFg = Color(0xFF15803D)
      statusLabel = if (isFr) "Terminé" else "Completed"
    }
    isNextActive || step.status.equals("Aujourd'hui", ignoreCase = true) || step.status.equals("En cours", ignoreCase = true) -> {
      cardBg = Color(0xFFFFFBEB)
      cardBorder = Color(0xFFFDE68A)
      statusBg = Color(0xFFFEF3C7)
      statusFg = Color(0xFFB45309)
      statusLabel = if (isFr) "Prochain" else "Next"
    }
    else -> {
      cardBg = Color(0xFFF0F9FF)
      cardBorder = Color(0xFFBAE6FD)
      statusBg = Color(0xFFE0F2FE)
      statusFg = Color(0xFF0369A1)
      statusLabel = if (isFr) "Planifié" else "Scheduled"
    }
  }

  val isActiveOrNext = isNextActive || step.status.equals("Aujourd'hui", ignoreCase = true) || step.status.equals("En cours", ignoreCase = true)

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("protocol_step_card_${step.id}"),
    shape = RoundedCornerShape(14.dp),
    color = cardBg,
    border = BorderStroke(if (isActiveOrNext) 1.5.dp else 1.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Top Row: Checkbox + Date Pill + Status Badge + Medication Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onToggle,
          modifier = Modifier
            .size(48.dp)
            .testTag("protocol_toggle_step_${step.id}")
        ) {
          Surface(
            shape = RoundedCornerShape(7.dp),
            color = if (step.isCompleted) Color(0xFFF0FDF4) else theme.cardSurface,
            border = BorderStroke(1.2.dp, if (step.isCompleted) Color(0xFF16A34A) else cardBorder),
            modifier = Modifier.size(22.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (step.isCompleted) {
                Icon(Icons.Default.Check, contentDescription = "Complété", tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val isActiveOrNext = isNextActive || step.status.equals("Aujourd'hui", ignoreCase = true) || step.status.equals("En cours", ignoreCase = true)
            // Date pill
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isActiveOrNext) theme.accent else theme.cardSurface,
              border = if (isActiveOrNext) null else BorderStroke(0.6.dp, theme.border)
            ) {
              Text(
                text = step.dateStr,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isActiveOrNext) Color.White else theme.primaryText
              )
            }

            // Status pill
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = statusBg
            ) {
              Text(
                text = statusLabel,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusFg
              )
            }
          }

          if (step.medication.isNotBlank() || step.dosage.isNotBlank()) {
            Text(
              text = listOf(step.medication, step.dosage).filter { it.isNotBlank() }.joinToString(" • "),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = theme.accent
            )
          }
        }
      }

      // Step Title
      Text(
        text = step.stepName,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = theme.primaryText,
        lineHeight = 20.sp,
        modifier = Modifier.padding(start = 10.dp)
      )

      // Clinical Notes / Details
      if (step.details.isNotBlank()) {
        Text(
          text = step.details,
          style = MaterialTheme.typography.bodySmall,
          color = if (isActiveOrNext) theme.primaryText else theme.mutedText,
          lineHeight = 18.sp,
          modifier = Modifier.padding(start = 10.dp)
        )
      }

      // Live Countdown Banner if applicable
      if (countdownText != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFFEF2F2),
          border = BorderStroke(0.6.dp, Color(0xFFFECACA)),
          modifier = Modifier.padding(start = 10.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "${if (isFr) "Prochaine prise dans" else "Next dose in"} : $countdownText",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFDC2626)
            )
          }
        }
      }

      // Bottom Actions: AddedBy, Alarm Bell, Copy, Modifier, Supprimer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${if (isFr) "Par" else "By"} ${step.addedBy}",
          style = MaterialTheme.typography.labelSmall,
          color = theme.mutedText,
          modifier = Modifier.padding(start = 10.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (!isPastItem) {
            IconButton(
              onClick = onToggleAlarm,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = if (step.hasAlarm) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                contentDescription = if (isFr) "Alarme" else "Alarm",
                tint = if (step.hasAlarm) theme.accent else theme.mutedText,
                modifier = Modifier.size(17.dp)
              )
            }
          }

          IconButton(
            onClick = onCopy,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = if (isFr) "Copier" else "Copy", tint = theme.mutedText, modifier = Modifier.size(16.dp))
          }

          TextButton(
            onClick = onEdit,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("protocol_edit_step_${step.id}")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isFr) "Modifier" else "Edit",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.accent
            )
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(48.dp)
              .testTag("protocol_delete_step_${step.id}")
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = if (isFr) "Supprimer" else "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun NovemberCalendarGrid(
  selectedDayNumber: Int,
  theme: ThemeColors,
  language: AppLanguage,
  onDayClick: (Int) -> Unit
) {
  Column(modifier = Modifier.padding(top = 8.dp)) {
    val weekDays = if (language == AppLanguage.FR) {
      listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    } else {
      listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      weekDays.forEach { wd ->
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
          Text(text = wd, style = MaterialTheme.typography.labelSmall, color = theme.mutedText, fontSize = 10.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    val totalLeadingDays = 6
    val daysInNov = 30
    val totalCells = ((totalLeadingDays + daysInNov + 6) / 7) * 7
    val highlightDays = setOf(5, 9, 12, 13, 17, 18, 19, 20, 27)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      for (weekRow in 0 until (totalCells / 7)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          for (col in 0 until 7) {
            val dayNum = weekRow * 7 + col - totalLeadingDays + 1
            if (dayNum in 1..daysInNov) {
              val isSel = selectedDayNumber == dayNum
              val isToday = dayNum == 18
              val hasEvent = dayNum in highlightDays
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                  isSel -> theme.accent
                  isToday -> theme.accent.copy(alpha = 0.16f)
                  else -> theme.background
                },
                border = BorderStroke(0.6.dp, if (isSel || isToday) theme.accent else theme.border),
                modifier = Modifier
                  .weight(1f)
                  .height(28.dp)
                  .padding(horizontal = 1.dp)
                  .clickable { onDayClick(dayNum) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "$dayNum",
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 10.sp,
                      fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSel) Color.White else theme.primaryText
                    )
                    if (hasEvent && !isSel) {
                      Spacer(modifier = Modifier.width(2.dp))
                      Box(
                        modifier = Modifier
                          .size(4.dp)
                          .clip(CircleShape)
                          .background(theme.accent)
                      )
                    }
                  }
                }
              }
            } else {
              Spacer(modifier = Modifier.weight(1f).padding(horizontal = 1.dp))
            }
          }
        }
      }
    }
  }
}
