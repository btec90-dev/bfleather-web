package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportCategory(val labelFr: String, val labelEn: String) {
    PREGNANCY("Suivi Grossesse & Bébé", "Pregnancy & Baby"),
    PROTOCOL("Protocole PMA / FIV", "IVF / FET Protocol"),
    MEDICATIONS("Médicaments & Rappels", "Medications & Reminders"),
    DOCTORS("Médecins & Cliniques", "Doctors & Clinics"),
    EXPENSES("Dépenses & Factures", "Expenses & Receipts"),
    APPOINTMENTS("Rendez-vous & Tâches", "Appointments & Tasks"),
    NOTES("Notes & Mémos Vocaux", "Notes & Voice Memos"),
    WALLET("Cartes Assurance (Métadonnées)", "Wallet Cards (Metadata)")
}

object ExportCenterUtils {
    private fun exportDir(context: Context): File = File(context.cacheDir, "exports").apply {
        if (!exists()) mkdirs()
    }

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

    fun generatePdfReport(
        context: Context,
        categories: Set<ExportCategory>,
        isFrench: Boolean,
        profile: SyncedUserProfile,
        protocolSteps: List<SyncedProtocolStep>,
        medReminders: List<SyncedMedReminder>,
        pregnancyMeds: List<SyncedPregnancyMed>,
        careProviders: List<SyncedCareProvider>,
        expenses: List<SyncedExpenseRecord>,
        appointments: List<SyncedAppointmentItem>,
        tasks: List<TaskItem>,
        notes: List<SyncedNoteItem>,
        voiceMemos: List<SyncedVoiceMemo>,
        walletCards: List<SyncedWalletCard>
    ): File {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - margin * 2f

        val titlePaint = Paint().apply {
            color = Color.rgb(62, 42, 32)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(168, 92, 58)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(45, 42, 40)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(120, 112, 108)
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(225, 215, 205)
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = 48f

        fun newPage() {
            doc.finishPage(page)
            pageNum += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            y = 48f
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 44f) {
                newPage()
            }
        }

        fun drawWrapped(text: String, paint: Paint, indent: Float = 0f, spacing: Float = 14f) {
            val words = text.replace("\n", " ").split(" ").filter { it.isNotEmpty() }
            if (words.isEmpty()) return
            var line = StringBuilder()
            val maxW = contentWidth - indent
            for (w in words) {
                val candidate = if (line.isEmpty()) w else "$line $w"
                if (paint.measureText(candidate) > maxW) {
                    ensureSpace(spacing)
                    canvas.drawText(line.toString(), margin + indent, y, paint)
                    y += spacing
                    line = StringBuilder(w)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(spacing)
                canvas.drawText(line.toString(), margin + indent, y, paint)
                y += spacing
            }
        }

        fun drawSectionHeader(title: String) {
            ensureSpace(32f)
            y += 8f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 16f
            canvas.drawText(title, margin, y, sectionPaint)
            y += 16f
        }

        // Document Title
        val headerTitle = if (isFrench) "B&F PARCOURS — Dossier Médical & Grossesse" else "B&F JOURNEY — Medical & Pregnancy Dossier"
        canvas.drawText(headerTitle, margin, y, titlePaint)
        y += 18f
        val dateNow = SimpleDateFormat("dd MMM yyyy HH:mm", if (isFrench) Locale.FRENCH else Locale.US).format(Date())
        canvas.drawText(
            "${profile.patientName} • ${if (isFrench) "Exporté le" else "Exported"} $dateNow • ${if (isFrench) "Profil actif" else "Active profile"}: ${profile.activePartnerProfile}",
            margin,
            y,
            mutedPaint
        )
        y += 18f

        if (ExportCategory.PREGNANCY in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.PREGNANCY.labelFr else ExportCategory.PREGNANCY.labelEn)
            drawWrapped("Semaine / Week: ${profile.gestationalWeeks}s+${profile.gestationalDaysRemainder}j • ${profile.currentTrimester}", bodyPaint)
            drawWrapped("DPA / Due Date: ${profile.dueDate} • Weight: ${profile.healthWeight} kg • Tension: ${profile.bloodPressure}", bodyPaint)
            drawWrapped("Humeur / Mood: ${profile.healthMood} • Hydratation: ${profile.waterGlasses} verres/glasses", bodyPaint)
        }

        if (ExportCategory.PROTOCOL in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.PROTOCOL.labelFr else ExportCategory.PROTOCOL.labelEn)
            protocolSteps.forEach { step ->
                val statusIcon = if (step.isCompleted) "[✓]" else "[ ]"
                drawWrapped("$statusIcon Phase ${step.phaseId} • ${step.dateStr} — ${step.stepName} (${step.status})", bodyPaint)
                if (step.medication.isNotBlank() || step.details.isNotBlank()) {
                    drawWrapped("${step.medication} ${step.dosage} — ${step.details} [${step.addedBy}]", mutedPaint, indent = 12f, spacing = 12f)
                }
            }
        }

        if (ExportCategory.MEDICATIONS in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.MEDICATIONS.labelFr else ExportCategory.MEDICATIONS.labelEn)
            medReminders.forEach { med ->
                val time = String.format(Locale.US, "%02d:%02d", med.hour, med.minute)
                drawWrapped("• ${med.title} (${med.medication} ${med.dosage}) — $time | Pills: ${med.pillsRemaining} | AM:${med.dailyAmTaken} PM:${med.dailyPmTaken}", bodyPaint)
            }
            if (pregnancyMeds.isNotEmpty()) {
                drawWrapped(if (isFrench) "Base médicaments grossesse (${pregnancyMeds.size} entrées):" else "Pregnancy Meds DB (${pregnancyMeds.size} entries):", mutedPaint)
                pregnancyMeds.take(25).forEach { pm ->
                    drawWrapped("  - ${pm.name} [${pm.safetyLevel}] (${pm.activeIngredient}) — ${pm.clinicalNotes}", mutedPaint, indent = 8f, spacing = 12f)
                }
            }
        }

        if (ExportCategory.DOCTORS in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.DOCTORS.labelFr else ExportCategory.DOCTORS.labelEn)
            careProviders.forEach { docItem ->
                drawWrapped("• ${docItem.name} (${docItem.providerType}) — ${docItem.affiliation}", bodyPaint)
                drawWrapped("Tel: ${docItem.phone} | Emergency: ${docItem.emergencyContact} | ${docItem.address}", mutedPaint, indent = 12f, spacing = 12f)
            }
        }

        if (ExportCategory.EXPENSES in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.EXPENSES.labelFr else ExportCategory.EXPENSES.labelEn)
            val totalsByCurrency = expenses.groupBy { it.currency }.mapValues { (_, list) -> list.sumOf { it.amount } }
            val summary = totalsByCurrency.entries.joinToString(" | ") { "${it.key}: ${String.format(Locale.US, "%.2f", it.value)}" }
            drawWrapped("${if (isFrench) "Totaux" else "Totals"}: ${summary.ifBlank { "0.00 USD" }}", bodyPaint)
            expenses.forEach { exp ->
                drawWrapped("• ${exp.dateStr} — ${exp.title}: ${exp.amount} ${exp.currency} [${exp.category} / ${exp.paymentStatus}] (${exp.addedBy})", bodyPaint)
                if (exp.notes.isNotBlank()) drawWrapped(exp.notes, mutedPaint, indent = 12f, spacing = 12f)
            }
        }

        if (ExportCategory.APPOINTMENTS in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.APPOINTMENTS.labelFr else ExportCategory.APPOINTMENTS.labelEn)
            appointments.forEach { appt ->
                drawWrapped("• ${appt.dateStr} ${appt.timeStr} — ${appt.title} (${appt.doctor} @ ${appt.location}) [${appt.addedBy}]", bodyPaint)
                if (appt.clinicalNotes.isNotBlank()) drawWrapped(appt.clinicalNotes, mutedPaint, indent = 12f, spacing = 12f)
            }
            tasks.forEach { t ->
                val mark = if (t.isCompleted) "[✓]" else "[ ]"
                drawWrapped("$mark ${t.date} — ${t.title} (${t.category}) [${t.addedBy}]", bodyPaint)
            }
        }

        if (ExportCategory.NOTES in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.NOTES.labelFr else ExportCategory.NOTES.labelEn)
            notes.forEach { n ->
                drawWrapped("• [${n.timestampText}] ${n.title} (${n.addedBy}): ${n.content}", bodyPaint)
            }
            voiceMemos.forEach { vm ->
                drawWrapped("• 🎙 ${vm.title} (${vm.durationSec}s • ${vm.timestampText} • ${vm.addedBy})", bodyPaint)
            }
        }

        if (ExportCategory.WALLET in categories) {
            drawSectionHeader(if (isFrench) ExportCategory.WALLET.labelFr else ExportCategory.WALLET.labelEn)
            walletCards.forEach { card ->
                val masked = if (card.cardNumber.length >= 4) "•••• ${card.cardNumber.takeLast(4)}" else "••••"
                drawWrapped("• ${card.cardTitle} (${card.cardCategory}) — $masked | ${card.holderName} | Exp: ${card.expiryDate}", bodyPaint)
            }
        }

        doc.finishPage(page)
        val outFile = File(exportDir(context), "bf_parcours_export_${stamp()}.pdf")
        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()
        return outFile
    }

    fun generateCsvReport(
        context: Context,
        categories: Set<ExportCategory>,
        protocolSteps: List<SyncedProtocolStep>,
        expenses: List<SyncedExpenseRecord>,
        appointments: List<SyncedAppointmentItem>,
        tasks: List<TaskItem>,
        notes: List<SyncedNoteItem>,
        medReminders: List<SyncedMedReminder>
    ): File {
        fun esc(s: String): String = "\"${s.replace("\"", "\"\"")}\""
        val sb = StringBuilder()
        sb.appendLine("Type,Date,Title,CategoryOrPhase,AmountOrDosage,CurrencyOrStatus,AddedBy,Notes")

        if (ExportCategory.EXPENSES in categories) {
            expenses.forEach { e ->
                sb.appendLine("${esc("Expense")},${esc(e.dateStr)},${esc(e.title)},${esc("${e.category} / ${e.phase}")},${e.amount},${esc("${e.currency} (${e.paymentStatus})")},${esc(e.addedBy)},${esc(e.notes)}")
            }
        }
        if (ExportCategory.PROTOCOL in categories) {
            protocolSteps.forEach { s ->
                sb.appendLine("${esc("Protocol")},${esc(s.dateStr)},${esc(s.stepName)},${esc("Phase ${s.phaseId}")},${esc("${s.medication} ${s.dosage}")},${esc(s.status)},${esc(s.addedBy)},${esc(s.details)}")
            }
        }
        if (ExportCategory.APPOINTMENTS in categories) {
            appointments.forEach { a ->
                sb.appendLine("${esc("Appointment")},${esc("${a.dateStr} ${a.timeStr}")},${esc(a.title)},${esc(a.eventType)},${esc(a.doctor)},${esc(a.location)},${esc(a.addedBy)},${esc(a.clinicalNotes)}")
            }
            tasks.forEach { t ->
                sb.appendLine("${esc("Task")},${esc(t.date)},${esc(t.title)},${esc(t.category)},${esc(if (t.isCompleted) "Done" else "Pending")},${esc("")},${esc(t.addedBy)},${esc(t.details)}")
            }
        }
        if (ExportCategory.MEDICATIONS in categories) {
            medReminders.forEach { m ->
                sb.appendLine("${esc("MedReminder")},${esc(String.format(Locale.US, "%02d:%02d", m.hour, m.minute))},${esc(m.title)},${esc(m.protocolPhase)},${esc("${m.medication} ${m.dosage}")},${esc("Pills:${m.pillsRemaining}")},${esc("Fatou")},${esc(m.notes)}")
            }
        }
        if (ExportCategory.NOTES in categories) {
            notes.forEach { n ->
                sb.appendLine("${esc("Note")},${esc(n.timestampText)},${esc(n.title)},${esc(n.category)},${esc(n.mood)},${esc("")},${esc(n.addedBy)},${esc(n.content)}")
            }
        }

        val outFile = File(exportDir(context), "bf_parcours_data_${stamp()}.csv")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    fun buildBackupJson(
        profile: SyncedUserProfile,
        tasks: List<TaskItem>,
        notes: List<SyncedNoteItem>,
        appointments: List<SyncedAppointmentItem>,
        protocolSteps: List<SyncedProtocolStep>,
        careProviders: List<SyncedCareProvider>,
        expenses: List<SyncedExpenseRecord>,
        medReminders: List<SyncedMedReminder>,
        pregnancyMeds: List<SyncedPregnancyMed>,
        checklists: List<SyncedChecklistItem>
    ): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAtEpochMillis", System.currentTimeMillis())
        root.put("profile", JSONObject().apply {
            put("patientName", profile.patientName)
            put("dueDate", profile.dueDate)
            put("gestationalWeeks", profile.gestationalWeeks)
            put("gestationalDaysRemainder", profile.gestationalDaysRemainder)
            put("languageCode", profile.languageCode)
            put("themePaletteName", profile.themePaletteName)
            put("activePartnerProfile", profile.activePartnerProfile)
            put("walletPin", profile.walletPin)
            put("isBetaHcgConfirmed", profile.isBetaHcgConfirmed)
            put("stageViewMode", profile.stageViewMode)
        })
        root.put("expenses", JSONArray().apply {
            expenses.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("title", e.title); put("amount", e.amount)
                    put("currency", e.currency); put("category", e.category)
                    put("phase", e.phase); put("paymentStatus", e.paymentStatus)
                    put("dateStr", e.dateStr); put("notes", e.notes); put("addedBy", e.addedBy)
                })
            }
        })
        root.put("protocolSteps", JSONArray().apply {
            protocolSteps.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("phaseId", s.phaseId); put("dateStr", s.dateStr)
                    put("stepName", s.stepName); put("medication", s.medication); put("dosage", s.dosage)
                    put("status", s.status); put("details", s.details); put("isCompleted", s.isCompleted)
                    put("isCriticalWindow", s.isCriticalWindow); put("targetEpochMillis", s.targetEpochMillis)
                    put("sortOrder", s.sortOrder); put("addedBy", s.addedBy)
                })
            }
        })
        root.put("appointments", JSONArray().apply {
            appointments.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("title", a.title); put("dateStr", a.dateStr)
                    put("timeStr", a.timeStr); put("doctor", a.doctor); put("location", a.location)
                    put("clinicalNotes", a.clinicalNotes); put("eventType", a.eventType)
                    put("dayOfMonth", a.dayOfMonth); put("monthIndex", a.monthIndex); put("yearNum", a.yearNum)
                    put("isCompleted", a.isCompleted); put("hasAlarm", a.hasAlarm)
                    put("alarmOffsetMinutes", a.alarmOffsetMinutes); put("addedBy", a.addedBy)
                })
            }
        })
        root.put("tasks", JSONArray().apply {
            tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("title", t.title); put("date", t.date)
                    put("category", t.category); put("details", t.details)
                    put("isCompleted", t.isCompleted); put("hasAlarm", t.hasAlarm)
                    put("alarmTimeMinutesBefore", t.alarmTimeMinutesBefore); put("addedBy", t.addedBy)
                })
            }
        })
        root.put("notes", JSONArray().apply {
            notes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id); put("title", n.title); put("content", n.content)
                    put("category", n.category); put("timestampText", n.timestampText)
                    put("mood", n.mood); put("addedBy", n.addedBy)
                })
            }
        })
        root.put("medReminders", JSONArray().apply {
            medReminders.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("title", m.title); put("medication", m.medication)
                    put("dosage", m.dosage); put("hour", m.hour); put("minute", m.minute)
                    put("repeatDaily", m.repeatDaily); put("isEnabled", m.isEnabled)
                    put("protocolPhase", m.protocolPhase); put("notes", m.notes)
                    put("pillsRemaining", m.pillsRemaining); put("dailyAmTaken", m.dailyAmTaken)
                    put("dailyPmTaken", m.dailyPmTaken); put("lastDoseDateStr", m.lastDoseDateStr)
                })
            }
        })
        root.put("checklists", JSONArray().apply {
            checklists.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("listKey", c.listKey); put("label", c.label)
                    put("isChecked", c.isChecked); put("sortOrder", c.sortOrder); put("addedBy", c.addedBy)
                })
            }
        })
        root.put("careProviders", JSONArray().apply {
            careProviders.forEach { cp ->
                put(JSONObject().apply {
                    put("id", cp.id); put("providerType", cp.providerType); put("name", cp.name)
                    put("affiliation", cp.affiliation); put("phone", cp.phone)
                    put("emergencyContact", cp.emergencyContact); put("address", cp.address)
                    put("portalLink", cp.portalLink)
                })
            }
        })
        root.put("pregnancyMeds", JSONArray().apply {
            pregnancyMeds.forEach { pm ->
                put(JSONObject().apply {
                    put("id", pm.id); put("name", pm.name); put("activeIngredient", pm.activeIngredient)
                    put("category", pm.category); put("safetyLevel", pm.safetyLevel)
                    put("trimester1", pm.trimester1); put("trimester2", pm.trimester2)
                    put("trimester3", pm.trimester3); put("clinicalNotes", pm.clinicalNotes)
                    put("addedBy", pm.addedBy)
                })
            }
        })
        return root.toString(2)
    }

    fun generateTextExportReport(
        context: Context,
        isFrench: Boolean,
        profile: SyncedUserProfile,
        protocolSteps: List<SyncedProtocolStep>,
        medReminders: List<SyncedMedReminder>,
        pregnancyMeds: List<SyncedPregnancyMed>,
        careProviders: List<SyncedCareProvider>,
        expenses: List<SyncedExpenseRecord>,
        appointments: List<SyncedAppointmentItem>,
        tasks: List<TaskItem>,
        notes: List<SyncedNoteItem>,
        walletCards: List<SyncedWalletCard>
    ): File {
        val file = File(exportDir(context), "BF_Journey_Full_Export_${stamp()}.txt")
        val sb = StringBuilder()
        val sep = "======================================================================\n"

        sb.append(sep)
        sb.append(if (isFrench) "B&F PARCOURS PMA & GROSSESSE — EXPORT COMPLET DU DOSSIER & DESIGN\n" else "B&F JOURNEY IVF & PREGNANCY — COMPLETE DOSSIER & DESIGN EXPORT\n")
        sb.append(if (isFrench) "Date d'export : " else "Export Date: ").append(SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())).append("\n")
        sb.append(if (isFrench) "Patiente : " else "Patient: ").append(profile.patientName).append(" (").append(profile.patientAge).append(" ans)\n")
        sb.append(if (isFrench) "Clinique / Médecin : " else "Clinic / Physician: ").append("RMA of New York — Dr. Anate Brauer (212-756-5777)\n")
        sb.append(if (isFrench) "Date de Transfert (TEC) : " else "FET Transfer Date: ").append("18 Nov 2026 (J5 Blastocyste)\n")
        sb.append(if (isFrench) "DPA (Terme Prévu) : " else "Due Date (DPA): ").append(profile.dueDate).append(" (06 Août 2027)\n")
        sb.append(sep).append("\n")

        // 1. Profile & Settings Spec
        sb.append("1. ").append(if (isFrench) "PROFIL & PARAMÈTRES" else "PROFILE & SETTINGS").append("\n")
        sb.append("----------------------------------------------------------------------\n")
        sb.append("• Active Partner Profile: ").append(profile.activePartnerProfile).append("\n")
        sb.append("• Stage View Mode: ").append(profile.stageViewMode).append("\n")
        sb.append("• Beta-hCG Confirmed: ").append(profile.isBetaHcgConfirmed).append("\n")
        sb.append("• Current Gestational Week: ").append(profile.currentWeek).append("\n")
        sb.append("• Days Remaining: ").append(profile.daysRemaining).append("\n")
        sb.append("• Health Vitals — Mood: ").append(profile.healthMood).append(", Weight: ").append(profile.healthWeight).append(" kg, Water: ").append(profile.waterGlasses).append(" glasses, BP: ").append(profile.bloodPressure).append("\n\n")

        // 2. Care Team & Emergency Directory
        sb.append("2. ").append(if (isFrench) "ÉQUIPE MÉDICALE & URGENCES" else "CARE TEAM & EMERGENCY DIRECTORY").append("\n")
        sb.append("----------------------------------------------------------------------\n")
        careProviders.forEach { cp ->
            val emergFlag = if (cp.isEmergency) " [🚨 URGENCE 24/7]" else ""
            sb.append("• ").append(cp.name).append(" (").append(cp.providerType).append(")").append(emergFlag).append("\n")
            sb.append("  Affiliation: ").append(cp.affiliation).append("\n")
            sb.append("  Phone: ").append(cp.phone)
            if (cp.emergencyContact.isNotBlank() && cp.emergencyContact != cp.phone) {
                sb.append(" | Emergency Line: ").append(cp.emergencyContact)
            }
            sb.append("\n")
            if (cp.email.isNotBlank()) sb.append("  Email: ").append(cp.email).append("\n")
            if (cp.address.isNotBlank()) sb.append("  Address: ").append(cp.address).append("\n")
            if (cp.portalLink.isNotBlank()) sb.append("  Portal/Web: ").append(cp.portalLink).append("\n")
            if (cp.notes.isNotBlank()) sb.append("  Notes: ").append(cp.notes).append("\n")
            sb.append("\n")
        }

        // 3. Protocol Steps & Immediate Tasks
        sb.append("3. ").append(if (isFrench) "PROTOCOLE TEC & ÉTAPES IMMÉDIATES" else "FET PROTOCOL & IMMEDIATE TASKS").append("\n")
        sb.append("----------------------------------------------------------------------\n")
        protocolSteps.forEach { step ->
            sb.append("[${if (step.isCompleted) "X" else " "}] Phase ").append(step.phaseId).append(" - ").append(step.stepName).append("\n")
            sb.append("    Date: ").append(step.dateStr).append(" | Med: ").append(step.medication).append(" (").append(step.dosage).append(")\n")
            sb.append("    Details: ").append(step.details).append("\n\n")
        }

        // 4. Medications & Reminders
        sb.append("4. ").append(if (isFrench) "MÉDICAMENTS & RAPPELS (ALARMES)" else "MEDICATIONS & ALARM REMINDERS").append("\n")
        sb.append("----------------------------------------------------------------------\n")
        medReminders.forEach { mr ->
            sb.append("• ").append(mr.title).append(" [").append(if (mr.isEnabled) "ACTIVE" else "DISABLED").append("]\n")
            sb.append("  Medication: ").append(mr.medication).append(" | Dosage: ").append(mr.dosage).append("\n")
            sb.append("  Time: ").append(mr.hour).append(":").append(String.format("%02d", mr.minute)).append(" | Pills Remaining: ").append(mr.pillsRemaining).append("\n")
            sb.append("  Notes: ").append(mr.notes).append("\n\n")
        }

        // 5. Expenses & Financial Ledger
        sb.append("5. ").append(if (isFrench) "FINANCES & DÉPENSES (USD)" else "FINANCIAL LEDGER & EXPENSES (USD)").append("\n")
        sb.append("----------------------------------------------------------------------\n")
        var totalExpUSD = 0.0
        expenses.forEach { exp ->
            totalExpUSD += exp.amount
            sb.append("• ").append(exp.dateStr).append(" - ").append(exp.title).append("\n")
            sb.append("  Amount Paid: $").append(String.format("%.2f", exp.amount)).append(" ").append(exp.currency).append(" | Insurance: $").append(String.format("%.2f", exp.insurancePaid)).append(" | Total Bill: $").append(String.format("%.2f", exp.totalBill)).append("\n")
            sb.append("  Category: ").append(exp.category).append(" | Phase: ").append(exp.phase).append(" | Added By: ").append(exp.addedBy).append("\n\n")
        }
        sb.append("TOTAL OUT-OF-POCKET PAID (USD): $").append(String.format("%.2f", totalExpUSD)).append("\n\n")

        // 6. Appointments & Calendar
        sb.append("6. ").append(if (isFrench) "RENDEZ-VOUS & CALENDRIER" else "APPOINTMENTS & CALENDAR").append("\n")
        sb.append("----------------------------------------------------------------------\n")
        appointments.forEach { apt ->
            sb.append("• ").append(apt.dateStr).append(" ").append(apt.timeStr).append(" - ").append(apt.title).append("\n")
            sb.append("  Location: ").append(apt.location).append(" | Completed: ").append(apt.isCompleted).append("\n\n")
        }

        // 7. Clinical Notes & Voice Memos
        sb.append("7. ").append(if (isFrench) "NOTES CLINIQUES & MÉMOS" else "CLINICAL NOTES & MEMOS").append("\n")
        sb.append("----------------------------------------------------------------------\n")
        notes.forEach { note ->
            sb.append("• ").append(note.timestampText).append(" - ").append(note.title).append("\n")
            sb.append("  Content: ").append(note.content).append("\n\n")
        }

        sb.append(sep)
        sb.append(if (isFrench) "Fin du rapport B&F Parcours." else "End of B&F Journey Export Report.")

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, subject: String, body: String = "") {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun sendViaEmail(context: Context, file: File?, mimeType: String, subject: String, summaryBody: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, summaryBody)
            if (file != null) {
                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }
}
