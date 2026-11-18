package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.BusinessCardOcrParser
import com.example.data.SyncedCareProvider
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Copies picked image Uri into local app storage to persist permanently across app restarts.
 */
private fun copyUriToLocalPhotoFile(context: Context, contactId: String, uri: Uri): String? {
  return try {
    val photosDir = File(context.filesDir, "contact_photos").apply { if (!exists()) mkdirs() }
    val destFile = File(photosDir, "contact_${contactId}_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
      FileOutputStream(destFile).use { output ->
        input.copyTo(output)
      }
    }
    destFile.absolutePath
  } catch (e: Exception) {
    uri.toString()
  }
}

/**
 * Saves a captured Bitmap locally into contact photos directory.
 */
private fun saveBitmapLocally(context: Context, contactId: String, bitmap: Bitmap): String? {
  return try {
    val photosDir = File(context.filesDir, "contact_photos").apply { if (!exists()) mkdirs() }
    val destFile = File(photosDir, "contact_${contactId}_${System.currentTimeMillis()}.jpg")
    FileOutputStream(destFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }
    destFile.absolutePath
  } catch (e: Exception) {
    null
  }
}

@Composable
fun PhoneBookScreen(
  providers: List<SyncedCareProvider>,
  searchQuery: String = "",
  theme: ThemeColors,
  language: AppLanguage,
  onSaveProvider: (SyncedCareProvider) -> Unit,
  onDeleteProvider: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isFr = language == AppLanguage.FR

  var localSearchQuery by remember { mutableStateOf("") }
  val activeQuery = searchQuery.ifBlank { localSearchQuery }

  var selectedCategoryFilter by remember { mutableStateOf("ALL") }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingProvider by remember { mutableStateOf<SyncedCareProvider?>(null) }
  var providerToDelete by remember { mutableStateOf<SyncedCareProvider?>(null) }

  // Filter contacts based on search query and category
  val filteredContacts = remember(providers, activeQuery, selectedCategoryFilter) {
    providers.filter { prov ->
      val matchesCategory = when (selectedCategoryFilter) {
        "ALL" -> true
        "EMERGENCY" -> prov.isEmergency
        else -> prov.category.equals(selectedCategoryFilter, ignoreCase = true)
      }
      val matchesQuery = activeQuery.isBlank() ||
        prov.name.contains(activeQuery, ignoreCase = true) ||
        prov.providerType.contains(activeQuery, ignoreCase = true) ||
        prov.affiliation.contains(activeQuery, ignoreCase = true) ||
        prov.phone.contains(activeQuery, ignoreCase = true) ||
        prov.emergencyContact.contains(activeQuery, ignoreCase = true) ||
        prov.email.contains(activeQuery, ignoreCase = true) ||
        prov.notes.contains(activeQuery, ignoreCase = true) ||
        prov.address.contains(activeQuery, ignoreCase = true)

      matchesCategory && matchesQuery
    }
  }

  val emergencyContacts = remember(providers) {
    providers.filter { it.isEmergency }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("phone_book_screen"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Header & Add Contact Button
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📞", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isFr) "Carnet d'Adresses & Contacts" else "Phone Book & Directory",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
          }
          Text(
            text = if (isFr)
              "${providers.size} contact${if (providers.size > 1) "s" else ""} • Équipe médicale, PMA & proches"
            else
              "${providers.size} contact${if (providers.size > 1) "s" else ""} • Medical, IVF & loved ones",
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedText
          )
        }

        Button(
          onClick = { showAddDialog = true },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("btn_add_contact")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isFr) "+ Ajouter" else "+ Add",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // Emergency Speed Dial Banner (if any emergency contacts defined)
    if (emergencyContacts.isNotEmpty()) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
        modifier = Modifier.fillMaxWidth().testTag("emergency_speed_dial_banner")
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🚨", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isFr) "Urgences & Garde Médicale 24/7" else "24/7 Emergency & On-Call",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF991B1B)
              )
            }
            Surface(
              shape = RoundedCornerShape(100.dp),
              color = Color(0xFFEF4444)
            ) {
              Text(
                text = if (isFr) "Prioritaire" else "Priority",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
            }
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(emergencyContacts, key = { "emerg_${it.id}" }) { contact ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.clickable {
                  try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                    context.startActivity(intent)
                  } catch (_: Exception) {
                    Toast.makeText(context, if (isFr) "Impossible d'appeler" else "Cannot dial", Toast.LENGTH_SHORT).show()
                  }
                }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                  Column {
                    Text(
                      text = contact.name,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF991B1B),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = contact.phone,
                      style = MaterialTheme.typography.bodySmall,
                      fontSize = 10.sp,
                      color = Color(0xFFB91C1C)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Local Search Input (if not already searching globally)
    if (searchQuery.isBlank()) {
      OutlinedTextField(
        value = localSearchQuery,
        onValueChange = { localSearchQuery = it },
        placeholder = {
          Text(
            if (isFr) "Rechercher par nom, clinique, téléphone, rôle..." else "Search by name, clinic, phone, role...",
            style = MaterialTheme.typography.bodySmall
          )
        },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
          if (localSearchQuery.isNotBlank()) {
            IconButton(onClick = { localSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Effacer", tint = theme.mutedText, modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = theme.cardSurface,
          unfocusedContainerColor = theme.cardSurface,
          focusedBorderColor = theme.accent,
          unfocusedBorderColor = theme.border
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("input_search_phone_book")
      )
    }

    // Category Filter Chips
    val categoryFilterList = remember(isFr) {
      listOf(
        "ALL" to (if (isFr) "Tous" else "All"),
        "MEDICAL" to (if (isFr) "🩺 Médical & PMA" else "🩺 Medical & IVF"),
        "CLINIC" to (if (isFr) "🏥 Cliniques" else "🏥 Clinics"),
        "PHARMACY" to (if (isFr) "💊 Pharmacies" else "💊 Pharmacies"),
        "FAMILY" to (if (isFr) "👨‍👩‍👦 Famille & Proches" else "👨‍👩‍👦 Family & Loved Ones"),
        "EMERGENCY" to (if (isFr) "🚨 Urgences" else "🚨 Emergencies")
      )
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(categoryFilterList) { (catKey, catLabel) ->
        val isSelected = selectedCategoryFilter == catKey
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategoryFilter = catKey },
          label = {
            Text(
              text = catLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = theme.accent,
            selectedLabelColor = Color.White
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = theme.border,
            selectedBorderColor = theme.accent
          ),
          modifier = Modifier.height(32.dp)
        )
      }
    }

    // Contact List
    if (filteredContacts.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp).fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("📇", fontSize = 36.sp)
          Text(
            text = if (isFr) "Aucun contact trouvé" else "No contacts found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
          Text(
            text = if (isFr) "Ajoutez votre équipe médicale, les urgences de la clinique ou vos proches." else "Add your medical team, clinic emergency numbers or loved ones.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.mutedText,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isFr) "Ajouter un contact" else "Add a contact", color = Color.White)
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        items(filteredContacts, key = { it.id }) { contact ->
          PhoneBookContactCard(
            contact = contact,
            theme = theme,
            isFr = isFr,
            context = context,
            onEdit = { editingProvider = contact },
            onDelete = { providerToDelete = contact }
          )
        }
      }
    }
  }

  // Add Contact Dialog
  if (showAddDialog) {
    EditCareProviderDialog(
      provider = null,
      existingProviders = providers,
      theme = theme,
      isFr = isFr,
      onDismiss = { showAddDialog = false },
      onSave = { newProvider ->
        onSaveProvider(newProvider)
        showAddDialog = false
        Toast.makeText(context, if (isFr) "Contact ajouté avec succès !" else "Contact added!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Edit Contact Dialog
  editingProvider?.let { provider ->
    EditCareProviderDialog(
      provider = provider,
      existingProviders = providers,
      theme = theme,
      isFr = isFr,
      onDismiss = { editingProvider = null },
      onSave = { updatedProvider ->
        onSaveProvider(updatedProvider)
        editingProvider = null
        Toast.makeText(context, if (isFr) "Contact mis à jour !" else "Contact updated!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Delete Confirmation Dialog
  providerToDelete?.let { contact ->
    AlertDialog(
      onDismissRequest = { providerToDelete = null },
      title = {
        Text(if (isFr) "Supprimer ce contact ?" else "Delete this contact?")
      },
      text = {
        Text(
          if (isFr)
            "Voulez-vous vraiment supprimer « ${contact.name} » de votre répertoire ? Cette action est irréversible."
          else
            "Are you sure you want to delete '${contact.name}' from your phone book? This cannot be undone."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteProvider(contact.id)
            providerToDelete = null
            Toast.makeText(context, if (isFr) "Contact supprimé" else "Contact deleted", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text(if (isFr) "Supprimer" else "Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { providerToDelete = null }) {
          Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }
}

/**
 * Individual Contact Card with picture support, quick call, SMS, email, location and full details.
 */
@Composable
private fun PhoneBookContactCard(
  contact: SyncedCareProvider,
  theme: ThemeColors,
  isFr: Boolean,
  context: Context,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val clipboardManager = LocalClipboardManager.current

  val categoryColor = when (contact.category.uppercase()) {
    "MEDICAL" -> Color(0xFF2563EB)
    "CLINIC" -> Color(0xFF0D9488)
    "PHARMACY" -> Color(0xFFD97706)
    "FAMILY" -> Color(0xFF7C3AED)
    "INSURANCE" -> Color(0xFF475569)
    else -> theme.accent
  }

  val defaultAvatarEmoji = when (contact.category.uppercase()) {
    "MEDICAL" -> "🩺"
    "CLINIC" -> "🏥"
    "PHARMACY" -> "💊"
    "FAMILY" -> "👨‍👩‍👦"
    "INSURANCE" -> "🛡️"
    else -> "👤"
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = theme.cardSurface,
    border = BorderStroke(
      width = if (contact.isEmergency) 1.2.dp else 1.dp,
      color = if (contact.isEmergency) Color(0xFFF87171) else theme.border
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("contact_card_${contact.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Top Row: Avatar Picture, Name, Category Badge & Action Icons
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Contact Picture or Styled Avatar
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(categoryColor.copy(alpha = 0.12f))
            .border(BorderStroke(1.5.dp, categoryColor.copy(alpha = 0.4f)), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (contact.photoUri.isNotBlank()) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(contact.photoUri)
                .crossfade(true)
                .build(),
              contentDescription = contact.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
          } else {
            // Initials or emoji fallback
            val initials = contact.name
              .split(" ")
              .filter { it.isNotBlank() && !it.startsWith("Dr.") && !it.startsWith("Dr") }
              .take(2)
              .mapNotNull { it.firstOrNull()?.uppercaseChar() }
              .joinToString("")

            if (initials.length >= 2) {
              Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = categoryColor
              )
            } else {
              Text(defaultAvatarEmoji, fontSize = 22.sp)
            }
          }
        }

        // Name, Role & Badges
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = contact.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )

            if (contact.isEmergency) {
              Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFFFEE2E2),
                border = BorderStroke(0.6.dp, Color(0xFFFCA5A5))
              ) {
                Text(
                  text = "🚨 Urgence",
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFDC2626)
                )
              }
            }
          }

          Text(
            text = contact.providerType,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = categoryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          if (contact.affiliation.isNotBlank()) {
            Text(
              text = contact.affiliation,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = theme.mutedText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Edit & Delete Actions
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp).testTag("btn_edit_contact_${contact.id}")
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = theme.mutedText, modifier = Modifier.size(16.dp))
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp).testTag("btn_delete_contact_${contact.id}")
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = theme.mutedText, modifier = Modifier.size(16.dp))
          }
        }
      }

      // Contact Details: Phone, Email, Address
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(theme.background)
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Primary Phone
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            SelectionContainer {
              Text(
                text = contact.phone,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
            }
          }

          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(contact.phone))
              Toast.makeText(context, if (isFr) "Numéro copié !" else "Number copied!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(22.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = theme.mutedText, modifier = Modifier.size(12.dp))
          }
        }

        // Secondary / Direct Line if different
        if (contact.emergencyContact.isNotBlank() && contact.emergencyContact != contact.phone) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${if (isFr) "Directe / Urgence :" else "Direct / Emergency:"} ${contact.emergencyContact}",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = theme.mutedText
            )
          }
        }

        // Email
        if (contact.email.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            SelectionContainer {
              Text(
                text = contact.email,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = theme.mutedText
              )
            }
          }
        }

        // Address
        if (contact.address.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = contact.address,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = theme.mutedText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Notes / Instructions
        if (contact.notes.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = theme.cardSurface,
            border = BorderStroke(0.5.dp, theme.border)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("📝", fontSize = 12.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = contact.notes,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = theme.primaryText.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      // Quick 1-Tap Action Buttons (Dial, SMS, Email, Map, Portal)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Call Button
        Button(
          onClick = {
            try {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
              context.startActivity(intent)
            } catch (_: Exception) {
              Toast.makeText(context, if (isFr) "Application d'appel indisponible" else "Dialer not available", Toast.LENGTH_SHORT).show()
            }
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(34.dp).testTag("btn_call_${contact.id}")
        ) {
          Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isFr) "Appeler" else "Call", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        // SMS Button
        OutlinedButton(
          onClick = {
            try {
              val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phone}"))
              context.startActivity(intent)
            } catch (_: Exception) {
              Toast.makeText(context, if (isFr) "Application SMS indisponible" else "SMS not available", Toast.LENGTH_SHORT).show()
            }
          },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(34.dp).testTag("btn_sms_${contact.id}")
        ) {
          Icon(Icons.Default.Message, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("SMS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = theme.primaryText)
        }

        // Email Button (if email is provided)
        if (contact.email.isNotBlank()) {
          OutlinedButton(
            onClick = {
              try {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                context.startActivity(intent)
              } catch (_: Exception) {
                Toast.makeText(context, if (isFr) "Application Email indisponible" else "Email app not available", Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Icon(Icons.Default.Email, contentDescription = "Email", tint = theme.accent, modifier = Modifier.size(14.dp))
          }
        }

        // Map / Navigation Button (if address is provided)
        if (contact.address.isNotBlank()) {
          OutlinedButton(
            onClick = {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(contact.address)}"))
                context.startActivity(intent)
              } catch (_: Exception) {
                Toast.makeText(context, if (isFr) "Application Cartes indisponible" else "Maps not available", Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Carte", tint = theme.accent, modifier = Modifier.size(14.dp))
          }
        }

        // Patient Portal Link Button (if provided)
        if (contact.portalLink.isNotBlank() && contact.portalLink.startsWith("http")) {
          OutlinedButton(
            onClick = {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contact.portalLink))
                context.startActivity(intent)
              } catch (_: Exception) {
                Toast.makeText(context, if (isFr) "Lien inaccessible" else "Link not accessible", Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Icon(Icons.Default.OpenInNew, contentDescription = "Portail", tint = theme.accent, modifier = Modifier.size(14.dp))
          }
        }
      }
    }
  }
}

/**
 * Dialog to Add or Edit a Phone Book Contact with full picture photo picker support,
 * OCR business card scanner, and smart autocomplete suggestions.
 *
 * Implements a scrollable form Column using Modifier.verticalScroll(rememberScrollState())
 * and a fixed-height footer using Modifier.fillMaxWidth() to ensure 'Annuler' and 'Ajouter'
 * are always 100% visible and accessible without cut-off.
 */
@Composable
fun EditCareProviderDialog(
  provider: SyncedCareProvider?,
  existingProviders: List<SyncedCareProvider> = emptyList(),
  theme: ThemeColors,
  isFr: Boolean,
  onDismiss: () -> Unit,
  onSave: (SyncedCareProvider) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isEdit = provider != null
  val contactId = remember { provider?.id ?: "cp_${System.currentTimeMillis()}" }

  var name by remember { mutableStateOf(provider?.name ?: "") }
  var providerType by remember { mutableStateOf(provider?.providerType ?: (if (isFr) "Médecin Référent" else "Doctor / Specialist")) }
  var affiliation by remember { mutableStateOf(provider?.affiliation ?: "") }
  var phone by remember { mutableStateOf(provider?.phone ?: "") }
  var emergencyContact by remember { mutableStateOf(provider?.emergencyContact ?: "") }
  var email by remember { mutableStateOf(provider?.email ?: "") }
  var address by remember { mutableStateOf(provider?.address ?: "") }
  var portalLink by remember { mutableStateOf(provider?.portalLink ?: "") }
  var notes by remember { mutableStateOf(provider?.notes ?: "") }
  var isEmergency by remember { mutableStateOf(provider?.isEmergency ?: false) }
  var category by remember { mutableStateOf(provider?.category ?: "MEDICAL") }
  var photoUri by remember { mutableStateOf(provider?.photoUri ?: "") }
  var isScanningOcr by remember { mutableStateOf(false) }

  // Autocomplete / Auto-Suggest Lists
  val defaultNameSuggestions = listOf(
    "Dr. Anate Brauer",
    "Alex — Embryologie",
    "Dr. Alan Copperman",
    "Dr. Laurent (OB/GYN)",
    "Gramercy Surgery Center",
    "Mandell's Clinical Pharmacy",
    "Metro Drugs Pharmacy",
    "Progyny Patient Advocate",
    "Bala",
    "Fatou"
  )

  val filteredNameSuggestions = remember(name, existingProviders) {
    val allNames = (defaultNameSuggestions + existingProviders.map { it.name }).distinct()
    if (name.isBlank()) allNames.take(5)
    else allNames.filter { it.contains(name, ignoreCase = true) && !it.equals(name, ignoreCase = true) }.take(4)
  }

  val defaultAffiliationSuggestions = listOf(
    "RMA of New York — East Side",
    "RMA of New York — Downtown",
    "Gramercy Surgery Center",
    "Centre de Santé Maternelle",
    "Mandell's Clinical Pharmacy",
    "Mount Sinai Hospital",
    "New York-Presbyterian",
    "Progyny Insurance"
  )

  val filteredAffiliationSuggestions = remember(affiliation, existingProviders) {
    val allAffiliations = (defaultAffiliationSuggestions + existingProviders.map { it.affiliation }.filter { it.isNotBlank() }).distinct()
    if (affiliation.isBlank()) allAffiliations.take(4)
    else allAffiliations.filter { it.contains(affiliation, ignoreCase = true) && !it.equals(affiliation, ignoreCase = true) }.take(3)
  }

  val roleSuggestions = listOf(
    if (isFr) "Médecin Référent PMA" else "Primary Fertility Specialist",
    if (isFr) "Embryologiste" else "Embryologist",
    if (isFr) "Gynécologue-Obstétricien" else "OB/GYN",
    if (isFr) "Sage-Femme" else "Midwife",
    if (isFr) "Pharmacie FIV" else "IVF Pharmacy",
    if (isFr) "Anesthésiste" else "Anesthesiologist",
    if (isFr) "Coordinatrice Fertilité" else "Fertility Coordinator",
    if (isFr) "Conjoint & Partenaire" else "Partner & Spouse"
  )

  // 1. Photo Picker Launcher (Android 13+ zero permission Photo Picker)
  val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      val savedPath = copyUriToLocalPhotoFile(context, contactId, uri)
      if (savedPath != null) {
        photoUri = savedPath
        Toast.makeText(context, if (isFr) "📷 Photo enregistrée !" else "📷 Photo attached!", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // 2. OCR Camera Scanner Launcher (Takes a picture of business card and parses text)
  val ocrCameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap ->
    if (bitmap != null) {
      coroutineScope.launch {
        isScanningOcr = true
        try {
          val extracted = BusinessCardOcrParser.scanBitmap(bitmap, isFr)
          if (extracted.name.isNotBlank()) name = extracted.name
          if (extracted.providerType.isNotBlank()) providerType = extracted.providerType
          if (extracted.affiliation.isNotBlank()) affiliation = extracted.affiliation
          if (extracted.phone.isNotBlank()) phone = extracted.phone
          if (extracted.emergencyPhone.isNotBlank()) emergencyContact = extracted.emergencyPhone
          if (extracted.email.isNotBlank()) email = extracted.email
          if (extracted.address.isNotBlank()) address = extracted.address
          if (extracted.website.isNotBlank()) portalLink = extracted.website
          if (extracted.notes.isNotBlank()) {
            notes = if (notes.isBlank()) extracted.notes else "$notes • ${extracted.notes}"
          }
          category = extracted.category
          val savedPath = saveBitmapLocally(context, contactId, bitmap)
          if (savedPath != null) photoUri = savedPath
          Toast.makeText(
            context,
            if (isFr) "✨ ${extracted.extractedFieldCount} champs remplis par scan OCR !" else "✨ ${extracted.extractedFieldCount} fields extracted!",
            Toast.LENGTH_SHORT
          ).show()
        } catch (e: Exception) {
          Toast.makeText(context, if (isFr) "Scan OCR incomplet" else "OCR scan error", Toast.LENGTH_SHORT).show()
        } finally {
          isScanningOcr = false
        }
      }
    }
  }

  // 3. OCR Document / Image Picker Launcher
  val ocrImagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        isScanningOcr = true
        try {
          val extracted = BusinessCardOcrParser.scanUri(context, uri, isFr)
          if (extracted.name.isNotBlank()) name = extracted.name
          if (extracted.providerType.isNotBlank()) providerType = extracted.providerType
          if (extracted.affiliation.isNotBlank()) affiliation = extracted.affiliation
          if (extracted.phone.isNotBlank()) phone = extracted.phone
          if (extracted.emergencyPhone.isNotBlank()) emergencyContact = extracted.emergencyPhone
          if (extracted.email.isNotBlank()) email = extracted.email
          if (extracted.address.isNotBlank()) address = extracted.address
          if (extracted.website.isNotBlank()) portalLink = extracted.website
          if (extracted.notes.isNotBlank()) {
            notes = if (notes.isBlank()) extracted.notes else "$notes • ${extracted.notes}"
          }
          category = extracted.category
          val savedPath = copyUriToLocalPhotoFile(context, contactId, uri)
          if (savedPath != null) photoUri = savedPath
          Toast.makeText(
            context,
            if (isFr) "✨ ${extracted.extractedFieldCount} champs remplis par scan !" else "✨ ${extracted.extractedFieldCount} fields extracted!",
            Toast.LENGTH_SHORT
          ).show()
        } catch (e: Exception) {
          Toast.makeText(context, if (isFr) "Erreur OCR : ${e.localizedMessage}" else "OCR error", Toast.LENGTH_SHORT).show()
        } finally {
          isScanningOcr = false
        }
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = theme.cardSurface,
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.92f)
        .padding(vertical = 10.dp)
        .testTag("dialog_edit_phone_book_contact")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        // Sticky Header at top
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📇", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isEdit) (if (isFr) "Modifier le contact" else "Edit Contact")
              else (if (isFr) "Nouveau contact" else "New Contact"),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = theme.mutedText)
          }
        }

        HorizontalDivider(color = theme.border, modifier = Modifier.padding(vertical = 8.dp))

        // Middle Scrollable Form Container using Modifier.verticalScroll(rememberScrollState())
        val formScrollState = rememberScrollState()
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(formScrollState)
            .padding(bottom = 20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // -------------------------------------------------------------
          // OCR BUSINESS CARD & NUMBER SCANNER BANNER (Requirement 4)
          // -------------------------------------------------------------
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = theme.accent.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("ocr_scanner_card")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("📸", fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFr) "Scanner Carte de Visite (OCR)" else "Scan Business Card (OCR)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent
                  )
                }

                if (isScanningOcr) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = theme.accent,
                    strokeWidth = 2.dp
                  )
                }
              }

              Text(
                text = if (isFr)
                  "Prenez une photo de la carte du médecin ou de l'ordonnance pour remplir automatiquement nom, téléphone, adresse, email et spécialité."
                else
                  "Capture a doctor's card or prescription to auto-populate name, phone, address, email and specialty.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = theme.mutedText
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { ocrCameraLauncher.launch(null) },
                  enabled = !isScanningOcr,
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier.weight(1f).height(34.dp).testTag("btn_ocr_camera_scan")
                ) {
                  Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isFr) "Caméra" else "Camera", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                  onClick = {
                    ocrImagePickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  enabled = !isScanningOcr,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier.weight(1f).height(34.dp).testTag("btn_ocr_gallery_scan")
                ) {
                  Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isFr) "Importer" else "Import", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = theme.primaryText)
                }
              }
            }
          }

          // -------------------------------------------------------------
          // PROFILE PICTURE & AVATAR
          // -------------------------------------------------------------
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = theme.background,
            border = BorderStroke(1.dp, theme.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = if (isFr) "Photo de Profil & Avatar" else "Profile Picture & Avatar",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )

              // Avatar Display
              Box(
                modifier = Modifier
                  .size(68.dp)
                  .clip(CircleShape)
                  .background(theme.accent.copy(alpha = 0.15f))
                  .border(BorderStroke(2.dp, theme.accent), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                if (photoUri.isNotBlank()) {
                  AsyncImage(
                    model = ImageRequest.Builder(context)
                      .data(photoUri)
                      .crossfade(true)
                      .build(),
                    contentDescription = "Photo contact",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(36.dp)
                  )
                }
              }

              // Photo Actions
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(32.dp).testTag("btn_pick_contact_photo")
                ) {
                  Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (photoUri.isBlank()) (if (isFr) "Choisir photo" else "Pick photo")
                    else (if (isFr) "Changer photo" else "Change photo"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }

                if (photoUri.isNotBlank()) {
                  OutlinedButton(
                    onClick = { photoUri = "" },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isFr) "Retirer" else "Remove", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                  }
                }
              }
            }
          }

          // -------------------------------------------------------------
          // CATEGORY CHIPS
          // -------------------------------------------------------------
          Text(if (isFr) "Catégorie" else "Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
          val categories = listOf(
            "MEDICAL" to (if (isFr) "🩺 Médical" else "🩺 Medical"),
            "CLINIC" to (if (isFr) "🏥 Clinique" else "🏥 Clinic"),
            "PHARMACY" to (if (isFr) "💊 Pharmacie" else "💊 Pharmacy"),
            "FAMILY" to (if (isFr) "👨‍👩‍👦 Famille" else "👨‍👩‍👦 Family"),
            "INSURANCE" to (if (isFr) "🛡️ Assurance" else "🛡️ Insurance")
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { (catKey, catLabel) ->
              FilterChip(
                selected = category == catKey,
                onClick = { category = catKey },
                label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = theme.accent,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.height(30.dp)
              )
            }
          }

          // -------------------------------------------------------------
          // FULL NAME WITH AUTO-SUGGEST (Requirement 5)
          // -------------------------------------------------------------
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if (isFr) "Nom & Prénom / Établissement *" else "Full Name / Establishment *") },
            placeholder = { Text(if (isFr) "Ex: Dr. Anate Brauer, Alex Embryologiste" else "Ex: Dr. Anate Brauer") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("input_contact_name")
          )

          if (filteredNameSuggestions.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
              Text("💡", fontSize = 11.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isFr) "Suggestions :" else "Suggestions:", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = theme.mutedText)
              Spacer(modifier = Modifier.width(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filteredNameSuggestions) { sug ->
                  AssistChip(
                    onClick = {
                      name = sug
                      val matched = existingProviders.firstOrNull { it.name.equals(sug, ignoreCase = true) }
                      if (matched != null) {
                        if (providerType.isBlank()) providerType = matched.providerType
                        if (affiliation.isBlank()) affiliation = matched.affiliation
                        if (phone.isBlank()) phone = matched.phone
                        if (email.isBlank()) email = matched.email
                        if (address.isBlank()) address = matched.address
                        category = matched.category
                      } else if (sug.contains("Brauer")) {
                        if (providerType.isBlank()) providerType = "Médecin Référent PMA"
                        if (affiliation.isBlank()) affiliation = "RMA of New York — East Side"
                        if (phone.isBlank()) phone = "212-756-5777"
                        if (email.isBlank()) email = "abrauer@rmany.com"
                        category = "MEDICAL"
                      } else if (sug.contains("Alex")) {
                        if (providerType.isBlank()) providerType = "Embryologiste"
                        if (affiliation.isBlank()) affiliation = "RMA of New York — Labo FIV"
                        if (phone.isBlank()) phone = "212-756-5777"
                        category = "MEDICAL"
                      } else if (sug.contains("Mandell")) {
                        if (providerType.isBlank()) providerType = "Pharmacie Spécialisée FIV"
                        if (phone.isBlank()) phone = "877-252-0553"
                        category = "PHARMACY"
                      }
                    },
                    label = { Text(sug, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp) },
                    modifier = Modifier.height(26.dp)
                  )
                }
              }
            }
          }

          // -------------------------------------------------------------
          // ROLE / SPECIALTY WITH AUTO-SUGGEST
          // -------------------------------------------------------------
          OutlinedTextField(
            value = providerType,
            onValueChange = { providerType = it },
            label = { Text(if (isFr) "Rôle / Spécialité *" else "Role / Specialty *") },
            placeholder = { Text(if (isFr) "Ex: Médecin Référent PMA, Embryologiste" else "Ex: Fertility Specialist") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("input_contact_role")
          )

          LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(roleSuggestions) { roleSug ->
              AssistChip(
                onClick = { providerType = roleSug },
                label = { Text(roleSug, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp) },
                modifier = Modifier.height(24.dp)
              )
            }
          }

          // -------------------------------------------------------------
          // AFFILIATION / CLINIC WITH AUTO-SUGGEST
          // -------------------------------------------------------------
          OutlinedTextField(
            value = affiliation,
            onValueChange = { affiliation = it },
            label = { Text(if (isFr) "Affiliation / Clinique / Service" else "Affiliation / Clinic / Hospital") },
            placeholder = { Text("Ex: RMA of New York — East Side") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("input_contact_affiliation")
          )

          if (filteredAffiliationSuggestions.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              items(filteredAffiliationSuggestions) { affSug ->
                AssistChip(
                  onClick = { affiliation = affSug },
                  label = { Text(affSug, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp) },
                  modifier = Modifier.height(24.dp)
                )
              }
            }
          }

          // -------------------------------------------------------------
          // PRIMARY PHONE
          // -------------------------------------------------------------
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(if (isFr) "Numéro de téléphone principal *" else "Primary Phone Number *") },
            placeholder = { Text("Ex: 212-756-5777") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("input_contact_phone")
          )

          // -------------------------------------------------------------
          // EMERGENCY / DIRECT PHONE
          // -------------------------------------------------------------
          OutlinedTextField(
            value = emergencyContact,
            onValueChange = { emergencyContact = it },
            label = { Text(if (isFr) "Ligne directe / Urgence 24h" else "Direct Line / 24h Emergency") },
            placeholder = { Text("Ex: 212-756-5777 ou 911") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          // -------------------------------------------------------------
          // EMAIL
          // -------------------------------------------------------------
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(if (isFr) "Adresse email" else "Email Address") },
            placeholder = { Text("Ex: contact@rmany.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          // -------------------------------------------------------------
          // PHYSICAL ADDRESS
          // -------------------------------------------------------------
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(if (isFr) "Adresse physique / Cabinet" else "Physical Address / Office") },
            placeholder = { Text("Ex: 635 Madison Ave, 10th Floor, NY 10022") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          // -------------------------------------------------------------
          // PORTAL OR WEBSITE URL
          // -------------------------------------------------------------
          OutlinedTextField(
            value = portalLink,
            onValueChange = { portalLink = it },
            label = { Text(if (isFr) "Lien portail patient / Site web" else "Patient Portal / Website URL") },
            placeholder = { Text("Ex: https://www.rmany.com") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = theme.mutedText, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          // -------------------------------------------------------------
          // CLINICAL NOTES / OPERATING HOURS
          // -------------------------------------------------------------
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(if (isFr) "Notes / Consignes / Horaires de garde" else "Notes / Instructions / Office Hours") },
            placeholder = { Text(if (isFr) "Ex: Prises de sang et échos le matin 7h-9h30" else "Ex: Call before 3 PM for blood test results") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          // -------------------------------------------------------------
          // PRIORITY EMERGENCY SPEED DIAL TOGGLE
          // -------------------------------------------------------------
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isEmergency) Color(0xFFFEF2F2) else theme.background,
            border = BorderStroke(1.dp, if (isEmergency) Color(0xFFFCA5A5) else theme.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (isFr) "🚨 Contact d'Urgence Prioritaire" else "🚨 Priority Emergency Speed Dial",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (isEmergency) Color(0xFF991B1B) else theme.primaryText
                )
                Text(
                  text = if (isFr) "Affiché dans le bandeau d'appel rapide 24/7 en haut du répertoire" else "Featured in the 24/7 speed dial banner at top of phone book",
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp,
                  color = theme.mutedText
                )
              }
              Switch(
                checked = isEmergency,
                onCheckedChange = { isEmergency = it },
                modifier = Modifier.testTag("switch_contact_is_emergency")
              )
            }
          }
        }

        // -------------------------------------------------------------
        // FIXED-HEIGHT FOOTER ACTION BAR (Requirement 1)
        // Pinned at the bottom, outside the scrollable form, 100% visible
        // -------------------------------------------------------------
        HorizontalDivider(color = theme.border, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).fillMaxHeight()
          ) {
            Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = {
              if (name.isNotBlank() && phone.isNotBlank()) {
                val updated = SyncedCareProvider(
                  id = contactId,
                  providerType = providerType.ifBlank { "Contact" },
                  name = name.trim(),
                  affiliation = affiliation.trim(),
                  phone = phone.trim(),
                  emergencyContact = emergencyContact.ifBlank { phone }.trim(),
                  address = address.trim(),
                  portalLink = portalLink.trim(),
                  email = email.trim(),
                  photoUri = photoUri.trim(),
                  notes = notes.trim(),
                  isEmergency = isEmergency,
                  category = category
                )
                onSave(updated)
              } else {
                Toast.makeText(
                  context,
                  if (isFr) "Veuillez renseigner le nom et le numéro de téléphone" else "Please enter name and phone number",
                  Toast.LENGTH_SHORT
                ).show()
              }
            },
            enabled = name.isNotBlank() && phone.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("btn_save_phone_book_contact")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isEdit) (if (isFr) "Enregistrer" else "Save") else (if (isFr) "Ajouter" else "Add"),
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
