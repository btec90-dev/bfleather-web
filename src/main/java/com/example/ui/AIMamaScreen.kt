package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiAssistantService
import com.example.data.PmaRepository
import com.example.data.SyncedCareProvider
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Chat & Care Provider Models
// -------------------------------------------------------------
data class ChatMessage(
  val id: String,
  val sender: String, // "mama_ai" or "user"
  val text: String,
  val timestamp: String,
  val isEmergency: Boolean = false,
  val hasProviderAction: Boolean = false
)

data class CareProvider(
  val id: String,
  var providerType: String, // "OB/GYN", "Pédiatre", "Doula", "Sage-femme", "Spécialiste"
  var name: String,
  var affiliation: String,
  var phone: String,
  var emergencyContact: String,
  var address: String,
  var portalLink: String = ""
)

@Composable
fun AIMamaScreen(
  careProviders: List<SyncedCareProvider>,
  onSaveProvider: (SyncedCareProvider) -> Unit,
  onDeleteProvider: (String) -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val resolvedDoctorName = remember(careProviders) {
    careProviders.firstOrNull {
      it.providerType.contains("OB", ignoreCase = true) ||
        it.providerType.contains("Fert", ignoreCase = true) ||
        it.name.isNotBlank()
    }?.name?.takeIf { it.isNotBlank() && !it.startsWith("[") } ?: PmaRepository.doctorName
  }
  val resolvedHospitalName = remember(careProviders) {
    careProviders.firstOrNull { it.affiliation.isNotBlank() && !it.affiliation.startsWith("[") }
      ?.affiliation ?: PmaRepository.clinicName
  }

  var userInput by remember { mutableStateOf("") }
  var showInlineAddDoctorCard by remember { mutableStateOf(false) }
  var showTopEmergencyBanner by remember { mutableStateOf(false) }
  var showProvidersListDialog by remember { mutableStateOf(false) }

  // Initial AI Mom Greeting grounded in clinical guidelines
  val messages = remember(resolvedDoctorName, resolvedHospitalName, language) {
    mutableStateListOf(
      ChatMessage(
        id = "m1",
        sender = "mama_ai",
        text = if (language == AppLanguage.FR)
          "Bonjour ! Je suis IA Maman, votre accompagnante en santé maternelle. Je m'appuie sur les recommandations certifiées (ACOG, OMS) pour répondre à vos questions sur vos symptômes, votre nutrition et votre suivi avec $resolvedDoctorName à $resolvedHospitalName."
        else
          "Hello! I am AI Mom, your evidence-based maternal care companion. Grounded in certified clinical guidelines (ACOG, WHO), I guide your symptoms, nutrition, and care management with $resolvedDoctorName at $resolvedHospitalName.",
        timestamp = if (language == AppLanguage.FR) "En ligne" else "Online"
      )
    )
  }

  val promptSuggestions = if (language == AppLanguage.FR) {
    listOf(
      "Ajouter un médecin ou une clinique" to "🩺 Ajouter Soignant",
      "Posologie Estrace & recommandations ACOG" to "💊 Protocole Médical",
      "Aliments recommandés et à éviter au T2" to "🥗 Nutrition T2",
      "Tiraillements pelviens : est-ce normal ?" to "🤰 Symptômes",
      "Signes d'alerte et protocole d'urgence" to "🚨 Signes d'Alerte",
      "Préparation valise & post-partum" to "🍼 Post-Partum"
    )
  } else {
    listOf(
      "Add a doctor or clinic" to "🩺 Add Provider",
      "Estrace protocol & ACOG guidance" to "💊 Medical Protocol",
      "Recommended foods & foods to avoid in T2" to "🥗 T2 Nutrition",
      "Mild pelvic twinges: are they normal?" to "🤰 Symptoms",
      "Warning signs & emergency escalation" to "🚨 Warning Signs",
      "Postpartum recovery & infant basics" to "🍼 Postpartum"
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
  ) {
    // -------------------------------------------------------------
    // 1. TOP COMPACT HEADER
    // -------------------------------------------------------------
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = theme.headerFooterBg,
      border = BorderStroke(1.dp, theme.border)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp)
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
              modifier = Modifier.size(38.dp),
              shape = CircleShape,
              color = theme.accent.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, theme.accent)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = "🤰", fontSize = 20.sp)
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = if (language == AppLanguage.FR) "IA Maman • Compagnon Santé" else "AI Mom • Health Companion",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText,
                  fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(100.dp),
                  color = Color(0xFFF0FDF4),
                  border = BorderStroke(0.5.dp, Color(0xFFDCFCE7))
                ) {
                  Text(
                    text = "ACOG/OMS",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Bold
                  )
                }
              }
              Text(
                text = if (language == AppLanguage.FR) "Accompagnement • $resolvedHospitalName" else "Care • $resolvedHospitalName",
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText,
                fontSize = 11.sp
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            // SOS Emergency toggle
            IconButton(
              onClick = { showTopEmergencyBanner = !showTopEmergencyBanner },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Emergency, contentDescription = "Urgence", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }

            // Care Providers Roster button
            IconButton(
              onClick = { showProvidersListDialog = true },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.MedicalServices, contentDescription = "Soignants", tint = theme.accent, modifier = Modifier.size(18.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Compact Quick Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { showInlineAddDoctorCard = !showInlineAddDoctorCard },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (language == AppLanguage.FR) "+ Ajouter Médecin" else "+ Add Doctor",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }

          OutlinedButton(
            onClick = { showProvidersListDialog = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, theme.border),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
          ) {
            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (language == AppLanguage.FR) "Mes Soignants (${careProviders.size})" else "Providers (${careProviders.size})",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }
        }
      }
    }

    // -------------------------------------------------------------
    // 2. TOP EMERGENCY WARNING BANNER (Section 6)
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = showTopEmergencyBanner,
      enter = expandVertically(),
      exit = shrinkVertically()
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "URGENCE MÉDICALE OBSTÉTRICALE 🚨",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF991B1B)
            )
            Text(
              text = if (language == AppLanguage.FR)
                "Saignements, fortes douleurs, troubles visuels ou diminution des mouvements : appelez le 15/911 ou allez à $resolvedHospitalName."
              else
                "Bleeding, severe pain, vision changes, or reduced movement: call 911/15 or go to $resolvedHospitalName.",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 11.sp,
              color = Color(0xFF7F1D1D),
              lineHeight = 14.sp
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = {
              val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
              context.startActivity(dialIntent)
            },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("911 / 15", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
          }
          IconButton(
            onClick = { showTopEmergencyBanner = false },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF991B1B), modifier = Modifier.size(14.dp))
          }
        }
      }
    }

    // -------------------------------------------------------------
    // 3. INLINE COMPACT "ADD PROVIDER / CONTACT" WIDGET (Section 5)
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = showInlineAddDoctorCard,
      enter = expandVertically(),
      exit = shrinkVertically()
    ) {
      CompactAddProviderCard(
        theme = theme,
        onClose = { showInlineAddDoctorCard = false },
        onSave = { newProvider ->
          onSaveProvider(
            SyncedCareProvider(
              id = newProvider.id,
              providerType = newProvider.providerType,
              name = newProvider.name,
              affiliation = newProvider.affiliation,
              phone = newProvider.phone,
              emergencyContact = newProvider.emergencyContact,
              address = newProvider.address,
              portalLink = newProvider.portalLink
            )
          )
          showInlineAddDoctorCard = false
          messages.add(
            ChatMessage(
              id = "ai_provider_${System.currentTimeMillis()}",
              sender = "mama_ai",
              text = "✅ Praticien enregistré :\n• Type : ${newProvider.providerType}\n• Nom : ${newProvider.name}\n• Affiliation : ${newProvider.affiliation}\n• Tél : ${newProvider.phone} | Urgences : ${newProvider.emergencyContact}\n• Adresse : ${newProvider.address}",
              timestamp = "Maintenant"
            )
          )
          Toast.makeText(context, "Soignant enregistré !", Toast.LENGTH_SHORT).show()
        }
      )
    }

    // -------------------------------------------------------------
    // 4. QUICK TOPIC SUGGESTION CHIPS
    // -------------------------------------------------------------
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(promptSuggestions) { (promptText, chipLabel) ->
        Surface(
          shape = RoundedCornerShape(100.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border),
          onClick = {
            if (chipLabel.contains("Ajouter Soignant") || chipLabel.contains("Add Provider")) {
              showInlineAddDoctorCard = true
            }
            sendUserMessage(
              query = promptText,
              messages = messages,
              language = language,
              doctorName = resolvedDoctorName,
              hospitalName = resolvedHospitalName,
              scope = coroutineScope,
              onTriggerEmergency = { showTopEmergencyBanner = true },
              onOpenProviderWidget = { showInlineAddDoctorCard = true }
            )
          }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = theme.accent, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = chipLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
              color = theme.primaryText
            )
          }
        }
      }
    }

    // -------------------------------------------------------------
    // 5. CHAT & COMPACT CLINICAL OVERVIEW (PREVENT EMPTY SCREENS)
    // -------------------------------------------------------------
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(top = 4.dp, bottom = 10.dp)
    ) {
      // Compact 2x2 Clinical Guidance Summary (maximizes screen utilization)
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Repères Cliniques & Suivi Actif (ACOG / OMS)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = theme.accent
              )
              Text(
                text = "Semaine 16 • T2",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = theme.mutedText
              )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // 2x2 Tight Grid
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CompactTile(
                title = "💊 Protocole Actif",
                subtitle = "Estrace 2 mg (AM & PM)",
                modifier = Modifier.weight(1f),
                theme = theme,
                onClick = {
                  sendUserMessage("Posologie Estrace & recommandations ACOG", messages, language, resolvedDoctorName, resolvedHospitalName, coroutineScope, { showTopEmergencyBanner = true }, { showInlineAddDoctorCard = true })
                }
              )
              CompactTile(
                title = "🥗 Nutrition & Eau",
                subtitle = "2L à 2.5L / jour • DHA",
                modifier = Modifier.weight(1f),
                theme = theme,
                onClick = {
                  sendUserMessage("Aliments recommandés et à éviter au T2", messages, language, resolvedDoctorName, resolvedHospitalName, coroutineScope, { showTopEmergencyBanner = true }, { showInlineAddDoctorCard = true })
                }
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CompactTile(
                title = if (language == AppLanguage.FR) "🩺 Soignant Référent" else "🩺 Referring Doctor",
                subtitle = resolvedDoctorName,
                modifier = Modifier.weight(1f),
                theme = theme,
                onClick = { showProvidersListDialog = true }
              )
              CompactTile(
                title = if (language == AppLanguage.FR) "🚨 Urgence & Signes" else "🚨 Urgent Care",
                subtitle = resolvedHospitalName,
                modifier = Modifier.weight(1f),
                theme = theme,
                onClick = { showTopEmergencyBanner = true }
              )
            }
          }
        }
      }

      // Chat Messages List
      items(messages, key = { it.id }) { msg ->
        val isUser = msg.sender == "user"
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
          if (!isUser) {
            Surface(
              modifier = Modifier.size(28.dp),
              shape = CircleShape,
              color = if (msg.isEmergency) Color(0xFFFEE2E2) else theme.accent.copy(alpha = 0.15f),
              border = if (msg.isEmergency) BorderStroke(1.dp, Color(0xFFEF4444)) else null
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = if (msg.isEmergency) "🚨" else "🤰", fontSize = 14.sp)
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(
              topStart = 14.dp,
              topEnd = 14.dp,
              bottomStart = if (isUser) 14.dp else 4.dp,
              bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            color = when {
              isUser -> theme.accent
              msg.isEmergency -> Color(0xFFFEF2F2)
              else -> theme.cardSurface
            },
            border = BorderStroke(
              1.dp,
              when {
                isUser -> theme.accent
                msg.isEmergency -> Color(0xFFFCA5A5)
                else -> theme.border
              }
            )
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              if (msg.isEmergency) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "PROTOCOLE D'URGENCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    fontSize = 10.sp
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
              }

              SelectionContainer {
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodySmall,
                  color = when {
                    isUser -> Color.White
                    msg.isEmergency -> Color(0xFF991B1B)
                    else -> theme.primaryText
                  },
                  lineHeight = 18.sp
                )
              }

              if (msg.hasProviderAction) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = { showInlineAddDoctorCard = true },
                  shape = RoundedCornerShape(6.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Ouvrir Formulaire Médecin", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = Color.White)
                }
              }

              if (msg.isEmergency) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Button(
                    onClick = {
                      val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                      context.startActivity(dialIntent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Appeler 911 / 15", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                  }
                  OutlinedButton(
                    onClick = { showProvidersListDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                    contentPadding = PaddingValues(vertical = 4.dp)
                  ) {
                    Text("Mes Soignants", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFFDC2626))
                  }
                }
              }

              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = msg.timestamp,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                color = if (isUser) Color.White.copy(alpha = 0.8f) else theme.mutedText,
                modifier = Modifier.align(Alignment.End)
              )
            }
          }

          if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              modifier = Modifier.size(28.dp),
              shape = CircleShape,
              color = theme.background,
              border = BorderStroke(1.dp, theme.border)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = "🌸", fontSize = 14.sp)
              }
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // 6. MESSAGE INPUT BAR (TIGHT & RESPONSIVE)
    // -------------------------------------------------------------
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = userInput,
          onValueChange = { userInput = it },
          placeholder = {
            Text(
              if (language == AppLanguage.FR) "Posez une question ou 'Ajouter médecin'..." else "Ask a question or 'Add doctor'...",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 12.sp,
              color = theme.mutedText
            )
          },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.accent,
            unfocusedBorderColor = theme.border
          ),
          maxLines = 2
        )
        Spacer(modifier = Modifier.width(6.dp))
        FilledIconButton(
          onClick = {
            if (userInput.isNotBlank()) {
              sendUserMessage(
                query = userInput,
                messages = messages,
                language = language,
                doctorName = resolvedDoctorName,
                hospitalName = resolvedHospitalName,
                scope = coroutineScope,
                onTriggerEmergency = { showTopEmergencyBanner = true },
                onOpenProviderWidget = { showInlineAddDoctorCard = true }
              )
              userInput = ""
            }
          },
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = theme.accent),
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
        }
      }
    }
  }

  // -------------------------------------------------------------
  // CARE PROVIDERS ROSTER LIST DIALOG
  // -------------------------------------------------------------
  if (showProvidersListDialog) {
    AlertDialog(
      onDismissRequest = { showProvidersListDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = theme.accent)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mes Soignants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          }
          Surface(
            shape = RoundedCornerShape(100.dp),
            color = theme.accent.copy(alpha = 0.15f)
          ) {
            Text(
              text = "${careProviders.size}",
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = theme.accent
            )
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          careProviders.forEach { provider ->
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = theme.background,
              border = BorderStroke(1.dp, theme.border)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = theme.accent.copy(alpha = 0.15f)
                  ) {
                    Text(
                      text = provider.providerType,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp,
                      color = theme.accent
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    // Share
                    IconButton(
                      onClick = {
                        val shareText = "🩺 Fiche Soignant :\nNom : ${provider.name} (${provider.providerType})\nAffiliation : ${provider.affiliation}\nTél : ${provider.phone} | Urgence : ${provider.emergencyContact}\nAdresse : ${provider.address}"
                        val sendIntent = Intent().apply {
                          action = Intent.ACTION_SEND
                          putExtra(Intent.EXTRA_TITLE, "Fiche Soignant")
                          putExtra(Intent.EXTRA_TEXT, shareText)
                          type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Partager"))
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(Icons.Default.Share, contentDescription = "Partager", tint = theme.accent, modifier = Modifier.size(15.dp))
                    }
                    // Delete
                    IconButton(
                      onClick = {
                        onDeleteProvider(provider.id)
                        Toast.makeText(context, "Supprimé", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = theme.mutedText, modifier = Modifier.size(15.dp))
                    }
                  }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = provider.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = provider.affiliation,
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp,
                  color = theme.mutedText
                )
                Text(
                  text = "📞 ${provider.phone} | 🚨 ${provider.emergencyContact}",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.primaryText,
                  fontSize = 11.sp
                )
                if (provider.address.isNotBlank()) {
                  Text(
                    text = "📍 ${provider.address}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = theme.mutedText
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                  onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone.replace(" ", "")}"))
                    context.startActivity(dialIntent)
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                  Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Appeler Praticien", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = Color.White)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showProvidersListDialog = false
            showInlineAddDoctorCard = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Nouveau Soignant", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showProvidersListDialog = false }) {
          Text("Fermer", color = theme.mutedText)
        }
      }
    )
  }
}

// -------------------------------------------------------------
// COMPACT ADD PROVIDER / CONTACT CARD WIDGET (Section 5)
// -------------------------------------------------------------
@Composable
private fun CompactAddProviderCard(
  theme: ThemeColors,
  onClose: () -> Unit,
  onSave: (CareProvider) -> Unit
) {
  val providerTypes = listOf("OB/GYN", "Pediatrician", "Doula", "Midwife", "Specialist")
  var selectedType by remember { mutableStateOf("OB/GYN") }
  var inputName by remember { mutableStateOf("") }
  var inputAffiliation by remember { mutableStateOf("") }
  var inputPhone by remember { mutableStateOf("") }
  var inputEmergency by remember { mutableStateOf("") }
  var inputAddress by remember { mutableStateOf("") }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp),
    shape = RoundedCornerShape(14.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.5.dp, theme.accent)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Add Provider / Contact",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
        }
        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Fermer", tint = theme.mutedText, modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Type Selector Chips
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        items(providerTypes) { pType ->
          val isSelected = selectedType == pType
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) theme.accent else theme.background,
            border = BorderStroke(0.5.dp, if (isSelected) theme.accent else theme.border),
            modifier = Modifier.clickable { selectedType = pType }
          ) {
            Text(
              text = pType,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              color = if (isSelected) Color.White else theme.primaryText,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Doctor / Clinic & Hospital Affiliation
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedTextField(
          value = inputName,
          onValueChange = { inputName = it },
          label = { Text("Doctor / Clinic", fontSize = 11.sp) },
          placeholder = { Text("[Enter Name]", fontSize = 11.sp) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        OutlinedTextField(
          value = inputAffiliation,
          onValueChange = { inputAffiliation = it },
          label = { Text("Hospital Affiliation", fontSize = 11.sp) },
          placeholder = { Text("[Enter Hospital]", fontSize = 11.sp) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Phone & Emergency (Side-by-Side Row to minimize height)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedTextField(
          value = inputPhone,
          onValueChange = { inputPhone = it },
          label = { Text("Phone", fontSize = 11.sp) },
          placeholder = { Text("[Enter Phone]", fontSize = 11.sp) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        OutlinedTextField(
          value = inputEmergency,
          onValueChange = { inputEmergency = it },
          label = { Text("Emergency", fontSize = 11.sp) },
          placeholder = { Text("[Enter Number]", fontSize = 11.sp) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Address
      OutlinedTextField(
        value = inputAddress,
        onValueChange = { inputAddress = it },
        label = { Text("Address", fontSize = 11.sp) },
        placeholder = { Text("[Enter Address]", fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = {
          val nameVal = inputName.trim().ifBlank { PmaRepository.doctorName }
          val affilVal = inputAffiliation.trim().ifBlank { PmaRepository.clinicName }
          val phoneVal = inputPhone.trim().ifBlank { "212-756-5777" }
          val emergVal = inputEmergency.trim().ifBlank { "911" }
          val addrVal = inputAddress.trim().ifBlank { PmaRepository.clinicAddress }

          onSave(
            CareProvider(
              id = "cp_${System.currentTimeMillis()}",
              providerType = selectedType,
              name = nameVal,
              affiliation = affilVal,
              phone = phoneVal,
              emergencyContact = emergVal,
              address = addrVal
            )
          )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        contentPadding = PaddingValues(vertical = 6.dp)
      ) {
        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Provider / Contact", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
  }
}

// -------------------------------------------------------------
// COMPACT GRID TILE HELPER
// -------------------------------------------------------------
@Composable
private fun CompactTile(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  theme: ThemeColors,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = theme.background,
    border = BorderStroke(0.5.dp, theme.border),
    modifier = modifier.clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = theme.primaryText
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        color = theme.mutedText
      )
    }
  }
}

// -------------------------------------------------------------
// AI MOM MESSAGE PROCESSING ENGINE (Firebase AI Logic + Grounded Fallback)
// -------------------------------------------------------------
private fun sendUserMessage(
  query: String,
  messages: MutableList<ChatMessage>,
  language: AppLanguage,
  doctorName: String,
  hospitalName: String,
  scope: CoroutineScope,
  onTriggerEmergency: () -> Unit,
  onOpenProviderWidget: () -> Unit
) {
  val nowStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
  val userMsgId = "u_${System.currentTimeMillis()}"
  messages.add(
    ChatMessage(
      id = userMsgId,
      sender = "user",
      text = query,
      timestamp = nowStr
    )
  )

  val q = query.lowercase().trim()

  // 1. SAFETY & ESCALATION PROTOCOL CHECK
  val isEmergency = q.contains("saignement") || q.contains("bleeding") ||
    q.contains("forte fièvre") || q.contains("high fever") || q.contains("39") || q.contains("40") ||
    q.contains("perte de vision") || q.contains("vision trouble") || q.contains("vision floue") || q.contains("vision change") ||
    q.contains("douleur aiguë") || q.contains("douleur violente") || q.contains("severe pain") ||
    q.contains("bébé ne bouge plus") || q.contains("mouvements diminués") || q.contains("reduced fetal movement") || q.contains("no movement")

  if (isEmergency) {
    onTriggerEmergency()
    val emergencyReply = if (language == AppLanguage.FR)
      "🚨 ALERTE MÉDICALE D'URGENCE :\nLes symptômes rapportés nécessitent une prise en charge médicale immédiate.\n\n• Appelez sans délai le 15, 112 ou 911.\n• Ou rendez-vous immédiatement aux urgences obstétriques de $hospitalName.\n• Contactez la ligne d'urgence de $doctorName.\nNe conduisez pas vous-même."
    else
      "🚨 URGENT EMERGENCY WARNING:\nYour reported symptoms require immediate medical intervention without delay.\n\n• Call emergency services immediately (911 / 112).\n• Or proceed directly to the obstetrics emergency department at $hospitalName.\n• Contact the on-call line of $doctorName.\nDo not drive yourself."

    messages.add(
      ChatMessage(
        id = "ai_${System.currentTimeMillis()}",
        sender = "mama_ai",
        text = emergencyReply,
        timestamp = nowStr,
        isEmergency = true
      )
    )
    return
  }

  // 2. AUTO-SUGGEST & CONTACT MANAGEMENT TRIGGER
  val isAddDoctorRequest = q.contains("ajouter médecin") || q.contains("ajouter soignant") ||
    q.contains("ajouter docteur") || q.contains("ajouter clinique") ||
    q.contains("add doctor") || q.contains("add clinic") || q.contains("nouveau médecin") ||
    q.contains("enregistrer soignant")

  if (isAddDoctorRequest) {
    onOpenProviderWidget()
    val providerReply = if (language == AppLanguage.FR)
      "📋 **Ajout de Soignant Référent** :\nLe formulaire d'ajout rapide est ouvert ci-dessus pour enregistrer votre praticien ou clinique (référent actuel : $doctorName • $hospitalName)."
    else
      "📋 **Add Provider / Contact**:\nThe quick entry card is open above to save your practitioner or hospital (current care team: $doctorName • $hospitalName)."

    messages.add(
      ChatMessage(
        id = "ai_${System.currentTimeMillis()}",
        sender = "mama_ai",
        text = providerReply,
        timestamp = nowStr,
        hasProviderAction = true
      )
    )
    return
  }

  // 3. Stream answer from Firebase AI Logic (gemini-3.8-flash), falling back to grounded ACOG/WHO guidance if offline
  val aiMsgId = "ai_${System.currentTimeMillis()}"
  messages.add(
    ChatMessage(
      id = aiMsgId,
      sender = "mama_ai",
      text = if (language == AppLanguage.FR) "Réflexion en cours..." else "Thinking...",
      timestamp = nowStr
    )
  )

  scope.launch {
    val buffer = StringBuilder()
    var hasStreamed = false
    val isFrench = language == AppLanguage.FR
    val contextStr = "Doctor: $doctorName | Clinic: $hospitalName | Patient: ${PmaRepository.patientName}"
    try {
      AiAssistantService.streamClinicalAnswer(query, contextStr, isFrench).collect { chunk ->
        buffer.append(chunk)
        hasStreamed = true
        val idx = messages.indexOfFirst { it.id == aiMsgId }
        if (idx >= 0) {
          messages[idx] = messages[idx].copy(text = buffer.toString())
        }
      }
    } catch (_: Exception) {
      // Fallback below when offline or App Check is unavailable
    }

    if (!hasStreamed || buffer.isBlank()) {
      val fallback = buildGroundedFallbackReply(q, language, doctorName, hospitalName)
      val idx = messages.indexOfFirst { it.id == aiMsgId }
      if (idx >= 0) {
        messages[idx] = messages[idx].copy(text = fallback)
      }
    }
  }
}

private fun buildGroundedFallbackReply(
  q: String,
  language: AppLanguage,
  doctorName: String,
  hospitalName: String
): String {
  return when {
    q.contains("estrace") || q.contains("œstrog") || q.contains("estrog") -> {
      if (language == AppLanguage.FR)
        "**Protocole Médical (Consensus ACOG / ESHRE)** :\n• **Objectif** : Épaississement trilaminaire et préparation endométriale optimale.\n• **Prise** : 1 comprimé le matin et 1 comprimé le soir avec de l'eau à horaires fixes.\n• Confirmez toute adaptation de posologie avec $doctorName."
      else
        "**Medical Protocol (ACOG / ESHRE Consensus)**:\n• **Goal**: Optimal trilaminar endometrial thickening.\n• **Administration**: 1 tablet AM and 1 tablet PM consistently with water.\n• Always verify dosage specifics with $doctorName."
    }
    q.contains("120") || q.contains("progest") -> {
      if (language == AppLanguage.FR)
        "**Règle de Synchronisation (120h)** :\n• L'imprégnation en progestérone doit débuter exactement 120 heures avant le transfert d'embryon J5.\n• Vérifiez l'heure exacte avec $doctorName ($hospitalName)."
      else
        "**Synchronization Rule (120h)**:\n• Progesterone administration begins exactly 120 hours prior to Day 5 embryo transfer.\n• Validate your schedule with $doctorName ($hospitalName)."
    }
    q.contains("aliment") || q.contains("nutrition") || q.contains("manger") || q.contains("food") || q.contains("régime") -> {
      if (language == AppLanguage.FR)
        "**Nutrition & Hydratation (ACOG / OMS)** :\n• **Clés** : Calcium (1000 mg/j), Fer, Folates, et Oméga-3 DHA.\n• **Eau** : 2 à 2,5 L/jour.\n• **À éviter** : Poissons/viandes crus, fromages non pasteurisés, caféine > 200 mg/j."
      else
        "**Nutrition & Hydration (ACOG / WHO)**:\n• **Key Nutrients**: Calcium (1000 mg/d), Iron, Folate, and Omega-3 DHA.\n• **Hydration**: 2 to 2.5 L/day.\n• **Avoid**: Raw meats/fish, unpasteurized dairy, caffeine > 200 mg/day."
    }
    q.contains("douleur") || q.contains("tiraillement") || q.contains("crampe") || q.contains("pain") || q.contains("twinge") -> {
      if (language == AppLanguage.FR)
        "**Tiraillements Pelviens (ACOG)** :\n• De légers tiraillements sont fréquents.\n• Reposez-vous sur le côté gauche.\n• *Alerte* : Si la douleur devient aiguë ou s'accompagne de saignements, contactez $hospitalName ou $doctorName."
      else
        "**Pelvic Twinges (ACOG)**:\n• Mild pelvic twinges can occur normally.\n• Rest on your left side with support.\n• *Alert*: If sharp, severe, or accompanied by spotting, contact $hospitalName or $doctorName."
    }
    else -> {
      if (language == AppLanguage.FR)
        "D'après les recommandations médicales générales (ACOG/OMS), notez cette observation dans votre journal pour en discuter lors de votre prochain suivi avec $doctorName à $hospitalName."
      else
        "Based on general clinical guidelines (ACOG/WHO), log this observation in your journal to review with $doctorName at $hospitalName."
    }
  }
}
