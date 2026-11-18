package com.example.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class ExtractedWalletCardInfo(
  val rawText: String,
  val suggestedTitle: String = "",
  val suggestedCategory: String = "",
  val cardNumber: String = "",
  val holderName: String = "",
  val expiryDate: String = "",
  val notes: String = ""
) {
  val extractedFieldCount: Int
    get() = listOf(suggestedTitle, cardNumber, holderName, expiryDate, notes).count { it.isNotBlank() }
}

/**
 * Offline on-device document & card reader powered by ML Kit Text Recognition
 * plus structured extraction for IDs, insurance cards, health cards, and memberships.
 */
object WalletCardOcrParser {

  suspend fun scanBitmap(bitmap: Bitmap, isFrench: Boolean): ExtractedWalletCardInfo {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
      val image = InputImage.fromBitmap(bitmap, 0)
      val result = recognizer.process(image).await()
      parseRecognizedText(result.text, isFrench)
    } finally {
      recognizer.close()
    }
  }

  fun parseRecognizedText(rawText: String, isFrench: Boolean): ExtractedWalletCardInfo {
    val lines = rawText
      .lines()
      .map { it.trim().replace(Regex("\\s+"), " ") }
      .filter { it.isNotBlank() }

    if (lines.isEmpty()) {
      return ExtractedWalletCardInfo(rawText = rawText)
    }

    val fullText = lines.joinToString("\n")
    val category = detectCategory(fullText, isFrench)
    val cardNumber = extractCardOrIdNumber(lines, fullText)
    val expiryDate = extractExpiryDate(fullText)
    val holderName = extractHolderName(lines)
    val title = detectTitle(lines, fullText, category, isFrench)
    val notes = extractSupplementalNotes(lines, fullText, holderName, cardNumber, isFrench)

    return ExtractedWalletCardInfo(
      rawText = rawText,
      suggestedTitle = title,
      suggestedCategory = category,
      cardNumber = cardNumber,
      holderName = holderName,
      expiryDate = expiryDate,
      notes = notes
    )
  }

  private fun detectCategory(fullText: String, isFrench: Boolean): String {
    val lower = fullText.lowercase()
    return when {
      lower.contains("driver") || lower.contains("license") || lower.contains("identification") ||
        lower.contains("class id") || lower.contains("state id") || lower.contains("new york") ||
        lower.contains("permis") || lower.contains("identit") || lower.contains("passeport") ||
        lower.contains("passport") || lower.contains("république") || lower.contains("temp. visitor") ||
        Regex("\\bdob\\b").containsMatchIn(lower) -> {
        if (isFrench) "Identité" else "ID / License"
      }

      lower.contains("carte vitale") || lower.contains("ameli") || lower.contains("sécurité sociale") ||
        lower.contains("ramq") || lower.contains("rxbin") || lower.contains("rxpcn") ||
        lower.contains("pharmacie") || lower.contains("pharmacy") || lower.contains("hôpital") ||
        lower.contains("hospital") || lower.contains("clinic") -> {
        if (isFrench) "Santé" else "Health"
      }

      lower.contains("insurance") || lower.contains("assurance") || lower.contains("mutuelle") ||
        lower.contains("policy") || lower.contains("police") || lower.contains("subscriber") ||
        lower.contains("member id") || lower.contains("aetna") || lower.contains("cigna") ||
        lower.contains("unitedhealth") || lower.contains("bluecross") || lower.contains("axa") ||
        lower.contains("allianz") || lower.contains("group #") -> {
        if (isFrench) "Assurance" else "Insurance"
      }

      lower.contains("visa") || lower.contains("mastercard") || lower.contains("american express") ||
        lower.contains("amex") || lower.contains("debit") || lower.contains("credit") ||
        lower.contains("valid thru") || lower.contains("iban") -> {
        if (isFrench) "Bancaire" else "Payment"
      }

      lower.contains("member") || lower.contains("membre") || lower.contains("club") ||
        lower.contains("rewards") || lower.contains("loyalty") || lower.contains("fidélité") -> {
        if (isFrench) "Membre" else "Membership"
      }

      else -> if (isFrench) "Identité" else "ID / License"
    }
  }

  private fun detectTitle(
    lines: List<String>,
    fullText: String,
    category: String,
    isFrench: Boolean
  ): String {
    val lower = fullText.lowercase()
    return when {
      lower.contains("new york") && (lower.contains("driver") || lower.contains("permis")) ->
        if (isFrench) "Permis New York" else "New York Driver License"
      lower.contains("new york") ->
        if (isFrench) "Carte d'Identité NY" else "New York State ID"
      lower.contains("driver") && lower.contains("license") ->
        if (isFrench) "Permis de Conduire" else "Driver License"
      lower.contains("permis de conduire") ->
        if (isFrench) "Permis de Conduire" else "Driver License"
      lower.contains("carte nationale") || lower.contains("république française") ->
        if (isFrench) "Carte d'Identité" else "National ID Card"
      lower.contains("passport") || lower.contains("passeport") ->
        if (isFrench) "Passeport" else "Passport"
      lower.contains("carte vitale") -> "Carte Vitale"
      lower.contains("mutuelle") -> "Mutuelle Santé"
      lower.contains("ramq") -> "Carte RAMQ"
      lower.contains("aetna") -> "Aetna Insurance"
      lower.contains("bluecross") || lower.contains("blue cross") -> "BlueCross Insurance"
      lower.contains("cigna") -> "Cigna Insurance"
      lower.contains("unitedhealth") -> "UnitedHealthcare"
      lower.contains("visa") -> "Visa"
      lower.contains("mastercard") -> "Mastercard"
      else -> {
        val brandLine = lines.firstOrNull { line ->
          line.length in 4..34 &&
            line.any { it.isLetter() } &&
            !Regex("^(id|dl|dob|exp|expires|issued|class)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) &&
            !Regex("\\d{2}/\\d{2}/\\d{2,4}").containsMatchIn(line)
        }
        brandLine ?: when (category) {
          "Assurance", "Insurance" -> if (isFrench) "Carte d'Assurance" else "Insurance Card"
          "Santé", "Health" -> if (isFrench) "Carte Santé" else "Health Card"
          "Bancaire", "Payment" -> if (isFrench) "Carte Bancaire" else "Payment Card"
          "Membre", "Membership" -> if (isFrench) "Carte Membre" else "Membership Card"
          else -> if (isFrench) "Pièce d'Identité" else "ID Card"
        }
      }
    }
  }

  private fun extractCardOrIdNumber(lines: List<String>, fullText: String): String {
    // 1. Labeled ID / Policy / Member / DL / Card numbers (must contain digits so "Class ID" is ignored)
    val labeledPatterns = listOf(
      Regex(
        """(?i)(?:^|\b)(?:id\s*(?:no|num|#|number)?|dl\s*(?:no|#)?|dln|member\s*id|subscriber\s*id|policy\s*(?:no|#|number)?|police\s*(?:n°|no)?|n°\s*carte|num[eé]ro|card\s*(?:no|#)|cin)\s*[:.#\-]?\s*([A-Z0-9][A-Z0-9\-\s]{4,24})"""
      ),
      Regex(
        """(?im)^\s*(?:id|dl)\s+([0-9][0-9\-\s]{5,22})$"""
      )
    )

    for (line in lines) {
      if (line.lowercase().startsWith("class ")) continue
      for (pattern in labeledPatterns) {
        val match = pattern.find(line) ?: continue
        val candidate = cleanNumberCandidate(match.groupValues[1])
        if (isValidIdOrCardCandidate(candidate)) {
          return candidate
        }
      }
    }

    for (pattern in labeledPatterns) {
      val match = pattern.find(fullText) ?: continue
      val candidate = cleanNumberCandidate(match.groupValues[1])
      if (isValidIdOrCardCandidate(candidate)) {
        return candidate
      }
    }

    // 2. Payment card 13-19 digit sequence (grouped or contiguous)
    val cardDigitMatch = Regex("""\b(?:\d{4}[\s\-]?){3}\d{3,4}\b""").find(fullText)
    if (cardDigitMatch != null) {
      return cardDigitMatch.value.trim().replace(Regex("\\s+"), " ")
    }

    // 3. Standalone grouped numeric ID (e.g., "140 251 148" or SSN/Health ID groups)
    val groupedDigitsMatch = Regex("""\b\d{3,4}[\s\-]\d{3,4}[\s\-]\d{3,5}\b""").findAll(fullText)
    for (m in groupedDigitsMatch) {
      val candidate = m.value.trim()
      val digitsOnly = candidate.filter { it.isDigit() }
      // Avoid 10-digit US phone numbers with parentheses/leading 1 unless nothing else matches
      if (digitsOnly.length >= 8 && !candidate.contains("/")) {
        return candidate
      }
    }

    // 4. Long alphanumeric policy/ID token (e.g. "POL-9482-7731", "AB1234567")
    val tokenMatch = Regex("""\b(?:[A-Z]{1,4}\-?\d{5,12}|\d{8,16})\b""").find(fullText)
    if (tokenMatch != null) {
      val candidate = tokenMatch.value.trim()
      if (!candidate.contains("/")) {
        return candidate
      }
    }

    return ""
  }

  private fun cleanNumberCandidate(raw: String): String {
    var s = raw.trim()
    // Strip trailing words like "Class", "Exp", "DOB", "Issued"
    s = s.replace(Regex("""(?i)\b(?:class|exp|expires|dob|issued|valid|name|nom)\b.*$"""), "").trim()
    return s.replace(Regex("\\s+"), " ").trimEnd('-', ':', '.', ',')
  }

  private fun isValidIdOrCardCandidate(candidate: String): Boolean {
    val digits = candidate.count { it.isDigit() }
    if (digits < 5) return false
    if (candidate.contains("/")) return false
    // Reject obvious ZIP+4 or standalone 5-digit ZIP codes
    if (candidate.length == 5 && digits == 5) return false
    return true
  }

  private fun extractExpiryDate(fullText: String): String {
    val labeledExpiry = Regex(
      """(?i)(?:expires?|exp\.?|valid\s*(?:thru|until)|valable\s*jusqu['’]?au|date\s*d['’]?expiration)\s*[:.\-]?\s*(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}|\d{1,2}[/\-.]\d{2,4}|\d{4}[/\-.]\d{2}[/\-.]\d{2})"""
    )
    val match = labeledExpiry.find(fullText)
    if (match != null) {
      return match.groupValues[1].trim()
    }
    return ""
  }

  private fun extractHolderName(lines: List<String>): String {
    // 1. Explicit label
    val labeledName = Regex(
      """(?i)^(?:name|nom(?:\s+et\s+pr[eé]nom)?|holder|titulaire|member\s*name|subscriber|patient)\s*[:.\-]\s*(.+)$"""
    )
    for (line in lines) {
      val match = labeledName.find(line)
      if (match != null) {
        val cleaned = match.groupValues[1].trim()
        if (cleaned.length in 3..42 && cleaned.none { it.isDigit() }) {
          return cleaned
        }
      }
    }

    // 2. Uppercase person name heuristic (handles 1-line "TRAORE FATOUMATA" or 2-line "TRAORE" / "FATOUMATA")
    val boilerplateWords = setOf(
      "NEW", "YORK", "STATE", "USA", "UNITED", "STATES", "IDENTIFICATION", "CARD", "DRIVER",
      "LICENSE", "PERMIS", "CONDUIRE", "CARTE", "NATIONALE", "IDENTITE", "PASSPORT", "PASSEPORT",
      "REPUBLIQUE", "FRANCAISE", "CLASS", "TEMP", "VISITOR", "EXPIRES", "EXP", "DOB", "ISSUED",
      "SEX", "EYES", "HGT", "HEIGHT", "WEIGHT", "END", "REST", "NONE", "DONOR", "ORGAN",
      "INSURANCE", "ASSURANCE", "HEALTH", "SANTE", "MUTUELLE", "VITALE", "MEMBER", "POLICY",
      "GROUP", "SUBSCRIBER", "PHARMACY", "PHARMACIE", "VALID", "THRU", "SIGNATURE", "FLUSHING",
      "NY", "ST", "AVE", "BLVD", "RD", "DR", "APT", "JUL", "JAN", "FEB", "MAR", "APR", "MAY",
      "JUN", "AUG", "SEP", "OCT", "NOV", "DEC"
    )

    fun isPureNameWord(token: String): Boolean {
      if (token.length < 2) return false
      if (token.any { it.isDigit() }) return false
      if (!token.all { it.isLetter() || it == '-' || it == '\'' }) return false
      if (token.uppercase() in boilerplateWords) return false
      return true
    }

    val nameLineCandidates = mutableListOf<Pair<Int, String>>()
    lines.forEachIndexed { idx, line ->
      val trimmed = line.trim()
      if (trimmed.length in 3..34 && trimmed.none { it.isDigit() }) {
        val upper = trimmed.uppercase()
        val words = upper.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isNotEmpty() && words.size <= 4 && words.all { isPureNameWord(it) } && trimmed == upper) {
          nameLineCandidates.add(idx to trimmed)
        }
      }
    }

    // Prefer consecutive uppercase name lines (e.g. Last Name line followed by First Name line)
    for (i in 0 until nameLineCandidates.size - 1) {
      val (idx1, line1) = nameLineCandidates[i]
      val (idx2, line2) = nameLineCandidates[i + 1]
      if (idx2 == idx1 + 1 && !line1.contains(" ") && !line2.contains(" ")) {
        return "$line1 $line2"
      }
    }

    // Or single multi-word uppercase name line
    val multiWord = nameLineCandidates.firstOrNull { it.second.contains(" ") }
    if (multiWord != null) {
      return multiWord.second
    }

    return ""
  }

  private fun extractSupplementalNotes(
    lines: List<String>,
    fullText: String,
    holderName: String,
    cardNumber: String,
    isFrench: Boolean
  ): String {
    val parts = mutableListOf<String>()

    Regex("""(?i)\b(?:dob|né\(e\)\s*le|birth)\s*[:.\-]?\s*(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4})""")
      .find(fullText)?.let {
        val label = if (isFrench) "Né(e)" else "DOB"
        parts.add("$label: ${it.groupValues[1].trim()}")
      }

    Regex("""(?i)\b(?:issued|délivrée?\s*le)\s*[:.\-]?\s*(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4})""")
      .find(fullText)?.let {
        val label = if (isFrench) "Émis" else "Issued"
        parts.add("$label: ${it.groupValues[1].trim()}")
      }

    Regex("""(?i)\b(?:group|grp)\s*(?:no|#|n°)?\s*[:.\-]?\s*([A-Z0-9\-]{3,16})""")
      .find(fullText)?.let {
        parts.add("Group: ${it.groupValues[1].trim()}")
      }

    Regex("""(?i)\b(?:rxbin|bin)\s*[:.\-]?\s*(\d{6})""")
      .find(fullText)?.let {
        parts.add("BIN: ${it.groupValues[1].trim()}")
      }

    // Address heuristic (street line starting with number + street word, optionally plus next city/zip line)
    val addressIdx = lines.indexOfFirst { line ->
      Regex("""^\d{1,5}\s+[A-Za-z0-9\s.'\-]+(?:ST|AVE|RD|BLVD|DR|LN|WAY|CT|PL|RUE|AVENUE|BD)\b""", RegexOption.IGNORE_CASE)
        .containsMatchIn(line)
    }
    if (addressIdx >= 0) {
      val street = lines[addressIdx]
      val cityLine = lines.getOrNull(addressIdx + 1)
        ?.takeIf { next ->
          next != holderName &&
            next != cardNumber &&
            Regex("""(?:[A-Z]{2}\s+\d{5}|\d{5}\s+[A-Za-z])""").containsMatchIn(next)
        }
      val fullAddr = if (cityLine != null) "$street, $cityLine" else street
      parts.add(fullAddr)
    }

    // Phone number if present (helpful for insurance cards)
    Regex("""(?:\+?1[\s\-.]?)?\(?\d{3}\)?[\s\-.]\d{3}[\s\-.]\d{4}""")
      .find(fullText)?.let {
        val tel = it.value.trim()
        if (tel != cardNumber) {
          parts.add("Tel: $tel")
        }
      }

    return parts.distinct().joinToString(" • ").take(160)
  }
}
