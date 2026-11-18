package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Rolling 3-Slot Live Countdown Queue displayed directly below the Digital Wallet.
 * Dynamically removes expired milestones when countdown hits 0 and shifts slots #2/#3 up.
 */
@Composable
fun RollingCountdownQueueSection(
    lang: AppLanguage,
    protocolSteps: List<SyncedProtocolStep>,
    appointments: List<SyncedAppointmentItem>,
    tasks: List<TaskItem>,
    onSlotClick: (RollingCountdownItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isFrench = lang == AppLanguage.FR
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val topThreeQueue = remember(protocolSteps, appointments, tasks, nowMillis) {
        DateSortUtils.buildRollingQueue(
            protocolSteps = protocolSteps,
            appointments = appointments,
            tasks = tasks,
            nowMillis = nowMillis
        ).take(3)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rolling_countdown_queue"),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = Strings.t("rolling_queue_title", lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            }

            if (topThreeQueue.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.background,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = if (isFrench) {
                            "Tous les jalons programmés sont complétés ou passés. Ajoutez un nouveau rendez-vous pour démarrer le compte à rebours."
                        } else {
                            "All scheduled milestones are completed or passed. Add a new visit to start a countdown."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                topThreeQueue.forEachIndexed { idx, item ->
                    val slotNumber = idx + 1
                    val isPrimary = slotNumber == 1
                    val seg = DateSortUtils.computeCountdownSegments(item.targetEpochMillis, nowMillis)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isPrimary) colors.accent.copy(alpha = 0.08f) else colors.background,
                        border = BorderStroke(
                            width = if (isPrimary) 1.5.dp else 1.dp,
                            color = if (isPrimary) colors.accent.copy(alpha = 0.6f) else colors.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("countdown_slot_$slotNumber")
                            .clickable { onSlotClick(item) }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPrimary) colors.accent else colors.mutedText.copy(alpha = 0.22f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$slotNumber",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPrimary) Color.White else colors.primaryText
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.dateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isPrimary) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.accent
                                    ) {
                                        Text(
                                            text = Strings.t("next_badge", lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val units = listOf(
                                    seg.days to Strings.t("countdown_days", lang),
                                    seg.hours to Strings.t("countdown_hrs", lang),
                                    seg.minutes to Strings.t("countdown_min", lang),
                                    seg.seconds to Strings.t("countdown_sec", lang)
                                )
                                units.forEachIndexed { unitIndex, (value, unitLabel) ->
                                    val emphasizeDays = isPrimary && unitIndex == 0
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (emphasizeDays) colors.accent else colors.cardSurface,
                                        border = if (emphasizeDays) null else BorderStroke(1.dp, colors.border),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%02d", value),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (emphasizeDays) Color.White else colors.primaryText,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = unitLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (emphasizeDays) Color.White.copy(alpha = 0.9f) else colors.mutedText,
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
    }
}
