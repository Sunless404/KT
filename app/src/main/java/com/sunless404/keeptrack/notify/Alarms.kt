package com.sunless404.keeptrack.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.MainActivity
import com.sunless404.keeptrack.R
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.DayType
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.widget.RoutineWidget
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Avisos. Solo hay una alarma exacta programada: la del próximo aviso. Al sonar
 * se lanzan las notificaciones pendientes y se programa la siguiente.
 */
class Alarms(private val app: App) {
    private val am = app.getSystemService(AlarmManager::class.java)
    private val nm = NotificationManagerCompat.from(app)
    private val mutex = Mutex()

    /** [entry] == null es el aviso de fin de día; [refresh] solo despierta para redibujar el widget. */
    private class Event(
        val at: Long,
        val entry: DayEntry?,
        val startAt: Long = at,
        val endAt: Long = at,
        val refresh: Boolean = false,
    )

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    /** Lanza los avisos vencidos y programa el siguiente. Seguro de llamar siempre que cambie algo. */
    suspend fun sync() = mutex.withLock {
        app.repo.ensureMaterialized()
        val prefs = app.prefs
        val now = System.currentTimeMillis()
        if (prefs.lastHandled == 0L || prefs.lastHandled > now) prefs.lastHandled = now
        val events = events()

        events.filter { it.at > prefs.lastHandled && it.at <= now }.forEach { ev ->
            if (ev.refresh) Unit else if (ev.entry == null) notifyEndOfDay() else if (now < ev.endAt) notifyEntry(ev.entry, ev.startAt, ev.endAt)
        }
        prefs.lastHandled = now

        val pi = PendingIntent.getBroadcast(
            app, 0, Intent(app, AlarmReceiver::class.java).setAction(ACTION_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val next = events.filter { it.at > now }.minOfOrNull { it.at }
        if (next == null) {
            am.cancel(pi)
        } else if (canScheduleExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } else {
            // Sin permiso de alarmas exactas: puede retrasarse unos minutos.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
        RoutineWidget.refresh(app)
    }

    private suspend fun events(): List<Event> {
        val prefs = app.prefs
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val out = ArrayList<Event>()
        app.repo.range(today.minusDays(1), today.plusDays(1)).values.forEach { day ->
            val midnight = LocalDate.ofEpochDay(day.epochDay).atStartOfDay(zone)
            day.entries.forEach { e ->
                val startAt = midnight.plusMinutes(e.startMin.toLong()).toInstant().toEpochMilli()
                val endAt = midnight.plusMinutes(e.endMin.toLong()).toInstant().toEpochMilli()
                // El widget cambia al empezar y al acabar cada bloque, avise o no.
                out += Event(startAt, null, refresh = true)
                out += Event(endAt, null, refresh = true)
                if (!e.notify || day.type != DayType.NORMAL) return@forEach
                val lead = (e.leadMin ?: prefs.defaultLeadMin) * 60_000L
                when (e.status) {
                    Status.PENDIENTE -> out += Event(startAt - lead, e, startAt, endAt)
                    Status.POSPUESTO -> e.snoozeUntil?.let {
                        out += Event(it, e, startAt, maxOf(endAt, it + 60_000))
                    }
                    else -> Unit
                }
            }
        }
        if (prefs.endOfDayEnabled) {
            for (d in 0L..1L) {
                val at = today.plusDays(d).atStartOfDay(zone).plusMinutes(prefs.endOfDayMin.toLong())
                out += Event(at.toInstant().toEpochMilli(), null)
            }
        }
        return out
    }

    // ---------- Notificaciones ----------

    private fun channel(silent: Boolean): String {
        val prefs = app.prefs
        val id = if (silent) "silencio" else "bloques_s${if (prefs.sound) 1 else 0}_v${if (prefs.vibrate) 1 else 0}"
        val ch = if (silent) {
            NotificationChannel(id, "Avisos en horas de silencio", NotificationManager.IMPORTANCE_LOW)
        } else {
            val name = "Avisos de bloques" + when {
                prefs.sound && prefs.vibrate -> ""
                prefs.sound -> " (solo sonido)"
                prefs.vibrate -> " (solo vibración)"
                else -> " (sin sonido)"
            }
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                if (!prefs.sound) setSound(null, null)
                enableVibration(prefs.vibrate)
            }
        }
        nm.createNotificationChannel(ch)
        return id
    }

    private fun nowIsQuiet(): Boolean {
        val t = Instant.now().atZone(ZoneId.systemDefault())
        return app.prefs.inQuietHours(t.hour * 60 + t.minute)
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        app, 1, Intent(app, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(entryId: Long, action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        app, (entryId * 4 + code).toInt(),
        Intent(app, AlarmReceiver::class.java).setAction(action).putExtra(EXTRA_ENTRY, entryId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notifyEntry(e: DayEntry, startAt: Long, endAt: Long) {
        if (!nm.areNotificationsEnabled()) return
        val now = System.currentTimeMillis()
        val mins = ((startAt - now + 59_000) / 60_000).toInt()
        val whenText = when {
            e.status == Status.POSPUESTO -> "Pospuesto: toca ahora"
            mins > 0 -> "Empieza en $mins min"
            else -> "Empieza ahora"
        }
        val n = NotificationCompat.Builder(app, channel(nowIsQuiet()))
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("${fmtTime(e.startMin)} ${e.title}")
            .setContentText("$whenText · hasta las ${fmtTime(e.endMin)}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$whenText · hasta las ${fmtTime(e.endMin)}" + if (e.notes.isNotBlank()) "\n${e.notes}" else ""
                )
            )
            .setColor(e.category.color.toInt())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setTimeoutAfter((endAt - now).coerceAtLeast(60_000))
            .addAction(0, "Hecho", action(e.id, ACTION_DONE, 0))
            .addAction(0, "Posponer 10 min", action(e.id, ACTION_SNOOZE, 1))
            .addAction(0, "Saltar", action(e.id, ACTION_SKIP, 2))
            .build()
        try {
            nm.notify(e.id.toInt(), n)
        } catch (_: SecurityException) {
        }
    }

    private suspend fun notifyEndOfDay() {
        if (!nm.areNotificationsEnabled()) return
        val today = LocalDate.now()
        val t = Instant.now().atZone(ZoneId.systemDefault())
        val nowMin = t.hour * 60 + t.minute
        val day = app.repo.range(today, today)[today.toEpochDay()] ?: return
        if (day.type != DayType.NORMAL) return
        val pending = day.entries.filter { it.status == Status.PENDIENTE && it.startMin <= nowMin }
        if (pending.isEmpty()) return
        val text = pending.joinToString { it.title }
        val n = NotificationCompat.Builder(app, channel(nowIsQuiet()))
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(
                if (pending.size == 1) "Queda 1 bloque sin marcar" else "Quedan ${pending.size} bloques sin marcar"
            )
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(END_OF_DAY_ID, n)
        } catch (_: SecurityException) {
        }
    }

    /** Notificación de prueba desde Ajustes. */
    fun notifyTest() {
        val n = NotificationCompat.Builder(app, channel(false))
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Aviso de prueba")
            .setContentText("Así sonarán los avisos de tus bloques.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(TEST_ID, n)
        } catch (_: SecurityException) {
        }
    }

    fun cancelNotification(entryId: Long) = nm.cancel(entryId.toInt())

    companion object {
        const val ACTION_ALARM = "com.sunless404.keeptrack.ALARM"
        const val ACTION_DONE = "com.sunless404.keeptrack.DONE"
        const val ACTION_SNOOZE = "com.sunless404.keeptrack.SNOOZE"
        const val ACTION_SKIP = "com.sunless404.keeptrack.SKIP"
        const val EXTRA_ENTRY = "entry"
        const val SNOOZE_MS = 10 * 60_000L
        private const val END_OF_DAY_ID = -1
        private const val TEST_ID = -2
    }
}
