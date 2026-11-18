package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class ContactItem(
  val id: String,
  val name: String,
  val role: String, // "Gynécologue", "Pédiatre", "Pharmacie", "Hôpital", "Urgence"
  val phone: String,
  val address: String
)

@Composable
fun ContactsDirectoryScreen(
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  var contacts by remember {
    mutableStateOf(
      listOf(
        ContactItem("c1", "Dr. Laurent (OB/GYN)", "Gynécologue", "+33 1 42 34 56 78", "Maternal Health Center, Paris"),
        ContactItem("c2", "Maternité / Clinique Port-Royal", "Hôpital", "+33 1 58 41 00 00", "Boulevard de Port-Royal, Paris"),
        ContactItem("c3", "Pharmacie Centrale de Garde", "Pharmacie", "+33 1 43 26 00 12", "Rue Saint-Jacques, Paris"),
        ContactItem("c4", "SAMU / Urgences Obstétricales", "Urgence", "15 / 911", "24h/24 7j/7")
      )
    )
  }

  var showAddDialog by remember { mutableStateOf(false) }
  var nameInput by remember { mutableStateOf("") }
  var roleInput by remember { mutableStateOf("Gynécologue") }
  var phoneInput by remember { mutableStateOf("") }
  var addressInput by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header & Add Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (language == AppLanguage.FR) "Contacts Importants" else "Important Contacts",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        Text(
          text = if (language == AppLanguage.FR) "Annuaire médical & urgences" else "Medical directory & emergency",
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
        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = if (language == AppLanguage.FR) "Ajouter" else "Add", color = Color.White, fontSize = 13.sp)
      }
    }

    // Contacts List
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      items(contacts, key = { it.id }) { contact ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
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
                Surface(
                  shape = CircleShape,
                  color = theme.accent.copy(alpha = 0.16f),
                  modifier = Modifier.size(42.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      when (contact.role) {
                        "Gynécologue" -> Icons.Default.MedicalServices
                        "Pharmacie" -> Icons.Default.LocalPharmacy
                        "Urgence" -> Icons.Default.Emergency
                        else -> Icons.Default.LocalHospital
                      },
                      contentDescription = null,
                      tint = theme.accent,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
                  Text("${contact.role} • ${contact.phone}", style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(contact.address, style = MaterialTheme.typography.bodySmall, color = theme.primaryText.copy(alpha = 0.8f))

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: Call, Message, Copy
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone.filter { it.isDigit() || it == '+' }}"))
                    context.startActivity(intent)
                  } catch (_: Exception) {
                    Toast.makeText(context, "Appel impossible", Toast.LENGTH_SHORT).show()
                  }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
              ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Appeler", style = MaterialTheme.typography.labelSmall, color = Color.White)
              }

              OutlinedButton(
                onClick = {
                  clipboardManager.setText(AnnotatedString("${contact.name} - ${contact.phone} - ${contact.address}"))
                  Toast.makeText(context, "Contact copié !", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, theme.border),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copier", style = MaterialTheme.typography.labelSmall, color = theme.primaryText)
              }
            }
          }
        }
      }
    }
  }

  // Add Contact Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(if (language == AppLanguage.FR) "Ajouter un contact" else "Add Contact", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Nom du praticien / structure") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = roleInput,
            onValueChange = { roleInput = it },
            label = { Text("Rôle (Gynécologue, Pédiatre...)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = phoneInput,
            onValueChange = { phoneInput = it },
            label = { Text("Téléphone") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = addressInput,
            onValueChange = { addressInput = it },
            label = { Text("Adresse / Clinique") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (nameInput.isNotBlank() && phoneInput.isNotBlank()) {
              val newC = ContactItem(
                id = "c_${System.currentTimeMillis()}",
                name = nameInput,
                role = roleInput,
                phone = phoneInput,
                address = addressInput
              )
              contacts = contacts + newC
              showAddDialog = false
              nameInput = ""
              phoneInput = ""
              addressInput = ""
              Toast.makeText(context, "Contact enregistré !", Toast.LENGTH_SHORT).show()
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
}
