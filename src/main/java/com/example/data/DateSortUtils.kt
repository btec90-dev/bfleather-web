package com.example.data

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

data class RollingCountdownItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateLabel: String,
    val sourceType: String, // "PROTOCOL", "APPOINTMENT", "TASK"
    val targetEpochMillis: Long
)

data class CountdownSegments(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long
)

data class PartitionedTimeline<T>(
    val upcoming: List<T>,
    val past: List<T>
)

object DateSortUtils {
    private val frenchMonths = mapOf(
        "jan" to 0, "janv" to 0, "janvier" to 0, "january" to 0,
        "fev" to 1, "fevr" to 1, "fevrier" to 1, "feb" to 1, "february" to 1,
        "mar" to 2, "mars" to 2, "march" to 2,
        "avr" to 3, "avril" to 3, "apr" to 3, "april" to 3,
        "mai" to 4, "may" to 4,
        "juin" to 5, "jun" to 5, "june" to 5,
        "juil" to 6, "juillet" to 6, "jul" to 6, "july" to 6,
        "aou" to 7, "aout" to 7, "aug" to 7, "august" to 7,
        "sep" to 8, "sept" to 8, "septembre" to 8, "september" to 8,
        "oct" to 9, "octobre" to 9, "october" to 9,
        "nov" to 10, "novembre" to 10, "november" to 10,
        "dec" to 11, "decembre" to 11, "december" to 11
    )

    private fun stripAccents(input: String): String {
        val norm = Normalizer.normalize(input, Normalizer.Form.NFD)
        return norm.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase(Locale.US)
    }

    /**
     * Parses free-form French or English date/time text (e.g., "13 nov. 2026 à 08:30",
     * "2026-11-18", "18 Nov", "20 Nov 2026 09:00") into epoch millis.
     * Returns null if no recognizable date exists.
     */
    fun parseFlexibleEpochMillis(
        dateText: String,
        timeText: String = "",
        defaultYear: Int = 2026
    ): Long? {
        val raw = "$dateText $timeText".trim()
        if (raw.isBlank()) return null

        val lower = stripAccents(raw)

        // Extract optional HH:mm or HHhmm
        val timeMatch = Regex("\\b(\\d{1,2})[:h](\\d{2})\\b").find(lower)
        val parsedHour = timeMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val parsedMin = timeMatch?.groupValues?.get(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        // ISO yyyy-MM-dd
        Regex("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b").find(lower)?.let { m ->
            val y = m.groupValues[1].toInt()
            val mo = (m.groupValues[2].toInt() - 1).coerceIn(0, 11)
            val d = m.groupValues[3].toInt().coerceIn(1, 31)
            return buildCalMillis(y, mo, d, parsedHour, parsedMin)
        }

        // dd/MM/yyyy or dd/MM
        Regex("\\b(\\d{1,2})/(\\d{1,2})(?:/(20\\d{2}))?\\b").find(lower)?.let { m ->
            val d = m.groupValues[1].toInt().coerceIn(1, 31)
            val mo = (m.groupValues[2].toInt() - 1).coerceIn(0, 11)
            val y = m.groupValues[3].toIntOrNull() ?: defaultYear
            return buildCalMillis(y, mo, d, parsedHour, parsedMin)
        }

        // "13 nov. 2026" or "13 nov" or "nov 13, 2026"
        val words = lower.replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.isNotBlank() }
        var foundMonth: Int? = null
        var foundDay: Int? = null
        var foundYear: Int? = null

        for (w in words) {
            if (foundMonth == null && frenchMonths.containsKey(w)) {
                foundMonth = frenchMonths[w]
                continue
            }
            val num = w.toIntOrNull()
            if (num != null) {
                if (num in 2020..2035 && foundYear == null) {
                    foundYear = num
                } else if (num in 1..31 && foundDay == null) {
                    foundDay = num
                }
            }
        }

        if (foundMonth != null && foundDay != null) {
            val year = foundYear ?: inferYearForMonth(foundMonth, defaultYear)
            return buildCalMillis(year, foundMonth, foundDay, parsedHour, parsedMin)
        }

        return null
    }

    private fun inferYearForMonth(monthIndex: Int, fallbackYear: Int): Int {
        // Protocol runs Oct-Dec 2026 into Jan-Aug 2027
        return if (monthIndex <= 7 && fallbackYear == 2026) 2027 else fallbackYear
    }

    private fun buildCalMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun appointmentEpochMillis(appt: SyncedAppointmentItem): Long {
        parseFlexibleEpochMillis(appt.dateStr, appt.timeStr, appt.yearNum.toInt())?.let { return it }
        val timeMatch = Regex("(\\d{1,2})[:h](\\d{2})").find(appt.timeStr)
        val h = timeMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = timeMatch?.groupValues?.get(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return buildCalMillis(
            year = appt.yearNum.toInt().coerceIn(2024, 2035),
            monthZeroBased = appt.monthIndex.toInt().coerceIn(0, 11),
            day = appt.dayOfMonth.toInt().coerceIn(1, 31),
            hour = h,
            minute = m
        )
    }

    fun protocolStepEpochMillis(step: SyncedProtocolStep): Long {
        if (step.targetEpochMillis > 0L) return step.targetEpochMillis
        return parseFlexibleEpochMillis(step.dateStr) ?: Long.MAX_VALUE
    }

    fun taskEpochMillis(task: TaskItem): Long {
        return parseFlexibleEpochMillis(task.date) ?: Long.MAX_VALUE
    }

    /**
     * Upcoming-first chronological sort:
     * 1) Upcoming items (>= nowMillis && !isCompleted) sorted ascending (soonest first)
     * 2) Past items (< nowMillis or completed) sorted descending (most recent first)
     */
    fun <T> partitionUpcomingFirst(
        items: List<T>,
        nowMillis: Long = System.currentTimeMillis(),
        epochProvider: (T) -> Long,
        completedProvider: (T) -> Boolean = { false }
    ): PartitionedTimeline<T> {
        val upcoming = items.filter { !completedProvider(it) && epochProvider(it) >= nowMillis }
            .sortedBy(epochProvider)
        val past = items.filter { completedProvider(it) || epochProvider(it) < nowMillis }
            .sortedByDescending { item ->
                val ts = epochProvider(item)
                if (ts == Long.MAX_VALUE) 0L else ts
            }
        return PartitionedTimeline(upcoming = upcoming, past = past)
    }

    /**
     * Builds the dynamic rolling FIFO countdown queue from protocol steps, appointments, and tasks,
     * sorted soonest-first for events strictly in the future (> nowMillis), deduplicated cleanly.
     */
    fun buildRollingQueue(
        protocolSteps: List<SyncedProtocolStep>,
        appointments: List<SyncedAppointmentItem>,
        tasks: List<TaskItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<RollingCountdownItem> {
        val combined = mutableListOf<RollingCountdownItem>()

        protocolSteps.forEach { step ->
            val ts = protocolStepEpochMillis(step)
            if (!step.isCompleted && ts != Long.MAX_VALUE && ts > nowMillis) {
                combined.add(
                    RollingCountdownItem(
                        id = "proto_${step.id}",
                        title = step.stepName,
                        subtitle = step.medication.ifBlank { step.status },
                        dateLabel = step.dateStr,
                        sourceType = "PROTOCOL",
                        targetEpochMillis = ts
                    )
                )
            }
        }

        appointments.forEach { appt ->
            val ts = appointmentEpochMillis(appt)
            if (!appt.isCompleted && ts > nowMillis) {
                combined.add(
                    RollingCountdownItem(
                        id = "appt_${appt.id}",
                        title = appt.title,
                        subtitle = listOf(appt.doctor, appt.location).filter { it.isNotBlank() }.joinToString(" • "),
                        dateLabel = listOf(appt.dateStr, appt.timeStr).filter { it.isNotBlank() }.joinToString(" • "),
                        sourceType = "APPOINTMENT",
                        targetEpochMillis = ts
                    )
                )
            }
        }

        tasks.forEach { task ->
            val ts = taskEpochMillis(task)
            if (!task.isCompleted && ts != Long.MAX_VALUE && ts > nowMillis) {
                combined.add(
                    RollingCountdownItem(
                        id = "task_${task.id}",
                        title = task.title,
                        subtitle = task.category,
                        dateLabel = task.date,
                        sourceType = "TASK",
                        targetEpochMillis = ts
                    )
                )
            }
        }

        return combined
            .sortedBy { it.targetEpochMillis }
            .distinctBy { "${it.title.lowercase(Locale.US)}_${it.targetEpochMillis / 60000L}" }
    }

    fun computeCountdownSegments(targetEpochMillis: Long, nowMillis: Long = System.currentTimeMillis()): CountdownSegments {
        val diffSeconds = ((targetEpochMillis - nowMillis).coerceAtLeast(0L)) / 1000L
        val days = diffSeconds / 86400L
        val hours = (diffSeconds % 86400L) / 3600L
        val minutes = (diffSeconds % 3600L) / 60L
        val seconds = diffSeconds % 60L
        return CountdownSegments(days = days, hours = hours, minutes = minutes, seconds = seconds)
    }
}
