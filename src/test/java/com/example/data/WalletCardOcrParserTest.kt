package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletCardOcrParserTest {

  @Test
  fun parseRecognizedText_extractsNewYorkStateIdFieldsAccurately() {
    val sampleIdOcr = """
      NEW YORK STATE
      IDENTIFICATION CARD
      ID 140 251 148
      Class ID
      TRAORE
      FATOUMATA
      3420 137TH ST 10L
      FLUSHING, NY 11354
      TEMP. VISITOR Expires 11/07/2023
      DOB 07/27/1998
      Issued 11/07/2022
    """.trimIndent()

    val result = WalletCardOcrParser.parseRecognizedText(sampleIdOcr, isFrench = false)

    assertEquals("New York State ID", result.suggestedTitle)
    assertEquals("ID / License", result.suggestedCategory)
    assertEquals("140 251 148", result.cardNumber)
    assertEquals("TRAORE FATOUMATA", result.holderName)
    assertEquals("11/07/2023", result.expiryDate)
    assertTrue(result.notes.contains("DOB: 07/27/1998"))
    assertTrue(result.notes.contains("3420 137TH ST 10L"))
    assertTrue(result.extractedFieldCount >= 4)
  }

  @Test
  fun parseRecognizedText_extractsInsuranceCardInFrench() {
    val sampleInsuranceOcr = """
      MUTUELLE SANTÉ COMPLÉMENTAIRE
      Titulaire: TRAORE FATOUMATA
      Police N°: POL-9482-7731
      Group: GRP-4401
      Valable jusqu'au 31/12/2027
      Tel: (718) 555-0192
    """.trimIndent()

    val result = WalletCardOcrParser.parseRecognizedText(sampleInsuranceOcr, isFrench = true)

    assertEquals("Assurance", result.suggestedCategory)
    assertEquals("Mutuelle Santé", result.suggestedTitle)
    assertEquals("POL-9482-7731", result.cardNumber)
    assertEquals("TRAORE FATOUMATA", result.holderName)
    assertEquals("31/12/2027", result.expiryDate)
    assertTrue(result.notes.contains("Group: GRP-4401"))
    assertTrue(result.notes.contains("Tel: (718) 555-0192"))
  }

  @Test
  fun parseRecognizedText_extractsHealthCardAndNumber() {
    val sampleVitaleOcr = """
      CARTE VITALE
      Assurance Maladie
      Numéro: 2 98 07 75 114 231 45
      Nom: TRAORE FATOUMATA
    """.trimIndent()

    val result = WalletCardOcrParser.parseRecognizedText(sampleVitaleOcr, isFrench = true)

    assertEquals("Santé", result.suggestedCategory)
    assertEquals("Carte Vitale", result.suggestedTitle)
    assertEquals("2 98 07 75 114 231 45", result.cardNumber)
    assertEquals("TRAORE FATOUMATA", result.holderName)
  }

  @Test
  fun parseRecognizedText_handlesBlankInputGracefully() {
    val result = WalletCardOcrParser.parseRecognizedText("   \n  ", isFrench = false)
    assertEquals("", result.suggestedTitle)
    assertEquals("", result.cardNumber)
    assertEquals("", result.holderName)
    assertEquals(0, result.extractedFieldCount)
  }
}
