package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudSyncSummary
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSyncExactTimestamp(epochMillis: Long?, language: AppLanguage): String {
  if (epochMillis == null) {
    return if (language == AppLanguage.FR) "En attente de synchronisation..." else "Awaiting initial sync..."
  }
  val locale = if (language == AppLanguage.FR) Locale.FRENCH else Locale.ENGLISH
  val fmt = SimpleDateFormat("dd MMM yyyy • HH:mm:ss", locale)
  return fmt.format(Date(epochMillis))
}

fun formatSyncShortTime(epochMillis: Long?): String {
  if (epochMillis == null) return "--:--:--"
  val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
  return fmt.format(Date(epochMillis))
}

private fun formatRelativeSyncAge(
  lastSyncedEpochMillis: Long?,
  nowEpochMillis: Long,
  language: AppLanguage
): String {
  if (lastSyncedEpochMillis == null) {
    return if (language == AppLanguage.FR) "Aucune réponse serveur" else "Not yet confirmed"
  }
  val diffSec = ((nowEpochMillis - lastSyncedEpochMillis) / 1000L).coerceAtLeast(0L)
  return if (language == AppLanguage.FR) {
    when {
      diffSec < 5L -> "À l'instant"
      diffSec < 60L -> "Il y a ${diffSec}s"
      diffSec < 3600L -> {
        val m = diffSec / 60L
        val s = diffSec % 60L
        "Il y a ${m}m ${s}s"
      }
      else -> "Il y a ${diffSec / 3600L}h"
    }
  } else {
    when {
      diffSec < 5L -> "Just now"
      diffSec < 60L -> "${diffSec}s ago"
      diffSec < 3600L -> {
        val m = diffSec / 60L
        val s = diffSec % 60L
        "${m}m ${s}s ago"
      }
      else -> "${diffSec / 3600L}h ago"
    }
  }
}

@Composable
fun SyncStatusDashboardCard(
  userEmail: String,
  syncSummary: CloudSyncSummary,
  onInitiateSync: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage,
  modifier: Modifier = Modifier
) {
  var nowEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(syncSummary.lastSyncedEpochMillis) {
    while (true) {
      nowEpochMillis = System.currentTimeMillis()
      delay(1000L)
    }
  }

  val hasError = syncSummary.syncError != null
  val isSyncing = syncSummary.isSyncing

  val statusColor by animateColorAsState(
    targetValue = when {
      hasError -> Color(0xFFDC2626)
      isSyncing -> theme.accent
      syncSummary.lastSyncedEpochMillis != null -> Color(0xFF16A34A)
      else -> theme.mutedText
    },
    animationSpec = tween(durationMillis = 250),
    label = "syncStatusColor"
  )

  val statusBgColor by animateColorAsState(
    targetValue = when {
      hasError -> Color(0xFFFEF2F2)
      isSyncing -> theme.accent.copy(alpha = 0.14f)
      syncSummary.lastSyncedEpochMillis != null -> Color(0xFFF0FDF4)
      else -> theme.background
    },
    animationSpec = tween(durationMillis = 250),
    label = "syncStatusBgColor"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "syncBeacon")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.24f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("sync_status_dashboard"),
    shape = RoundedCornerShape(22.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, if (hasError) Color(0xFFFCA5A5) else theme.border)
  ) {
    Column {
      // Top Accent Header Band
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors = listOf(
                theme.accent.copy(alpha = 0.14f),
                theme.cardSurface
              )
            )
          )
          .padding(horizontal = 16.dp, vertical = 14.dp)
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
            Box(
              modifier = Modifier.size(42.dp),
              contentAlignment = Alignment.Center
            ) {
              if (!hasError && (isSyncing || syncSummary.lastSyncedEpochMillis != null)) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .scale(if (isSyncing) pulseScale else 1.06f)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.16f))
                )
              }
              Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = statusBgColor,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (isSyncing) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(16.dp),
                      strokeWidth = 2.dp,
                      color = statusColor
                    )
                  } else {
                    Icon(
                      imageVector = if (hasError) Icons.Default.CloudOff else Icons.Default.CloudDone,
                      contentDescription = "Sync Status",
                      tint = statusColor,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = "Sync Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
              Text(
                text = userEmail,
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(100.dp),
            color = statusBgColor,
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
          ) {
            Text(
              text = when {
                hasError -> if (language == AppLanguage.FR) "Erreur" else "Issue"
                isSyncing -> if (language == AppLanguage.FR) "Sync..." else "Syncing"
                syncSummary.lastSyncedEpochMillis != null -> if (language == AppLanguage.FR) "À jour" else "Synced"
                else -> if (language == AppLanguage.FR) "En attente" else "Pending"
              },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = statusColor
            )
          }
        }
      }

      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Hero Timestamp Confirmation Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = theme.background,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = null,
                  tint = theme.accent,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (language == AppLanguage.FR) "Dernière synchro Firestore" else "Last Firestore Sync",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = theme.mutedText
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = theme.accent.copy(alpha = 0.12f)
              ) {
                Text(
                  text = if (syncSummary.lastSyncTrigger == "MANUAL") {
                    if (language == AppLanguage.FR) "Fetch Cloud" else "Cloud Fetch"
                  } else {
                    if (language == AppLanguage.FR) "Flux Temps Réel" else "Live Stream"
                  },
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = theme.accent
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = formatSyncExactTimestamp(syncSummary.lastSyncedEpochMillis, language),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText,
              modifier = Modifier.testTag("last_sync_time_text")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = formatRelativeSyncAge(syncSummary.lastSyncedEpochMillis, nowEpochMillis, language),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = if (language == AppLanguage.FR) {
                  "${syncSummary.totalSyncedRecords} éléments dans le Cloud"
                } else {
                  "${syncSummary.totalSyncedRecords} records in Cloud"
                },
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText
              )
            }
          }
        }

        // Collection Breakdown Strip
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SyncCollectionBadge(
            icon = Icons.Default.TaskAlt,
            count = syncSummary.tasksCount,
            label = if (language == AppLanguage.FR) "Tâches" else "Tasks",
            theme = theme,
            modifier = Modifier.weight(1f)
          )
          SyncCollectionBadge(
            icon = Icons.Default.EventNote,
            count = syncSummary.appointmentsCount,
            label = if (language == AppLanguage.FR) "Visites" else "Visits",
            theme = theme,
            modifier = Modifier.weight(1f)
          )
          SyncCollectionBadge(
            icon = Icons.Default.Notes,
            count = syncSummary.notesCount + syncSummary.voiceMemosCount,
            label = "Notes",
            theme = theme,
            modifier = Modifier.weight(1f)
          )
          SyncCollectionBadge(
            icon = Icons.Default.FactCheck,
            count = syncSummary.checklistsCount +
              syncSummary.documentsCount +
              syncSummary.protocolStepsCount +
              syncSummary.walletCardsCount +
              syncSummary.medRemindersCount +
              syncSummary.healthHubRecordsCount,
            label = if (language == AppLanguage.FR) "Suivi" else "Records",
            theme = theme,
            modifier = Modifier.weight(1f)
          )
        }

        // Error Message Callout if present
        AnimatedVisibility(visible = hasError) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
          ) {
            Text(
              text = syncSummary.syncError ?: "",
              modifier = Modifier.padding(10.dp),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFB91C1C)
            )
          }
        }

        // Manual Initiate Sync Button
        Button(
          onClick = onInitiateSync,
          enabled = !isSyncing,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("initiate_sync_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = theme.accent,
            disabledContainerColor = theme.accent.copy(alpha = 0.55f)
          ),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
          if (isSyncing) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (language == AppLanguage.FR) "Récupération Cloud..." else "Fetching from Cloud...",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          } else {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = "Initiate Sync",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Initiate Sync",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SyncCollectionBadge(
  icon: ImageVector,
  count: Int,
  label: String,
  theme: ThemeColors,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = theme.background,
    border = BorderStroke(0.8.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = theme.accent,
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = theme.primaryText
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        color = theme.mutedText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun SyncStatusDashboardDialog(
  userEmail: String,
  syncSummary: CloudSyncSummary,
  onInitiateSync: () -> Unit,
  onDismiss: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = theme.cardSurface,
    text = {
      SyncStatusDashboardCard(
        userEmail = userEmail,
        syncSummary = syncSummary,
        onInitiateSync = onInitiateSync,
        theme = theme,
        language = language
      )
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("close_sync_status_dialog_button")
      ) {
        Text(
          text = if (language == AppLanguage.FR) "Fermer" else "Close",
          color = theme.accent,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  )
}
