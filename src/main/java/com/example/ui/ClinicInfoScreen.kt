package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PmaRepository
import com.example.ui.theme.CoolTitaniumTheme
import com.example.ui.theme.SoftChalkTheme

/**
 * Redesigned Clinic Info Screen with:
 * - Palette 5 (COOL TITANIUM)
 * - Clean titanium tint background (#F3F4F6) with pure white cards (#FFFFFF)
 * - Ultra-thin borders (#E5E7EB)
 * - Periwinkle soft accents (#818CF8)
 */
@Composable
fun ClinicInfoScreen() {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(CoolTitaniumTheme.background)
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
    contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp)
  ) {
    item {
      Column {
        Text(
          text = "Établissements & Équipe Référente",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          color = CoolTitaniumTheme.primaryText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Centres cliniques et chirurgicaux accrédités RMA New York",
          style = MaterialTheme.typography.bodyMedium,
          color = CoolTitaniumTheme.mutedText
        )
      }
    }

    // 1. Primary Clinic: RMA East Side
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CoolTitaniumTheme.cardSurface,
        border = BorderStroke(1.dp, CoolTitaniumTheme.border)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = CoolTitaniumTheme.accent.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = CoolTitaniumTheme.accent,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Centre de Suivi & Monitorage",
                  style = MaterialTheme.typography.labelSmall,
                  color = CoolTitaniumTheme.mutedText
                )
                Text(
                  text = "RMA of New York",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = CoolTitaniumTheme.primaryText
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(100.dp),
              color = Color(0xFFEEF2FF),
              border = BorderStroke(0.5.dp, Color(0xFFE0E7FF))
            ) {
              Text(
                text = "East Side",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = CoolTitaniumTheme.accent,
                fontWeight = FontWeight.Medium
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Cabinet dédié aux monitorages sanguins quotidiens, échographies pelviennes de folliculométrie, contrôles endométriaux et consultations du protocole.",
            style = MaterialTheme.typography.bodyMedium,
            color = CoolTitaniumTheme.primaryText,
            lineHeight = 20.sp
          )

          Spacer(modifier = Modifier.height(16.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(CoolTitaniumTheme.border)
          )
          Spacer(modifier = Modifier.height(14.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = Color(0xFFF3F4F6),
              modifier = Modifier.size(24.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = CoolTitaniumTheme.primaryText,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Médecin Référent : ${PmaRepository.doctorName}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = CoolTitaniumTheme.primaryText
            )
          }
        }
      }
    }

    // 2. Surgical Center: Gramercy Surgery Center
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CoolTitaniumTheme.cardSurface,
        border = BorderStroke(1.dp, CoolTitaniumTheme.border)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = Color(0xFFF0FDF4),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Plateau Chirurgical Ambulatoire",
                  style = MaterialTheme.typography.labelSmall,
                  color = CoolTitaniumTheme.mutedText
                )
                Text(
                  text = "Gramercy Surgery Center",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = CoolTitaniumTheme.primaryText
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(100.dp),
              color = Color(0xFFF0FDF4),
              border = BorderStroke(0.5.dp, Color(0xFFDCFCE7))
            ) {
              Text(
                text = "Accrédité",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF16A34A),
                fontWeight = FontWeight.Medium
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = CoolTitaniumTheme.mutedText,
              modifier = Modifier.size(16.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "380 2nd Avenue, Suite 1000, New York, NY",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = CoolTitaniumTheme.primaryText
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Centre d'excellence ambulatoire pour les actes interventionnels : ponction ovocytaire (OPU), hystéroscopie opératoire et transferts sous contrôle écho-guidé.",
            style = MaterialTheme.typography.bodySmall,
            color = CoolTitaniumTheme.mutedText,
            lineHeight = 18.sp
          )
        }
      }
    }

    // 3. Coordination Clinique Note
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Permanence & Coordination",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = SoftChalkTheme.primaryText
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Pour toute question urgente relative aux horaires de trigger shot ou à l'introduction de la progestérone à 120h exactes, contacter la ligne directe des infirmières coordinatrices du portail patient RMA.",
            style = MaterialTheme.typography.bodySmall,
            color = SoftChalkTheme.mutedText,
            lineHeight = 18.sp
          )
        }
      }
    }
  }
}
