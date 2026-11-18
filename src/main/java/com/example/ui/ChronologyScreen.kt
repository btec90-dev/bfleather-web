package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PhaseSection
import com.example.data.PmaRepository
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors

@Composable
fun ChronologyScreen(theme: ThemeColors, language: AppLanguage) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(theme.background)
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp) // Shifted higher
  ) {
    item {
      Column {
        Text(
          text = if (language == AppLanguage.FR) "Chronologie du Parcours Médical" else "Medical Journey Timeline",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          color = theme.primaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (language == AppLanguage.FR) "De la stimulation ovarienne au suivi obstétrical" else "From ovarian stimulation to obstetric care",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      }
    }

    items(PmaRepository.phases) { phase ->
      PhaseCard(phase = phase, theme = theme)
    }
  }
}

@Composable
private fun PhaseCard(phase: PhaseSection, theme: ThemeColors) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = theme.accent.copy(alpha = 0.12f),
            border = BorderStroke(0.5.dp, theme.accent.copy(alpha = 0.3f))
          ) {
            Text(
              text = "Phase ${phase.phaseNumber}",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = theme.primaryText
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = phase.dateRange,
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedText
          )
        }

        Surface(
          shape = RoundedCornerShape(100.dp),
          color = when (phase.status) {
            "Complétée" -> Color(0xFFF0FDF4)
            "En cours" -> Color(0xFFEFF6FF)
            else -> Color(0xFFF8FAFC)
          },
          border = BorderStroke(
            0.5.dp,
            when (phase.status) {
              "Complétée" -> Color(0xFFDCFCE7)
              "En cours" -> Color(0xFFDBEAFE)
              else -> Color(0xFFE2E8F0)
            }
          )
        ) {
          Text(
            text = phase.status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = when (phase.status) {
              "Complétée" -> Color(0xFF16A34A)
              "En cours" -> Color(0xFF2563EB)
              else -> theme.mutedText
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = phase.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = theme.primaryText
      )

      phase.yieldBadge?.let { yieldText ->
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = theme.background,
          border = BorderStroke(0.5.dp, theme.border),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = yieldText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = theme.primaryText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = phase.description,
        style = MaterialTheme.typography.bodySmall,
        color = theme.mutedText,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(14.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(theme.border)
      )
      Spacer(modifier = Modifier.height(14.dp))

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        phase.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
          ) {
            Surface(
              shape = CircleShape,
              color = if (item.isCompleted) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
              modifier = Modifier.size(18.dp).padding(top = 2.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (item.isCompleted) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                } else {
                  Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(8.dp))
                }
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = theme.primaryText)
                Text(text = item.date, style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(text = item.details, style = MaterialTheme.typography.bodySmall, color = theme.mutedText, lineHeight = 17.sp)
            }
          }
        }
      }
    }
  }
}
