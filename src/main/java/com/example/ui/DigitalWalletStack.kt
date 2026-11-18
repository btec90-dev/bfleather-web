package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.data.SyncedWalletCard
import com.example.data.WalletCardOcrParser
import com.example.data.WalletImageUtils
import com.example.data.WalletSyncStatus
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class WalletGradientPreset(
  val key: String,
  val labelFr: String,
  val labelEn: String,
  val colors: List<Color>,
  val glowColor: Color
) {
  TEAL(
    key = "teal",
    labelFr = "Turquoise",
    labelEn = "Teal",
    colors = listOf(Color(0xEB0D9488), Color(0xEB0F766E), Color(0xF0115E59)),
    glowColor = Color(0xFF2DD4BF)
  ),
  SUNSET_ORANGE(
    key = "sunset_orange",
    labelFr = "Soleil",
    labelEn = "Sunset",
    colors = listOf(Color(0xEBF97316), Color(0xEBEA580C), Color(0xF0DB2777)),
    glowColor = Color(0xFFFB923C)
  ),
  MAGENTA_BLUE(
    key = "magenta_blue",
    labelFr = "Magenta-Bleu",
    labelEn = "Magenta-Blue",
    colors = listOf(Color(0xEBEC4899), Color(0xEB7C3AED), Color(0xF02563EB)),
    glowColor = Color(0xFFF472B6)
  ),
  DEEP_PURPLE(
    key = "deep_purple",
    labelFr = "Violet Nuit",
    labelEn = "Deep Purple",
    colors = listOf(Color(0xEB8B5CF6), Color(0xEB6D28D9), Color(0xF0312E81)),
    glowColor = Color(0xFFA78BFA)
  );

  companion object {
    fun fromKey(key: String?): WalletGradientPreset =
      entries.firstOrNull { it.key == key } ?: TEAL
  }
}

private fun maskCardNumber(raw: String): String {
  val trimmed = raw.trim()
  if (trimmed.isBlank()) return "•••• ••••"
  val compact = trimmed.replace(" ", "")
  if (compact.length <= 4) return compact
  val last4 = compact.takeLast(4)
  return "••••  ••••  $last4"
}

private fun iconForCategory(category: String): ImageVector {
  val normalized = category.lowercase()
  return when {
    normalized.contains("assur") || normalized.contains("insur") || normalized.contains("sant") || normalized.contains("health") ->
      Icons.Default.HealthAndSafety
    normalized.contains("id") || normalized.contains("permis") || normalized.contains("licen") ->
      Icons.Default.Badge
    normalized.contains("membr") || normalized.contains("member") || normalized.contains("club") ->
      Icons.Default.CardMembership
    normalized.contains("bank") || normalized.contains("banc") || normalized.contains("credit") || normalized.contains("pay") ->
      Icons.Default.CreditCard
    else -> Icons.Default.Wallet
  }
}

@Composable
fun DigitalWalletStackSection(
  userId: String,
  cards: List<SyncedWalletCard>,
  syncStatus: WalletSyncStatus,
  onSaveCard: (SyncedWalletCard) -> Unit,
  onDeleteCard: (String) -> Unit,
  onSeedSampleCards: () -> Unit,
  theme: ThemeColors,
  language: AppLanguage,
  modifier: Modifier = Modifier,
  isAnchoredBottomDock: Boolean = false,
  isExpanded: Boolean = true,
  onToggleExpanded: (() -> Unit)? = null,
  walletPin: String = "8888",
  userEmail: String = "",
  externalOpenCardId: String? = null,
  onClearExternalOpenCard: () -> Unit = {},
  onUpdateWalletPin: (String) -> Unit = {},
  onLogExpenseFromCard: (SyncedWalletCard) -> Unit = {}
) {
  val context = LocalContext.current

  if (isAnchoredBottomDock && isExpanded && onToggleExpanded != null) {
    BackHandler { onToggleExpanded() }
  }

  var selectedCardId by remember { mutableStateOf<String?>(null) }
  var pendingPinCard by remember { mutableStateOf<SyncedWalletCard?>(null) }
  var expandedCard by remember { mutableStateOf<SyncedWalletCard?>(null) }
  var showAddEditModal by remember { mutableStateOf(false) }
  var editingCard by remember { mutableStateOf<SyncedWalletCard?>(null) }

  LaunchedEffect(externalOpenCardId, cards) {
    if (!externalOpenCardId.isNullOrBlank()) {
      val match = cards.firstOrNull { it.id == externalOpenCardId }
      if (match != null) {
        selectedCardId = match.id
        pendingPinCard = match
      }
      onClearExternalOpenCard()
    }
  }

  // Keep selectedCardId valid as cards stream in
  LaunchedEffect(cards) {
    if (cards.isEmpty()) {
      selectedCardId = null
    } else if (selectedCardId == null || cards.none { it.id == selectedCardId }) {
      selectedCardId = cards.first().id
    }
    // Keep expanded modal synchronized with live Firestore updates
    expandedCard?.let { currentExpanded ->
      val updated = cards.firstOrNull { it.id == currentExpanded.id }
      if (updated != null && updated != currentExpanded) {
        expandedCard = updated
      }
    }
  }

  val activeIndex = remember(cards, selectedCardId) {
    cards.indexOfFirst { it.id == selectedCardId }.let { if (it >= 0) it else 0 }
  }

  val visibleMaxCards = if (isAnchoredBottomDock && !isExpanded) 3 else 4
  val visibleWindowCards = remember(cards, activeIndex, visibleMaxCards) {
    if (cards.size <= visibleMaxCards) {
      cards
    } else {
      val start = (activeIndex - 1).coerceIn(0, (cards.size - visibleMaxCards).coerceAtLeast(0))
      cards.subList(start, (start + visibleMaxCards).coerceAtMost(cards.size))
    }
  }

  val activePosInWindow = remember(visibleWindowCards, selectedCardId) {
    visibleWindowCards.indexOfFirst { it.id == selectedCardId }.let { if (it >= 0) it else 0 }
  }

  val isCompactPeek = isAnchoredBottomDock && !isExpanded

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("digital_wallet_section"),
    shape = if (isAnchoredBottomDock) {
      RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    } else {
      RoundedCornerShape(26.dp)
    },
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border),
    shadowElevation = if (isAnchoredBottomDock) 14.dp else 6.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              theme.cardSurface,
              theme.background.copy(alpha = 0.72f)
            )
          )
        )
        .padding(
          horizontal = 14.dp,
          vertical = if (isCompactPeek) 6.dp else 16.dp
        ),
      verticalArrangement = Arrangement.spacedBy(if (isCompactPeek) 4.dp else 12.dp)
    ) {
      // Wallet Header: Title + Subtle Sync Status Badge + Expand Toggle + Add Card Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .let { base ->
              if (onToggleExpanded != null) base.clickable { onToggleExpanded() } else base
            }
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AccountBalanceWallet,
              contentDescription = null,
              tint = theme.accent,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
              text = if (language == AppLanguage.FR) "Portefeuille Numérique" else "Digital Wallet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          WalletSyncStatusBadge(
            syncStatus = syncStatus,
            cardCount = cards.size,
            language = language,
            theme = theme
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (onToggleExpanded != null) {
            IconButton(
              onClick = onToggleExpanded,
              modifier = Modifier
                .size(48.dp)
                .testTag("wallet_expand_toggle_button")
            ) {
              Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) {
                  if (language == AppLanguage.FR) "Réduire le portefeuille" else "Collapse wallet"
                } else {
                  if (language == AppLanguage.FR) "Agrandir le portefeuille" else "Expand wallet"
                },
                tint = theme.primaryText
              )
            }
          }

          Button(
            onClick = {
              editingCard = null
              showAddEditModal = true
            },
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("wallet_add_card_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AddCard,
              contentDescription = if (language == AppLanguage.FR) "Ajouter une carte" else "Add Card",
              tint = Color.White,
              modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (language == AppLanguage.FR) "+ Ajouter" else "+ Add Card",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }
        }
      }

      if (cards.isEmpty()) {
        WalletEmptyStateCard(
          language = language,
          theme = theme,
          onAddCard = {
            editingCard = null
            showAddEditModal = true
          },
          onSeedDefaults = onSeedSampleCards
        )
      } else {
        // Quick switcher pill strip when expanded & >4 cards
        if (!isCompactPeek && cards.size > 4) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(cards, key = { it.id }) { card ->
              val isSelected = card.id == selectedCardId
              val preset = WalletGradientPreset.fromKey(card.gradientKey)
              Surface(
                onClick = { selectedCardId = card.id },
                shape = RoundedCornerShape(100.dp),
                color = if (isSelected) preset.colors.first() else theme.background,
                border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.5f) else theme.border)
              ) {
                Text(
                  text = card.cardTitle,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) Color.White else theme.primaryText,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1
                )
              }
            }
          }
        }

        // Interactive Vertical Card Stack (slimmer when collapsed dock so Dashboard content, Upcoming Actions & Quick Add stay clearly visible)
        val cardHeight = if (isCompactPeek) 74.dp else 206.dp
        val abovePeekStep = if (isCompactPeek) 16.dp else 48.dp
        val belowPeekStep = if (isCompactPeek) 12.dp else 40.dp
        val aboveCount = activePosInWindow
        val belowCount = (visibleWindowCards.size - 1 - activePosInWindow).coerceAtLeast(0)
        val totalStackHeight by animateDpAsState(
          targetValue = cardHeight + (abovePeekStep * aboveCount) + (belowPeekStep * belowCount),
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
          ),
          label = "walletStackHeight"
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(totalStackHeight)
            .testTag("wallet_card_stack"),
          contentAlignment = Alignment.TopCenter
        ) {
          visibleWindowCards.forEachIndexed { index, card ->
            val distance = index - activePosInWindow
            val isActive = distance == 0
            val absDistance = abs(distance)

            val targetOffsetY = when {
              distance < 0 -> abovePeekStep * index
              distance == 0 -> abovePeekStep * aboveCount
              else -> (abovePeekStep * aboveCount) + (belowPeekStep * distance)
            }
            val targetScale = when (absDistance) {
              0 -> 1.0f
              1 -> 0.93f
              else -> 0.87f
            }
            val targetZIndex = if (isActive) 20f else (10f - absDistance)

            val animatedOffsetY by animateDpAsState(
              targetValue = targetOffsetY,
              animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
              ),
              label = "cardOffsetY_${card.id}"
            )
            val animatedScale by animateFloatAsState(
              targetValue = targetScale,
              animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
              ),
              label = "cardScale_${card.id}"
            )

            WalletStackedCardView(
              userId = userId,
              card = card,
              isActive = isActive,
              isBelowActive = distance > 0,
              isCompact = isCompactPeek,
              language = language,
              modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .offset(y = animatedOffsetY)
                .zIndex(targetZIndex)
                .graphicsLayer {
                  scaleX = animatedScale
                  scaleY = animatedScale
                }
                .testTag("wallet_card_${card.id}"),
              onClick = {
                if (isActive) {
                  pendingPinCard = card
                } else {
                  selectedCardId = card.id
                }
              }
            )
          }
        }

        // Subtle helper hint beneath stack when expanded
        if (!isCompactPeek) {
          Text(
            text = if (language == AppLanguage.FR) {
              "Touchez une carte en arrière-plan pour l'amener au premier plan • Code PIN requis pour ouvrir les détails"
            } else {
              "Tap any background card edge to bring it forward • 4-digit PIN protects card details"
            },
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = theme.mutedText
          )
        }
      }
    }
  }

  // 4-Digit PIN Lock Gate before viewing Card Details
  pendingPinCard?.let { lockedCard ->
    WalletPinLockModal(
      expectedPin = walletPin.filter { it.isDigit() }.take(4).ifBlank { "8888" },
      userEmail = userEmail,
      language = language,
      theme = theme,
      onUnlocked = {
        pendingPinCard = null
        expandedCard = lockedCard
      },
      onResetPin = { newPin ->
        onUpdateWalletPin(newPin)
      },
      onDismiss = { pendingPinCard = null }
    )
  }

  // Flip / Expand Preview Dialog
  expandedCard?.let { card ->
    WalletCardDetailFlipDialog(
      userId = userId,
      card = card,
      language = language,
      theme = theme,
      onEdit = {
        expandedCard = null
        editingCard = card
        showAddEditModal = true
      },
      onDelete = {
        expandedCard = null
        onDeleteCard(card.id)
        Toast.makeText(
          context,
          if (language == AppLanguage.FR) "Carte supprimée du portefeuille" else "Card removed from wallet",
          Toast.LENGTH_SHORT
        ).show()
      },
      onLogExpense = {
        expandedCard = null
        onLogExpenseFromCard(card)
      },
      onDismiss = { expandedCard = null }
    )
  }

  // Add / Scan / Edit Card Dialog
  if (showAddEditModal) {
    WalletAddEditCardDialog(
      existingCard = editingCard,
      nextSortOrder = (cards.maxOfOrNull { it.sortOrder } ?: -1L) + 1L,
      language = language,
      theme = theme,
      onSave = { savedCard ->
        onSaveCard(savedCard)
        selectedCardId = savedCard.id
        showAddEditModal = false
        editingCard = null
        Toast.makeText(
          context,
          if (language == AppLanguage.FR) "Carte enregistrée & synchronisée !" else "Card saved & syncing!",
          Toast.LENGTH_SHORT
        ).show()
      },
      onDismiss = {
        showAddEditModal = false
        editingCard = null
      }
    )
  }
}

@Composable
private fun WalletSyncStatusBadge(
  syncStatus: WalletSyncStatus,
  cardCount: Int,
  language: AppLanguage,
  theme: ThemeColors
) {
  val (badgeColor, bgColor, icon, label) = when (syncStatus) {
    WalletSyncStatus.SYNCED -> Quadruple(
      Color(0xFF15803D),
      Color(0xFFF0FDF4),
      Icons.Default.CloudDone,
      if (language == AppLanguage.FR) "Synchronisé • $cardCount carte(s)" else "Synced • $cardCount card(s)"
    )
    WalletSyncStatus.OFFLINE_LOCAL -> Quadruple(
      Color(0xFFB45309),
      Color(0xFFFFFBEB),
      Icons.Default.CloudOff,
      if (language == AppLanguage.FR) "Hors ligne - Sauvegardé localement" else "Offline - Saved Locally"
    )
    WalletSyncStatus.SYNCING -> Quadruple(
      theme.accent,
      theme.accent.copy(alpha = 0.12f),
      Icons.Default.CloudSync,
      if (language == AppLanguage.FR) "Synchronisation..." else "Syncing..."
    )
    WalletSyncStatus.ERROR -> Quadruple(
      Color(0xFFB91C1C),
      Color(0xFFFEF2F2),
      Icons.Default.CloudOff,
      if (language == AppLanguage.FR) "En attente de reconnexion" else "Sync pending"
    )
  }

  Surface(
    modifier = Modifier.testTag("wallet_sync_badge"),
    shape = RoundedCornerShape(100.dp),
    color = bgColor,
    border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.35f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = badgeColor,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = badgeColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun WalletEmptyStateCard(
  language: AppLanguage,
  theme: ThemeColors,
  onAddCard: () -> Unit,
  onSeedDefaults: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = theme.background,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = Icons.Default.CreditCard,
        contentDescription = null,
        tint = theme.accent,
        modifier = Modifier.size(36.dp)
      )
      Text(
        text = if (language == AppLanguage.FR) "Aucune carte enregistrée" else "No cards in your wallet yet",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = theme.primaryText
      )
      Text(
        text = if (language == AppLanguage.FR) {
          "Numérisez votre carte d'assurance, permis ou carte membre pour un accès hors ligne instantané."
        } else {
          "Scan your insurance card, driver's license, or membership card for instant offline access."
        },
        style = MaterialTheme.typography.bodySmall,
        color = theme.mutedText
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onAddCard,
          modifier = Modifier.heightIn(min = 48.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(if (language == AppLanguage.FR) "Ajouter une carte" else "Add Card", color = Color.White)
        }
        OutlinedButton(
          onClick = onSeedDefaults,
          modifier = Modifier.heightIn(min = 48.dp),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(if (language == AppLanguage.FR) "Modèles" else "Load Starters", color = theme.primaryText)
        }
      }
    }
  }
}

@Composable
private fun WalletStackedCardView(
  userId: String,
  card: SyncedWalletCard,
  isActive: Boolean,
  isBelowActive: Boolean,
  isCompact: Boolean,
  language: AppLanguage,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val context = LocalContext.current
  val preset = WalletGradientPreset.fromKey(card.gradientKey)
  val hasFrontScan = card.frontImageBase64.isNotBlank()
  val hasBackScan = card.backImageBase64.isNotBlank()

  val primaryScanBase64 = if (hasFrontScan) card.frontImageBase64 else card.backImageBase64
  val primaryScanSide = if (hasFrontScan) "front" else "back"
  var thumbnailBitmap by remember(card.id, primaryScanBase64) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(card.id, primaryScanBase64) {
    thumbnailBitmap = if (primaryScanBase64.isNotBlank()) {
      WalletImageUtils.decodeAndCacheCardBitmap(
        context = context,
        userId = userId,
        cardId = card.id,
        side = primaryScanSide,
        base64 = primaryScanBase64
      )
    } else {
      null
    }
  }

  Surface(
    modifier = modifier.clickable(onClick = onClick),
    shape = RoundedCornerShape(if (isCompact) 18.dp else 22.dp),
    color = Color.Transparent,
    shadowElevation = if (isActive) 16.dp else 4.dp,
    border = BorderStroke(
      width = if (isActive) 1.4.dp else 1.dp,
      color = Color.White.copy(alpha = if (isActive) 0.52f else 0.28f)
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(colors = preset.colors))
    ) {
      // Subtle translucent glassmorphic sheen overlay
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color.White.copy(alpha = if (isActive) 0.22f else 0.10f),
                Color.Transparent
              )
            )
          )
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(
            horizontal = if (isCompact) 14.dp else 18.dp,
            vertical = if (isCompact) 8.dp else 14.dp
          ),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Row: Title top-left + Mini Thumbnail (when peeking above/active) + Category Pill top-right
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = card.cardTitle.ifBlank { "Carte" },
              style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (!isCompact && isActive && card.holderName.isNotBlank()) {
              Text(
                text = card.holderName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Show small peek indicator dot / thumbnail badge if inactive card has a scan
          if (!isActive && thumbnailBitmap != null) {
            Box(
              modifier = Modifier
                .size(width = 34.dp, height = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(0.8.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            ) {
              Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = card.cardTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color.White.copy(alpha = 0.22f),
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.35f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = iconForCategory(card.cardCategory),
                contentDescription = card.cardCategory,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = card.cardCategory,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
              )
            }
          }
        }

        // Compact bottom summary when collapsed: masked number + crisp scan thumbnail
        if (isActive && isCompact) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = maskCardNumber(card.cardNumber),
              style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
              ),
              fontWeight = FontWeight.SemiBold,
              color = Color.White.copy(alpha = 0.94f),
              maxLines = 1,
              modifier = Modifier.weight(1f)
            )
            if (thumbnailBitmap != null) {
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .size(width = 48.dp, height = 28.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color.Black.copy(alpha = 0.25f))
                  .border(0.8.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                  .testTag("wallet_card_compact_thumb_${card.id}")
              ) {
                Image(
                  bitmap = thumbnailBitmap!!.asImageBitmap(),
                  contentDescription = card.cardTitle,
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              }
            }
          }
        }

        // Middle Row on Active Card: Crisp Scan Thumbnail + Scan Badges / Expiry (expanded view)
        if (isActive && !isCompact) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (hasFrontScan || hasBackScan) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color.Black.copy(alpha = 0.24f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.PhotoCamera,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val scanLabel = when {
                      hasFrontScan && hasBackScan -> if (language == AppLanguage.FR) "Recto + Verso" else "Front + Back"
                      hasFrontScan -> if (language == AppLanguage.FR) "Recto scanné" else "Front scanned"
                      else -> if (language == AppLanguage.FR) "Verso scanné" else "Back scanned"
                    }
                    Text(
                      text = scanLabel,
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 10.sp,
                      color = Color.White
                    )
                  }
                }
              }

              if (card.expiryDate.isNotBlank()) {
                Text(
                  text = "Exp. ${card.expiryDate}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.88f)
                )
              }
            }

            if (thumbnailBitmap != null) {
              Spacer(modifier = Modifier.width(10.dp))
              Box(
                modifier = Modifier
                  .size(width = 116.dp, height = 72.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color.Black.copy(alpha = 0.25f))
                  .border(1.2.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                  .testTag("wallet_card_thumbnail_${card.id}")
              ) {
                Image(
                  bitmap = thumbnailBitmap!!.asImageBitmap(),
                  contentDescription = if (language == AppLanguage.FR) "Miniature du scan ${card.cardTitle}" else "${card.cardTitle} scan thumbnail",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
                if (hasFrontScan && hasBackScan) {
                  Surface(
                    modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .padding(3.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = Color.Black.copy(alpha = 0.62f)
                  ) {
                    Text(
                      text = "R/V",
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }
              }
            }
          }
        } else if (isActive && isCompact && (thumbnailBitmap != null || hasFrontScan || hasBackScan)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (hasFrontScan || hasBackScan) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.24f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = when {
                      hasFrontScan && hasBackScan -> if (language == AppLanguage.FR) "Recto + Verso" else "Front + Back"
                      hasFrontScan -> if (language == AppLanguage.FR) "Recto" else "Front"
                      else -> if (language == AppLanguage.FR) "Verso" else "Back"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    color = Color.White
                  )
                }
              }
            } else {
              Spacer(modifier = Modifier.width(1.dp))
            }

            if (thumbnailBitmap != null) {
              Box(
                modifier = Modifier
                  .size(width = 58.dp, height = 30.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(Color.Black.copy(alpha = 0.25f))
                  .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(7.dp))
                  .testTag("wallet_card_thumbnail_${card.id}")
              ) {
                Image(
                  bitmap = thumbnailBitmap!!.asImageBitmap(),
                  contentDescription = if (language == AppLanguage.FR) "Miniature du scan ${card.cardTitle}" else "${card.cardTitle} scan thumbnail",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              }
            }
          }
        }

        // Bottom Row: Masked Card / Policy Number + Flip hint (or card title when peeking below)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = if (isBelowActive) {
              "${card.cardTitle} • ${maskCardNumber(card.cardNumber)}"
            } else {
              maskCardNumber(card.cardNumber)
            },
            style = MaterialTheme.typography.titleSmall.copy(
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.sp
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          if (isActive) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color.White.copy(alpha = 0.22f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Flip,
                  contentDescription = if (language == AppLanguage.FR) "Retourner" else "Flip",
                  tint = Color.White,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (language == AppLanguage.FR) "Ouvrir" else "Flip",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WalletPinLockModal(
  expectedPin: String,
  userEmail: String,
  language: AppLanguage,
  theme: ThemeColors,
  onUnlocked: () -> Unit,
  onResetPin: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isFrench = language == AppLanguage.FR
  var enteredPin by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }
  var showForgotFlow by remember { mutableStateOf(false) }

  var verificationInput by remember { mutableStateOf("") }
  var newPinInput by remember { mutableStateOf("") }
  var confirmPinInput by remember { mutableStateOf("") }
  var dispatchedCode by remember { mutableStateOf<String?>(null) }

  val isRecoveryEmailAllowed = userEmail.trim().equals("bala1fatou@gmail.com", ignoreCase = true)

  fun appendDigit(d: String) {
    if (enteredPin.length < 4) {
      val next = enteredPin + d
      enteredPin = next
      pinError = null
      if (next.length == 4) {
        if (next == expectedPin) {
          onUnlocked()
        } else {
          pinError = if (isFrench) "Code PIN incorrect" else "Incorrect PIN"
          enteredPin = ""
        }
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border),
      shadowElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text = if (isFrench) "Code PIN Portefeuille" else "Wallet PIN Lock",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = if (isFrench) "Fermer" else "Close", tint = theme.mutedText)
          }
        }

        if (!showForgotFlow) {
          Text(
            text = if (isFrench) "Entrez votre code à 4 chiffres" else "Enter your 4-digit PIN to unlock",
            style = MaterialTheme.typography.bodySmall,
            color = theme.mutedText
          )

          Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { idx ->
              val filled = idx < enteredPin.length
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(if (filled) theme.accent else theme.background)
                  .border(1.5.dp, if (filled) theme.accent else theme.border, CircleShape)
              )
            }
          }

          if (!pinError.isNullOrBlank()) {
            Text(
              text = pinError ?: "",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFDC2626),
              fontWeight = FontWeight.SemiBold
            )
          }

          val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
          )
          rows.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
              row.forEach { key ->
                if (key.isEmpty()) {
                  Spacer(modifier = Modifier.size(64.dp, 50.dp))
                } else if (key == "DEL") {
                  OutlinedButton(
                    onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(64.dp, 50.dp),
                    contentPadding = PaddingValues(0.dp)
                  ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = theme.primaryText, modifier = Modifier.size(18.dp))
                  }
                } else {
                  OutlinedButton(
                    onClick = { appendDigit(key) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .size(64.dp, 50.dp)
                      .testTag("wallet_pin_key_$key"),
                    contentPadding = PaddingValues(0.dp)
                  ) {
                    Text(
                      text = key,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText
                    )
                  }
                }
              }
            }
          }

          TextButton(
            onClick = { showForgotFlow = true; pinError = null },
            modifier = Modifier.testTag("wallet_forgot_pin_button")
          ) {
            Text(
              text = if (isFrench) "Code PIN oublié ?" else "Forgot PIN?",
              style = MaterialTheme.typography.labelMedium,
              color = theme.accent,
              fontWeight = FontWeight.SemiBold
            )
          }
        } else {
          // Email-scoped Reset Flow for bala1fatou@gmail.com
          if (!isRecoveryEmailAllowed) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFFDECE9)
            ) {
              Text(
                text = if (isFrench) {
                  "La réinitialisation par e-mail est réservée exclusivement au compte bala1fatou@gmail.com."
                } else {
                  "Email PIN reset is restricted exclusively to bala1fatou@gmail.com."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB54938),
                modifier = Modifier.padding(12.dp)
              )
            }
            TextButton(onClick = { showForgotFlow = false }) {
              Text(if (isFrench) "Retour au clavier" else "Back to PIN")
            }
          } else {
            Text(
              text = if (isFrench) {
                "Réinitialisation sécurisée pour bala1fatou@gmail.com"
              } else {
                "Verified PIN reset for bala1fatou@gmail.com"
              },
              style = MaterialTheme.typography.bodySmall,
              color = theme.primaryText,
              fontWeight = FontWeight.SemiBold
            )

            Button(
              onClick = {
                val code = (100000..999999).random().toString()
                dispatchedCode = code
                context.getSharedPreferences("wallet_pin_reset", android.content.Context.MODE_PRIVATE)
                  .edit()
                  .putString("pending_code", code)
                  .putLong("expires_at", System.currentTimeMillis() + 15L * 60_000L)
                  .apply()
                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                  data = android.net.Uri.parse("mailto:bala1fatou@gmail.com")
                  putExtra(android.content.Intent.EXTRA_SUBJECT, "B&F Parcours — Code PIN Reset: $code")
                  putExtra(android.content.Intent.EXTRA_TEXT, "Verification code: $code (valid 15 min)")
                }
                runCatching { context.startActivity(emailIntent) }
                Toast.makeText(
                  context,
                  if (isFrench) "Code envoyé vers bala1fatou@gmail.com" else "Reset code prepared for bala1fatou@gmail.com",
                  Toast.LENGTH_SHORT
                ).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
              Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(Modifier.width(6.dp))
              Text(if (isFrench) "Envoyer code à bala1fatou@gmail.com" else "Send Code to bala1fatou@gmail.com", color = Color.White)
            }

            if (dispatchedCode != null) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = theme.background,
                border = BorderStroke(1.dp, theme.border)
              ) {
                Text(
                  text = if (isFrench) {
                    "Code temporaire émis (${dispatchedCode}) — saisissez-le ci-dessous pour valider."
                  } else {
                    "Verification code issued (${dispatchedCode}) — enter below to set new PIN."
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }

            OutlinedTextField(
              value = verificationInput,
              onValueChange = { if (it.length <= 6) verificationInput = it.filter { ch -> ch.isDigit() } },
              label = { Text(if (isFrench) "Code 6 chiffres" else "6-Digit Code") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = newPinInput,
              onValueChange = { if (it.length <= 4) newPinInput = it.filter { ch -> ch.isDigit() } },
              label = { Text(if (isFrench) "Nouveau PIN (4 chiffres)" else "New 4-Digit PIN") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = confirmPinInput,
              onValueChange = { if (it.length <= 4) confirmPinInput = it.filter { ch -> ch.isDigit() } },
              label = { Text(if (isFrench) "Confirmer PIN" else "Confirm PIN") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            if (!pinError.isNullOrBlank()) {
              Text(text = pinError ?: "", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              TextButton(
                onClick = { showForgotFlow = false; pinError = null },
                modifier = Modifier.weight(1f)
              ) {
                Text(if (isFrench) "Annuler" else "Cancel")
              }
              Button(
                onClick = {
                  val prefs = context.getSharedPreferences("wallet_pin_reset", android.content.Context.MODE_PRIVATE)
                  val expectedCode = prefs.getString("pending_code", null)
                  val expiresAt = prefs.getLong("expires_at", 0L)
                  when {
                    expectedCode.isNullOrBlank() || System.currentTimeMillis() > expiresAt ->
                      pinError = if (isFrench) "Envoyez d'abord le code par e-mail" else "Send verification code first"
                    verificationInput != expectedCode ->
                      pinError = if (isFrench) "Code de vérification invalide" else "Invalid verification code"
                    newPinInput.length != 4 || newPinInput != confirmPinInput ->
                      pinError = if (isFrench) "Les 4 chiffres ne correspondent pas" else "4-digit PINs do not match"
                    else -> {
                      onResetPin(newPinInput)
                      prefs.edit().clear().apply()
                      onUnlocked()
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                modifier = Modifier.weight(1f).height(48.dp)
              ) {
                Text(if (isFrench) "Valider" else "Reset PIN", color = Color.White)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WalletCardDetailFlipDialog(
  userId: String,
  card: SyncedWalletCard,
  language: AppLanguage,
  theme: ThemeColors,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onLogExpense: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val preset = WalletGradientPreset.fromKey(card.gradientKey)

  var showBackSide by remember { mutableStateOf(false) }
  var isNumberRevealed by remember { mutableStateOf(false) }
  var frontBitmap by remember(card.id, card.frontImageBase64) { mutableStateOf<Bitmap?>(null) }
  var backBitmap by remember(card.id, card.backImageBase64) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(card.id, card.frontImageBase64, card.backImageBase64) {
    frontBitmap = WalletImageUtils.decodeAndCacheCardBitmap(
      context, userId, card.id, "front", card.frontImageBase64
    )
    backBitmap = WalletImageUtils.decodeAndCacheCardBitmap(
      context, userId, card.id, "back", card.backImageBase64
    )
  }

  val rotationYAngle by animateFloatAsState(
    targetValue = if (showBackSide) 180f else 0f,
    animationSpec = tween(durationMillis = 420),
    label = "walletCardFlip"
  )
  val isShowingBackVisual = rotationYAngle > 90f

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp)
        .widthIn(max = 520.dp),
      shape = RoundedCornerShape(24.dp),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border)
    ) {
      Column(
        modifier = Modifier
          .padding(18.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = iconForCategory(card.cardCategory),
              contentDescription = null,
              tint = theme.accent,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = card.cardTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primaryText
              )
              Text(
                text = card.cardCategory,
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedText
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = if (language == AppLanguage.FR) "Fermer" else "Close",
              tint = theme.mutedText
            )
          }
        }

        // Compact Icon-Only Front / Back Toggle Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (!showBackSide) theme.accent else theme.background,
            border = BorderStroke(1.dp, if (!showBackSide) theme.accent else theme.border)
          ) {
            IconButton(
              onClick = { showBackSide = false },
              modifier = Modifier
                .size(48.dp)
                .testTag("wallet_flip_front_button")
            ) {
              Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = if (language == AppLanguage.FR) "Recto" else "Front",
                tint = if (!showBackSide) Color.White else theme.primaryText
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (showBackSide) theme.accent else theme.background,
            border = BorderStroke(1.dp, if (showBackSide) theme.accent else theme.border)
          ) {
            IconButton(
              onClick = { showBackSide = true },
              modifier = Modifier
                .size(48.dp)
                .testTag("wallet_flip_back_button")
            ) {
              Icon(
                imageVector = Icons.Default.FlipToBack,
                contentDescription = if (language == AppLanguage.FR) "Verso" else "Back",
                tint = if (showBackSide) Color.White else theme.primaryText
              )
            }
          }
        }

        // Flippable Card Image / Styled Face Container
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .graphicsLayer {
              rotationY = rotationYAngle
              cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(preset.colors))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { showBackSide = !showBackSide }
        ) {
          val displayedBitmap = if (isShowingBackVisual) backBitmap else frontBitmap
          Box(
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                if (isShowingBackVisual) rotationY = 180f
              }
          ) {
            if (displayedBitmap != null) {
              Image(
                bitmap = displayedBitmap.asImageBitmap(),
                contentDescription = if (isShowingBackVisual) "Back scan" else "Front scan",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
              Surface(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.55f)
              ) {
                Text(
                  text = if (isShowingBackVisual) {
                    if (language == AppLanguage.FR) "VERSO" else "BACK"
                  } else {
                    if (language == AppLanguage.FR) "RECTO" else "FRONT"
                  },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
              }
            } else {
              // Styled fallback face when no photo attached for this side
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = if (isShowingBackVisual) {
                      if (language == AppLanguage.FR) "VERSO • DÉTAILS" else "BACK • DETAILS"
                    } else {
                      card.cardTitle
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Icon(
                    imageVector = Icons.Default.Flip,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f)
                  )
                }

                if (!isShowingBackVisual) {
                  Text(
                    text = if (isNumberRevealed) {
                      card.cardNumber.ifBlank { "•••• •••• ••••" }
                    } else {
                      maskCardNumber(card.cardNumber)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontFamily = FontFamily.Monospace,
                      letterSpacing = 1.2.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                } else {
                  Text(
                    text = card.notes.ifBlank {
                      if (language == AppLanguage.FR) "Aucune photo verso attachée. Touchez Modifier pour numériser."
                      else "No back scan attached yet. Tap Edit to scan or attach an image."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = card.holderName.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                  )
                  if (card.expiryDate.isNotBlank()) {
                    Text(
                      text = "Exp: ${card.expiryDate}",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.White.copy(alpha = 0.85f)
                    )
                  }
                }
              }
            }
          }
        }

        // Full Card / Policy Details + Masked Toggle + Quick Copy
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = theme.background,
          border = BorderStroke(1.dp, theme.border)
        ) {
          SelectionContainer {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (language == AppLanguage.FR) "Numéro de Carte / Police" else "Card / Policy Number",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                  Text(
                    text = if (isNumberRevealed) card.cardNumber.ifBlank { "—" } else maskCardNumber(card.cardNumber),
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                }

                if (card.cardNumber.isNotBlank()) {
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                      onClick = { isNumberRevealed = !isNumberRevealed },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("wallet_reveal_number_button")
                    ) {
                      Icon(
                        imageVector = if (isNumberRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isNumberRevealed) "Hide number" else "Reveal number",
                        tint = theme.primaryText
                      )
                    }
                    IconButton(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(card.cardNumber))
                        Toast.makeText(
                          context,
                          if (language == AppLanguage.FR) "Numéro copié !" else "Number copied!",
                          Toast.LENGTH_SHORT
                        ).show()
                      },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("wallet_copy_number_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = if (language == AppLanguage.FR) "Copier" else "Copy",
                        tint = theme.accent
                      )
                    }
                  }
                }
              }

              if (card.holderName.isNotBlank() || card.expiryDate.isNotBlank()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  if (card.holderName.isNotBlank()) {
                    Column {
                      Text(
                        text = if (language == AppLanguage.FR) "Titulaire" else "Cardholder",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.mutedText
                      )
                      Text(
                        text = card.holderName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryText
                      )
                    }
                  }
                  if (card.expiryDate.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = if (language == AppLanguage.FR) "Expiration" else "Expiry",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.mutedText
                      )
                      Text(
                        text = card.expiryDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryText
                      )
                    }
                  }
                }
              }

              if (card.notes.isNotBlank()) {
                Column {
                  Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                  Text(
                    text = card.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.primaryText
                  )
                }
              }
            }
          }
        }

        // Compact Bottom Actions: Icon-Only Delete + Icon-Only Edit + Log Expense Pill
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFFFDECE9),
              border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
              IconButton(
                onClick = onDelete,
                modifier = Modifier
                  .size(48.dp)
                  .testTag("wallet_delete_card_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = if (language == AppLanguage.FR) "Supprimer" else "Delete",
                  tint = Color(0xFFDC2626)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = theme.background,
              border = BorderStroke(1.dp, theme.border)
            ) {
              IconButton(
                onClick = onEdit,
                modifier = Modifier
                  .size(48.dp)
                  .testTag("wallet_edit_card_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = if (language == AppLanguage.FR) "Modifier / Scan" else "Edit / Scan",
                  tint = theme.primaryText
                )
              }
            }
          }

          Button(
            onClick = onLogExpense,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("wallet_log_expense_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
          ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (language == AppLanguage.FR) "Noter Dépense" else "Log Expense",
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WalletAddEditCardDialog(
  existingCard: SyncedWalletCard?,
  nextSortOrder: Long,
  language: AppLanguage,
  theme: ThemeColors,
  onSave: (SyncedWalletCard) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isFrench = language == AppLanguage.FR

  var cardTitle by remember(existingCard) { mutableStateOf(existingCard?.cardTitle ?: "") }
  var cardCategory by remember(existingCard) {
    mutableStateOf(existingCard?.cardCategory ?: if (isFrench) "Assurance" else "Insurance")
  }
  var cardNumber by remember(existingCard) { mutableStateOf(existingCard?.cardNumber ?: "") }
  var holderName by remember(existingCard) { mutableStateOf(existingCard?.holderName ?: "") }
  var expiryDate by remember(existingCard) { mutableStateOf(existingCard?.expiryDate ?: "") }
  var notes by remember(existingCard) { mutableStateOf(existingCard?.notes ?: "") }
  var gradientKey by remember(existingCard) { mutableStateOf(existingCard?.gradientKey ?: WalletGradientPreset.TEAL.key) }

  var frontBase64 by remember(existingCard) { mutableStateOf(existingCard?.frontImageBase64 ?: "") }
  var backBase64 by remember(existingCard) { mutableStateOf(existingCard?.backImageBase64 ?: "") }
  var frontPreview by remember { mutableStateOf<Bitmap?>(null) }
  var backPreview by remember { mutableStateOf<Bitmap?>(null) }
  var isCompressingSide by remember { mutableStateOf<String?>(null) }
  var isReadingOcr by remember { mutableStateOf(false) }
  var ocrStatusMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(frontBase64) {
    frontPreview = WalletImageUtils.decodePreviewBitmap(frontBase64)
  }
  LaunchedEffect(backBase64) {
    backPreview = WalletImageUtils.decodePreviewBitmap(backBase64)
  }

  suspend fun runDocumentReader(bitmap: Bitmap, overwriteExisting: Boolean) {
    isReadingOcr = true
    try {
      val extracted = WalletCardOcrParser.scanBitmap(bitmap, isFrench)
      var filledCount = 0
      if (extracted.suggestedCategory.isNotBlank() && (cardTitle.isBlank() || overwriteExisting)) {
        cardCategory = extracted.suggestedCategory
      }
      if (extracted.suggestedTitle.isNotBlank() && (cardTitle.isBlank() || overwriteExisting)) {
        cardTitle = extracted.suggestedTitle
        filledCount++
      }
      if (extracted.cardNumber.isNotBlank() && (cardNumber.isBlank() || overwriteExisting)) {
        cardNumber = extracted.cardNumber
        filledCount++
      }
      if (extracted.holderName.isNotBlank() && (holderName.isBlank() || overwriteExisting)) {
        holderName = extracted.holderName
        filledCount++
      }
      if (extracted.expiryDate.isNotBlank() && (expiryDate.isBlank() || overwriteExisting)) {
        expiryDate = extracted.expiryDate
        filledCount++
      }
      if (extracted.notes.isNotBlank() && (notes.isBlank() || overwriteExisting)) {
        notes = extracted.notes
        filledCount++
      }

      ocrStatusMessage = if (filledCount > 0) {
        if (isFrench) "Lecteur Auto : $filledCount champ(s) extrait(s) du document"
        else "Document Reader: $filledCount field(s) auto-filled"
      } else if (extracted.rawText.isNotBlank()) {
        if (isFrench) "Texte lu — vérifiez ou complétez les champs ci-dessous"
        else "Text scanned — verify or refine the fields below"
      } else {
        if (isFrench) "Aucun texte lisible détecté sur cette image"
        else "No readable text detected on this image"
      }
    } catch (_: Exception) {
      ocrStatusMessage = if (isFrench) "Lecture auto indisponible sur cette image" else "Auto-read could not parse this image"
    } finally {
      isReadingOcr = false
    }
  }

  suspend fun handleProcessedImage(side: String, encoded: String) {
    if (side == "front") frontBase64 = encoded else backBase64 = encoded
    val decodedBitmap = WalletImageUtils.decodePreviewBitmap(encoded)
    if (decodedBitmap != null) {
      runDocumentReader(decodedBitmap, overwriteExisting = false)
    } else {
      Toast.makeText(
        context,
        if (isFrench) "Image compressée et prête !" else "Photo compressed & ready!",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  var pendingCaptureSide by remember { mutableStateOf("front") }
  var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    val uri = pendingCameraUri
    if (success && uri != null) {
      val side = pendingCaptureSide
      coroutineScope.launch {
        isCompressingSide = side
        val encoded = WalletImageUtils.compressAndEncodeUri(context, uri)
        isCompressingSide = null
        if (encoded != null) {
          handleProcessedImage(side, encoded)
        } else {
          Toast.makeText(
            context,
            if (isFrench) "Impossible de traiter l'image" else "Could not process image",
            Toast.LENGTH_SHORT
          ).show()
        }
      }
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      val uri = WalletImageUtils.createTempCameraCaptureUri(context)
      pendingCameraUri = uri
      takePictureLauncher.launch(uri)
    } else {
      Toast.makeText(
        context,
        if (isFrench) "Permission caméra requise pour scanner" else "Camera permission required to scan",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  val galleryPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val side = pendingCaptureSide
      coroutineScope.launch {
        isCompressingSide = side
        val encoded = WalletImageUtils.compressAndEncodeUri(context, uri)
        isCompressingSide = null
        if (encoded != null) {
          handleProcessedImage(side, encoded)
        }
      }
    }
  }

  fun launchCameraFor(side: String) {
    pendingCaptureSide = side
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      val uri = WalletImageUtils.createTempCameraCaptureUri(context)
      pendingCameraUri = uri
      takePictureLauncher.launch(uri)
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  fun launchGalleryFor(side: String) {
    pendingCaptureSide = side
    galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
  }

  val categoryOptions = if (isFrench) {
    listOf("Assurance", "Identité", "Membre", "Bancaire", "Santé")
  } else {
    listOf("Insurance", "ID / License", "Membership", "Payment", "Health")
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 14.dp, vertical = 12.dp)
        .widthIn(max = 560.dp)
        .heightIn(max = 640.dp),
      shape = RoundedCornerShape(24.dp),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // 1. PINNED HEADER
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (existingCard != null) {
              if (isFrench) "Modifier & Lire la Carte" else "Edit & Scan Card"
            } else {
              if (isFrench) "Scanner / Ajouter une Carte" else "Scan / Add Card"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = if (isFrench) "Fermer" else "Close", tint = theme.mutedText)
          }
        }

        HorizontalDivider(color = theme.border)

        // 2. SCROLLABLE FORM & SCANS BODY
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Document Reader Auto-Scan Strip
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = theme.accent.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.28f))
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.DocumentScanner,
                  contentDescription = null,
                  tint = theme.accent,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isFrench) "Lecteur de Document (Remplissage Auto)" else "Document Reader (Auto-Fill Info)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                  Text(
                    text = if (isFrench) {
                      "Scannez une pièce d'identité ou carte pour extraire nom, numéro & expiration."
                    } else {
                      "Snap or attach a card/ID to automatically extract name, number & expiry."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { launchCameraFor("front") },
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("wallet_smart_scan_camera_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFrench) "Scanner Caméra" else "Scan Camera",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1
                  )
                }

                OutlinedButton(
                  onClick = { launchGalleryFor("front") },
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("wallet_smart_scan_gallery_button"),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.5f)),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.accent, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isFrench) "Importer & Lire" else "Attach & Read",
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.accent,
                    maxLines = 1
                  )
                }
              }

              AnimatedVisibility(visible = isReadingOcr || ocrStatusMessage != null) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = theme.cardSurface,
                  border = BorderStroke(0.8.dp, theme.accent.copy(alpha = 0.35f))
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 7.dp)
                      .testTag("wallet_ocr_status_banner"),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    if (isReadingOcr) {
                      CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = theme.accent)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = if (isFrench) "Lecture intelligente du document..." else "Reading document text...",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.accent,
                        fontWeight = FontWeight.SemiBold
                      )
                    } else if (ocrStatusMessage != null) {
                      Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = ocrStatusMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.primaryText,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }
                }
              }
            }
          }

          // Gradient Color Picker
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = if (isFrench) "Style de dégradé :" else "Card Gradient:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              WalletGradientPreset.entries.forEach { preset ->
                val isSelected = gradientKey == preset.key
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { gradientKey = preset.key }
                    .padding(vertical = 4.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(Brush.linearGradient(preset.colors))
                      .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) theme.primaryText else theme.border,
                        shape = CircleShape
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = preset.labelEn,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = if (isFrench) preset.labelFr else preset.labelEn,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isSelected) theme.primaryText else theme.mutedText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                  )
                }
              }
            }
          }

          // Category pills
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categoryOptions) { cat ->
              val isSelected = cardCategory == cat
              Surface(
                onClick = { cardCategory = cat },
                shape = RoundedCornerShape(100.dp),
                color = if (isSelected) theme.accent else theme.background,
                border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
                modifier = Modifier.heightIn(min = 38.dp)
              ) {
                Text(
                  text = cat,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) Color.White else theme.primaryText,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              }
            }
          }

          OutlinedTextField(
            value = cardTitle,
            onValueChange = { cardTitle = it },
            label = { Text(if (isFrench) "Nom de la carte *" else "Card Name *") },
            placeholder = { Text(if (isFrench) "ex: Carte Assurance Santé" else "e.g. Health Insurance Card") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("wallet_card_name_input"),
            singleLine = true
          )

          OutlinedTextField(
            value = cardNumber,
            onValueChange = { cardNumber = it },
            label = { Text(if (isFrench) "Numéro de carte / police" else "Card / Policy Number") },
            placeholder = { Text("ex: 140 251 148") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("wallet_card_number_input"),
            singleLine = true
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = holderName,
              onValueChange = { holderName = it },
              label = { Text(if (isFrench) "Titulaire" else "Holder Name") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = expiryDate,
              onValueChange = { expiryDate = it },
              label = { Text(if (isFrench) "Expiration" else "Expiry") },
              placeholder = { Text("MM/AAAA") },
              modifier = Modifier.weight(0.85f),
              singleLine = true
            )
          }

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes / Contacts") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
          )

          // Front & Back Scan Attachment Pickers (bigger crisp preview + Auto-Read button)
          WalletScanSlotSection(
            title = if (isFrench) "Photo Recto (Face)" else "Front Scan (Image)",
            bitmap = frontPreview,
            isCompressing = isCompressingSide == "front",
            isReadingOcr = isReadingOcr,
            language = language,
            theme = theme,
            onCameraClick = { launchCameraFor("front") },
            onGalleryClick = { launchGalleryFor("front") },
            onAutoReadClick = {
              frontPreview?.let { bmp ->
                coroutineScope.launch { runDocumentReader(bmp, overwriteExisting = true) }
              }
            },
            onRemoveClick = { frontBase64 = "" }
          )

          WalletScanSlotSection(
            title = if (isFrench) "Photo Verso (Dos)" else "Back Scan (Image)",
            bitmap = backPreview,
            isCompressing = isCompressingSide == "back",
            isReadingOcr = isReadingOcr,
            language = language,
            theme = theme,
            onCameraClick = { launchCameraFor("back") },
            onGalleryClick = { launchGalleryFor("back") },
            onAutoReadClick = {
              backPreview?.let { bmp ->
                coroutineScope.launch { runDocumentReader(bmp, overwriteExisting = true) }
              }
            },
            onRemoveClick = { backBase64 = "" }
          )
        }

        HorizontalDivider(color = theme.border)

        // 3. PINNED STICKY FOOTER (Always visible regardless of image preview size)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("wallet_cancel_card_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(if (isFrench) "Annuler" else "Cancel", color = theme.mutedText)
          }

          Button(
            onClick = {
              val finalTitle = cardTitle.trim().ifBlank {
                if (isFrench) "Nouvelle Carte" else "New Card"
              }
              val saved = SyncedWalletCard(
                id = existingCard?.id?.ifBlank { null } ?: "wc_${System.currentTimeMillis()}",
                cardTitle = finalTitle,
                cardCategory = cardCategory.ifBlank { "Assurance" },
                cardNumber = cardNumber.trim(),
                holderName = holderName.trim(),
                expiryDate = expiryDate.trim(),
                notes = notes.trim(),
                gradientKey = gradientKey,
                frontImageBase64 = frontBase64,
                backImageBase64 = backBase64,
                sortOrder = existingCard?.sortOrder ?: nextSortOrder,
                createdAt = existingCard?.createdAt
              )
              onSave(saved)
            },
            enabled = isCompressingSide == null && !isReadingOcr,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("wallet_save_card_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isFrench) "Enregistrer" else "Save Card", color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
private fun WalletScanSlotSection(
  title: String,
  bitmap: Bitmap?,
  isCompressing: Boolean,
  isReadingOcr: Boolean,
  language: AppLanguage,
  theme: ThemeColors,
  onCameraClick: () -> Unit,
  onGalleryClick: () -> Unit,
  onAutoReadClick: () -> Unit,
  onRemoveClick: () -> Unit
) {
  val isFrench = language == AppLanguage.FR
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = theme.background,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        if (bitmap != null) {
          TextButton(onClick = onRemoveClick) {
            Icon(Icons.Default.Delete, contentDescription = "Remove scan", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isFrench) "Supprimer" else "Delete", color = Color(0xFFDC2626), fontSize = 12.sp)
          }
        }
      }

      AnimatedVisibility(visible = isCompressing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = theme.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isFrench) "Compression optimale..." else "Compressing image...",
            style = MaterialTheme.typography.labelSmall,
            color = theme.accent
          )
        }
      }

      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = title,
          modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.06f))
            .border(1.dp, theme.border, RoundedCornerShape(10.dp)),
          contentScale = ContentScale.Fit
        )

        OutlinedButton(
          onClick = onAutoReadClick,
          enabled = !isReadingOcr && !isCompressing,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("wallet_ocr_read_slot_button"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.45f))
        ) {
          Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFrench) "Lire les infos automatiquement (OCR)" else "Auto-Read Info from Scan (OCR)",
            style = MaterialTheme.typography.labelMedium,
            color = theme.accent,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onCameraClick,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (bitmap != null) {
              if (isFrench) "Reprendre" else "Retake"
            } else {
              if (isFrench) "Caméra" else "Camera"
            },
            style = MaterialTheme.typography.labelSmall,
            color = theme.primaryText
          )
        }

        OutlinedButton(
          onClick = onGalleryClick,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (isFrench) "Galerie" else "Gallery",
            style = MaterialTheme.typography.labelSmall,
            color = theme.primaryText
          )
        }
      }
    }
  }
}
