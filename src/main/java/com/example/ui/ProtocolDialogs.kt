package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncedProtocolStep
import com.example.data.TaskItem
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.util.Calendar

@Composable
fun ProtocolImmediateTaskDialog(
  existingTask: TaskItem?,
  activePartnerProfile: String = "Fatou",
  theme: ThemeColors,
  language: AppLanguage,
  onSave: (TaskItem) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isFr = language == AppLanguage.FR

  var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
  var date by remember(existingTask) { mutableStateOf(existingTask?.date ?: "13 nov.") }
  var category by remember(existingTask) { mutableStateOf(existingTask?.category ?: "Cabinet East Side") }
  var details by remember(existingTask) { mutableStateOf(existingTask?.details ?: "") }
  var isCompleted by remember(existingTask) { mutableStateOf(existingTask?.isCompleted ?: false) }
  var hasAlarm by remember(existingTask) { mutableStateOf(existingTask?.hasAlarm ?: false) }
  var alarmMinutesBefore by remember(existingTask) { mutableIntStateOf(existingTask?.alarmTimeMinutesBefore ?: 15) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Assignment, contentDescription = null, tint = theme.accent)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (existingTask == null) {
            if (isFr) "Nouvelle Action Clinique" else "New Clinical Action"
          } else {
            if (isFr) "Modifier l'Action Clinique" else "Edit Clinical Action"
          },
          color = theme.primaryText,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
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
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(if (isFr) "Titre de l'action *" else "Action Title *") },
          placeholder = { Text("ex: 📅 Confirmation de la date du TEC") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("protocol_immediate_title_input"),
          singleLine = true
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text(if (isFr) "Date" else "Date") },
            placeholder = { Text("18 nov.") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text(if (isFr) "Lieu / Service" else "Clinic / Unit") },
            placeholder = { Text("Cabinet East Side") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        OutlinedTextField(
          value = details,
          onValueChange = { details = it },
          label = { Text(if (isFr) "Statut & Notes cliniques" else "Status & Clinical Notes") },
          placeholder = { Text(if (isFr) "Détails du rendez-vous ou résultat..." else "Appointment details or outcome...") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 5
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isCompleted) Color(0xFFF0FDF4) else theme.background,
          border = BorderStroke(1.dp, if (isCompleted) Color(0xFFBBF7D0) else theme.border),
          onClick = { isCompleted = !isCompleted }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isCompleted) {
                if (isFr) "Statut : Complété / Validé" else "Status: Completed"
              } else {
                if (isFr) "Statut : En cours / Planifié" else "Status: Active / Planned"
              },
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = if (isCompleted) Color(0xFF15803D) else theme.primaryText
            )
            Switch(
              checked = isCompleted,
              onCheckedChange = { isCompleted = it }
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = theme.background,
          border = BorderStroke(1.dp, theme.border),
          onClick = { hasAlarm = !hasAlarm }
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isFr) "🔔 Alarme de rappel" else "🔔 Reminder Alarm",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryText
              )
              Switch(checked = hasAlarm, onCheckedChange = { hasAlarm = it })
            }
            if (hasAlarm) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val offsets = listOf(0 to "0m", 15 to "15m", 60 to "1h", 1440 to "1d")
                items(offsets) { (mins, lbl) ->
                  FilterChip(
                    selected = alarmMinutesBefore == mins,
                    onClick = { alarmMinutesBefore = mins },
                    label = { Text(lbl, maxLines = 1, softWrap = false) }
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            Toast.makeText(
              context,
              if (isFr) "Veuillez saisir un titre" else "Please enter a title",
              Toast.LENGTH_SHORT
            ).show()
          } else {
            val savedTask = TaskItem(
              id = existingTask?.id?.ifBlank { null } ?: "t_${System.currentTimeMillis()}",
              title = title.trim(),
              date = date.trim().ifBlank { "13 nov." },
              category = category.trim().ifBlank { "RMA East Side" },
              details = details.trim(),
              isCompleted = isCompleted,
              hasAlarm = hasAlarm,
              alarmTimeMinutesBefore = alarmMinutesBefore,
              addedBy = existingTask?.addedBy ?: activePartnerProfile
            )
            val epoch = com.example.data.DateSortUtils.parseFlexibleEpochMillis(savedTask.date)
            if (savedTask.hasAlarm && !savedTask.isCompleted && epoch != null) {
              com.example.alarm.AlarmScheduler.scheduleAppointmentAlarm(
                context = context,
                itemId = savedTask.id,
                title = savedTask.title,
                details = savedTask.details.ifBlank { savedTask.category },
                eventEpochMillis = epoch,
                offsetMinutes = savedTask.alarmTimeMinutesBefore.toLong()
              )
            } else {
              com.example.alarm.AlarmScheduler.cancelAppointmentAlarm(context, savedTask.id)
            }
            onSave(savedTask)
          }
        },
        modifier = Modifier.testTag("protocol_immediate_save_button"),
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

@Composable
fun ProtocolStepEditDialog(
  existingStep: SyncedProtocolStep?,
  initialPhaseId: Int,
  initialDateStr: String,
  nextSortOrder: Long,
  activePartnerProfile: String = "Fatou",
  theme: ThemeColors,
  language: AppLanguage,
  onSave: (SyncedProtocolStep) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isFr = language == AppLanguage.FR

  var selectedCategoryTab by remember { mutableStateOf(ProtocolCategory.MEDICATIONS) }
  var phaseId by remember(existingStep, initialPhaseId) {
    mutableIntStateOf((existingStep?.phaseId?.toInt() ?: initialPhaseId).coerceIn(1, 4))
  }
  var dateField by remember(existingStep, initialDateStr) {
    mutableStateOf(existingStep?.dateStr ?: initialDateStr)
  }
  var stepName by remember(existingStep) { mutableStateOf(existingStep?.stepName ?: "") }
  var medication by remember(existingStep) { mutableStateOf(existingStep?.medication ?: "") }
  var dosage by remember(existingStep) { mutableStateOf(existingStep?.dosage ?: "") }
  var status by remember(existingStep) { mutableStateOf(existingStep?.status ?: "Planifié") }
  var details by remember(existingStep) { mutableStateOf(existingStep?.details ?: "") }
  var isCompleted by remember(existingStep) { mutableStateOf(existingStep?.isCompleted ?: false) }
  var isCriticalWindow by remember(existingStep) { mutableStateOf(existingStep?.isCriticalWindow ?: false) }
  var hasAlarm by remember(existingStep) { mutableStateOf(existingStep?.hasAlarm ?: false) }
  var alarmOffsetMinutes by remember(existingStep) { mutableLongStateOf(existingStep?.alarmOffsetMinutes ?: 15L) }

  val statusOptions = listOf("Aujourd'hui", "En cours", "Planifié", "Prioritaire", "Complété")

  val openCalendarTimePicker = {
    val now = Calendar.getInstance()
    val dpd = DatePickerDialog(
      context,
      { _, year, month, day ->
        val tpd = TimePickerDialog(
          context,
          { _, hour, minute ->
            val shortMonths = if (isFr) {
              arrayOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.")
            } else {
              arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            }
            val mLabel = shortMonths.getOrElse(month) { "" }
            val hh = hour.toString().padStart(2, '0')
            val mm = minute.toString().padStart(2, '0')
            dateField = "$day $mLabel $year • ${hh}h$mm"
          },
          now.get(Calendar.HOUR_OF_DAY),
          now.get(Calendar.MINUTE),
          true
        )
        tpd.setTitle(if (isFr) "Sélectionner l'heure" else "Select time")
        tpd.show()
      },
      2026,
      Calendar.NOVEMBER,
      13
    )
    dpd.setTitle(if (isFr) "Sélectionner la Date" else "Select Date")
    dpd.show()
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.EventNote, contentDescription = null, tint = theme.accent)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (existingStep == null) {
            if (isFr) "Ajouter une Étape au Protocole" else "Add Protocol Milestone"
          } else {
            if (isFr) "Modifier l'Étape" else "Edit Protocol Milestone"
          },
          color = theme.primaryText,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Phase selector
        Text(
          text = if (isFr) "Phase du Parcours :" else "Timeline Phase:",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(protocolPhasesMeta) { meta ->
            val isSel = phaseId == meta.phaseId
            FilterChip(
              selected = isSel,
              onClick = { phaseId = meta.phaseId },
              label = { Text("${meta.emoji} Phase ${meta.phaseId}", fontSize = 11.sp, maxLines = 1, softWrap = false) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = theme.accent.copy(alpha = 0.16f),
                selectedLabelColor = theme.accent
              )
            )
          }
        }

        // Status selector
        Text(
          text = if (isFr) "Statut de l'étape :" else "Step Status:",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(statusOptions) { st ->
            val isSel = status == st
            FilterChip(
              selected = isSel,
              onClick = {
                status = st
                isCompleted = (st == "Complété")
                if (st == "Aujourd'hui" || st == "Prioritaire") {
                  isCriticalWindow = true
                }
              },
              label = { Text(st, fontSize = 11.sp, maxLines = 1, softWrap = false) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = when (st) {
                  "Complété" -> Color(0xFFF0FDF4)
                  "Aujourd'hui" -> theme.accent.copy(alpha = 0.18f)
                  "Prioritaire" -> Color(0xFFFEF2F2)
                  else -> theme.accent.copy(alpha = 0.14f)
                },
                selectedLabelColor = when (st) {
                  "Complété" -> Color(0xFF15803D)
                  "Prioritaire" -> Color(0xFFDC2626)
                  else -> theme.accent
                }
              )
            )
          }
        }

        // 1-Click Presets (helpful when adding)
        if (existingStep == null) {
          Text(
            text = if (isFr) "Modèles rapides (optionnel) :" else "Quick Presets (optional):",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(ProtocolCategory.entries) { cat ->
              val isSel = selectedCategoryTab == cat
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSel) theme.accent else theme.background,
                border = BorderStroke(0.5.dp, if (isSel) theme.accent else theme.border),
                modifier = Modifier.clickable { selectedCategoryTab = cat }
              ) {
                Text(
                  text = "${cat.icon} ${if (isFr) cat.labelFr else cat.labelEn}",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = if (isSel) Color.White else theme.primaryText
                )
              }
            }
          }

          val presets = categoryPresetsMap[selectedCategoryTab] ?: emptyList()
          LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(presets) { p ->
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = theme.accent.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, theme.accent),
                modifier = Modifier.clickable {
                  stepName = if (isFr) p.nameFr else p.nameEn
                  medication = p.medication
                  dosage = p.dosage
                  phaseId = p.phaseId
                  details = if (isFr) {
                    "${p.descriptionFr}. ${p.instructionsFr} (Heure : ${p.time})"
                  } else {
                    "${p.descriptionEn}. ${p.instructionsEn} (Time: ${p.time})"
                  }
                }
              ) {
                Text(
                  text = "${p.icon} ${if (isFr) p.nameFr else p.nameEn}",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = theme.accent,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = dateField,
          onValueChange = { dateField = it },
          label = { Text(if (isFr) "Date & Créneau" else "Date & Time") },
          placeholder = { Text("13 nov. (Aujourd'hui)") },
          trailingIcon = {
            IconButton(onClick = openCalendarTimePicker) {
              Icon(Icons.Default.CalendarMonth, contentDescription = "Calendrier", tint = theme.accent)
            }
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = stepName,
          onValueChange = { stepName = it },
          label = { Text(if (isFr) "Titre de l'étape *" else "Milestone Title *") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("protocol_step_title_input"),
          singleLine = true
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = medication,
            onValueChange = { medication = it },
            label = { Text(if (isFr) "Médication / Lieu" else "Medication / Clinic") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text(if (isFr) "Dosage / Info" else "Dosage / Info") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        OutlinedTextField(
          value = details,
          onValueChange = { details = it },
          label = { Text(if (isFr) "Notes cliniques & consignes" else "Clinical Notes & Instructions") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 6
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isFr) "Étape complétée" else "Step completed",
            style = MaterialTheme.typography.bodySmall,
            color = theme.primaryText,
            fontWeight = FontWeight.Medium
          )
          Switch(
            checked = isCompleted,
            onCheckedChange = { checked ->
              isCompleted = checked
              status = if (checked) "Complété" else if (status == "Complété") "En cours" else status
            }
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = theme.background,
          border = BorderStroke(1.dp, theme.border),
          onClick = { hasAlarm = !hasAlarm }
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isFr) "🔔 Alarme de rappel" else "🔔 Reminder Alarm",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryText
              )
              Switch(checked = hasAlarm, onCheckedChange = { hasAlarm = it })
            }
            if (hasAlarm) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val offsets = listOf(0L to "0m", 15L to "15m", 60L to "1h", 1440L to "1d")
                items(offsets) { (mins, lbl) ->
                  FilterChip(
                    selected = alarmOffsetMinutes == mins,
                    onClick = { alarmOffsetMinutes = mins },
                    label = { Text(lbl, maxLines = 1, softWrap = false) }
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (stepName.isBlank()) {
            Toast.makeText(
              context,
              if (isFr) "Veuillez saisir le titre de l'étape" else "Please enter a step title",
              Toast.LENGTH_SHORT
            ).show()
          } else {
            val saved = SyncedProtocolStep(
              id = existingStep?.id?.ifBlank { null } ?: "p_custom_${System.currentTimeMillis()}",
              phaseId = phaseId.toLong(),
              dateStr = dateField.trim().ifBlank { "13 nov." },
              stepName = stepName.trim(),
              medication = medication.trim(),
              dosage = dosage.trim(),
              status = if (isCompleted) "Complété" else status,
              details = details.trim(),
              isCompleted = isCompleted,
              isCriticalWindow = isCriticalWindow,
              targetEpochMillis = existingStep?.targetEpochMillis ?: 0L,
              sortOrder = existingStep?.sortOrder ?: nextSortOrder,
              hasAlarm = hasAlarm,
              alarmOffsetMinutes = alarmOffsetMinutes,
              addedBy = existingStep?.addedBy ?: activePartnerProfile
            )
            val epoch = com.example.data.DateSortUtils.protocolStepEpochMillis(saved)
            if (saved.hasAlarm && !saved.isCompleted && epoch != Long.MAX_VALUE) {
              com.example.alarm.AlarmScheduler.scheduleAppointmentAlarm(
                context = context,
                itemId = saved.id,
                title = saved.stepName,
                details = saved.details.ifBlank { "${saved.medication} ${saved.dosage}" },
                eventEpochMillis = epoch,
                offsetMinutes = saved.alarmOffsetMinutes
              )
            } else {
              com.example.alarm.AlarmScheduler.cancelAppointmentAlarm(context, saved.id)
            }
            onSave(saved)
          }
        },
        modifier = Modifier.testTag("protocol_step_save_button"),
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

@Composable
fun ProtocolResetConfirmDialog(
  theme: ThemeColors,
  language: AppLanguage,
  onConfirmReset: () -> Unit,
  onDismiss: () -> Unit
) {
  val isFr = language == AppLanguage.FR
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFDC2626))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isFr) "Réinitialiser le protocole ?" else "Reset Protocol to Default?",
          color = theme.primaryText,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Text(
        text = if (isFr) {
          "Cette action restaurera les 5 actions cliniques du Tableau de Bord Immédiat ainsi que les 4 phases complètes du calendrier RMA of New York (Dr. Anate Brauer), en remplaçant vos modifications personnalisées."
        } else {
          "This will restore the 5 immediate clinical actions and all 4 medical phases of the RMA of New York protocol back to default, replacing custom edits."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = theme.mutedText,
        lineHeight = 20.sp
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirmReset,
        modifier = Modifier.testTag("protocol_confirm_reset_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
      ) {
        Text(if (isFr) "Réinitialiser" else "Reset", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
      }
    }
  )
}
