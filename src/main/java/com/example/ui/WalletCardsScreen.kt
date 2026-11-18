package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors

data class QuickAccessCard(
  val id: String,
  val title: String,
  val category: String, // "Digital ID", "Credit & Debit", "Travel Companion", "Insurance & Health", "Gift Cards & Rewards"
  val numberOrId: String,
  val details: String,
  val expiryDate: String,
  val hasRecto: Boolean = true,
  val hasVerso: Boolean = true
)

@Composable
fun WalletCardsScreen(
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  var cards by remember {
    mutableStateOf(
      listOf(
        QuickAccessCard("c1", "Carte d'Identité / ID", "Digital ID", "CNI FR 987654321", "Préfet de Police • Paris", "10/32", true, true),
        QuickAccessCard("c2", "Permis de Conduire", "Digital ID", "DL 49205819284", "Catégorie B • Validité permanente", "11/35", true, true),
        QuickAccessCard("c3", "Carte Bancaire Visa Premier", "Credit & Debit", "•••• •••• •••• 4829", "Solde actif • Plafond 5000€", "08/29", true, true),
        QuickAccessCard("c4", "Passeport International", "Travel Companion", "PS 782910293", "Visa États-Unis & Schengen", "05/34", true, true),
        QuickAccessCard("c5", "Carte Mutuelle / Santé Vitale", "Insurance & Health", "SECU 29405789123", "Régime Général • Tiers payant", "12/28", true, true),
        QuickAccessCard("c6", "Carte Cadeau & Fidélité Sephora", "Gift Cards & Rewards", "GIFT 88291029", "Solde : 75.00 € • Code barre actif", "12/26", true, true)
      )
    )
  }

  var selectedCategory by remember { mutableStateOf("All") }
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedCardForZoom by remember { mutableStateOf<QuickAccessCard?>(null) }
  var isVersoView by remember { mutableStateOf(false) }

  var titleInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("Digital ID") }
  var numberInput by remember { mutableStateOf("") }
  var detailsInput by remember { mutableStateOf("") }
  var expiryInput by remember { mutableStateOf("") }
  var attachedRecto by remember { mutableStateOf(false) }
  var attachedVerso by remember { mutableStateOf(false) }

  val categories = listOf(
    "All" to (if (language == AppLanguage.FR) "Tous" else "All"),
    "Digital ID" to (if (language == AppLanguage.FR) "Identité & ID" else "Digital ID"),
    "Credit & Debit" to (if (language == AppLanguage.FR) "Cartes Bancaires" else "Credit & Debit"),
    "Travel Companion" to (if (language == AppLanguage.FR) "Voyage & Passeport" else "Travel Companion"),
    "Insurance & Health" to (if (language == AppLanguage.FR) "Assurance & Santé" else "Insurance & Health"),
    "Gift Cards & Rewards" to (if (language == AppLanguage.FR) "Cadeaux & Fidélité" else "Gift Cards & Rewards")
  )

  val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
    if (bitmap != null) {
      attachedRecto = true
      Toast.makeText(context, "📷 Recto capturé avec succès !", Toast.LENGTH_SHORT).show()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
    if (isGranted) {
      cameraLauncher.launch(null)
    } else {
      Toast.makeText(context, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
    }
  }

  val galleryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      attachedVerso = true
      Toast.makeText(context, "📁 Fichier/verso joint avec succès !", Toast.LENGTH_SHORT).show()
    }
  }

  val filteredCards = if (selectedCategory == "All") {
    cards
  } else {
    cards.filter { it.category == selectedCategory }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header & Quick-Add Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (language == AppLanguage.FR) "Portefeuille & Cartes Rapides" else "Quick Access Wallets",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        Text(
          text = if (language == AppLanguage.FR) "Identité, paiements, voyage & santé" else "ID, payments, travel & health",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      }

      Button(
        onClick = { showAddDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = if (language == AppLanguage.FR) "Ajouter" else "Add", color = Color.White, fontSize = 13.sp)
      }
    }

    // Horizontal Tabbed Category Filter
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(categories) { (key, label) ->
        val isSelected = selectedCategory == key
        val count = if (key == "All") cards.size else cards.count { it.category == key }
        Surface(
          onClick = { selectedCategory = key },
          shape = RoundedCornerShape(20.dp),
          color = if (isSelected) theme.accent else theme.cardSurface,
          border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
          modifier = Modifier.height(36.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "$label ($count)",
              style = MaterialTheme.typography.labelMedium,
              color = if (isSelected) Color.White else theme.primaryText,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
          }
        }
      }
    }

    // Cards Grid / List
    if (filteredCards.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(54.dp), tint = theme.mutedText.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (language == AppLanguage.FR) "Aucune carte dans cette catégorie" else "No cards in this category",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.mutedText
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(filteredCards, key = { it.id }) { card ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .height(170.dp)
              .clickable {
                isVersoView = false
                selectedCardForZoom = card
              },
            shape = RoundedCornerShape(18.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.5.dp, theme.accent.copy(alpha = 0.5f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(theme.cardSurface, theme.accent.copy(alpha = 0.2f))
                  )
                )
                .padding(16.dp)
            ) {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      when (card.category) {
                        "Digital ID" -> Icons.Default.Badge
                        "Credit & Debit" -> Icons.Default.CreditCard
                        "Travel Companion" -> Icons.Default.Flight
                        "Insurance & Health" -> Icons.Default.HealthAndSafety
                        else -> Icons.Default.CardGiftcard
                      },
                      contentDescription = null,
                      tint = theme.accent,
                      modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
                  }
                  Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = theme.accent.copy(alpha = 0.15f)
                  ) {
                    Text(card.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = theme.accent)
                  }
                }

                Text(
                  card.numberOrId,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText,
                  letterSpacing = 1.5.sp
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(card.details, style = MaterialTheme.typography.bodySmall, color = theme.mutedText, maxLines = 1)
                  TextButton(onClick = {
                    isVersoView = false
                    selectedCardForZoom = card
                  }) {
                    Text("Voir / Zoom", color = theme.accent, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Add Card Dialog with Camera & Gallery Attachment
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(if (language == AppLanguage.FR) "Ajouter au Portefeuille" else "Add to Wallet", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text("Titre de la carte (ex. Passeport / Mutuelle)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = categoryInput,
            onValueChange = { categoryInput = it },
            label = { Text("Catégorie (Digital ID, Credit & Debit, Travel...)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = numberInput,
            onValueChange = { numberInput = it },
            label = { Text("Numéro / Code / Identifiant") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = detailsInput,
            onValueChange = { detailsInput = it },
            label = { Text("Détails / Émetteur / Notes") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = expiryInput,
            onValueChange = { expiryInput = it },
            label = { Text("Expiration (MM/AA)") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(4.dp))
          Text("Pièces jointes & Scans (Recto/Verso) :", style = MaterialTheme.typography.labelMedium, color = theme.primaryText, fontWeight = FontWeight.Bold)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                  cameraLauncher.launch(null)
                } else {
                  cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
              },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = if (attachedRecto) Color(0xFF16A34A) else theme.accent)
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (attachedRecto) "Recto ✓" else "Prendre Recto", fontSize = 11.sp, color = Color.White)
            }

            OutlinedButton(
              onClick = {
                galleryPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.weight(1f),
              border = BorderStroke(1.dp, if (attachedVerso) Color(0xFF16A34A) else theme.accent)
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (attachedVerso) Color(0xFF16A34A) else theme.accent)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (attachedVerso) "Verso ✓" else "Joindre Fichier", fontSize = 11.sp, color = theme.primaryText)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (titleInput.isNotBlank() && numberInput.isNotBlank()) {
              val newCard = QuickAccessCard(
                id = "c_${System.currentTimeMillis()}",
                title = titleInput,
                category = categoryInput,
                numberOrId = numberInput,
                details = detailsInput.ifBlank { "Vérifié & Enregistré" },
                expiryDate = expiryInput.ifBlank { "12/30" },
                hasRecto = attachedRecto,
                hasVerso = attachedVerso
              )
              cards = listOf(newCard) + cards
              showAddDialog = false
              titleInput = ""
              numberInput = ""
              detailsInput = ""
              expiryInput = ""
              attachedRecto = false
              attachedVerso = false
              Toast.makeText(context, "Carte ajoutée au portefeuille !", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text("Enregistrer")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Annuler", color = theme.mutedText)
        }
      }
    )
  }

  // Secure Preview & Zoom Modal
  selectedCardForZoom?.let { card ->
    AlertDialog(
      onDismissRequest = { selectedCardForZoom = null },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(card.title, color = theme.primaryText)
          TextButton(onClick = { isVersoView = !isVersoView }) {
            Text(if (isVersoView) "Voir Recto" else "Voir Verso", color = theme.accent, fontWeight = FontWeight.Bold)
          }
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp),
            shape = RoundedCornerShape(16.dp),
            color = theme.cardSurface,
            border = BorderStroke(2.dp, theme.accent)
          ) {
            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = if (isVersoView) "${card.category.uppercase()} — VERSO / DOSSIER" else "${card.category.uppercase()} — RECTO",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.accent,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                SelectionContainer {
                  Text(card.numberOrId, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = theme.primaryText, letterSpacing = 2.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(card.details, style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Expiration : ${card.expiryDate}", style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
              }
            }
          }

          Button(
            onClick = {
              clipboardManager.setText(AnnotatedString(card.numberOrId))
              Toast.makeText(context, "Numéro copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copier le numéro sécurisé", color = Color.White)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedCardForZoom = null }) {
          Text("Fermer")
        }
      }
    )
  }
}
