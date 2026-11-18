package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

data class ExtractedContactInfo(
  val rawText: String,
  val name: String = "",
  val providerType: String = "",
  val affiliation: String = "",
  val phone: String = "",
  val emergencyPhone: String = "",
  val email: String = "",
  val address: String = "",
  val website: String = "",
  val notes: String = "",
  val category: String = "MEDICAL"
) {
  val extractedFieldCount: Int
    get() = listOf(name, providerType, affiliation, phone, email, address, website).count { it.isNotBlank() }
}

/**
 * On-device OCR scanner using ML Kit Text Recognition to extract
 * business card & clinical contacts into structured fields.
 */
object BusinessCardOcrParser {

  private val EMAIL_PATTERN = Pattern.compile(
    "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
    Pattern.CASE_INSENSITIVE
  )

  private val PHONE_PATTERN = Pattern.compile(
    "(?:(?:\\+?1[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4})|(?:(?:\\+33|0)[1-9](?:[-.\\s]?\\d{2}){4})|\\b\\d{3}[-.\\s]\\d{3}[-.\\s]\\d{4}\\b"
  )

  private val URL_PATTERN = Pattern.compile(
    "(?:https?://)?(?:www\\.)?[a-zA-Z0-9.-]+\\.(?:com|org|net|edu|gov|fr|io|us)(?:/[^\\s]*)?",
    Pattern.CASE_INSENSITIVE
  )

  suspend fun scanUri(context: Context, uri: Uri, isFrench: Boolean): ExtractedContactInfo =
    withContext(Dispatchers.IO) {
      val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
      try {
        val inputImage = try {
          InputImage.fromFilePath(context, uri)
        } catch (_: Exception) {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
            InputImage.fromBitmap(bitmap, 0)
          } ?: throw IllegalStateException("Cannot load image from Uri")
        }

        val visionText = recognizer.process(inputImage).await()
        parseText(visionText.text, isFrench)
      } finally {
        recognizer.close()
      }
    }

  suspend fun scanBitmap(bitmap: Bitmap, isFrench: Boolean): ExtractedContactInfo =
    withContext(Dispatchers.IO) {
      val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
      try {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(inputImage).await()
        parseText(visionText.text, isFrench)
      } finally {
        recognizer.close()
      }
    }

  fun parseText(rawText: String, isFrench: Boolean): ExtractedContactInfo {
    val cleanLines = rawText
      .lines()
      .map { it.trim().replace(Regex("\\s+"), " ") }
      .filter { it.isNotBlank() }

    if (cleanLines.isEmpty()) {
      return ExtractedContactInfo(rawText = rawText)
    }

    var extractedEmail = ""
    var extractedWebsite = ""
    var extractedPhone = ""
    var extractedEmergencyPhone = ""
    val addressParts = mutableListOf<String>()
    var detectedRole = ""
    var detectedAffiliation = ""
    var candidateName = ""
    val remainingLines = mutableListOf<String>()

    for (line in cleanLines) {
      // 1. Email detection
      val emailMatcher = EMAIL_PATTERN.matcher(line)
      if (emailMatcher.find() && extractedEmail.isBlank()) {
        extractedEmail = emailMatcher.group().lowercase()
      }

      // 2. URL / Website detection
      val urlMatcher = URL_PATTERN.matcher(line)
      if (urlMatcher.find() && extractedWebsite.isBlank()) {
        val candidateUrl = urlMatcher.group()
        if (!candidateUrl.contains("@")) {
          extractedWebsite = if (candidateUrl.startsWith("http://") || candidateUrl.startsWith("https://")) {
            candidateUrl
          } else {
            "https://$candidateUrl"
          }
        }
      }

      // 3. Phone detection
      val phoneMatcher = PHONE_PATTERN.matcher(line)
      if (phoneMatcher.find()) {
        val foundPhone = phoneMatcher.group()
        val lower = line.lowercase()
        if (lower.contains("fax")) {
          // Skip or ignore fax numbers
        } else if (lower.contains("emerg") || lower.contains("urgenc") || lower.contains("direct") || lower.contains("garde")) {
          if (extractedEmergencyPhone.isBlank()) extractedEmergencyPhone = foundPhone
        } else if (extractedPhone.isBlank()) {
          extractedPhone = foundPhone
        } else if (extractedEmergencyPhone.isBlank() && foundPhone != extractedPhone) {
          extractedEmergencyPhone = foundPhone
        }
      }

      // 4. Address detection
      val lower = line.lowercase()
      val isAddressLine = lower.contains("ave") || lower.contains("avenue") ||
        lower.contains("street") || lower.contains(" st.") || lower.contains("boulevard") ||
        lower.contains("blvd") || lower.contains("suite") || lower.contains("floor") ||
        lower.contains("new york") || lower.contains("ny 100") || lower.contains("route") ||
        Regex("\\b\\d{1,5}\\s+[a-zA-Z]").containsMatchIn(line) ||
        Regex("\\b\\d{5}\\b").containsMatchIn(line)

      if (isAddressLine && !line.contains("@") && !line.startsWith("www.") && !line.startsWith("http")) {
        addressParts.add(line)
      } else {
        remainingLines.add(line)
      }
    }

    // 5. Detect Medical Specialty or Role
    val roleKeywords = listOf(
      "Endocrinolog", "Gynécolog", "Gynecolog", "OB/GYN", "OBGYN", "Obstétri", "Obstetric",
      "Embryolog", "Reproductive", "Fertility", "Specialist", "Spécialiste",
      "Surgeon", "Chirurgien", "Pédiatre", "Pediatric", "Sage-Femme", "Midwife",
      "Pharmac", "Nurse", "Infirmière", "Anesthési", "Anesthesi", "Director", "Directeur",
      "Advocate", "Coordinat", "Médecin Référent", "Médecin", "Doctor"
    )

    for (line in remainingLines) {
      if (detectedRole.isBlank()) {
        for (kw in roleKeywords) {
          if (line.contains(kw, ignoreCase = true)) {
            detectedRole = line
            break
          }
        }
      }
    }

    // 6. Detect Affiliation / Clinic
    val clinicKeywords = listOf(
      "RMA", "Reproductive Medicine", "Hospital", "Hôpital", "Mount Sinai", "NYU",
      "Surgery Center", "Ambulatory", "Center for", "Centre de", "Clinic", "Clinique",
      "Pharmacy", "Pharmacie", "Laboratoire", "Laboratory", "Health", "Santé", "Progyny",
      "Medical Center", "Maternité"
    )

    for (line in remainingLines) {
      if (detectedAffiliation.isBlank() && line != detectedRole) {
        for (kw in clinicKeywords) {
          if (line.contains(kw, ignoreCase = true)) {
            detectedAffiliation = line
            break
          }
        }
      }
    }

    // 7. Detect Name (Look for Doctor prefix or first sensible title)
    for (line in remainingLines) {
      if (line != detectedRole && line != detectedAffiliation) {
        if (line.contains("Dr.", ignoreCase = true) || line.contains("Dr ", ignoreCase = true) ||
          line.contains("Docteur", ignoreCase = true) || line.contains("MD", ignoreCase = true) ||
          line.contains("PhD", ignoreCase = true)
        ) {
          candidateName = line
          break
        }
      }
    }

    if (candidateName.isBlank()) {
      // Pick the first line that is neither phone, email, address, role, nor affiliation
      for (line in remainingLines) {
        if (line != detectedRole && line != detectedAffiliation &&
          !line.contains("@") && !line.contains("http") && !line.contains("www.")
        ) {
          // If it has at least 3 chars and is not all numbers
          if (line.length >= 3 && !line.matches(Regex("^[0-9+()\\s.-]+$"))) {
            candidateName = line
            break
          }
        }
      }
    }

    // 8. Determine Category
    val fullLower = rawText.lowercase()
    val category = when {
      fullLower.contains("pharmacy") || fullLower.contains("pharmacie") || fullLower.contains("rx") -> "PHARMACY"
      fullLower.contains("surgery") || fullLower.contains("hospital") || fullLower.contains("hôpital") || fullLower.contains("clinic") || fullLower.contains("clinique") -> "CLINIC"
      fullLower.contains("insurance") || fullLower.contains("assurance") || fullLower.contains("progyny") -> "INSURANCE"
      fullLower.contains("family") || fullLower.contains("famille") || fullLower.contains("conjoint") -> "FAMILY"
      else -> "MEDICAL"
    }

    val finalAddress = addressParts.joinToString(", ")
    val fullNotes = cleanLines
      .filter { it != candidateName && it != detectedRole && it != detectedAffiliation && !it.contains(extractedPhone) && !it.contains(extractedEmail) && it !in addressParts }
      .take(3)
      .joinToString(" • ")

    return ExtractedContactInfo(
      rawText = rawText,
      name = candidateName.take(120),
      providerType = detectedRole.ifBlank { if (isFrench) "Médecin / Spécialiste" else "Doctor / Specialist" }.take(60),
      affiliation = detectedAffiliation.take(120),
      phone = extractedPhone,
      emergencyPhone = extractedEmergencyPhone,
      email = extractedEmail,
      address = finalAddress.take(200),
      website = extractedWebsite,
      notes = fullNotes.take(300),
      category = category
    )
  }
}
