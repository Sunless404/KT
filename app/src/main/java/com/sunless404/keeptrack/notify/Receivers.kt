package com.sunless404.keeptrack.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.Status
import kotlinx.coroutines.launch

/** Recibe la alarma del próximo aviso y las acciones de las notificaciones. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        val pending = goAsync()
        app.scope.launch {
            try {
                val id = intent.getLongExtra(Alarms.EXTRA_ENTRY, 0L)
                when (intent.action) {
                    Alarms.ACTION_DONE -> mark(app, id, Status.HECHO)
                    Alarms.ACTION_SKIP -> mark(app, id, Status.SALTADO)
                    Alarms.ACTION_SNOOZE -> mark(app, id, Status.POSPUESTO)
                    else -> app.alarms.sync()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun mark(app: App, entryId: Long, status: Status) {
        app.alarms.cancelNotification(entryId)
        // setStatus avisa a onChanged, que vuelve a programar las alarmas.
        app.repo.setStatus(
            entryId, status,
            snoozeUntil = if (status == Status.POSPUESTO) System.currentTimeMillis() + Alarms.SNOOZE_MS else null,
        )
        app.alarms.sync()
    }
}

/** Reprograma las alarmas tras reiniciar, actualizar la app o cambiar la hora. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        val pending = goAsync()
        app.scope.launch {
            try {
                app.alarms.sync()
            } finally {
                pending.finish()
            }
        }
    }
}
