package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemePalette(val displayNameEn: String, val displayNameFr: String) {
  CARAMEL("1. Warm Ivory & Terracotta", "1. Ivoire Chaud & Terre Cuite"),
  MARRON("2. Chestnut & Cream", "2. Châtaigne & Crème"),
  ROSE("3. Rose Quartz & Blush", "3. Quartz Rose & Poudré"),
  SAUGE("4. Botanical Sage", "4. Sauge Botanique"),
  LAVANDE("5. Soft Lavender", "5. Lavande Douce"),
  SUNSET("6. Peach & Coral Glow", "6. Pêche & Corail Doux"),
  SLATE("7. Pure Minimal", "7. Minimaliste Épuré"),
  OCEAN("8. Sky & Coastal Mint", "8. Ciel & Menthe Douce")
}

data class ThemeColors(
  val background: Color,
  val cardSurface: Color,
  val headerFooterBg: Color,
  val primaryText: Color,
  val mutedText: Color,
  val border: Color,
  val accent: Color
)

data class CategoryBadgeStyle(
  val containerColor: Color,
  val contentColor: Color
)

/**
 * Provides soft, airy pastel badge styling for categories across cards and filter pills
 * while adapting cleanly to light/warm palettes.
 */
fun getCategoryBadgeColors(categoryKey: String, isDarkAccent: Boolean = false): CategoryBadgeStyle {
  val lower = categoryKey.lowercase()
  return when {
    lower.contains("prescri") || lower.contains("ordonn") || lower.contains("med") || lower.contains("avoid") || lower.contains("évit") -> CategoryBadgeStyle(
      containerColor = Color(0xFFFDECE9),
      contentColor = Color(0xFFB54938)
    )
    lower.contains("baby") || lower.contains("bébé") || lower.contains("safe") || lower.contains("sûr") || lower.contains("eat") || lower.contains("sain") -> CategoryBadgeStyle(
      containerColor = Color(0xFFEAF6EE),
      contentColor = Color(0xFF2D6E48)
    )
    lower.contains("mom") || lower.contains("maman") || lower.contains("caution") || lower.contains("prud") || lower.contains("limit") -> CategoryBadgeStyle(
      containerColor = Color(0xFFFFF4E5),
      contentColor = Color(0xFFA85D17)
    )
    lower.contains("reimburs") || lower.contains("rembours") || lower.contains("paid") -> CategoryBadgeStyle(
      containerColor = Color(0xFFE9F4FC),
      contentColor = Color(0xFF23668E)
    )
    lower.contains("insurance") || lower.contains("copay") || lower.contains("assur") -> CategoryBadgeStyle(
      containerColor = Color(0xFFF1EDFC),
      contentColor = Color(0xFF5A459E)
    )
    else -> CategoryBadgeStyle(
      containerColor = Color(0xFFF5EFE8),
      contentColor = Color(0xFF6B5344)
    )
  }
}

fun getThemeColors(palette: AppThemePalette): ThemeColors {
  return when (palette) {
    AppThemePalette.CARAMEL -> ThemeColors(
      background = Color(0xFFFAF8F5),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFFAF6F0),
      primaryText = Color(0xFF433328),
      mutedText = Color(0xFF8C7768),
      border = Color(0xFFEDE4D9),
      accent = Color(0xFFD2774B) // Warm terracotta coral
    )
    AppThemePalette.MARRON -> ThemeColors(
      background = Color(0xFFFAF7F4),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFF7F0E9),
      primaryText = Color(0xFF423024),
      mutedText = Color(0xFF877163),
      border = Color(0xFFE9DED3),
      accent = Color(0xFFA5633B)
    )
    AppThemePalette.ROSE -> ThemeColors(
      background = Color(0xFFFDF9FA),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFFBF2F4),
      primaryText = Color(0xFF4B373C),
      mutedText = Color(0xFF967C83),
      border = Color(0xFFF1E1E4),
      accent = Color(0xFFD86C82)
    )
    AppThemePalette.SAUGE -> ThemeColors(
      background = Color(0xFFF7FAF7),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFF0F5F0),
      primaryText = Color(0xFF334335),
      mutedText = Color(0xFF728674),
      border = Color(0xFFE0E8E0),
      accent = Color(0xFF5A8A62)
    )
    AppThemePalette.LAVANDE -> ThemeColors(
      background = Color(0xFFF9F8FD),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFF3F1FC),
      primaryText = Color(0xFF3F3B54),
      mutedText = Color(0xFF7F7A9C),
      border = Color(0xFFE6E3F7),
      accent = Color(0xFF7E71BF)
    )
    AppThemePalette.SUNSET -> ThemeColors(
      background = Color(0xFFFDF8F5),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFFCF0E9),
      primaryText = Color(0xFF4A342C),
      mutedText = Color(0xFF947568),
      border = Color(0xFFF2DFD5),
      accent = Color(0xFFDF724E)
    )
    AppThemePalette.SLATE -> ThemeColors(
      background = Color(0xFFF8FAFC),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFF1F5F9),
      primaryText = Color(0xFF273444),
      mutedText = Color(0xFF64748B),
      border = Color(0xFFE2E8F0),
      accent = Color(0xFF4E657E)
    )
    AppThemePalette.OCEAN -> ThemeColors(
      background = Color(0xFFF6FAFC),
      cardSurface = Color(0xFFFFFFFF),
      headerFooterBg = Color(0xFFEEF6F9),
      primaryText = Color(0xFF2A414D),
      mutedText = Color(0xFF66808E),
      border = Color(0xFFDCE9F0),
      accent = Color(0xFF3D8DAB)
    )
  }
}

// Backward compatibility objects for legacy screens
object SoftChalkTheme {
  val background = Color(0xFFFFFFFF)
  val cardSurface = Color(0xFFF8FAFC)
  val primaryText = Color(0xFF475569)
  val mutedText = Color(0xFF94A3B8)
  val border = Color(0xFFE2E8F0)
  val accent = Color(0xFF38BDF8)
}

object CoolTitaniumTheme {
  val background = Color(0xFFF3F4F6)
  val cardSurface = Color(0xFFFFFFFF)
  val primaryText = Color(0xFF4B5563)
  val mutedText = Color(0xFF9CA3AF)
  val border = Color(0xFFE5E7EB)
  val accent = Color(0xFF818CF8)
}
