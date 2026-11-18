package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.abs

data class LocalMedAlarm(
  val id: String,
  val title: String,
  val medication: String,
  val dosage: String,
  val hour: Int,
  val minute: Int,
  val repeatDaily: Boolean,
  val isEnabled: Boolean,
  val protocolPhase: String = "",
  val notes: String = ""
) {
  val requestCode: Int
    get() = 20000 + abs(id.hashCode() % 50000)
}

class MedicationBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED,
      AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
        AlarmScheduler.rescheduleAllStoredAlarms(context)
      }
    }
  }
}

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val notificationId = intent.getIntExtra(EXTRA_ID, 1001)
    val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: ""

    if (intent.action == ACTION_MARK_TAKEN) {
      if (reminderId.isNotBlank()) {
        AlarmScheduler.markMedicationTakenNow(context, reminderId)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.cancel(notificationId)
      return
    }

    if (intent.action == ACTION_SNOOZE) {
      val title = intent.getStringExtra(EXTRA_TITLE) ?: "Rappel PMA"
      val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Rappel reporté de 10 minutes."
      val snoozeAt = System.currentTimeMillis() + 10L * 60L * 1000L
      AlarmScheduler.scheduleReminder(
        context = context,
        id = notificationId + 5000,
        title = title,
        message = message,
        triggerAtMillis = snoozeAt
      )
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.cancel(notificationId)
      return
    }

    val title = intent.getStringExtra(EXTRA_TITLE) ?: "Rappel Médicament PMA"
    val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Il est l'heure de prendre votre traitement."
    val repeatDaily = intent.getBooleanExtra(EXTRA_REPEAT_DAILY, false)
    val hour = intent.getIntExtra(EXTRA_HOUR, -1)
    val minute = intent.getIntExtra(EXTRA_MINUTE, -1)

    showNotification(context, title, message, notificationId, reminderId)

    // Automatically schedule next occurrence for recurring daily reminders
    if (repeatDaily && reminderId.isNotBlank() && hour in 0..23 && minute in 0..59) {
      val stored = AlarmScheduler.loadStoredAlarms(context).firstOrNull { it.id == reminderId }
      if (stored != null && stored.isEnabled) {
        AlarmScheduler.scheduleNextOccurrence(context, stored, forceTomorrow = true)
      }
    }
  }

  private fun showNotification(
    context: Context,
    title: String,
    message: String,
    notificationId: Int,
    reminderId: String
  ) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    AlarmScheduler.ensureChannel(context, notificationManager)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val builder = NotificationCompat.Builder(context, AlarmScheduler.CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_fg_google)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setSound(soundUri)
      .setVibrate(longArrayOf(0L, 250L, 180L, 250L))
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)

    val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
      action = ACTION_SNOOZE
      putExtra(EXTRA_ID, notificationId)
      putExtra(EXTRA_TITLE, title)
      putExtra(EXTRA_MESSAGE, message)
    }
    val snoozePending = PendingIntent.getBroadcast(
      context,
      notificationId + 200000,
      snoozeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    builder.addAction(0, "Snooze 10m", snoozePending)

    if (reminderId.isNotBlank()) {
      val markTakenIntent = Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_MARK_TAKEN
        putExtra(EXTRA_ID, notificationId)
        putExtra(EXTRA_REMINDER_ID, reminderId)
      }
      val markTakenPending = PendingIntent.getBroadcast(
        context,
        notificationId + 100000,
        markTakenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      builder.addAction(0, "Pris / Taken ✓", markTakenPending)
    }

    notificationManager.notify(notificationId, builder.build())
  }

  companion object {
    const val ACTION_MARK_TAKEN = "com.example.alarm.ACTION_MARK_TAKEN"
    const val ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE"
    const val EXTRA_ID = "EXTRA_ID"
    const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
    const val EXTRA_TITLE = "EXTRA_TITLE"
    const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
    const val EXTRA_REPEAT_DAILY = "EXTRA_REPEAT_DAILY"
    const val EXTRA_HOUR = "EXTRA_HOUR"
    const val EXTRA_MINUTE = "EXTRA_MINUTE"
  }
}

object AlarmScheduler {
  const val CHANNEL_ID = "pregnancy_pma_reminders"
  private const val PREFS_NAME = "pma_med_alarms"
  private const val KEY_ALARMS_JSON = "alarms_json"
  private const val KEY_TAKEN_PREFIX = "taken_at_"

  fun ensureChannel(context: Context, manager: NotificationManager? = null) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = manager ?: context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Rappels Médicaments & PMA",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notifications quotidiennes pour Estrace, Progestérone, vitamines et rendez-vous PMA"
        enableVibration(true)
        vibrationPattern = longArrayOf(0L, 250L, 180L, 250L)
        setSound(soundUri, audioAttrs)
      }
      nm.createNotificationChannel(channel)
    }
  }

  fun computeNextTriggerMillis(
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
    forceTomorrow: Boolean = false
  ): Long {
    val target = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
      set(Calendar.MINUTE, minute.coerceIn(0, 59))
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    if (forceTomorrow || target.timeInMillis <= nowMillis) {
      target.add(Calendar.DAY_OF_MONTH, 1)
    }
    return target.timeInMillis
  }

  fun syncDailyMedicationAlarms(context: Context, alarms: List<LocalMedAlarm>) {
    val previous = loadStoredAlarms(context)
    val incomingIds = alarms.map { it.id }.toSet()
    previous.filter { it.id !in incomingIds }.forEach { removed ->
      cancelReminder(context, removed.requestCode)
    }
    saveStoredAlarms(context, alarms)
    alarms.forEach { alarm ->
      if (alarm.isEnabled) {
        scheduleNextOccurrence(context, alarm, forceTomorrow = false)
      } else {
        cancelReminder(context, alarm.requestCode)
      }
    }
  }

  fun rescheduleAllStoredAlarms(context: Context) {
    loadStoredAlarms(context).filter { it.isEnabled }.forEach { alarm ->
      scheduleNextOccurrence(context, alarm, forceTomorrow = false)
    }
  }

  fun scheduleNextOccurrence(context: Context, alarm: LocalMedAlarm, forceTomorrow: Boolean = false) {
    val triggerAt = computeNextTriggerMillis(alarm.hour, alarm.minute, forceTomorrow = forceTomorrow)
    val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
    val body = buildString {
      if (alarm.medication.isNotBlank()) append(alarm.medication)
      if (alarm.dosage.isNotBlank()) append(" (${alarm.dosage})")
      append(" • $timeStr")
      if (alarm.notes.isNotBlank()) append(" — ${alarm.notes}")
    }.ifBlank { "Prise prévue à $timeStr" }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      putExtra(AlarmReceiver.EXTRA_ID, alarm.requestCode)
      putExtra(AlarmReceiver.EXTRA_REMINDER_ID, alarm.id)
      putExtra(AlarmReceiver.EXTRA_TITLE, "💊 ${alarm.title}")
      putExtra(AlarmReceiver.EXTRA_MESSAGE, body)
      putExtra(AlarmReceiver.EXTRA_REPEAT_DAILY, alarm.repeatDaily)
      putExtra(AlarmReceiver.EXTRA_HOUR, alarm.hour)
      putExtra(AlarmReceiver.EXTRA_MINUTE, alarm.minute)
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      alarm.requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    scheduleExactOrFallback(alarmManager, triggerAt, pendingIntent)
  }

  fun scheduleReminder(
    context: Context,
    id: Int,
    title: String,
    message: String,
    triggerAtMillis: Long
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      putExtra(AlarmReceiver.EXTRA_ID, id)
      putExtra(AlarmReceiver.EXTRA_TITLE, title)
      putExtra(AlarmReceiver.EXTRA_MESSAGE, message)
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      id,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
  }

  fun appointmentRequestCode(itemId: String): Int = 80000 + abs(itemId.hashCode() % 40000)

  fun scheduleAppointmentAlarm(
    context: Context,
    itemId: String,
    title: String,
    details: String,
    eventEpochMillis: Long,
    offsetMinutes: Long
  ) {
    val reqCode = appointmentRequestCode(itemId)
    val triggerAt = eventEpochMillis - offsetMinutes.coerceAtLeast(0L) * 60_000L
    if (triggerAt <= System.currentTimeMillis()) {
      // If reminder window already started but event is still ahead, fire shortly
      if (eventEpochMillis > System.currentTimeMillis()) {
        scheduleReminder(context, reqCode, "📅 $title", details, System.currentTimeMillis() + 5_000L)
      }
      return
    }
    scheduleReminder(context, reqCode, "📅 $title", details, triggerAt)
  }

  fun cancelAppointmentAlarm(context: Context, itemId: String) {
    cancelReminder(context, appointmentRequestCode(itemId))
  }

  private fun scheduleExactOrFallback(
    alarmManager: AlarmManager,
    triggerAtMillis: Long,
    pendingIntent: PendingIntent
  ) {
    try {
      val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
      } else {
        true
      }
      if (canExact) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
          alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    } catch (_: SecurityException) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    }
  }

  fun cancelReminder(context: Context, id: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      id,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }

  fun triggerTestNotification(
    context: Context,
    title: String = "💊 Test Rappel Médicament PMA",
    message: String = "Estrace 2 mg PO / Progestérone IM — Votre système de notifications locales fonctionne parfaitement."
  ): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }
    val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
      putExtra(AlarmReceiver.EXTRA_ID, 99001)
      putExtra(AlarmReceiver.EXTRA_REMINDER_ID, "test_med_reminder")
      putExtra(AlarmReceiver.EXTRA_TITLE, title)
      putExtra(AlarmReceiver.EXTRA_MESSAGE, message)
      putExtra(AlarmReceiver.EXTRA_REPEAT_DAILY, false)
    }
    AlarmReceiver().onReceive(context, receiverIntent)
    return true
  }

  fun markMedicationTakenNow(context: Context, reminderId: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putLong("$KEY_TAKEN_PREFIX$reminderId", System.currentTimeMillis()).apply()
  }

  fun getLastTakenMillis(context: Context, reminderId: String): Long? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val ts = prefs.getLong("$KEY_TAKEN_PREFIX$reminderId", 0L)
    return if (ts > 0L) ts else null
  }

  fun saveStoredAlarms(context: Context, alarms: List<LocalMedAlarm>) {
    val arr = JSONArray()
    alarms.forEach { alarm ->
      arr.put(
        JSONObject().apply {
          put("id", alarm.id)
          put("title", alarm.title)
          put("medication", alarm.medication)
          put("dosage", alarm.dosage)
          put("hour", alarm.hour)
          put("minute", alarm.minute)
          put("repeatDaily", alarm.repeatDaily)
          put("isEnabled", alarm.isEnabled)
          put("protocolPhase", alarm.protocolPhase)
          put("notes", alarm.notes)
        }
      )
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_ALARMS_JSON, arr.toString())
      .apply()
  }

  fun loadStoredAlarms(context: Context): List<LocalMedAlarm> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_ALARMS_JSON, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          add(
            LocalMedAlarm(
              id = obj.optString("id"),
              title = obj.optString("title"),
              medication = obj.optString("medication"),
              dosage = obj.optString("dosage"),
              hour = obj.optInt("hour", 8),
              minute = obj.optInt("minute", 0),
              repeatDaily = obj.optBoolean("repeatDaily", true),
              isEnabled = obj.optBoolean("isEnabled", true),
              protocolPhase = obj.optString("protocolPhase"),
              notes = obj.optString("notes")
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }
}
