package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val APP_CHECK_TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

/**
 * Configures Firebase App Check before any Firebase service calls.
 * Must run synchronously in MainActivity.onCreate() before Firebase.ai / Firebase.firestore / Firebase.auth access.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(APP_CHECK_TAG, "App Check debug secret configured.")
            } else {
                Log.w(APP_CHECK_TAG, "No debug token found in Intent extra.")
            }
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            val siteKey = if (resId != 0) context.getString(resId) else ""
            if (siteKey.isNotBlank()) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
            } else {
                Log.w(APP_CHECK_TAG, "recaptcha_site_key string resource missing or blank. Falling back to Debug provider factory.")
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            }
        }
    } catch (e: Exception) {
        Log.e(APP_CHECK_TAG, "Failed to initialize Firebase App Check", e)
    }
}

sealed interface VoiceIntentAction {
    data class AddExpense(
        val title: String,
        val amount: Double,
        val currency: String,
        val category: String,
        val notes: String
    ) : VoiceIntentAction

    data class AddNote(
        val title: String,
        val content: String,
        val category: String
    ) : VoiceIntentAction

    data class AddAppointment(
        val title: String,
        val dateStr: String,
        val timeStr: String,
        val location: String
    ) : VoiceIntentAction

    data class MarkMedDose(
        val period: String, // "AM" or "PM"
        val medicationHint: String
    ) : VoiceIntentAction

    data class AnswerOnly(
        val reply: String
    ) : VoiceIntentAction
}

object AiAssistantService {
    private const val MODEL_NAME = "gemini-3.8-flash"

    fun streamClinicalAnswer(
        prompt: String,
        systemContext: String,
        isFrench: Boolean
    ): Flow<String> = flow {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    if (isFrench) {
                        """
                        Tu es l'assistant clinique bienveillant de l'application B&F PARCOURS (suivi PMA / FIV / TEC et grossesse).
                        Contexte du dossier patient:
                        $systemContext
                        Réponds en français clair, structuré, rassurant et précis. Rappelle toujours de consulter l'équipe médicale RMA of New York / OB-GYN pour toute décision thérapeutique urgente.
                        """.trimIndent()
                    } else {
                        """
                        You are the compassionate clinical assistant inside B&F JOURNEY (IVF/FET protocol & pregnancy tracking app).
                        Patient chart context:
                        $systemContext
                        Respond in clear, structured, warm English. Always remind the user to confirm urgent medical changes with their RMA of New York / OB-GYN care team.
                        """.trimIndent()
                    }
                )
            }
        )

        withTimeout(60_000L) {
            model.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text
                if (!part.isNullOrEmpty()) {
                    emit(part)
                }
            }
        }
    }

    suspend fun parseVoiceCommand(
        transcript: String,
        systemContext: String,
        isFrench: Boolean
    ): VoiceIntentAction {
        val schema = Schema.obj(
            properties = mapOf(
                "actionType" to Schema.enumeration(
                    values = listOf("ADD_EXPENSE", "ADD_NOTE", "ADD_APPOINTMENT", "MARK_MED_DOSE", "ANSWER"),
                    description = "Intent detected in user voice command"
                ),
                "title" to Schema.string("Short concise title for expense, note, or appointment"),
                "amount" to Schema.double("Numeric expense amount if ADD_EXPENSE, otherwise 0"),
                "currency" to Schema.string("Currency code USD, EUR, or XOF"),
                "category" to Schema.string("Category name: Prescriptions, Baby Stuff, For the Mom, or Others"),
                "dateStr" to Schema.string("Date text e.g. 16 Nov 2026 if applicable"),
                "timeStr" to Schema.string("Time text e.g. 09:30 if applicable"),
                "period" to Schema.string("AM or PM for MARK_MED_DOSE"),
                "reply" to Schema.string("Helpful bilingual response or confirmation summary")
            ),
            optionalProperties = listOf("amount", "currency", "category", "dateStr", "timeStr", "period")
        )

        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            },
            systemInstruction = content {
                text(
                    """
                    Parse the user's voice request into one structured action or medical answer.
                    Patient context: $systemContext
                    If the user says they spent money or bought something, return ADD_EXPENSE.
                    If the user asks to note a symptom, feeling, or reminder, return ADD_NOTE.
                    If the user mentions scheduling a visit/ultrasound/blood draw, return ADD_APPOINTMENT.
                    If the user confirms they took their morning/evening pill or Estrace/Progesterone, return MARK_MED_DOSE.
                    Otherwise return ANSWER with a clear clinical response in ${if (isFrench) "French" else "English"}.
                    """.trimIndent()
                )
            }
        )

        val response = withTimeout(60_000L) {
            model.generateContent(transcript)
        }
        val rawJson = response.text ?: return VoiceIntentAction.AnswerOnly(
            if (isFrench) "Aucune réponse générée." else "No response generated."
        )
        val obj = JSONObject(rawJson)
        return when (obj.optString("actionType", "ANSWER")) {
            "ADD_EXPENSE" -> VoiceIntentAction.AddExpense(
                title = obj.optString("title").ifBlank { transcript.take(60) },
                amount = obj.optDouble("amount", 0.0).coerceAtLeast(0.0),
                currency = obj.optString("currency", "USD").uppercase().takeIf { it in listOf("USD", "EUR", "XOF") } ?: "USD",
                category = obj.optString("category", "Others"),
                notes = obj.optString("reply", "")
            )
            "ADD_NOTE" -> VoiceIntentAction.AddNote(
                title = obj.optString("title").ifBlank { if (isFrench) "Note vocale" else "Voice Note" },
                content = transcript,
                category = "clinical"
            )
            "ADD_APPOINTMENT" -> VoiceIntentAction.AddAppointment(
                title = obj.optString("title").ifBlank { transcript.take(60) },
                dateStr = obj.optString("dateStr").ifBlank { "16 Nov 2026" },
                timeStr = obj.optString("timeStr").ifBlank { "09:00" },
                location = "RMA of New York — East Side"
            )
            "MARK_MED_DOSE" -> VoiceIntentAction.MarkMedDose(
                period = if (obj.optString("period").equals("PM", ignoreCase = true)) "PM" else "AM",
                medicationHint = obj.optString("title")
            )
            else -> VoiceIntentAction.AnswerOnly(
                obj.optString("reply").ifBlank { transcript }
            )
        }
    }

    suspend fun parseReceiptWithGemini(
        bytes: ByteArray,
        mimeType: String,
        isFrench: Boolean
    ): ExtractedReceiptInfo {
        val schema = Schema.obj(
            properties = mapOf(
                "description" to Schema.string("Real provider/store name AND specific service/medication description, e.g. Progyny - S0122 MENOPUR 75IU VIAL"),
                "amountPaid" to Schema.double("Amount I Paid out-of-pocket by patient in USD, e.g. 30.00"),
                "insurancePaid" to Schema.double("Amount covered/adjusted by insurance, positive number, e.g. 3547.44"),
                "totalCharges" to Schema.double("Full billed charges before insurance, e.g. 3577.44"),
                "date" to Schema.string("Exact Date of Service or Billing Date printed on document, e.g. 25 Sep 2026 or 2026-09-25"),
                "category" to Schema.string("Category: Medical, Baby, Prescriptions, Baby Stuff, For the Mom, or Others"),
                "paymentMethod" to Schema.string("Payment card or method if present, e.g. MC 5928")
            ),
            optionalProperties = listOf("insurancePaid", "totalCharges", "category", "paymentMethod")
        )

        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            },
            systemInstruction = content {
                text(
                    """
                    You are an expert financial and medical receipt parser.
                    Extract exact values from the attached PDF or image document in USD:
                    - description: Real provider/store name + specific service/medication description (e.g. "Progyny - S0122 MENOPUR 75IU VIAL").
                    - amountPaid: Actual amount paid out-of-pocket by patient (e.g. 30.00 from Total Paid: $30.00).
                    - insurancePaid: Amount covered/adjusted by insurance. Convert parentheses like ($3547.44) into a positive number (3547.44). If not present, 0.0.
                    - totalCharges: Full billed charges before insurance (e.g. 3577.44 from Charges: $3577.44).
                    - date: Exact Date of Service or Billing Date printed on document (e.g. "25 Sep 2026" or "2026-09-25"). NEVER use today's date unless printed on document.
                    - category: "Medical" for medical bills/pharmacy/labs, "Baby" for baby gear/supplies, "Prescriptions", "Baby Stuff", "For the Mom", or "Others".
                    - paymentMethod: Payment card/method if present (e.g. "MC 5928").
                    """.trimIndent()
                )
            }
        )

        val isPdf = mimeType.contains("pdf", ignoreCase = true) || mimeType.contains("octet-stream", ignoreCase = true)
        val cleanMimeType = if (isPdf) "application/pdf" else mimeType

        val bitmap = if (isPdf) {
            runCatching {
                val tempFile = File.createTempFile("receipt", ".pdf")
                tempFile.writeBytes(bytes)
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val page = renderer.openPage(0)
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                tempFile.delete()
                bmp
            }.getOrNull()
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        val response = withTimeout(60_000L) {
            model.generateContent(
                content {
                    if (bitmap != null) {
                        image(bitmap)
                    } else {
                        inlineData(bytes, cleanMimeType)
                    }
                    text("Extract all receipt details into the JSON schema.")
                }
            )
        }

        val rawText = response.text ?: "{}"
        val cleanedJson = rawText
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        val obj = try {
            JSONObject(cleanedJson)
        } catch (e: Exception) {
            Log.e("AiAssistantService", "Failed to parse Gemini response as JSON: $rawText", e)
            JSONObject()
        }

        val description = obj.optString("description").ifBlank {
            obj.optString("title").ifBlank { "Progyny Statement / Receipt" }
        }
        val amount = when {
            obj.has("amountPaid") -> obj.optDouble("amountPaid", 0.0)
            obj.has("amount") -> obj.optDouble("amount", 0.0)
            else -> 0.0
        }.coerceAtLeast(0.0)

        val insurance = obj.optDouble("insurancePaid", 0.0).coerceAtLeast(0.0)

        val totalCharges = when {
            obj.has("totalCharges") -> obj.optDouble("totalCharges", 0.0)
            obj.has("totalBill") -> obj.optDouble("totalBill", 0.0)
            else -> 0.0
        }.coerceAtLeast(0.0)

        val dateStr = obj.optString("date").ifBlank {
            obj.optString("dateStr").ifBlank { "25 Sep 2026" }
        }

        val paymentMethod = obj.optString("paymentMethod", "")
        val category = obj.optString("category").ifBlank { "Medical" }

        return ExtractedReceiptInfo(
            title = description,
            amount = amount,
            insurancePaid = insurance,
            totalBill = totalCharges,
            dateStr = dateStr,
            paymentMethod = paymentMethod,
            category = category
        )
    }
}

data class ExtractedReceiptInfo(
    val title: String,
    val amount: Double,
    val insurancePaid: Double,
    val totalBill: Double,
    val dateStr: String,
    val paymentMethod: String,
    val category: String
) {
    val description: String get() = title
    val amountPaid: Double get() = amount
    val totalCharges: Double get() = totalBill
    val date: String get() = dateStr
}
