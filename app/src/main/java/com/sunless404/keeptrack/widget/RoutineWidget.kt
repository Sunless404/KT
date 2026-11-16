package com.sunless404.keeptrack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.MainActivity
import com.sunless404.keeptrack.R
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.notify.AlarmReceiver
import com.sunless404.keeptrack.notify.Alarms
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Widget de pantalla de inicio: bloque actual, siguiente y tick rápido. */
class RoutineWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext as App
        val pending = goAsync()
        app.scope.launch {
            try {
                app.repo.ensureMaterialized()
                refresh(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Redibuja todos los widgets colocados. No hace nada si no hay ninguno. */
        suspend fun refresh(app: App) {
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, RoutineWidget::class.java))
            if (ids.isEmpty()) return

            val today = LocalDate.now()
            val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
            val days = app.repo.range(today.minusDays(1), today)
            val entries = days[today.toEpochDay()]?.entries.orEmpty()
            val carry = days[today.toEpochDay() - 1]?.entries.orEmpty().filter { it.endMin - 1440 > nowMin }
            val current = entries.lastOrNull { nowMin >= it.startMin && nowMin < it.endMin } ?: carry.firstOrNull()
            val next = entries.firstOrNull { it.startMin > nowMin }

            val views = RemoteViews(app.packageName, R.layout.widget)
            if (current != null) {
                views.setTextViewText(R.id.widget_now_label, "Ahora · hasta las ${fmtTime(current.endMin)}")
                views.setTextViewText(
                    R.id.widget_now,
                    (if (current.status == Status.HECHO) "✓ " else "") + current.title,
                )
            } else {
                views.setTextViewText(R.id.widget_now_label, "Ahora")
                views.setTextViewText(R.id.widget_now, "Hueco libre")
            }
            views.setTextViewText(
                R.id.widget_next,
                if (next != null) "Siguiente: ${fmtTime(next.startMin)} ${next.title} (en ${fmtDuration(next.startMin - nowMin)})"
                else "No queda nada más hoy",
            )

            val canTick = current != null && current.status != Status.HECHO
            views.setViewVisibility(R.id.widget_tick, if (canTick) View.VISIBLE else View.GONE)
            if (canTick) {
                views.setOnClickPendingIntent(
                    R.id.widget_tick,
                    PendingIntent.getBroadcast(
                        app, 900_000,
                        Intent(app, AlarmReceiver::class.java).setAction(Alarms.ACTION_DONE)
                            .putExtra(Alarms.EXTRA_ENTRY, current!!.id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    app, 2, Intent(app, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            manager.updateAppWidget(ids, views)
        }
    }
}
