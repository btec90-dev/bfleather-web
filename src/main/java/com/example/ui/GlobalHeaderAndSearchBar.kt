package com.example.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.*
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.theme.AppTheme
import com.example.ui.theme.getCategoryBadgeColors
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

data class GlobalSearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val targetTab: Int,
    val requiresWalletPin: Boolean = false
)

@Composable
fun GlobalHeaderBar(
    lang: AppLanguage,
    coupleAvatarBase64: String,
    activePartnerProfile: String,
    onTogglePartnerProfile: (String) -> Unit,
    onUpdateCoupleAvatar: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPhoneBook: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    val avatarBitmap = remember(coupleAvatarBase64) {
        if (coupleAvatarBase64.isBlank()) null
        else runCatching {
            val bytes = Base64.decode(coupleAvatarBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        val maxDim = 256f
                        val scale = minOf(maxDim / decoded.width, maxDim / decoded.height, 1f)
                        val scaled = Bitmap.createScaledBitmap(
                            decoded,
                            (decoded.width * scale).toInt().coerceAtLeast(1),
                            (decoded.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
                        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        onUpdateCoupleAvatar(b64)
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.headerFooterBg,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Clickable circular Couple Profile Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("couple_avatar_button")
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = if (lang == AppLanguage.FR) "Photo du couple" else "Couple profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, colors.accent.copy(alpha = 0.65f), CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_couple_avatar),
                        contentDescription = if (lang == AppLanguage.FR) "Avatar couple" else "Couple avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, colors.accent.copy(alpha = 0.65f), CircleShape)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = colors.accent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .border(1.5.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = if (lang == AppLanguage.FR) "Changer photo" else "Change photo",
                        tint = Color.White,
                        modifier = Modifier.padding(3.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.t("app_title", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (lang == AppLanguage.FR) "Suivi PMA & Grossesse" else "IVF & Pregnancy Hub",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Compact Partner Mode Quick Switcher Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.testTag("partner_mode_switcher")
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFatou = activePartnerProfile != "Partner"
                    listOf("Fatou" to isFatou, "Partner" to !isFatou).forEach { (label, selected) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) colors.accent else Color.Transparent,
                            modifier = Modifier
                                .testTag("partner_pill_${label.lowercase()}")
                                .clickable { onTogglePartnerProfile(label) }
                        ) {
                            Text(
                                text = if (label == "Partner" && lang == AppLanguage.FR) "Partenaire" else label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else colors.mutedText,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Phone Book / Directory Direct Access Button
            IconButton(
                onClick = onOpenPhoneBook,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("header_phonebook_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = if (lang == AppLanguage.FR) "Répertoire médical & Urgences" else "Phone directory & Emergencies",
                    tint = colors.primaryText
                )
            }

            // Settings Gear Button on Far Right
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("header_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = Strings.t("settings_tab", lang),
                    tint = colors.primaryText
                )
            }
        }
    }
}

@Composable
fun GlobalSearchAndVoiceBar(
    lang: AppLanguage,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    pregnancyMeds: List<SyncedPregnancyMed>,
    medReminders: List<SyncedMedReminder>,
    appointments: List<SyncedAppointmentItem>,
    expenses: List<SyncedExpenseRecord>,
    walletCards: List<SyncedWalletCard>,
    notes: List<SyncedNoteItem>,
    voiceMemos: List<SyncedVoiceMemo>,
    protocolSteps: List<SyncedProtocolStep>,
    nutritionFoods: List<SyncedNutritionFood>,
    careProviders: List<SyncedCareProvider>,
    onResultClick: (GlobalSearchResultItem) -> Unit,
    showDropdown: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isFrench = lang == AppLanguage.FR

    val results = remember(
        query,
        pregnancyMeds,
        medReminders,
        appointments,
        expenses,
        walletCards,
        notes,
        voiceMemos,
        protocolSteps,
        nutritionFoods,
        careProviders
    ) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.length < 2) emptyList()
        else buildList {
            medReminders.filter {
                it.title.lowercase().contains(q) || it.medication.lowercase().contains(q) || it.notes.lowercase().contains(q)
            }.forEach { m ->
                add(
                    GlobalSearchResultItem(
                        id = "mr_${m.id}",
                        title = m.title,
                        subtitle = "${m.medication} • ${m.dosage} • ${String.format(Locale.US, "%02d:%02d", m.hour, m.minute)}",
                        badge = if (isFrench) "Rappel Méd." else "Med Reminder",
                        targetTab = 0
                    )
                )
            }
            pregnancyMeds.filter {
                it.name.lowercase().contains(q) || it.activeIngredient.lowercase().contains(q) || it.clinicalNotes.lowercase().contains(q)
            }.forEach { pm ->
                add(
                    GlobalSearchResultItem(
                        id = "pm_${pm.id}",
                        title = pm.name,
                        subtitle = "${pm.safetyLevel} • ${pm.activeIngredient}",
                        badge = if (isFrench) "Base Méd." else "Med DB",
                        targetTab = 3
                    )
                )
            }
            appointments.filter {
                it.title.lowercase().contains(q) || it.doctor.lowercase().contains(q) || it.location.lowercase().contains(q) || it.dateStr.lowercase().contains(q)
            }.forEach { apt ->
                add(
                    GlobalSearchResultItem(
                        id = "apt_${apt.id}",
                        title = apt.title,
                        subtitle = "${apt.dateStr} ${apt.timeStr} • ${apt.location}",
                        badge = if (isFrench) "Rendez-vous" else "Visit",
                        targetTab = 1
                    )
                )
            }
            protocolSteps.filter {
                it.stepName.lowercase().contains(q) || it.medication.lowercase().contains(q) || it.details.lowercase().contains(q) || it.dateStr.lowercase().contains(q)
            }.forEach { step ->
                add(
                    GlobalSearchResultItem(
                        id = "ps_${step.id}",
                        title = step.stepName,
                        subtitle = "Phase ${step.phaseId} • ${step.dateStr} • ${step.status}",
                        badge = if (isFrench) "Protocole" else "Protocol",
                        targetTab = 1
                    )
                )
            }
            expenses.filter {
                it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.notes.lowercase().contains(q)
            }.forEach { exp ->
                add(
                    GlobalSearchResultItem(
                        id = "exp_${exp.id}",
                        title = exp.title,
                        subtitle = "${exp.amount} ${exp.currency} • ${exp.category} • ${exp.dateStr}",
                        badge = if (isFrench) "Dépense" else "Expense",
                        targetTab = 4
                    )
                )
            }
            walletCards.filter {
                it.cardTitle.lowercase().contains(q) || it.cardCategory.lowercase().contains(q) || it.notes.lowercase().contains(q)
            }.forEach { card ->
                add(
                    GlobalSearchResultItem(
                        id = "wc_${card.id}",
                        title = card.cardTitle,
                        subtitle = "${card.cardCategory} • •••• ${card.cardNumber.takeLast(4)}",
                        badge = if (isFrench) "Portefeuille" else "Wallet",
                        targetTab = 0,
                        requiresWalletPin = true
                    )
                )
            }
            notes.filter {
                it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
            }.forEach { n ->
                add(
                    GlobalSearchResultItem(
                        id = "nt_${n.id}",
                        title = n.title.ifBlank { n.content.take(40) },
                        subtitle = "${n.timestampText} • ${n.addedBy}",
                        badge = "Note",
                        targetTab = 0
                    )
                )
            }
            voiceMemos.filter {
                it.title.lowercase().contains(q)
            }.forEach { vm ->
                add(
                    GlobalSearchResultItem(
                        id = "vm_${vm.id}",
                        title = vm.title,
                        subtitle = "${vm.durationSec}s • ${vm.timestampText}",
                        badge = if (isFrench) "Mémo Vocal" else "Voice Memo",
                        targetTab = 0
                    )
                )
            }
            nutritionFoods.filter {
                it.name.lowercase().contains(q) || it.category.lowercase().contains(q) || it.highlights.lowercase().contains(q)
            }.forEach { nf ->
                add(
                    GlobalSearchResultItem(
                        id = "nf_${nf.id}",
                        title = nf.name,
                        subtitle = "${nf.safetyStatus} • ${nf.category}",
                        badge = "Nutrition",
                        targetTab = 3
                    )
                )
            }
            careProviders.filter {
                it.name.lowercase().contains(q) || it.affiliation.lowercase().contains(q) || it.providerType.lowercase().contains(q)
            }.forEach { cp ->
                add(
                    GlobalSearchResultItem(
                        id = "cp_${cp.id}",
                        title = cp.name,
                        subtitle = "${cp.providerType} • ${cp.phone}",
                        badge = if (isFrench) "Médecin" else "Doctor",
                        targetTab = 3
                    )
                )
            }
        }.take(20)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = if (isFrench) "Rechercher" else "Search",
                    tint = colors.mutedText,
                    modifier = Modifier.size(20.dp)
                )
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = Strings.t("global_search_hint", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("global_search_input")
                )
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("global_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isFrench) "Effacer" else "Clear",
                            tint = colors.mutedText
                        )
                    }
                }
                IconButton(
                    onClick = onOpenVoiceAssistant,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("global_voice_ai_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accent.copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (isFrench) "Assistant Vocal IA" else "AI Voice Assistant",
                            tint = colors.accent,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showDropdown && query.trim().length >= 2) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .heightIn(max = 280.dp)
            ) {
                if (results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFrench) "Aucun résultat trouvé pour « $query »" else "No results found for \"$query\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(results, key = { it.id }) { item ->
                            val badgeColors = getCategoryBadgeColors(item.badge)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onResultClick(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = badgeColors.containerColor
                                    ) {
                                        Text(
                                            text = item.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColors.contentColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.mutedText,
                                        modifier = Modifier.size(18.dp)
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

@Composable
fun AiVoiceAssistantDialog(
    lang: AppLanguage,
    systemContext: String,
    onExecuteExpense: (VoiceIntentAction.AddExpense) -> Unit,
    onExecuteNote: (VoiceIntentAction.AddNote) -> Unit,
    onExecuteAppointment: (VoiceIntentAction.AddAppointment) -> Unit,
    onExecuteMedDose: (VoiceIntentAction.MarkMedDose) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val isFrench = lang == AppLanguage.FR
    val scope = rememberCoroutineScope()

    var inputCommand by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusResponse by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                inputCommand = spoken
            }
        }
    }

    fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isFrench) "fr-FR" else "en-US")
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (isFrench) "Parlez (ex: Dépense 45$ pharmacie, ou question médicale)" else "Speak (e.g. Spent $45 at pharmacy, or ask a question)"
            )
        }
        runCatching { speechLauncher.launch(intent) }
            .onFailure {
                errorMessage = if (isFrench) {
                    "Reconnaissance vocale système indisponible — tapez votre commande ci-dessous."
                } else {
                    "System speech recognition unavailable — type your command below."
                }
            }
    }

    fun submitCommand() {
        val clean = inputCommand.trim()
        if (clean.isBlank() || isProcessing) return
        scope.launch {
            isProcessing = true
            errorMessage = null
            try {
                when (val action = AiAssistantService.parseVoiceCommand(clean, systemContext, isFrench)) {
                    is VoiceIntentAction.AddExpense -> {
                        onExecuteExpense(action)
                        statusResponse = if (isFrench) {
                            "Dépense enregistrée : ${action.title} (${action.amount} ${action.currency})"
                        } else {
                            "Expense logged: ${action.title} (${action.amount} ${action.currency})"
                        }
                    }
                    is VoiceIntentAction.AddNote -> {
                        onExecuteNote(action)
                        statusResponse = if (isFrench) {
                            "Note ajoutée au journal : ${action.title}"
                        } else {
                            "Note added to journal: ${action.title}"
                        }
                    }
                    is VoiceIntentAction.AddAppointment -> {
                        onExecuteAppointment(action)
                        statusResponse = if (isFrench) {
                            "Rendez-vous planifié : ${action.title} (${action.dateStr} ${action.timeStr})"
                        } else {
                            "Appointment scheduled: ${action.title} (${action.dateStr} ${action.timeStr})"
                        }
                    }
                    is VoiceIntentAction.MarkMedDose -> {
                        onExecuteMedDose(action)
                        statusResponse = if (isFrench) {
                            "Prise ${action.period} validée pour ${action.medicationHint.ifBlank { "Estrace / Traitement" }} ✓"
                        } else {
                            "${action.period} dose marked taken for ${action.medicationHint.ifBlank { "Estrace / Treatment" }} ✓"
                        }
                    }
                    is VoiceIntentAction.AnswerOnly -> {
                        statusResponse = action.reply
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: (if (isFrench) "Erreur de traitement IA" else "AI processing error")
            } finally {
                isProcessing = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accent.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFrench) "Assistant Vocal & Commandes IA" else "AI Voice Assistant & Commands",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = if (isFrench) "Dépenses, prises Estrace, notes ou questions" else "Log expenses, check doses, add notes, or ask Q&A",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = Strings.t("cancel", lang), tint = colors.mutedText)
                    }
                }

                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    label = {
                        Text(
                            if (isFrench) "Ex : « Dépense 45 USD pharmacie » ou « Quand est le TEC ? »"
                            else "E.g. \"Spent $45 at pharmacy\" or \"When is the FET?\""
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_ai_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = ::launchSpeechRecognizer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_ai_speak_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isFrench) "Dicter" else "Speak", maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = ::submitCommand,
                        enabled = !isProcessing && inputCommand.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_ai_send_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isFrench) "Exécuter" else "Run", maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFDECE9)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB54938),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (statusResponse.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.background,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        SelectionContainer {
                            Text(
                                text = statusResponse,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primaryText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
