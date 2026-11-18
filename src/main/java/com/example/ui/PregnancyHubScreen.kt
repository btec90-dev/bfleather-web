package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncedAppointmentItem
import com.example.data.SyncedChecklistItem
import com.example.data.SyncedMedicalDocItem
import com.example.data.SyncedUserProfile
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun formatHubShortDateFr(): String =
  SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date())

private fun parseDayMonthYear(
  dateStr: String,
  fallbackDay: Long,
  fallbackMonth: Long,
  fallbackYear: Long
): Triple<Long, Long, Long> {
  val parts = dateStr.trim().split(Regex("\\s+"))
  val day = parts.getOrNull(0)?.toLongOrNull()?.coerceIn(1L, 31L) ?: fallbackDay
  val monthToken = parts.getOrNull(1)?.lowercase(Locale.FRENCH) ?: ""
  val month = when {
    monthToken.startsWith("jan") -> 0L
    monthToken.startsWith("fév") || monthToken.startsWith("fev") || monthToken.startsWith("feb") -> 1L
    monthToken.startsWith("mar") -> 2L
    monthToken.startsWith("avr") || monthToken.startsWith("apr") -> 3L
    monthToken.startsWith("mai") || monthToken.startsWith("may") -> 4L
    monthToken.startsWith("juin") || monthToken.startsWith("jun") -> 5L
    monthToken.startsWith("juil") || monthToken.startsWith("jul") -> 6L
    monthToken.startsWith("ao") || monthToken.startsWith("aug") -> 7L
    monthToken.startsWith("sep") -> 8L
    monthToken.startsWith("oct") -> 9L
    monthToken.startsWith("nov") -> 10L
    monthToken.startsWith("déc") || monthToken.startsWith("dec") -> 11L
    else -> fallbackMonth
  }
  val year = parts.getOrNull(2)?.toLongOrNull()?.coerceIn(2020L, 2035L) ?: fallbackYear
  return Triple(day, month, year)
}

@Composable
fun PregnancyHubScreen(
  userEmail: String,
  profile: SyncedUserProfile,
  appointments: List<SyncedAppointmentItem>,
  documents: List<SyncedMedicalDocItem>,
  checklists: List<SyncedChecklistItem>,
  galleryViewModel: GalleryViewModel,
  activePartnerProfile: String = "Fatou",
  onConfirmPregnancy: () -> Unit = {},
  onUpdateProfile: (name: String, age: String, week: Int, dueDate: String) -> Unit,
  onUpdateWeek: (Int) -> Unit,
  onUpdateWater: (Int) -> Unit,
  onSaveHealthVitals: (mood: String, weight: String, water: Int, bp: String, notesText: String) -> Unit,
  onSaveAppointment: (SyncedAppointmentItem) -> Unit,
  onToggleAppointment: (aptId: String, currentChecked: Boolean) -> Unit,
  onSaveDocument: (SyncedMedicalDocItem) -> Unit,
  onSaveChecklistItem: (SyncedChecklistItem) -> Unit,
  onToggleChecklistItem: (itemId: String, currentChecked: Boolean) -> Unit,
  onNavigateToExpensePrefill: (ExpensePrefill) -> Unit = {},
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isFr = language == AppLanguage.FR

  // Synced Maternal Profile State
  val patientName = profile.patientName
  val patientAge = profile.patientAge
  val currentWeek = profile.currentWeek.toInt()
  val dueDate = profile.dueDate
  val daysRemaining = profile.daysRemaining.toInt()
  var showEditProfileDialog by remember { mutableStateOf(false) }

  // Monthly Calendar State (Default November 2026)
  var calendarYear by remember { mutableIntStateOf(2026) }
  var calendarMonth by remember { mutableIntStateOf(10) } // 0-based: 10 = November
  var selectedCalendarDay by remember { mutableIntStateOf(13) }
  var showAddCalendarEventDialog by remember { mutableStateOf(false) }

  // Daily Health & Vitals Draft State synced from profile
  var healthMoodInput by remember(profile.healthMood) { mutableStateOf(profile.healthMood) }
  var healthWeightInput by remember(profile.healthWeight) { mutableStateOf(profile.healthWeight) }
  var bloodPressureInput by remember(profile.bloodPressure) { mutableStateOf(profile.bloodPressure) }
  var personalNotesInput by remember(profile.personalNotesText) { mutableStateOf(profile.personalNotesText) }
  val waterGlasses = profile.waterGlasses.toInt()

  // Modals State
  var showAddDocDialog by remember { mutableStateOf(false) }
  var showCompleteArchiveDialog by remember { mutableStateOf(false) }
  var showAddAppointmentDialog by remember { mutableStateOf(false) }
  var showAddChecklistItemDialog by remember { mutableStateOf(false) }
  var targetChecklist by remember { mutableStateOf("Maman") }

  // Filtered Checklists from Firestore
  val momBagItems = remember(checklists) { checklists.filter { it.listKey == "mom_bag" } }
  val babyBagItems = remember(checklists) { checklists.filter { it.listKey == "baby_bag" } }
  val partnerBagItems = remember(checklists) { checklists.filter { it.listKey == "partner_bag" } }
  val beforeBirthItems = remember(checklists) { checklists.filter { it.listKey == "before_birth" } }
  val afterBirthItems = remember(checklists) { checklists.filter { it.listKey == "after_birth" } }

  // Dynamic calendar calculations for current month/year
  val monthCal = remember(calendarYear, calendarMonth) {
    Calendar.getInstance().apply {
      set(Calendar.YEAR, calendarYear)
      set(Calendar.MONTH, calendarMonth)
      set(Calendar.DAY_OF_MONTH, 1)
    }
  }
  val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val startOffset = ((monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) + 7) % 7

  val selectedDayAppointments = remember(appointments, selectedCalendarDay, calendarMonth, calendarYear) {
    appointments.filter {
      it.dayOfMonth.toInt() == selectedCalendarDay &&
        it.monthIndex.toInt() == calendarMonth &&
        it.yearNum.toInt() == calendarYear
    }
  }

  // -----------------------------------------------------------
  // MAIN SCREEN CONTENT (All on one screen - No left/right tabs)
  // -----------------------------------------------------------
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
  ) {
    // ---------------------------------------------------------
    // HEADER: Profile Summary & Direct Archive Access
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
                color = theme.accent.copy(alpha = 0.16f),
                modifier = Modifier.size(46.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(profile.momAvatar.ifBlank { "🤰" }, fontSize = 24.sp)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "$patientName ($patientAge ans)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = "Semaine $currentWeek • DPA : $dueDate",
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(100.dp),
              color = theme.accent.copy(alpha = 0.15f),
              border = BorderStroke(0.5.dp, theme.accent)
            ) {
              Text(
                text = "J-$daysRemaining",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = theme.accent
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { showEditProfileDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              modifier = Modifier
                .weight(1f)
                .testTag("pregnancy_edit_profile_button"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Modifier Profil", tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.FR) "Modifier Profil" else "Edit Profile", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }

            OutlinedButton(
              onClick = { showCompleteArchiveDialog = true },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, theme.border),
              modifier = Modifier
                .weight(1f)
                .testTag("pregnancy_full_archive_button"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(Icons.Default.FolderSpecial, contentDescription = "Archive Complète", tint = theme.accent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.FR) "Archive Complète" else "Full Archive", style = MaterialTheme.typography.labelSmall, color = theme.primaryText)
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION: GALERIE PHOTO & ALBUM MAMAN & BÉBÉ (Moved to Top)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
              Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Galerie Photo & Album Maman & Bébé" else "Mom & Baby Photo Gallery & Album",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(480.dp)
          ) {
            PhotoGalleryGrid(
              viewModel = galleryViewModel,
              theme = theme,
              language = language,
              modifier = Modifier.fillMaxSize(),
              activePartnerProfile = activePartnerProfile,
              onLogExpenseFromPrescription = { title, cat ->
                onNavigateToExpensePrefill(
                  ExpensePrefill(
                    title = title,
                    category = cat,
                    notes = if (language == AppLanguage.FR) "Créé depuis l'ordonnance / galerie" else "Created from prescription gallery",
                    attachmentTitle = title
                  )
                )
              }
            )
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 1: CALENDRIER MENSUEL INTERACTIF
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
              Icon(Icons.Default.CalendarMonth, contentDescription = "Calendrier", tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${getMonthName(calendarMonth, language)} $calendarYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = {
                  if (calendarMonth == 0) {
                    calendarMonth = 11
                    calendarYear--
                  } else {
                    calendarMonth--
                  }
                  selectedCalendarDay = 1
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent", tint = theme.primaryText)
              }
              IconButton(
                onClick = {
                  if (calendarMonth == 11) {
                    calendarMonth = 0
                    calendarYear++
                  } else {
                    calendarMonth++
                  }
                  selectedCalendarDay = 1
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant", tint = theme.primaryText)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          val dayHeaders = if (language == AppLanguage.FR) listOf("L", "M", "M", "J", "V", "S", "D") else listOf("M", "T", "W", "T", "F", "S", "S")
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            dayHeaders.forEach { h ->
              Text(h, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = theme.mutedText, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until totalCells / 7) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (dayIndex in 0 until 7) {
                  val dayNum = week * 7 + dayIndex - startOffset + 1
                  if (dayNum in 1..daysInMonth) {
                    val isSelected = selectedCalendarDay == dayNum
                    val hasAppointment = appointments.any {
                      it.dayOfMonth.toInt() == dayNum &&
                        it.monthIndex.toInt() == calendarMonth &&
                        it.yearNum.toInt() == calendarYear
                    }

                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSelected) theme.accent else if (hasAppointment) theme.accent.copy(alpha = 0.16f) else Color.Transparent,
                      border = if (isSelected) null else if (hasAppointment) BorderStroke(1.dp, theme.accent) else null,
                      modifier = Modifier
                        .size(36.dp)
                        .clickable { selectedCalendarDay = dayNum }
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                          Text(
                            text = dayNum.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected || hasAppointment) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else theme.primaryText
                          )
                          if (hasAppointment && !isSelected) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(theme.accent))
                          }
                        }
                      }
                    }
                  } else {
                    Box(modifier = Modifier.size(36.dp))
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.background,
            border = BorderStroke(1.dp, theme.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = if (isFr) "Jour sélectionné : $selectedCalendarDay ${getMonthName(calendarMonth, language)} $calendarYear"
                  else "Selected Day: $selectedCalendarDay ${getMonthName(calendarMonth, language)} $calendarYear",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = if (selectedDayAppointments.isNotEmpty()) {
                    "${selectedDayAppointments.size} ${if (isFr) "événement(s)" else "event(s)"}: " +
                      selectedDayAppointments.joinToString(" • ") { it.title }
                  } else {
                    if (isFr) "Estrace 2 mg PO (Matin & Soir)" else "Estrace 2 mg PO (AM & PM)"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.accent
                )
              }
              Button(
                onClick = { showAddCalendarEventDialog = true },
                modifier = Modifier.testTag("pregnancy_calendar_add_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = if (isFr) "Ajouter événement" else "Add event", tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isFr) "+ Ajouter" else "+ Add", style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, softWrap = false)
              }
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 2: MENU VISITES & RENDEZ-VOUS MÉDICAUX (Upcoming-First)
    // ---------------------------------------------------------
    item {
      val partitionedApts = remember(appointments) {
        com.example.data.DateSortUtils.partitionUpcomingFirst(
          items = appointments,
          nowMillis = System.currentTimeMillis(),
          epochProvider = { com.example.data.DateSortUtils.appointmentEpochMillis(it) },
          completedProvider = { it.isCompleted }
        )
      }
      var showPastVisits by remember { mutableStateOf(false) }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
              Icon(Icons.Default.MedicalServices, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (isFr) "Menu Visites & Rendez-vous" else "Prenatal Visits & Appointments",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = if (isFr) "${appointments.size} visites programmées" else "${appointments.size} scheduled visits",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText
                )
              }
            }

            IconButton(
              onClick = { showAddAppointmentDialog = true },
              modifier = Modifier
                .size(48.dp)
                .testTag("pregnancy_add_appointment_button")
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .background(theme.accent, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Add, contentDescription = if (isFr) "Ajouter visite" else "Add visit", tint = Color.White, modifier = Modifier.size(18.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            partitionedApts.upcoming.forEach { apt ->
              AppointmentTimelineCard(
                apt = apt,
                isPastItem = false,
                isFr = isFr,
                theme = theme,
                language = language,
                onToggleCompleted = { onToggleAppointment(apt.id, apt.isCompleted) },
                onToggleAlarm = {
                  val nextAlarm = !apt.hasAlarm
                  val updated = apt.copy(hasAlarm = nextAlarm)
                  val epoch = com.example.data.DateSortUtils.appointmentEpochMillis(updated)
                  if (nextAlarm) {
                    com.example.alarm.AlarmScheduler.scheduleAppointmentAlarm(
                      context = context,
                      itemId = updated.id,
                      title = updated.title,
                      details = "${updated.doctor} • ${updated.location}",
                      eventEpochMillis = epoch,
                      offsetMinutes = updated.alarmOffsetMinutes
                    )
                    Toast.makeText(context, com.example.i18n.Strings.t("alarm_scheduled_msg", language), Toast.LENGTH_SHORT).show()
                  } else {
                    com.example.alarm.AlarmScheduler.cancelAppointmentAlarm(context, updated.id)
                  }
                  onSaveAppointment(updated)
                }
              )
            }

            if (partitionedApts.past.isNotEmpty()) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = theme.background,
                border = BorderStroke(0.8.dp, theme.border),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showPastVisits = !showPastVisits }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${com.example.i18n.Strings.t("past_divider", language)} (${partitionedApts.past.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.mutedText
                  )
                  Icon(
                    if (showPastVisits) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = theme.mutedText
                  )
                }
              }

              if (showPastVisits) {
                partitionedApts.past.forEach { apt ->
                  AppointmentTimelineCard(
                    apt = apt,
                    isPastItem = true,
                    isFr = isFr,
                    theme = theme,
                    language = language,
                    onToggleCompleted = { onToggleAppointment(apt.id, apt.isCompleted) },
                    onToggleAlarm = {}
                  )
                }
              }
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 2: JALONS DE GROSSESSE (SEMAINES 1 À 41) - ÉDITABLE & SANS 3D
    // ---------------------------------------------------------
    item {
      PregnancyMilestonesSection(
        currentWeek = currentWeek,
        theme = theme,
        language = language,
        onWeekSelected = { onUpdateWeek(it) }
      )
    }

    // ---------------------------------------------------------
    // SECTION 4: NUTRITION MAMAN & BÉBÉ
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
              Icon(Icons.Default.Restaurant, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Nutrition Maman & Bébé" else "Mom & Baby Nutrition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
            Surface(
              shape = RoundedCornerShape(100.dp),
              color = theme.accent.copy(alpha = 0.15f)
            ) {
              Text(
                text = when {
                  currentWeek < 14 -> "Trimestre 1"
                  currentWeek < 28 -> "Trimestre 2"
                  else -> "Trimestre 3"
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = theme.accent,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Hydration tracker (Interactive Water glasses synced to Firestore)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.background,
            border = BorderStroke(1.dp, theme.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Hydratation Quotidienne :", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                Text("$waterGlasses verres (${waterGlasses * 250} ml)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                  onClick = { if (waterGlasses > 0) onUpdateWater(waterGlasses - 1) },
                  modifier = Modifier
                    .size(34.dp)
                    .background(theme.cardSurface, CircleShape)
                    .testTag("pregnancy_water_minus_button")
                ) {
                  Text("-", style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
                }
                IconButton(
                  onClick = { onUpdateWater(waterGlasses + 1) },
                  modifier = Modifier
                    .size(34.dp)
                    .background(theme.accent, CircleShape)
                    .testTag("pregnancy_water_plus_button")
                ) {
                  Text("+", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          val foodPoints = listOf(
            "🌱 Folates (Vitamine B9) : Épinards, lentilles, pois chiches pour le tube neural.",
            "🥩 Fer & Vitamine C : Viandes maigres, agrumes, persil pour le volume sanguin.",
            "💧 Eau & Tisanes : Boire au minimum 1,5 à 2 litres par jour pour le liquide amniotique.",
            "🚫 À Proscrire : Aliments crus, fromages au lait cru, poissons prédateurs riches en mercure."
          )
          foodPoints.forEach { pt ->
            Text(pt, style = MaterialTheme.typography.bodySmall, color = theme.primaryText, modifier = Modifier.padding(vertical = 3.dp), lineHeight = 17.sp)
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 5: OUTILS & SANTÉ MATERNELLE (Editable & Synced)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (language == AppLanguage.FR) "Outils & Santé Maternelle" else "Health & Maternal Tools",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = healthWeightInput,
              onValueChange = { healthWeightInput = it },
              label = { Text("Poids (kg)", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
              value = bloodPressureInput,
              onValueChange = { bloodPressureInput = it },
              label = { Text("Tension", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Mood selector chips
          Text("Humeur :", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
          Spacer(modifier = Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Calme", "Sereine", "Fatiguée", "Émotive", "En forme")) { moodOption ->
              FilterChip(
                selected = healthMoodInput == moodOption,
                onClick = { healthMoodInput = moodOption },
                label = { Text(moodOption, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = personalNotesInput,
            onValueChange = { personalNotesInput = it },
            label = { Text("Notes personnelles & symptômes du jour", style = MaterialTheme.typography.bodySmall) },
            placeholder = { Text("Consignez vos ressentis ou observations...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = {
              onSaveHealthVitals(
                healthMoodInput,
                healthWeightInput,
                waterGlasses,
                bloodPressureInput,
                personalNotesInput
              )
              Toast.makeText(context, "Paramètres santé & notes sauvegardés !", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pregnancy_save_vitals_button"),
            contentPadding = PaddingValues(vertical = 8.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = "Enregistrer", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Enregistrer les Paramètres Santé", style = MaterialTheme.typography.labelSmall, color = Color.White)
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 6: COFFRE & DOCUMENTS MÉDICAUX
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
              Icon(Icons.Default.Shield, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Coffre & Documents Médicaux" else "Vault & Medical Documents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
            TextButton(
              onClick = { showAddDocDialog = true },
              modifier = Modifier.testTag("pregnancy_add_doc_button")
            ) {
              Text("+ Ajouter", color = theme.accent, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            documents.forEach { doc ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = theme.background,
                border = BorderStroke(1.dp, theme.border)
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    SelectionContainer {
                      Column {
                        Text(doc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = theme.primaryText)
                        Text("${doc.category} • ${doc.dateStr}", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(doc.summary, style = MaterialTheme.typography.bodySmall, color = theme.primaryText.copy(alpha = 0.8f))
                      }
                    }
                  }
                  IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString("${doc.title} - ${doc.summary}"))
                    Toast.makeText(context, "Copié !", Toast.LENGTH_SHORT).show()
                  }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = theme.accent, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }



    // ---------------------------------------------------------
    // SECTION 7: GUIDE & ÉCHOGRAPHIES (Conseils & Jalons T1-T3)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (language == AppLanguage.FR) "Guide & Échographies" else "Guides & Ultrasounds",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          val guidesList = listOf(
            "Échographie T1 (11-13 SA)" to "Mesure de la clarté nucale, datation précise et détection précoce.",
            "Échographie Morphologique T2 (20-24 SA)" to "Analyse détaillée des organes, biométrie et croissance.",
            "Échographie T3 (31-34 SA)" to "Position fœtale, estimation pondérale et bien-être placentaire.",
            "Préparation à la Naissance & Allaitement" to "Respiration, postures de travail et mise au sein précoce."
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            guidesList.forEach { (title, desc) ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.background,
                border = BorderStroke(0.5.dp, theme.border)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
                  Text(desc, style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                }
              }
            }
          }
        }
      }
    }

    // ---------------------------------------------------------
    // SECTION 8: ORGANISATION & VALISE MATERNITÉ (Checklists)
    // ---------------------------------------------------------
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
              Icon(Icons.Default.Luggage, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (language == AppLanguage.FR) "Organisation & Valise Maternité" else "Checklists & Hospital Bag",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
            TextButton(
              onClick = { showAddChecklistItemDialog = true },
              modifier = Modifier.testTag("pregnancy_add_checklist_button")
            ) {
              Text("+ Ajouter Article", color = theme.accent, style = MaterialTheme.typography.labelSmall)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text("Valise Maternité (Maman) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            momBagItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("Valise Maternité (Bébé - Jour J) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            babyBagItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("Valise Accompagnant (Papa) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            partnerBagItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("À Acheter Avant la Naissance :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            beforeBirthItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text("À Acheter Après la Naissance :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            afterBirthItems.forEach { item ->
              ChecklistRow(item = item, theme = theme, onToggle = { onToggleChecklistItem(item.id, item.isChecked) })
            }
          }
        }
      }
    }
  }

  // -----------------------------------------------------------
  // DIALOGS & EDITABLE MODALS
  // -----------------------------------------------------------

  // Edit Profile Modal
  if (showEditProfileDialog) {
    var editName by remember(patientName) { mutableStateOf(patientName) }
    var editAge by remember(patientAge) { mutableStateOf(patientAge) }
    var editWeek by remember(currentWeek) { mutableStateOf(currentWeek.toString()) }
    var editDueDate by remember(dueDate) { mutableStateOf(dueDate) }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      title = { Text(if (language == AppLanguage.FR) "Modifier le Profil Maman" else "Edit Maternal Profile", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Âge") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = editWeek, onValueChange = { editWeek = it }, label = { Text("Semaine d'aménorrhée (SA)") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = editDueDate, onValueChange = { editDueDate = it }, label = { Text("Date d'accouchement estimée") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateProfile(
              editName.ifBlank { patientName },
              editAge.ifBlank { patientAge },
              editWeek.toIntOrNull() ?: currentWeek,
              editDueDate.ifBlank { dueDate }
            )
            showEditProfileDialog = false
            Toast.makeText(context, "Profil mis à jour & synchronisé !", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Enregistrer")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Add Appointment Modal (Menu Visite)
  if (showAddAppointmentDialog) {
    var newAptTitle by remember { mutableStateOf("") }
    var newAptDate by remember { mutableStateOf("13 Nov 2026") }
    var newAptTime by remember { mutableStateOf("09:00") }
    var newAptDoc by remember { mutableStateOf("Dr. Laurent (OB/GYN)") }
    var newAptLoc by remember { mutableStateOf("Maternité / Clinique") }
    var newAptNotes by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddAppointmentDialog = false },
      title = { Text(if (isFr) "Ajouter un Rendez-vous Prénatal" else "Add Prenatal Appointment", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = newAptTitle, onValueChange = { newAptTitle = it }, label = { Text(if (isFr) "Titre de la consultation" else "Visit Title") }, placeholder = { Text(if (isFr) "ex: Échographie T2" else "e.g. Anatomy Scan") }, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = newAptDate, onValueChange = { newAptDate = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = newAptTime, onValueChange = { newAptTime = it }, label = { Text(if (isFr) "Heure" else "Time") }, modifier = Modifier.weight(1f))
          }
          OutlinedTextField(value = newAptDoc, onValueChange = { newAptDoc = it }, label = { Text(if (isFr) "Médecin / Praticien" else "Doctor / Provider") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = newAptLoc, onValueChange = { newAptLoc = it }, label = { Text(if (isFr) "Lieu / Centre" else "Location / Clinic") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = newAptNotes, onValueChange = { newAptNotes = it }, label = { Text(if (isFr) "Notes cliniques" else "Clinical Notes") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newAptTitle.isNotBlank()) {
              val (parsedDay, parsedMonth, parsedYear) = parseDayMonthYear(
                dateStr = newAptDate,
                fallbackDay = selectedCalendarDay.toLong(),
                fallbackMonth = calendarMonth.toLong(),
                fallbackYear = calendarYear.toLong()
              )
              onSaveAppointment(
                SyncedAppointmentItem(
                  id = "apt_${System.currentTimeMillis()}",
                  title = newAptTitle,
                  dateStr = newAptDate,
                  timeStr = newAptTime,
                  doctor = newAptDoc,
                  location = newAptLoc,
                  clinicalNotes = newAptNotes,
                  eventType = if (isFr) "Rendez-vous" else "Appointment",
                  dayOfMonth = parsedDay,
                  monthIndex = parsedMonth,
                  yearNum = parsedYear,
                  isCompleted = false,
                  addedBy = activePartnerProfile
                )
              )
              showAddAppointmentDialog = false
              Toast.makeText(context, if (isFr) "Rendez-vous ajouté & synchronisé !" else "Appointment saved & synced!", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(if (isFr) "Ajouter" else "Add", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddAppointmentDialog = false }) {
          Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }

  // Add Calendar Event Modal (Calendar day add)
  if (showAddCalendarEventDialog) {
    var eventTitle by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf(if (isFr) "Rendez-vous" else "Appointment") }

    AlertDialog(
      onDismissRequest = { showAddCalendarEventDialog = false },
      title = {
        Text(
          text = if (isFr) "Ajouter pour le $selectedCalendarDay ${getMonthName(calendarMonth, language)}"
          else "Add for $selectedCalendarDay ${getMonthName(calendarMonth, language)}",
          color = theme.primaryText
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = eventTitle,
            onValueChange = { eventTitle = it },
            label = { Text("Description") },
            placeholder = { Text(if (isFr) "ex: Prise Estrace ou Prise de sang" else "e.g. Estrace Dose or Bloodwork") },
            modifier = Modifier.fillMaxWidth()
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val types = if (isFr) listOf("Rendez-vous", "Médicament", "Rappel") else listOf("Appointment", "Medication", "Reminder")
            items(types) { type ->
              FilterChip(
                selected = eventType == type,
                onClick = { eventType = type },
                label = { Text(type, maxLines = 1, softWrap = false) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (eventTitle.isNotBlank()) {
              onSaveAppointment(
                SyncedAppointmentItem(
                  id = "apt_${System.currentTimeMillis()}",
                  title = eventTitle,
                  dateStr = "$selectedCalendarDay ${getMonthName(calendarMonth, language)} $calendarYear",
                  timeStr = "09:00",
                  doctor = "Dr. Laurent (OB/GYN)",
                  location = if (isFr) "Maternité" else "Maternity Clinic",
                  clinicalNotes = if (isFr) "Ajouté depuis le calendrier mensuel ($eventType)" else "Added from monthly calendar ($eventType)",
                  eventType = eventType,
                  dayOfMonth = selectedCalendarDay.toLong(),
                  monthIndex = calendarMonth.toLong(),
                  yearNum = calendarYear.toLong(),
                  isCompleted = false,
                  addedBy = activePartnerProfile
                )
              )
              showAddCalendarEventDialog = false
              Toast.makeText(context, if (isFr) "Événement planifié & synchronisé !" else "Event scheduled & synced!", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(if (isFr) "Enregistrer" else "Save", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddCalendarEventDialog = false }) {
          Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }

  // Add Document Dialog
  if (showAddDocDialog) {
    var docTitle by remember { mutableStateOf("") }
    var docCat by remember { mutableStateOf(if (isFr) "Ordonnance" else "Prescription") }
    var docSummary by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddDocDialog = false },
      title = { Text(if (isFr) "Nouveau Document Médical" else "New Medical Document", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = docTitle, onValueChange = { docTitle = it }, label = { Text(if (isFr) "Titre du document" else "Document Title") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = docCat, onValueChange = { docCat = it }, label = { Text(if (isFr) "Catégorie (Écho, Ordonnance, Bilan)" else "Category (Scan, Prescription, Lab)") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = docSummary, onValueChange = { docSummary = it }, label = { Text(if (isFr) "Résumé ou compte-rendu" else "Summary / Report") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (docTitle.isNotBlank()) {
              onSaveDocument(
                SyncedMedicalDocItem(
                  id = "doc_${System.currentTimeMillis()}",
                  title = docTitle,
                  category = docCat,
                  dateStr = formatHubShortDateFr(),
                  summary = docSummary,
                  source = "hub",
                  addedBy = activePartnerProfile
                )
              )
              showAddDocDialog = false
              Toast.makeText(context, if (isFr) "Document sauvegardé & synchronisé !" else "Document saved & synced!", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Sauvegarder")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDocDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Add Checklist Item Dialog
  if (showAddChecklistItemDialog) {
    var newChecklistLabel by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddChecklistItemDialog = false },
      title = { Text("Ajouter un article à la liste", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = newChecklistLabel, onValueChange = { newChecklistLabel = it }, label = { Text("Article / Équipement") }, modifier = Modifier.fillMaxWidth())
          Text("Sélectionnez la liste cible :", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Maman", "Bébé", "Papa", "Avant Naissance", "Après Naissance")) { target ->
              FilterChip(
                selected = targetChecklist == target,
                onClick = { targetChecklist = target },
                label = { Text(target) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newChecklistLabel.isNotBlank()) {
              val listKey = when (targetChecklist) {
                "Maman" -> "mom_bag"
                "Bébé" -> "baby_bag"
                "Papa" -> "partner_bag"
                "Avant Naissance" -> "before_birth"
                else -> "after_birth"
              }
              onSaveChecklistItem(
                SyncedChecklistItem(
                  id = "cl_${System.currentTimeMillis()}",
                  listKey = listKey,
                  label = newChecklistLabel,
                  isChecked = false,
                  sortOrder = checklists.size.toLong()
                )
              )
              showAddChecklistItemDialog = false
              Toast.makeText(context, "Article ajouté & synchronisé !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Ajouter")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddChecklistItemDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Complete Archive View Dialog (Dynamic Synced Summary)
  if (showCompleteArchiveDialog) {
    val checkedCount = checklists.count { it.isChecked }
    val archiveSummary = """
      MAMAN & BÉBÉ - ARCHIVE CLINIQUE COMPLÈTE
      Patiente : $patientName ($patientAge ans) • Compte : $userEmail
      Semaine $currentWeek • DPA : $dueDate ($daysRemaining jours restants)
      Hydratation : $waterGlasses verres • Poids : ${profile.healthWeight} kg • Tension : ${profile.bloodPressure}
      ${appointments.size} Visites Prénatales enregistrées • ${documents.size} Documents médicaux
      Checklists maternité : $checkedCount / ${checklists.size} éléments préparés
    """.trimIndent()

    AlertDialog(
      onDismissRequest = { showCompleteArchiveDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.FolderZip, contentDescription = null, tint = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text("MAMAN & BÉBÉ - ARCHIVE CLINIQUE", color = theme.primaryText, style = MaterialTheme.typography.titleMedium)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = archiveSummary,
            style = MaterialTheme.typography.bodySmall,
            color = theme.mutedText
          )
          Spacer(modifier = Modifier.height(4.dp))
          Button(
            onClick = {
              clipboardManager.setText(AnnotatedString(archiveSummary))
              Toast.makeText(context, "Archive copiée dans le presse-papier !", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copier archive", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copier l'Archive Complète", color = Color.White)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCompleteArchiveDialog = false }) {
          Text("Fermer", color = theme.accent)
        }
      }
    )
  }
}

// -------------------------------------------------------------
// Interactive Checklist Item Row (Pure Reactive State)
// -------------------------------------------------------------
@Composable
private fun ChecklistRow(
  item: SyncedChecklistItem,
  theme: ThemeColors,
  onToggle: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggle() },
    shape = RoundedCornerShape(8.dp),
    color = theme.background,
    border = BorderStroke(0.5.dp, theme.border)
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = if (item.isChecked) "Coché" else "Non coché",
        tint = if (item.isChecked) Color(0xFF16A34A) else theme.mutedText,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = item.label,
        style = MaterialTheme.typography.bodySmall,
        color = if (item.isChecked) theme.mutedText else theme.primaryText,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )
    }
  }
}

/*
// -------------------------------------------------------------
// Fetal Development Overview (Week 1 to 41 Realistic Milestone Visualizer)
// -------------------------------------------------------------
private data class WeekFetalMilestone(
  val week: Int,
  val sizeCm: String,
  val weightGrams: String,
  val fruitOrObject: String,
  val fruitEmoji: String,
  val trimester: Int,
  val anatomicalFocus: String,
  val description: String,
  val maternalTip: String,
  val tags: List<String>
)

private fun getWeekFetalMilestone(week: Int, isFr: Boolean): WeekFetalMilestone {
  val w = week.coerceIn(1, 41)
  return when (w) {
    1 -> WeekFetalMilestone(
      week = 1, sizeCm = "0.1 mm", weightGrams = "< 1 mg", fruitOrObject = if (isFr) "Ovule fécondé" else "Fertilized Ovum", fruitEmoji = "✨",
      trimester = 1, anatomicalFocus = if (isFr) "Fécondation & Fusion cellulaire" else "Fertilization & Genetic Fusion",
      description = if (isFr) "Fécondation et formation de la cellule œuf (zygote). Les 46 chromosomes sont réunis pour fixer l'ADN unique du futur bébé." else "Fertilization and zygote formation uniting the 46 chromosomes.",
      maternalTip = if (isFr) "Prendre de l'acide folique (400–800 mcg) quotidiennement pour soutenir le tube neural." else "Take daily folic acid to support neural tube development.",
      tags = listOf("🧬 Caryotype fixé", "✨ Stade zygote", "🔬 Division cellulaire")
    )
    2 -> WeekFetalMilestone(
      week = 2, sizeCm = "0.2 mm", weightGrams = "< 1 mg", fruitOrObject = if (isFr) "Blastocyste J5" else "Blastocyst Day 5", fruitEmoji = "🔬",
      trimester = 1, anatomicalFocus = if (isFr) "Stade blastocyste & bouton embryonnaire" else "Blastocyst & Inner Cell Mass",
      description = if (isFr) "Division cellulaire exponentielle jusqu'à une centaine de cellules (blastocyste J5/J6). La masse interne deviendra le bébé et le trophoblaste formera le placenta." else "Rapid cell division reaching 100+ cells (Day 5 blastocyst ready for transfer).",
      maternalTip = if (isFr) "Soutien progestatif (PIO) régulier pour favoriser la réceptivité endométriale." else "Maintain regular progesterone injections for endometrial receptivity.",
      tags = listOf("🔬 100+ cellules", "🛡️ Trophoblaste actif", "🎯 Prêt au transfert")
    )
    3 -> WeekFetalMilestone(
      week = 3, sizeCm = "0.4 mm", weightGrams = "< 1 mg", fruitOrObject = if (isFr) "Graine de vanille" else "Vanilla Seed", fruitEmoji = "🌱",
      trimester = 1, anatomicalFocus = if (isFr) "Implantation & Nidation utérine" else "Implantation & Uterine Nidation",
      description = if (isFr) "Le blastocyste s'enfouit doucement dans l'endomètre réceptif. Les cellules trophoblastiques commencent à sécréter la précieuse hormone hCG." else "The blastocyst implants in the endometrial wall and begins secreting hCG.",
      maternalTip = if (isFr) "Repos relatif, bonne hydratation et éviter les bains très chauds ou saunas." else "Gentle rest, hydration, and avoid excessive heat sources.",
      tags = listOf("🌱 Nidation active", "🧪 Sécrétion Bêta-hCG", "🛡️ Accueil endométrial")
    )
    4 -> WeekFetalMilestone(
      week = 4, sizeCm = "1.0 mm", weightGrams = "< 1 g", fruitOrObject = if (isFr) "Graine de pavot" else "Poppy Seed", fruitEmoji = "🌾",
      trimester = 1, anatomicalFocus = if (isFr) "Disque embryonnaire & sac amniotique" else "Embryonic Disk & Amniotic Sac",
      description = if (isFr) "Différenciation en trois feuillets primordiaux (ectoderme, mésoderme, endoderme) qui donneront naissance à tous les futurs organes." else "Differentiation into 3 germ layers forming all future bodily organs.",
      maternalTip = if (isFr) "Le test sanguin Bêta-hCG confirme la grossesse biologique avec certitude." else "Beta-hCG blood test reliably confirms biological pregnancy.",
      tags = listOf("🌾 3 feuillets embryonnaires", "🩸 Détection Bêta-hCG", "🫧 Sac gestationnel")
    )
    5 -> WeekFetalMilestone(
      week = 5, sizeCm = "2.0 mm", weightGrams = "< 0.1 g", fruitOrObject = if (isFr) "Graine de sésame" else "Sesame Seed", fruitEmoji = "🌱",
      trimester = 1, anatomicalFocus = if (isFr) "Tube neural & pulsations primitives" else "Neural Tube & Heart Pulsations",
      description = if (isFr) "Fermeture du tube neural (cerveau et moelle épinière). Le premier tube cardiaque primitif commence ses toutes premières contractions." else "Neural tube closure and initial rhythmic contractions of the primitive heart tube.",
      maternalTip = if (isFr) "Petits repas fractionnés et tisane de gingembre en cas de premières nausées." else "Small frequent meals and ginger tea for mild early nausea.",
      tags = listOf("❤️ Premières pulsations", "🧠 Tube neural fermé", "🌱 Ébauche vasculaire")
    )
    6 -> WeekFetalMilestone(
      week = 6, sizeCm = "4.0 mm", weightGrams = "0.2 g", fruitOrObject = if (isFr) "Grain de lentille" else "Lentil", fruitEmoji = "🌿",
      trimester = 1, anatomicalFocus = if (isFr) "Battements cardiaques & bourgeons des membres" else "Heartbeat & Limb Buds",
      description = if (isFr) "Le cœur bat à 100–120 bpm ! Les petites ébauches des bras et des jambes apparaissent sous forme de minuscules bourgeons." else "Heart is beating at 100–120 bpm. Limb buds for arms and legs become visible.",
      maternalTip = if (isFr) "Première échographie de viabilité pour visualiser le sac gestationnel et la vésicule vitelline." else "First viability ultrasound to observe gestational sac and yolk sac.",
      tags = listOf("💓 Cœur régulier 110 bpm", "👀 Vésicules optiques", "🌿 Bourgeons des membres")
    )
    7 -> WeekFetalMilestone(
      week = 7, sizeCm = "1.0 cm", weightGrams = "0.5 g", fruitOrObject = if (isFr) "Myrtille" else "Blueberry", fruitEmoji = "🫐",
      trimester = 1, anatomicalFocus = if (isFr) "Hémisphères cérébraux & visage" else "Cerebral Hemispheres & Facial Outline",
      description = if (isFr) "Croissance fulgurante du cerveau (100 000 neurones générés par minute !). Ébauche des narines, de la bouche et de la rétine des yeux." else "Rapid brain growth (100k neurons/min). Nostrils, mouth, and retina form.",
      maternalTip = if (isFr) "Boire 1.5L à 2L d'eau par jour pour soutenir l'augmentation du volume sanguin." else "Drink 1.5–2L of water daily to support blood volume expansion.",
      tags = listOf("🧠 100k neurones/min", "👁️ Rétines formées", "🦷 Bourgeons dentaires")
    )
    8 -> WeekFetalMilestone(
      week = 8, sizeCm = "1.6 cm", weightGrams = "1.0 g", fruitOrObject = if (isFr) "Framboise" else "Raspberry", fruitEmoji = "🍓",
      trimester = 1, anatomicalFocus = if (isFr) "Doigts et orteils en séparation" else "Fingers & Toes Separation",
      description = if (isFr) "Les bras et jambes s'allongent et se fléchissent. Les doigts palmés commencent à se séparer. Les paupières recouvrent délicatement les yeux." else "Arms and legs flex, webbed digits separate, eyelids cover developing eyes.",
      maternalTip = if (isFr) "Poursuivre les vitamines prénatales riches en fer, iode et vitamine B9." else "Continue prenatal vitamins rich in iron, iodine, and vitamin B9.",
      tags = listOf("🖐️ Séparation des doigts", "🫁 Arborescence bronchique", "🦵 Articulations")
    )
    9 -> WeekFetalMilestone(
      week = 9, sizeCm = "2.3 cm", weightGrams = "2.0 g", fruitOrObject = if (isFr) "Cerise" else "Cherry", fruitEmoji = "🍒",
      trimester = 1, anatomicalFocus = if (isFr) "Passage officiel au statut de Fœtus" else "Transition from Embryo to Fetus",
      description = if (isFr) "La queue embryonnaire a totalement disparu ! L'embryon devient officiellement un fœtus. Les coudes et genoux peuvent fléchir." else "Embryonic tail is gone; baby is now officially a fetus with movable joints.",
      maternalTip = if (isFr) "Le repos en début d'après-midi aide à lutter contre la somnolence hormonale." else "Afternoon rest helps combat progesterone-induced fatigue.",
      tags = listOf("👶 Statut fœtal officiel", "💪 Coudes et genoux", "🫀 4 cavités cardiaques")
    )
    10 -> WeekFetalMilestone(
      week = 10, sizeCm = "3.1 cm", weightGrams = "4.0 g", fruitOrObject = if (isFr) "Fraise" else "Strawberry", fruitEmoji = "🍓",
      trimester = 1, anatomicalFocus = if (isFr) "Ossification du squelette & déglutition" else "Bone Ossification & Swallowing",
      description = if (isFr) "Les organes vitaux (reins, foie, intestins) fonctionnent ensemble. Le fœtus déglutit ses premières gorgées de liquide amniotique." else "Vital organs are functional. Fetus swallows first sips of amniotic fluid.",
      maternalTip = if (isFr) "Les reins fœtaux éliminent leurs premières urines stériles dans la cavité amniotique." else "Fetal kidneys produce clear sterile urine into the amniotic sac.",
      tags = listOf("🦴 Début ossification", "💧 Déglutition réflexe", "🫁 Mouvements thoraciques")
    )
    11 -> WeekFetalMilestone(
      week = 11, sizeCm = "4.1 cm", weightGrams = "7.0 g", fruitOrObject = if (isFr) "Figue" else "Fig", fruitEmoji = "🫐",
      trimester = 1, anatomicalFocus = if (isFr) "Mouvements spontanés & ongles" else "Spontaneous Movement & Nails",
      description = if (isFr) "Le fœtus s'étire et fait de gracieux mouvements dans l'utérus. Les follicules pileux et les ongles microscopiques apparaissent." else "Active stretches in the amniotic fluid. Hair follicles and nail beds emerge.",
      maternalTip = if (isFr) "Période idéale pour planifier la prise de sang DPNI (dépistage prénatal non invasif)." else "Ideal window to schedule NIPT non-invasive prenatal testing.",
      tags = listOf("🤸 Mouvements spontanés", "💅 Ébauche des ongles", "🩸 Bilan DPNI possible")
    )
    12 -> WeekFetalMilestone(
      week = 12, sizeCm = "5.4 cm", weightGrams = "14 g", fruitOrObject = if (isFr) "Citron vert" else "Lime", fruitEmoji = "🍋",
      trimester = 1, anatomicalFocus = if (isFr) "Échographie T1 & Clarté nucale" else "First Trimester Scan & Nuchal Translucency",
      description = if (isFr) "Échographie capitale du 1er trimestre : mesure de la clarté nucale, visualisation du profil facial, des bras, jambes et du cœur à 150 bpm." else "Key T1 ultrasound: nuchal translucency measurement, full facial profile and heart at 150 bpm.",
      maternalTip = if (isFr) "Grand moment : vous pouvez entendre le galop vigoureux du cœur au Doppler !",
      tags = listOf("🩺 Clarté nucale mesurée", "❤️ Cœur 150 bpm au Doppler", "🧬 Dépistage T1")
    )
    13 -> WeekFetalMilestone(
      week = 13, sizeCm = "7.4 cm", weightGrams = "23 g", fruitOrObject = if (isFr) "Prune" else "Plum", fruitEmoji = "🍑",
      trimester = 1, anatomicalFocus = if (isFr) "Fin du 1er trimestre & empreintes digitales" else "End of 1st Trimester & Fingerprints",
      description = if (isFr) "Dernière semaine du premier trimestre ! Les empreintes digitales uniques et définitives se gravent sur la pulpe de ses petits doigts." else "Final week of T1. Unique permanent fingerprints form on tiny fingertips.",
      maternalTip = if (isFr) "Le placenta assure désormais l'autonomie hormonale de la grossesse." else "The placenta now fully assumes hormonal production.",
      tags = listOf("🖐️ Empreintes digitales uniques", "🛡️ Placenta autonome", "🎉 Fin du 1er trimestre")
    )
    14 -> WeekFetalMilestone(
      week = 14, sizeCm = "8.7 cm", weightGrams = "43 g", fruitOrObject = if (isFr) "Citron jaune" else "Lemon", fruitEmoji = "🍋",
      trimester = 2, anatomicalFocus = if (isFr) "Bienvenue au 2ème trimestre ! Expressions faciales" else "Welcome to 2nd Trimester! Facial Expressions",
      description = if (isFr) "Le fœtus fronce les sourcils, sourit et suce son pouce ! Les muscles du visage s'exercent activement." else "Fetus makes facial expressions, smiles, frowns, and sucks thumb.",
      maternalTip = if (isFr) "Gain d'énergie fréquent chez la future maman (la fameuse lune de miel de la grossesse).",
      tags = listOf("😊 Mimiques faciales", "👍 Succion du pouce", "🌟 Bienvenue au 2ème trimestre")
    )
    15 -> WeekFetalMilestone(
      week = 15, sizeCm = "10.1 cm", weightGrams = "70 g", fruitOrObject = if (isFr) "Pomme" else "Apple", fruitEmoji = "🍎",
      trimester = 2, anatomicalFocus = if (isFr) "Audition des sons graves & perception lumineuse" else "Auditory Development & Light Perception",
      description = if (isFr) "Les petits osselets de l'oreille interne se consolident. Le fœtus perçoit les voix graves et la lumière à travers la peau abdominale." else "Inner ear bones harden; baby can perceive low-pitch parental voices.",
      maternalTip = if (isFr) "Parler ou chantonner doucement au bébé : les fréquences apaisantes traversent le liquide amniotique.",
      tags = listOf("👂 Osselets auditifs", "💡 Perception lumineuse", "🦴 Squelette articulé")
    )
    16 -> WeekFetalMilestone(
      week = 16, sizeCm = "11.6 cm", weightGrams = "100 g", fruitOrObject = if (isFr) "Avocat" else "Avocado", fruitEmoji = "🥑",
      trimester = 2, anatomicalFocus = if (isFr) "Pompe cardiaque 28L/jour & cordon robuste" else "Cardiac Pump (28L/day) & Strong Cord",
      description = if (isFr) "Le cœur fœtal pompe près de 28 litres de sang par jour ! Le cordon ombilical est vigoureux avec ses 2 artères et sa veine.",
      maternalTip = if (isFr) "De légers frémissements (comme des ailes de papillon) peuvent être ressentis dans le bas-ventre.",
      tags = listOf("❤️ 28L de sang pompés/j", "🌀 Cordon à 3 vaisseaux", "🦋 Premiers frémissements")
    )
    17 -> WeekFetalMilestone(
      week = 17, sizeCm = "13.0 cm", weightGrams = "140 g", fruitOrObject = if (isFr) "Navet" else "Turnip", fruitEmoji = "🥔",
      trimester = 2, anatomicalFocus = if (isFr) "Graisse brune thermorégulatrice" else "Brown Adipose Tissue",
      description = if (isFr) "Accumulation de tissu adipeux brun sous la peau, indispensable pour maintenir sa température corporelle à la naissance.",
      maternalTip = if (isFr) "Adopter de bonnes postures assises pour soulager les ligaments utérins et le dos.",
      tags = listOf("🔥 Graisse brune", "🦵 Élongation des membres", "🧘 Étirements doux")
    )
    18 -> WeekFetalMilestone(
      week = 18, sizeCm = "14.2 cm", weightGrams = "190 g", fruitOrObject = if (isFr) "Poire" else "Pear", fruitEmoji = "🍐",
      trimester = 2, anatomicalFocus = if (isFr) "Perception des mouvements (Quickening)" else "Fetal Movements (Quickening)",
      description = if (isFr) "Le fœtus est très actif : coups de pieds, flexions et cabrioles ! Les mouvements sont désormais nettement perceptibles.",
      maternalTip = if (isFr) "Fatou peut guider la main de Bala sur le ventre pour ressentir ensemble ces premiers coups !",
      tags = listOf("🦶 Premiers coups nets", "🧠 Gaine de myéline", "❤️ Moment magique à deux")
    )
    19 -> WeekFetalMilestone(
      week = 19, sizeCm = "15.3 cm", weightGrams = "240 g", fruitOrObject = if (isFr) "Mangue" else "Mango", fruitEmoji = "🥭",
      trimester = 2, anatomicalFocus = if (isFr) "Vernix caseosa protecteur" else "Protective Vernix Caseosa",
      description = if (isFr) "Une substance blanche crémeuse (le vernix) recouvre et protège la peau délicate du bébé contre l'immersion prolongée dans le liquide amniotique.",
      maternalTip = if (isFr) "Hydrater la peau du ventre avec de l'huile d'amande douce ou beurre de karité.",
      tags = listOf("🛡️ Manteau de vernix", "👂 Reconnaissance des voix", "💧 Peau protégée")
    )
    20 -> WeekFetalMilestone(
      week = 20, sizeCm = "16.4 cm", weightGrams = "300 g", fruitOrObject = if (isFr) "Banane" else "Banana", fruitEmoji = "🍌",
      trimester = 2, anatomicalFocus = if (isFr) "Mi-parcours exact ! Échographie morphologique T2" else "Mid-Pregnancy Milestone! T2 Anatomy Scan",
      description = if (isFr) "20 semaines : la moitié exacte de la grossesse est franchie ! Examen morphologique minutieux de chaque organe, du cerveau et des 4 cavités cardiaques.",
      maternalTip = if (isFr) "Émotion garantie : vous admirez en direct le profil complet et les mimiques de votre petit garçon !",
      tags = listOf("🎯 50% du voyage atteint", "🩺 Écho morphologique T2", "💙 Garçon confirmé")
    )
    21 -> WeekFetalMilestone(
      week = 21, sizeCm = "26.7 cm", weightGrams = "360 g", fruitOrObject = if (isFr) "Carotte" else "Carrot", fruitEmoji = "🥕",
      trimester = 2, anatomicalFocus = if (isFr) "Mesure tête-talons & cycles de sommeil" else "Crown-to-Heel Measurement & Sleep Cycles",
      description = if (isFr) "Mesuré de la tête aux talons ! Le fœtus a des cycles de sommeil bien réglés et bouge avec coordination.",
      maternalTip = if (isFr) "Le système digestif fœtal absorbe des petites quantités de sucres du liquide amniotique dégluti.",
      tags = listOf("📏 Mesure tête-talons", "😴 Sommeil régulier", "🥕 Prise de longueur")
    )
    22 -> WeekFetalMilestone(
      week = 22, sizeCm = "27.8 cm", weightGrams = "430 g", fruitOrObject = if (isFr) "Papaye" else "Papaya", fruitEmoji = "🍈",
      trimester = 2, anatomicalFocus = if (isFr) "Sens du toucher & préhension du cordon" else "Sense of Touch & Cord Grasping",
      description = if (isFr) "Bébé explore son environnement : il touche son visage, ses pieds et serre le cordon ombilical avec ses petits doigts.",
      maternalTip = if (isFr) "Prendre des respirations profondes et relaxer le plancher pelvien.",
      tags = listOf("🖐️ Préhension active", "👃 Narines ouvertes", "🦶 Caresse du visage")
    )
    23 -> WeekFetalMilestone(
      week = 23, sizeCm = "28.9 cm", weightGrams = "500 g", fruitOrObject = if (isFr) "Pamplemousse" else "Grapefruit", fruitEmoji = "🍊",
      trimester = 2, anatomicalFocus = if (isFr) "Cap des 500 grammes & mouvements respiratoires" else "500g Milestone & Breathing Practice",
      description = if (isFr) "Cap symbolique du demi-kilo franchi ! Le thorax s'exerce aux mouvements respiratoires en soulevant doucement le diaphragme.",
      maternalTip = if (isFr) "Le bébé peut avoir de petits hoquets perceptibles sous forme de soubresauts rythmiques.",
      tags = listOf("⚖️ Poids 500g atteint", "🫁 Entraînement pulmonaire", "🎈 Petits hoquets bénins")
    )
    24 -> WeekFetalMilestone(
      week = 24, sizeCm = "30.0 cm", weightGrams = "600 g", fruitOrObject = if (isFr) "Épi de maïs" else "Corn Ear", fruitEmoji = "🌽",
      trimester = 2, anatomicalFocus = if (isFr) "Seuil de viabilité médicale & surfactant" else "Viability Threshold & Surfactant",
      description = if (isFr) "Étape fondamentale : seuil de viabilité médicale franchi. Les poumons commencent à fabriquer le surfactant pour garder les alvéoles ouvertes.",
      maternalTip = if (isFr) "Test d'hyperglycémie provoquée (HGPO) au laboratoire entre 24 et 28 SA pour dépister le diabète gestationnel.",
      tags = listOf("⭐ Seuil de viabilité franchi", "🫁 Début du surfactant", "🧪 Dépistage glucose HGPO")
    )
    25 -> WeekFetalMilestone(
      week = 25, sizeCm = "34.6 cm", weightGrams = "660 g", fruitOrObject = if (isFr) "Rutabaga" else "Rutabaga", fruitEmoji = "🥔",
      trimester = 2, anatomicalFocus = if (isFr) "Moelle osseuse active & capillaires sanguins" else "Bone Marrow Activity & Capillaries",
      description = if (isFr) "La moelle osseuse assure l'essentiel de la production de globules rouges. Les capillaires sanguins donnent une belle couleur rosée à la peau.",
      maternalTip = if (isFr) "Maintenir des apports en fer satisfaisants pour prévenir l'anémie maternelle.",
      tags = listOf("🩸 Hématopoïèse active", "👂 Réagit aux voix familières", "🌸 Teint rosé")
    )
    26 -> WeekFetalMilestone(
      week = 26, sizeCm = "35.6 cm", weightGrams = "760 g", fruitOrObject = if (isFr) "Courgette" else "Zucchini", fruitEmoji = "🥒",
      trimester = 2, anatomicalFocus = if (isFr) "Ouverture des paupières & cils" else "Eye Opening & Eyelashes",
      description = if (isFr) "Les yeux s'ouvrent ! Les cils et sourcils sont nettement dessinés. Bébé cligne des yeux et perçoit les faisceaux de lumière.",
      maternalTip = if (isFr) "De légères marches en plein air favorisent le retour veineux et le sommeil.",
      tags = listOf("👁️ Yeux grands ouverts", "👀 Réflexe de clignement", "🚶 Marche quotidienne")
    )
    27 -> WeekFetalMilestone(
      week = 27, sizeCm = "36.6 cm", weightGrams = "875 g", fruitOrObject = if (isFr) "Chou-fleur" else "Cauliflower", fruitEmoji = "🥦",
      trimester = 2, anatomicalFocus = if (isFr) "Fin du 2ème trimestre ! Cortex cérébral plissé" else "End of 2nd Trimester! Complex Brain Cortex",
      description = if (isFr) "Dernière semaine du deuxième trimestre ! Le cerveau multiplie ses connexions. Le bébé reconnaît instantanément la voix de Fatou et de Bala.",
      maternalTip = if (isFr) "Commencer à rassembler les éléments de la chambre de bébé en toute sérénité.",
      tags = listOf("🧠 Cortex cérébral affiné", "🎵 Reconnaissance vocale", "🎉 Fin du 2ème trimestre")
    )
    28 -> WeekFetalMilestone(
      week = 28, sizeCm = "37.6 cm", weightGrams = "1 005 g", fruitOrObject = if (isFr) "Aubergine" else "Eggplant", fruitEmoji = "🍆",
      trimester = 3, anatomicalFocus = if (isFr) "Bienvenue au 3ème trimestre ! Cap des 1 kg" else "Welcome to 3rd Trimester! 1 kg Milestone",
      description = if (isFr) "Entrée dans le troisième trimestre ! Le bébé pèse plus d'un kilo. Ses cycles d'éveil et de repos sont très perceptibles.",
      maternalTip = if (isFr) "Consultation mensuelle du T3 et bilan sanguin de contrôle.",
      tags = listOf("⚖️ Cap 1 kg dépassé", "🥉 Bienvenue au T3", "👶 Présence utérine affirmée")
    )
    29 -> WeekFetalMilestone(
      week = 29, sizeCm = "38.6 cm", weightGrams = "1 150 g", fruitOrObject = if (isFr) "Butternut" else "Butternut Squash", fruitEmoji = "🎃",
      trimester = 3, anatomicalFocus = if (isFr) "Minéralisation osseuse & réserves de calcium" else "Bone Mineralization & Calcium Stores",
      description = if (isFr) "Les os s'enrichissent en calcium pour durcir. Le crâne reste volontairement malléable pour permettre le futur passage lors de l'accouchement.",
      maternalTip = if (isFr) "Consommer des sources saines de calcium et de vitamine D.",
      tags = listOf("🦴 Minéralisation du squelette", "🧠 Contrôle autonome", "🥛 Calcium & Vitamine D")
    )
    30 -> WeekFetalMilestone(
      week = 30, sizeCm = "39.9 cm", weightGrams = "1 320 g", fruitOrObject = if (isFr) "Concombre" else "Cucumber", fruitEmoji = "🥒",
      trimester = 3, anatomicalFocus = if (isFr) "Maturation cérébrale & rêves fœtaux" else "Brain Development & Fetal Dreams",
      description = if (isFr) "Le cerveau forme de profondes circonvolutions. Les tracés électriques montrent des phases de sommeil paradoxal correspondant aux rêves fœtaux.",
      maternalTip = if (isFr) "Dormir sur le côté gauche avec un coussin entre les jambes pour maximiser le flux sanguin placentaire.",
      tags = listOf("💭 Phases de rêves fœtaux", "👁️ Pupilles contractiles", "🛏️ Sommeil côté gauche")
    )
    31 -> WeekFetalMilestone(
      week = 31, sizeCm = "41.1 cm", weightGrams = "1 500 g", fruitOrObject = if (isFr) "Noix de coco" else "Coconut", fruitEmoji = "🥥",
      trimester = 3, anatomicalFocus = if (isFr) "Les 5 sens pleinement connectés" else "All 5 Senses Connected",
      description = if (isFr) "Bébé goûte le liquide amniotique, écoute les voix, ressent la lumière et réagit aux caresses bienveillantes sur le ventre.",
      maternalTip = if (isFr) "Séances d'haptonomie : poser des mains chaleureuses sur le ventre pour communiquer avec lui.",
      tags = listOf("✋ 5 sens connectés", "🍯 Goût des saveurs", "❤️ Haptonomie en couple")
    )
    32 -> WeekFetalMilestone(
      week = 32, sizeCm = "42.4 cm", weightGrams = "1 700 g", fruitOrObject = if (isFr) "Chou frisé" else "Kale", fruitEmoji = "🥬",
      trimester = 3, anatomicalFocus = if (isFr) "Échographie T3 & chute du lanugo" else "T3 Ultrasound & Lanugo Shedding",
      description = if (isFr) "Dernière échographie de référence : biométrie fœtale, estimation du poids à terme, évaluation du liquide amniotique et du placenta.",
      maternalTip = if (isFr) "Le fin duvet (lanugo) commence à tomber, laissant place à une peau douce et dodue.",
      tags = listOf("🩺 Échographie T3 de croissance", "👶 Peau rose et dodue", "⚖️ Poids 1.7 kg")
    )
    33 -> WeekFetalMilestone(
      week = 33, sizeCm = "43.7 cm", weightGrams = "1 920 g", fruitOrObject = if (isFr) "Ananas" else "Pineapple", fruitEmoji = "🍍",
      trimester = 3, anatomicalFocus = if (isFr) "Transfert massif d'anticorps maternels" else "Maternal Antibody Transfer",
      description = if (isFr) "La maman transmet par le placenta une grande quantité d'anticorps protecteurs (IgG) pour bâtir le système immunitaire du nouveau-né.",
      maternalTip = if (isFr) "Boire régulièrement de l'eau : le volume de liquide amniotique se renouvelle toutes les 3 heures.",
      tags = listOf("🛡️ Bouclier immunitaire", "💧 Liquide renouvelé", "🍍 Longueur 43 cm")
    )
    34 -> WeekFetalMilestone(
      week = 34, sizeCm = "45.0 cm", weightGrams = "2 150 g", fruitOrObject = if (isFr) "Cantaloup" else "Cantaloupe", fruitEmoji = "🍈",
      trimester = 3, anatomicalFocus = if (isFr) "Poumons matures & valise de maternité" else "Mature Lungs & Hospital Bag",
      description = if (isFr) "Les alvéoles pulmonaires ont atteint une maturité quasi-complète. Le bébé est prêt pour ses premiers échanges gazeux à l'air libre.",
      maternalTip = if (isFr) "Préparer la valise de maternité pour la maman et le nouveau-né en toute quiétude.",
      tags = listOf("🫁 Poumons quasi-matures", "👜 Valise de maternité", "👶 Bébé tonique")
    )
    35 -> WeekFetalMilestone(
      week = 35, sizeCm = "46.2 cm", weightGrams = "2 380 g", fruitOrObject = if (isFr) "Melon miel" else "Honeydew", fruitEmoji = "🍈",
      trimester = 3, anatomicalFocus = if (isFr) "Prise de poids accélérée (230g/semaine)" else "Rapid Weight Gain (+230g/week)",
      description = if (isFr) "L'espace devient douillettement serré : bébé ne fait plus de culbutes mais pousse vigoureusement avec ses talons et ses fesses.",
      maternalTip = if (isFr) "Prélèvement de dépistage du Streptocoque du groupe B (SGB) entre 35 et 37 SA.",
      tags = listOf("📈 +230g par semaine", "🦶 Poussées vigoureuses", "🩺 Dépistage Streptocoque B")
    )
    36 -> WeekFetalMilestone(
      week = 36, sizeCm = "47.4 cm", weightGrams = "2 620 g", fruitOrObject = if (isFr) "Laitue romaine" else "Romaine Lettuce", fruitEmoji = "🥬",
      trimester = 3, anatomicalFocus = if (isFr) "Position céphalique tête en bas" else "Cephalic Head-Down Position",
      description = if (isFr) "La grande majorité des bébés sont désormais en position céphalique (tête en bas, menton fléchi), prête pour l'engagement dans le bassin.",
      maternalTip = if (isFr) "Consultation pré-anesthésique obligatoire en maternité.",
      tags = listOf("👶 Tête en bas engagée", "🩺 Consultation anesthésie", "🌿 Fin du 8ème mois")
    )
    37 -> WeekFetalMilestone(
      week = 37, sizeCm = "48.6 cm", weightGrams = "2 860 g", fruitOrObject = if (isFr) "Bette à carde" else "Swiss Chard", fruitEmoji = "🌿",
      trimester = 3, anatomicalFocus = if (isFr) "Plein terme précoce atteint ! (Early Term)" else "Early Full Term Reached!",
      description = if (isFr) "Cap médical extraordinaire : bébé est désormais à terme ! S'il naît aujourd'hui, il n'est plus considéré comme prématuré.",
      maternalTip = if (isFr) "Surveiller les signes annonciateurs : contractions rythmées et perte des eaux.",
      tags = listOf("🎉 Plein terme précoce", "✅ Zéro prématurité", "👶 Prêt pour la naissance")
    )
    38 -> WeekFetalMilestone(
      week = 38, sizeCm = "49.8 cm", weightGrams = "3 080 g", fruitOrObject = if (isFr) "Poireau géant" else "Giant Leek", fruitEmoji = "🪴",
      trimester = 3, anatomicalFocus = if (isFr) "Cap des 3 kg & prise ferme" else "3 kg Milestone & Firm Grasp",
      description = if (isFr) "Le bébé dépasse les 3 kilogrammes ! Sa prise en main est ferme, ses ongles dépassent le bout de ses doigts et son regard est vif.",
      maternalTip = if (isFr) "Installer définitivement le siège-auto homologué dans la voiture.",
      tags = listOf("⚖️ Poids > 3 kg", "🚗 Siège-auto installé", "🪴 Cap des 38 SA")
    )
    39 -> WeekFetalMilestone(
      week = 39, sizeCm = "50.7 cm", weightGrams = "3 290 g", fruitOrObject = if (isFr) "Mini-pastèque" else "Mini Watermelon", fruitEmoji = "🍉",
      trimester = 3, anatomicalFocus = if (isFr) "100% prêt à naître" else "100% Ready for Birth",
      description = if (isFr) "Le développement physique est entièrement accompli. Le bébé continue uniquement de stocker du glycogène pour l'effort de la naissance.",
      maternalTip = if (isFr) "Se reposer, se relaxer sur le ballon de grossesse (gym ball) pour bercer le bassin.",
      tags = listOf("🌟 100% prêt à naître", "🧘 Relaxation bassin", "🍉 Plein terme accompli")
    )
    40 -> WeekFetalMilestone(
      week = 40, sizeCm = "51.2 cm", weightGrams = "3 460 g", fruitOrObject = if (isFr) "Pastèque à terme" else "Full Term Watermelon", fruitEmoji = "🍉",
      trimester = 3, anatomicalFocus = if (isFr) "DPA Officielle (Mercredi 6 août 2027) !" else "Official Due Date (Wednesday, August 6, 2027)!",
      description = if (isFr) "Le jour J tant attendu est arrivé ! Votre petit garçon est prêt à rencontrer ses parents aimants Fatou et Bala.",
      maternalTip = if (isFr) "Rester sereine, respirer calmement et se rendre en salle de naissance dès des contractions toutes les 5 minutes.",
      tags = listOf("🎉 DPA 06 Août 2027", "👶 Rencontre magique", "❤️ Bienvenue au monde")
    )
    else -> WeekFetalMilestone(
      week = 41, sizeCm = "51.7 cm", weightGrams = "3 600 g", fruitOrObject = if (isFr) "Pastèque mûre" else "Ripe Watermelon", fruitEmoji = "🍉",
      trimester = 3, anatomicalFocus = if (isFr) "Surveillance rapprochée post-terme" else "Post-Term Close Monitoring",
      description = if (isFr) "Bébé profite encore un peu du nid maternel ! Suivi rapproché par cardiotocographie et contrôle régulier du liquide amniotique.",
      maternalTip = if (isFr) "Contrôle à la maternité toutes les 48 heures pour vérifier le bien-être fœtal optimal.",
      tags = listOf("🩺 Monitoring rapproché", "❤️ Surveillance attentive", "👶 Arrivée imminente")
    )
  }
}

@Composable
private fun RealisticFetalVisualizer(
  week: Int,
  theme: ThemeColors,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "fetal_heartbeat")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(210.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.radialGradient(
          colors = listOf(
            Color(0xFF381E29),
            Color(0xFF22131F),
            Color(0xFF140D17)
          )
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val cx = w / 2f
      val cy = h / 2f

      // 1. Soft amniotic fluid halo
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0x35F472B6),
            Color(0x15FB7185),
            Color.Transparent
          ),
          center = Offset(cx, cy),
          radius = (w * 0.45f) * pulseScale
        ),
        radius = (w * 0.45f) * pulseScale,
        center = Offset(cx, cy)
      )

      // 2. Placental wall gently curving at top-right
      val placentaPath = Path().apply {
        moveTo(w * 0.65f, 0f)
        cubicTo(w * 0.75f, h * 0.15f, w * 0.95f, h * 0.25f, w, h * 0.3f)
        lineTo(w, 0f)
        close()
      }
      drawPath(
        path = placentaPath,
        color = Color(0x30E11D48)
      )

      // 3. Umbilical cord winding gently
      if (week >= 5) {
        val cordPath = Path().apply {
          moveTo(w * 0.82f, h * 0.16f)
          cubicTo(
            w * 0.72f, h * 0.35f,
            w * 0.58f, h * 0.42f,
            cx, cy + (if (week <= 12) 10f else 22f)
          )
        }
        drawPath(
          path = cordPath,
          color = Color(0x6038BDF8),
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
      }

      // 4. Anatomical developmental silhouette based on gestation stage
      when {
        // Stage 1 (W1–W4): Blastocyst / Embryonic Disc
        week <= 4 -> {
          drawCircle(
            brush = Brush.radialGradient(
              listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color.Transparent),
              center = Offset(cx, cy),
              radius = 24.dp.toPx()
            ),
            radius = 24.dp.toPx() * pulseScale,
            center = Offset(cx, cy)
          )
          drawCircle(
            color = Color(0x90FDE047),
            radius = 32.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
          )
          for (i in 0 until 8) {
            val angle = (i * Math.PI / 4).toFloat()
            val ox = cx + Math.cos(angle.toDouble()).toFloat() * 12.dp.toPx()
            val oy = cy + Math.sin(angle.toDouble()).toFloat() * 12.dp.toPx()
            drawCircle(
              color = Color(0xFFFEF08A),
              radius = 4.dp.toPx(),
              center = Offset(ox, oy)
            )
          }
        }

        // Stage 2 (W5–W8): C-Shaped Embryo with Limb Buds & Heart Tube
        week <= 8 -> {
          val embryoPath = Path().apply {
            moveTo(cx - 20.dp.toPx(), cy - 25.dp.toPx())
            cubicTo(
              cx + 35.dp.toPx(), cy - 30.dp.toPx(),
              cx + 40.dp.toPx(), cy + 25.dp.toPx(),
              cx - 15.dp.toPx(), cy + 28.dp.toPx()
            )
            cubicTo(
              cx - 30.dp.toPx(), cy + 15.dp.toPx(),
              cx - 35.dp.toPx(), cy - 10.dp.toPx(),
              cx - 20.dp.toPx(), cy - 25.dp.toPx()
            )
            close()
          }
          drawPath(
            path = embryoPath,
            brush = Brush.linearGradient(
              listOf(Color(0xFFFFD1DC), Color(0xFFF472B6))
            )
          )
          drawCircle(
            color = Color(0xFFFFAEC9),
            radius = 18.dp.toPx(),
            center = Offset(cx - 10.dp.toPx(), cy - 22.dp.toPx())
          )
          drawCircle(
            color = Color(0xFF475569),
            radius = 2.2.dp.toPx(),
            center = Offset(cx - 16.dp.toPx(), cy - 22.dp.toPx())
          )
          drawCircle(
            color = Color(0xFFEF4444).copy(alpha = pulseAlpha),
            radius = 7.dp.toPx() * pulseScale,
            center = Offset(cx, cy - 2.dp.toPx())
          )
        }

        // Stage 3 (W9–W13): Transition to Fetus
        week <= 13 -> {
          drawCircle(
            brush = Brush.radialGradient(
              listOf(Color(0xFFFFE4E6), Color(0xFFF43F5E)),
              center = Offset(cx - 15.dp.toPx(), cy - 32.dp.toPx()),
              radius = 26.dp.toPx()
            ),
            radius = 25.dp.toPx(),
            center = Offset(cx - 15.dp.toPx(), cy - 32.dp.toPx())
          )
          drawCircle(
            color = Color(0xFF1E293B),
            radius = 2.5.dp.toPx(),
            center = Offset(cx - 26.dp.toPx(), cy - 32.dp.toPx())
          )
          val torsoPath = Path().apply {
            moveTo(cx - 10.dp.toPx(), cy - 10.dp.toPx())
            cubicTo(
              cx + 38.dp.toPx(), cy - 5.dp.toPx(),
              cx + 36.dp.toPx(), cy + 45.dp.toPx(),
              cx - 8.dp.toPx(), cy + 42.dp.toPx()
            )
            cubicTo(
              cx - 22.dp.toPx(), cy + 28.dp.toPx(),
              cx - 20.dp.toPx(), cy + 5.dp.toPx(),
              cx - 10.dp.toPx(), cy - 10.dp.toPx()
            )
            close()
          }
          drawPath(
            path = torsoPath,
            brush = Brush.linearGradient(
              listOf(Color(0xFFFFD1DC), Color(0xFFFB7185))
            )
          )
          val legPath = Path().apply {
            moveTo(cx + 8.dp.toPx(), cy + 32.dp.toPx())
            cubicTo(
              cx - 18.dp.toPx(), cy + 45.dp.toPx(),
              cx - 28.dp.toPx(), cy + 36.dp.toPx(),
              cx - 24.dp.toPx(), cy + 26.dp.toPx()
            )
          }
          drawPath(
            path = legPath,
            color = Color(0xFFFFD1DC),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
          )
          val armPath = Path().apply {
            moveTo(cx + 4.dp.toPx(), cy + 4.dp.toPx())
            cubicTo(
              cx - 15.dp.toPx(), cy + 2.dp.toPx(),
              cx - 22.dp.toPx(), cy - 12.dp.toPx(),
              cx - 24.dp.toPx(), cy - 22.dp.toPx()
            )
          }
          drawPath(
            path = armPath,
            color = Color(0xFFFFD1DC),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
          )
          drawCircle(
            color = Color(0xFFEF4444).copy(alpha = pulseAlpha),
            radius = 8.dp.toPx() * pulseScale,
            center = Offset(cx - 2.dp.toPx(), cy + 6.dp.toPx())
          )
        }

        // Stage 4 (W14–W27): 2nd Trimester Curled Baby with Facial Profile
        week <= 27 -> {
          val scale = (1f + (week - 14) * 0.015f).coerceIn(1f, 1.22f)
          drawCircle(
            brush = Brush.radialGradient(
              listOf(Color(0xFFFFF1F2), Color(0xFFFDA4AF)),
              center = Offset(cx - 24.dp.toPx() * scale, cy - 36.dp.toPx() * scale),
              radius = 32.dp.toPx() * scale
            ),
            radius = 32.dp.toPx() * scale,
            center = Offset(cx - 24.dp.toPx() * scale, cy - 36.dp.toPx() * scale)
          )
          val faceProfile = Path().apply {
            moveTo(cx - 44.dp.toPx() * scale, cy - 42.dp.toPx() * scale)
            cubicTo(
              cx - 52.dp.toPx() * scale, cy - 36.dp.toPx() * scale,
              cx - 48.dp.toPx() * scale, cy - 30.dp.toPx() * scale,
              cx - 42.dp.toPx() * scale, cy - 26.dp.toPx() * scale
            )
          }
          drawPath(
            path = faceProfile,
            color = Color(0xFFFB7185),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
          )
          val eyeLid = Path().apply {
            moveTo(cx - 36.dp.toPx() * scale, cy - 38.dp.toPx() * scale)
            cubicTo(
              cx - 32.dp.toPx() * scale, cy - 34.dp.toPx() * scale,
              cx - 28.dp.toPx() * scale, cy - 38.dp.toPx() * scale,
              cx - 24.dp.toPx() * scale, cy - 36.dp.toPx() * scale
            )
          }
          drawPath(
            path = eyeLid,
            color = Color(0xFF64748B),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
          )
          val bodyPath = Path().apply {
            moveTo(cx - 12.dp.toPx() * scale, cy - 14.dp.toPx() * scale)
            cubicTo(
              cx + 45.dp.toPx() * scale, cy - 10.dp.toPx() * scale,
              cx + 42.dp.toPx() * scale, cy + 50.dp.toPx() * scale,
              cx - 12.dp.toPx() * scale, cy + 46.dp.toPx() * scale
            )
            cubicTo(
              cx - 32.dp.toPx() * scale, cy + 30.dp.toPx() * scale,
              cx - 28.dp.toPx() * scale, cy + 2.dp.toPx() * scale,
              cx - 12.dp.toPx() * scale, cy - 14.dp.toPx() * scale
            )
            close()
          }
          drawPath(
            path = bodyPath,
            brush = Brush.linearGradient(
              listOf(Color(0xFFFFE4E6), Color(0xFFFB7185))
            )
          )
          val legPath = Path().apply {
            moveTo(cx + 10.dp.toPx() * scale, cy + 35.dp.toPx() * scale)
            cubicTo(
              cx - 22.dp.toPx() * scale, cy + 56.dp.toPx() * scale,
              cx - 38.dp.toPx() * scale, cy + 44.dp.toPx() * scale,
              cx - 32.dp.toPx() * scale, cy + 26.dp.toPx() * scale
            )
          }
          drawPath(
            path = legPath,
            color = Color(0xFFFFE4E6),
            style = Stroke(width = 11.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
          )
          val armPath = Path().apply {
            moveTo(cx + 6.dp.toPx() * scale, cy + 2.dp.toPx() * scale)
            cubicTo(
              cx - 16.dp.toPx() * scale, cy + 8.dp.toPx() * scale,
              cx - 28.dp.toPx() * scale, cy - 10.dp.toPx() * scale,
              cx - 32.dp.toPx() * scale, cy - 24.dp.toPx() * scale
            )
          }
          drawPath(
            path = armPath,
            color = Color(0xFFFFE4E6),
            style = Stroke(width = 8.dp.toPx() * scale, cap = StrokeCap.Round)
          )
          drawCircle(
            color = Color(0xFFEF4444).copy(alpha = pulseAlpha),
            radius = 9.dp.toPx() * pulseScale,
            center = Offset(cx - 2.dp.toPx() * scale, cy + 8.dp.toPx() * scale)
          )
        }

        // Stage 5 (W28–W41): 3rd Trimester Fully Plump Baby Nestled Vertex (Head Down)
        else -> {
          val matureScale = (1.15f + (week - 28) * 0.012f).coerceIn(1.15f, 1.35f)
          drawCircle(
            brush = Brush.radialGradient(
              listOf(Color(0xFFFFF1F2), Color(0xFFFDA4AF)),
              center = Offset(cx - 26.dp.toPx() * matureScale, cy + 32.dp.toPx() * matureScale),
              radius = 36.dp.toPx() * matureScale
            ),
            radius = 36.dp.toPx() * matureScale,
            center = Offset(cx - 26.dp.toPx() * matureScale, cy + 32.dp.toPx() * matureScale)
          )
          drawCircle(
            color = Color(0x60F43F5E),
            radius = 10.dp.toPx(),
            center = Offset(cx - 34.dp.toPx() * matureScale, cy + 36.dp.toPx() * matureScale)
          )
          val eyeLid = Path().apply {
            moveTo(cx - 38.dp.toPx() * matureScale, cy + 28.dp.toPx() * matureScale)
            cubicTo(
              cx - 34.dp.toPx() * matureScale, cy + 32.dp.toPx() * matureScale,
              cx - 30.dp.toPx() * matureScale, cy + 28.dp.toPx() * matureScale,
              cx - 26.dp.toPx() * matureScale, cy + 30.dp.toPx() * matureScale
            )
          }
          drawPath(
            path = eyeLid,
            color = Color(0xFF475569),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
          )
          val backPath = Path().apply {
            moveTo(cx - 10.dp.toPx() * matureScale, cy + 12.dp.toPx() * matureScale)
            cubicTo(
              cx + 52.dp.toPx() * matureScale, cy + 20.dp.toPx() * matureScale,
              cx + 48.dp.toPx() * matureScale, cy - 42.dp.toPx() * matureScale,
              cx - 15.dp.toPx() * matureScale, cy - 38.dp.toPx() * matureScale
            )
            cubicTo(
              cx - 35.dp.toPx() * matureScale, cy - 22.dp.toPx() * matureScale,
              cx - 28.dp.toPx() * matureScale, cy - 2.dp.toPx() * matureScale,
              cx - 10.dp.toPx() * matureScale, cy + 12.dp.toPx() * matureScale
            )
            close()
          }
          drawPath(
            path = backPath,
            brush = Brush.linearGradient(
              listOf(Color(0xFFFFF1F2), Color(0xFFFB7185))
            )
          )
          val legPath = Path().apply {
            moveTo(cx + 8.dp.toPx() * matureScale, cy - 28.dp.toPx() * matureScale)
            cubicTo(
              cx - 24.dp.toPx() * matureScale, cy - 45.dp.toPx() * matureScale,
              cx - 38.dp.toPx() * matureScale, cy - 30.dp.toPx() * matureScale,
              cx - 28.dp.toPx() * matureScale, cy - 14.dp.toPx() * matureScale
            )
          }
          drawPath(
            path = legPath,
            color = Color(0xFFFFE4E6),
            style = Stroke(width = 13.dp.toPx() * matureScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
          )
          val armPath = Path().apply {
            moveTo(cx + 4.dp.toPx() * matureScale, cy + 4.dp.toPx() * matureScale)
            cubicTo(
              cx - 14.dp.toPx() * matureScale, cy + 12.dp.toPx() * matureScale,
              cx - 24.dp.toPx() * matureScale, cy + 18.dp.toPx() * matureScale,
              cx - 28.dp.toPx() * matureScale, cy + 26.dp.toPx() * matureScale
            )
          }
          drawPath(
            path = armPath,
            color = Color(0xFFFFE4E6),
            style = Stroke(width = 9.dp.toPx() * matureScale, cap = StrokeCap.Round)
          )
          drawCircle(
            color = Color(0xFFEF4444).copy(alpha = pulseAlpha),
            radius = 10.dp.toPx() * pulseScale,
            center = Offset(cx - 2.dp.toPx() * matureScale, cy - 2.dp.toPx() * matureScale)
          )
        }
      }
    }

    // Overlay telemetry badges at the bottom
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(100.dp),
        color = Color.Black.copy(alpha = 0.55f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(Color(0xFF22C55E))
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "💓 140–160 bpm",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(100.dp),
        color = Color.Black.copy(alpha = 0.55f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f))
      ) {
        Text(
          text = if (week <= 4) "🔬 Stade Embryonnaire" else if (week <= 13) "👶 1er Trimestre" else if (week <= 27) "👶 2ème Trimestre" else "👶 3ème Trimestre (Terme)",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
      }
    }
  }
}
*/


private fun getMonthName(monthIndex: Int, language: AppLanguage): String {
  val monthsFr = listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
  val monthsEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
  return if (language == AppLanguage.FR) monthsFr.getOrElse(monthIndex) { "Mois" } else monthsEn.getOrElse(monthIndex) { "Month" }
}

@Composable
private fun AppointmentTimelineCard(
  apt: SyncedAppointmentItem,
  isPastItem: Boolean,
  isFr: Boolean,
  theme: ThemeColors,
  language: AppLanguage,
  onToggleCompleted: () -> Unit,
  onToggleAlarm: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = if (isPastItem) theme.background.copy(alpha = 0.72f) else theme.background,
    border = BorderStroke(1.dp, if (apt.isCompleted) Color(0xFF16A34A).copy(alpha = 0.4f) else theme.border)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = apt.title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText,
          modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (isPastItem) {
            Surface(
              shape = RoundedCornerShape(100.dp),
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
          } else {
            IconButton(
              onClick = onToggleAlarm,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = if (apt.hasAlarm) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                contentDescription = if (isFr) "Alarme" else "Alarm",
                tint = if (apt.hasAlarm) theme.accent else theme.mutedText,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(100.dp),
            color = if (apt.isCompleted) Color(0xFFDCFCE7) else theme.accent.copy(alpha = 0.15f),
            modifier = Modifier.clickable { onToggleCompleted() }
          ) {
            Text(
              text = if (apt.isCompleted) (if (isFr) "Effectué ✓" else "Done ✓") else (if (isFr) "Prévu" else "Scheduled"),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              color = if (apt.isCompleted) Color(0xFF16A34A) else theme.accent,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccessTime, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isFr) "${apt.dateStr} à ${apt.timeStr}" else "${apt.dateStr} at ${apt.timeStr}",
          style = MaterialTheme.typography.labelSmall,
          color = theme.mutedText
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(Icons.Default.Person, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = apt.doctor, style = MaterialTheme.typography.labelSmall, color = theme.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }

      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "${if (isFr) "Par" else "By"} ${apt.addedBy}",
        style = MaterialTheme.typography.labelSmall,
        color = theme.mutedText
      )

      if (apt.clinicalNotes.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        SelectionContainer {
          Text(
            text = apt.clinicalNotes,
            style = MaterialTheme.typography.bodySmall,
            color = theme.primaryText.copy(alpha = 0.85f),
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
      }
    }
  }
}
