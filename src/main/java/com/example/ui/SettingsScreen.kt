package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.ThemeColors
import com.example.ui.theme.getThemeColors
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
  userEmail: String,
  syncSummary: CloudSyncSummary,
  profile: SyncedUserProfile,
  protocolSteps: List<SyncedProtocolStep> = emptyList(),
  medReminders: List<SyncedMedReminder> = emptyList(),
  pregnancyMeds: List<SyncedPregnancyMed> = emptyList(),
  careProviders: List<SyncedCareProvider> = emptyList(),
  expenses: List<SyncedExpenseRecord> = emptyList(),
  appointments: List<SyncedAppointmentItem> = emptyList(),
  tasks: List<TaskItem> = emptyList(),
  notes: List<SyncedNoteItem> = emptyList(),
  voiceMemos: List<SyncedVoiceMemo> = emptyList(),
  walletCards: List<SyncedWalletCard> = emptyList(),
  backups: List<SyncedBackupSnapshot> = emptyList(),
  onAvatarsChanged: (mom: String, partner: String, baby: String) -> Unit,
  onPartnerProfileChanged: (String) -> Unit = {},
  onCoupleAvatarChanged: (String) -> Unit = {},
  onUpdateWalletPin: (String) -> Unit = {},
  onCreateCloudBackup: () -> Unit = {},
  onRestoreBackupJson: (String) -> Unit = {},
  onToggleAutoBackup: (Boolean) -> Unit = {},
  currentPalette: AppThemePalette,
  onPaletteChanged: (AppThemePalette) -> Unit,
  currentLanguage: AppLanguage,
  onLanguageChanged: (AppLanguage) -> Unit,
  onInitiateSync: () -> Unit,
  onSignOut: () -> Unit,
  onCloseSettings: (() -> Unit)? = null,
  theme: ThemeColors
) {
  val context = LocalContext.current
  val isFr = currentLanguage == AppLanguage.FR
  val momAvatar = profile.momAvatar
  val partnerAvatar = profile.partnerAvatar
  val babyAvatar = profile.babyAvatar

  var showPinChangeDialog by remember { mutableStateOf(false) }
  var pendingRestoreSnapshot by remember { mutableStateOf<SyncedBackupSnapshot?>(null) }

  // Export Category Selection
  val selectedCategories = remember {
    mutableStateMapOf<ExportCategory, Boolean>().apply {
      ExportCategory.entries.forEach { put(it, true) }
    }
  }
  val activeCategories = selectedCategories.filterValues { it }.keys

  // Couple Photo Picker
  val couplePhotoLauncher = rememberLauncherForActivityResult(
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
            onCoupleAvatarChanged(b64)
            Toast.makeText(
              context,
              if (isFr) "Photo du couple mise à jour !" else "Couple photo updated!",
              Toast.LENGTH_SHORT
            ).show()
          }
        }
      }
    }
  }

  // JSON Export SAF Launcher
  val createJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      runCatching {
        val json = ExportCenterUtils.buildBackupJson(
          profile = profile,
          tasks = tasks,
          notes = notes,
          appointments = appointments,
          protocolSteps = protocolSteps,
          careProviders = careProviders,
          expenses = expenses,
          medReminders = medReminders,
          pregnancyMeds = pregnancyMeds,
          checklists = emptyList()
        )
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(json.toByteArray(Charsets.UTF_8))
        }
        Toast.makeText(
          context,
          if (isFr) "Sauvegarde JSON enregistrée !" else "JSON backup exported!",
          Toast.LENGTH_SHORT
        ).show()
      }.onFailure {
        Toast.makeText(
          context,
          if (isFr) "Erreur export JSON" else "JSON export error",
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  // JSON Import SAF Launcher
  val openJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      runCatching {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
        if (json.isNotBlank()) {
          onRestoreBackupJson(json)
          Toast.makeText(
            context,
            if (isFr) "Données importées & synchronisées !" else "Backup restored & synced!",
            Toast.LENGTH_SHORT
          ).show()
        }
      }.onFailure {
        Toast.makeText(
          context,
          if (isFr) "Fichier JSON invalide" else "Invalid JSON file",
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (onCloseSettings != null) {
          IconButton(
            onClick = onCloseSettings,
            modifier = Modifier
              .size(48.dp)
              .testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = if (isFr) "Retour" else "Back",
              tint = theme.primaryText
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = Strings.t("settings_tab", currentLanguage),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = theme.primaryText
          )
          Text(
            text = if (isFr)
              "Couple, Sécurité PIN, Exports & Sauvegarde Cloud"
            else
              "Couple Profile, PIN Security, Exports & Cloud Backup",
            style = MaterialTheme.typography.bodySmall,
            color = theme.mutedText
          )
        }
      }
    }

    // 1. Live Sync Status Dashboard & Account Sign-Out
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SyncStatusDashboardCard(
          userEmail = userEmail,
          syncSummary = syncSummary,
          onInitiateSync = onInitiateSync,
          theme = theme,
          language = currentLanguage
        )

        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("sign_out_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, theme.border),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.cardSurface)
        ) {
          Text(
            text = "${Strings.t("sign_out", currentLanguage)} ($userEmail)",
            style = MaterialTheme.typography.bodySmall,
            color = theme.primaryText,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    // 2. Partner Collaboration Mode, Couple Avatar & Wallet 4-Digit PIN
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Group, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = if (isFr) "Collaboration Couple & Sécurité" else "Partner Collaboration & Security",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          // Active Partner Profile Switcher
          Text(
            text = if (isFr) "Profil actif sur cet appareil :" else "Active profile on this device:",
            style = MaterialTheme.typography.labelMedium,
            color = theme.mutedText
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            listOf("Fatou" to "Fatou", "Partner" to if (isFr) "Partenaire" else "Partner").forEach { (key, label) ->
              val selected = profile.activePartnerProfile == key
              FilterChip(
                selected = selected,
                onClick = { onPartnerProfileChanged(key) },
                label = { Text(label, maxLines = 1, softWrap = false) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
              )
            }
          }

          HorizontalDivider(color = theme.border)

          // Couple Profile Photo Picker
          val avatarBitmap = remember(profile.coupleAvatarBase64) {
            if (profile.coupleAvatarBase64.isBlank()) null
            else runCatching {
              val bytes = Base64.decode(profile.coupleAvatarBase64, Base64.DEFAULT)
              BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (avatarBitmap != null) {
              Image(
                bitmap = avatarBitmap.asImageBitmap(),
                contentDescription = "Couple Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .border(2.dp, theme.accent, CircleShape)
              )
            } else {
              Image(
                painter = painterResource(id = R.drawable.img_couple_avatar),
                contentDescription = "Default Couple Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .border(2.dp, theme.accent, CircleShape)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isFr) "Photo du Couple (En-tête)" else "Couple Header Photo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryText
              )
              Text(
                text = if (isFr) "Synchronisée entre les 2 téléphones" else "Synced across both devices",
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText
              )
            }
            OutlinedButton(
              onClick = {
                couplePhotoLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.heightIn(min = 48.dp)
            ) {
              Text(if (isFr) "Changer" else "Change", maxLines = 1, softWrap = false)
            }
          }

          HorizontalDivider(color = theme.border)

          // Wallet PIN Security Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isFr) "Code PIN du Portefeuille Numérique" else "Digital Wallet 4-Digit PIN",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryText
              )
              Text(
                text = if (isFr) "PIN actuel actif : •••• (par défaut 8888)" else "Current active PIN: •••• (default 8888)",
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText
              )
            }
            Button(
              onClick = { showPinChangeDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              modifier = Modifier.heightIn(min = 48.dp).testTag("settings_change_pin_button")
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(Modifier.width(6.dp))
              Text(if (isFr) "Modifier PIN" else "Change PIN", color = Color.White, maxLines = 1, softWrap = false)
            }
          }
        }
      }
    }

    // 3. Export Center, Email & Cloud Backup / Point-in-Time Restore
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.IosShare, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = Strings.t("export_center", currentLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          Text(
            text = if (isFr) "Sélectionnez les données à inclure dans l'export PDF / CSV :" else "Select categories to include in PDF / CSV export:",
            style = MaterialTheme.typography.labelMedium,
            color = theme.mutedText
          )

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExportCategory.entries.forEach { cat ->
              val checked = selectedCategories[cat] ?: true
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { selectedCategories[cat] = !checked }
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = { selectedCategories[cat] = it }
                )
                Text(
                  text = if (isFr) cat.labelFr else cat.labelEn,
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.primaryText
                )
              }
            }
          }

          // PDF, CSV, Email Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val cats = activeCategories.ifEmpty { ExportCategory.entries.toSet() }
                val pdf = ExportCenterUtils.generatePdfReport(
                  context = context,
                  categories = cats,
                  isFrench = isFr,
                  profile = profile,
                  protocolSteps = protocolSteps,
                  medReminders = medReminders,
                  pregnancyMeds = pregnancyMeds,
                  careProviders = careProviders,
                  expenses = expenses,
                  appointments = appointments,
                  tasks = tasks,
                  notes = notes,
                  voiceMemos = voiceMemos,
                  walletCards = walletCards
                )
                ExportCenterUtils.shareFile(
                  context = context,
                  file = pdf,
                  mimeType = "application/pdf",
                  subject = if (isFr) "Rapport B&F Parcours" else "B&F Journey Report"
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(Modifier.width(4.dp))
              Text("PDF", color = Color.White, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = {
                val cats = activeCategories.ifEmpty { ExportCategory.entries.toSet() }
                val csv = ExportCenterUtils.generateCsvReport(
                  context = context,
                  categories = cats,
                  protocolSteps = protocolSteps,
                  expenses = expenses,
                  appointments = appointments,
                  tasks = tasks,
                  notes = notes,
                  medReminders = medReminders
                )
                ExportCenterUtils.shareFile(
                  context = context,
                  file = csv,
                  mimeType = "text/csv",
                  subject = if (isFr) "Export CSV B&F Parcours" else "B&F Journey CSV Export"
                )
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.accent)
              Spacer(Modifier.width(4.dp))
              Text("CSV", maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = {
                val cats = activeCategories.ifEmpty { ExportCategory.entries.toSet() }
                val pdf = ExportCenterUtils.generatePdfReport(
                  context = context,
                  categories = cats,
                  isFrench = isFr,
                  profile = profile,
                  protocolSteps = protocolSteps,
                  medReminders = medReminders,
                  pregnancyMeds = pregnancyMeds,
                  careProviders = careProviders,
                  expenses = expenses,
                  appointments = appointments,
                  tasks = tasks,
                  notes = notes,
                  voiceMemos = voiceMemos,
                  walletCards = walletCards
                )
                ExportCenterUtils.sendViaEmail(
                  context = context,
                  file = pdf,
                  mimeType = "application/pdf",
                  subject = if (isFr) "Dossier Médical B&F Parcours" else "B&F Journey Medical Summary",
                  summaryBody = if (isFr) "Veuillez trouver ci-joint le rapport complet généré depuis B&F Parcours." else "Attached is the exported summary report from B&F Journey."
                )
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.accent)
              Spacer(Modifier.width(4.dp))
              Text("Email", maxLines = 1, softWrap = false)
            }
          }

          // Complete Text Report & Design Specs Export Button
          OutlinedButton(
            onClick = {
              val txtFile = ExportCenterUtils.generateTextExportReport(
                context = context,
                isFrench = isFr,
                profile = profile,
                protocolSteps = protocolSteps,
                medReminders = medReminders,
                pregnancyMeds = pregnancyMeds,
                careProviders = careProviders,
                expenses = expenses,
                appointments = appointments,
                tasks = tasks,
                notes = notes,
                walletCards = walletCards
              )
              ExportCenterUtils.shareFile(
                context = context,
                file = txtFile,
                mimeType = "text/plain",
                subject = if (isFr) "Rapport Texte & Design B&F Parcours" else "B&F Journey Complete Text & Design Report"
              )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, theme.accent)
          ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp), tint = theme.accent)
            Spacer(Modifier.width(8.dp))
            Text(
              text = if (isFr) "📄 Exporter Dossier Complet & Design (.txt)" else "📄 Export Complete Dossier & Design Report (.txt)",
              color = theme.primaryText,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false
            )
          }

          // JSON Export & Import
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                createJsonLauncher.launch("bf_parcours_backup_$stamp.json")
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.accent)
              Spacer(Modifier.width(4.dp))
              Text(if (isFr) "Export JSON" else "Export JSON", maxLines = 1, softWrap = false)
            }
            OutlinedButton(
              onClick = { openJsonLauncher.launch(arrayOf("application/json", "*/*")) },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.accent)
              Spacer(Modifier.width(4.dp))
              Text(if (isFr) "Import JSON" else "Import JSON", maxLines = 1, softWrap = false)
            }
          }

          HorizontalDivider(color = theme.border)

          // Auto-Backup 24h Toggle & Cloud Backup Now
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = Strings.t("auto_backup", currentLanguage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryText
              )
              Text(
                text = if (isFr) "Enregistre un instantané cloud quotidiennement" else "Saves a daily encrypted Firestore snapshot",
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText
              )
            }
            Switch(
              checked = profile.autoCloudBackupEnabled,
              onCheckedChange = onToggleAutoBackup
            )
          }

          Button(
            onClick = {
              onCreateCloudBackup()
              Toast.makeText(
                context,
                if (isFr) "Instantané Cloud créé !" else "Cloud backup snapshot created!",
                Toast.LENGTH_SHORT
              ).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
          ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Strings.t("backup_now", currentLanguage), color = Color.White, fontWeight = FontWeight.SemiBold)
          }

          if (backups.isNotEmpty()) {
            Text(
              text = if (isFr) "Instantanés Cloud récents (${backups.size}) :" else "Recent Cloud Snapshots (${backups.size}):",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              backups.take(5).forEach { snap ->
                val dateFmt = SimpleDateFormat("dd MMM yyyy HH:mm", if (isFr) Locale.FRENCH else Locale.US)
                val snapTime = if (snap.timestampEpochMillis > 0L) dateFmt.format(Date(snap.timestampEpochMillis)) else ""
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
                        text = "$snapTime (${snap.triggerType})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryText
                      )
                      Text(
                        text = "${snap.recordCount} records • ${snap.addedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.mutedText
                      )
                    }
                    TextButton(
                      onClick = { pendingRestoreSnapshot = snap },
                      modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                      Text(
                        if (isFr) "Restaurer" else "Restore",
                        color = theme.accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
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

    // 4. Family & Profile Avatars Card (Synced via Firestore)
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Face, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = Strings.t("profile_avatars", currentLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "${Strings.t("mom_avatar", currentLanguage)} : $momAvatar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = theme.primaryText
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("🌸", "🤰", "👑", "🌿", "✨").forEach { avatar ->
              val isSel = momAvatar == avatar
              Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSel) theme.accent.copy(alpha = 0.2f) else theme.background,
                border = BorderStroke(1.dp, if (isSel) theme.accent else theme.border),
                onClick = { onAvatarsChanged(avatar, partnerAvatar, babyAvatar) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = avatar, fontSize = 20.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "${Strings.t("partner_avatar", currentLanguage)} : $partnerAvatar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = theme.primaryText
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("💙", "👔", "🌟", "🕊️", "☕").forEach { avatar ->
              val isSel = partnerAvatar == avatar
              Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSel) theme.accent.copy(alpha = 0.2f) else theme.background,
                border = BorderStroke(1.dp, if (isSel) theme.accent else theme.border),
                onClick = { onAvatarsChanged(momAvatar, avatar, babyAvatar) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = avatar, fontSize = 20.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "${Strings.t("baby_avatar", currentLanguage)} : $babyAvatar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = theme.primaryText
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("👶", "🍼", "🧸", "🐣", "⭐").forEach { avatar ->
              val isSel = babyAvatar == avatar
              Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSel) theme.accent.copy(alpha = 0.2f) else theme.background,
                border = BorderStroke(1.dp, if (isSel) theme.accent else theme.border),
                onClick = { onAvatarsChanged(momAvatar, partnerAvatar, avatar) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(text = avatar, fontSize = 20.sp)
                }
              }
            }
          }
        }
      }
    }

    // 5. Language Selection Card
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Translate, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = Strings.t("language_selection", currentLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
              selected = currentLanguage == AppLanguage.FR,
              onClick = { onLanguageChanged(AppLanguage.FR) },
              label = { Text("Français (FR)", maxLines = 1, softWrap = false) },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = theme.accent.copy(alpha = 0.15f),
                selectedLabelColor = theme.primaryText
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = theme.border,
                selectedBorderColor = theme.accent,
                enabled = true,
                selected = currentLanguage == AppLanguage.FR
              )
            )
            FilterChip(
              selected = currentLanguage == AppLanguage.EN,
              onClick = { onLanguageChanged(AppLanguage.EN) },
              label = { Text("English (EN)", maxLines = 1, softWrap = false) },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = theme.accent.copy(alpha = 0.15f),
                selectedLabelColor = theme.primaryText
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = theme.border,
                selectedBorderColor = theme.accent,
                enabled = true,
                selected = currentLanguage == AppLanguage.EN
              )
            )
          }
        }
      }
    }

    // 6. Theme Selection Card
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = Strings.t("theme_selection", currentLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemePalette.entries.forEach { palette ->
              val isSelected = currentPalette == palette
              val paletteName = if (currentLanguage == AppLanguage.FR) palette.displayNameFr else palette.displayNameEn
              val pColors = getThemeColors(palette)

              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) theme.accent.copy(alpha = 0.12f) else theme.background,
                border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
                onClick = { onPaletteChanged(palette) }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      modifier = Modifier.size(16.dp),
                      shape = CircleShape,
                      color = pColors.accent,
                      border = BorderStroke(0.5.dp, pColors.border)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      modifier = Modifier.size(16.dp),
                      shape = CircleShape,
                      color = pColors.cardSurface,
                      border = BorderStroke(0.5.dp, pColors.border)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = paletteName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                      color = theme.primaryText
                    )
                  }

                  if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Change Wallet PIN Modal
  if (showPinChangeDialog) {
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinErr by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showPinChangeDialog = false },
      containerColor = theme.cardSurface,
      title = {
        Text(
          text = if (isFr) "Modifier le code PIN du Portefeuille" else "Change Digital Wallet PIN",
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = currentPinInput,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { currentPinInput = it; pinErr = null } },
            label = { Text(if (isFr) "PIN actuel" else "Current PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newPinInput,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { newPinInput = it; pinErr = null } },
            label = { Text(if (isFr) "Nouveau PIN (4 chiffres)" else "New PIN (4 digits)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          if (pinErr != null) {
            Text(pinErr!!, color = Color(0xFFB54938), style = MaterialTheme.typography.labelSmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (currentPinInput != profile.walletPin) {
              pinErr = if (isFr) "PIN actuel incorrect" else "Current PIN is incorrect"
            } else if (newPinInput.length != 4) {
              pinErr = if (isFr) "Entrez exactement 4 chiffres" else "Enter exactly 4 digits"
            } else {
              onUpdateWalletPin(newPinInput)
              showPinChangeDialog = false
              Toast.makeText(context, if (isFr) "PIN mis à jour !" else "PIN updated!", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(if (isFr) "Enregistrer" else "Save", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPinChangeDialog = false }) {
          Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }

  // Restore Snapshot Confirmation Dialog
  pendingRestoreSnapshot?.let { snap ->
    AlertDialog(
      onDismissRequest = { pendingRestoreSnapshot = null },
      containerColor = theme.cardSurface,
      title = {
        Text(
          text = if (isFr) "Restaurer l'instantané Cloud ?" else "Restore Cloud Snapshot?",
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
      },
      text = {
        Text(
          text = if (isFr)
            "Les enregistrements de cette sauvegarde (${snap.recordCount} éléments) seront restaurés et fusionnés dans votre dossier synchronisé."
          else
            "Records from this backup (${snap.recordCount} items) will be restored and synced across your devices.",
          color = theme.mutedText
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onRestoreBackupJson(snap.payloadJson)
            pendingRestoreSnapshot = null
            Toast.makeText(context, if (isFr) "Restauration terminée !" else "Snapshot restored!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(if (isFr) "Restaurer" else "Restore", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingRestoreSnapshot = null }) {
          Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }
}
