package com.sunless404.rutina.data

import android.content.Context
import android.content.SharedPreferences

/** Ajustes de la app (SharedPreferences). */
class Prefs(context: Context) {
    val sp: SharedPreferences = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    var seeded: Boolean
        get() = sp.getBoolean("seeded", false)
        set(v) = sp.edit().putBoolean("seeded", v).apply()

    /** Minutos de antelación por defecto de los avisos. */
    var defaultLeadMin: Int
        get() = sp.getInt("defaultLeadMin", 0)
        set(v) = sp.edit().putInt("defaultLeadMin", v).apply()

    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit().putBoolean("sound", v).apply()

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit().putBoolean("vibrate", v).apply()

    var quietEnabled: Boolean
        get() = sp.getBoolean("quietEnabled", false)
        set(v) = sp.edit().putBoolean("quietEnabled", v).apply()

    /** Inicio y fin de las horas de silencio, en minutos desde las 00:00. */
    var quietStartMin: Int
        get() = sp.getInt("quietStartMin", 23 * 60)
        set(v) = sp.edit().putInt("quietStartMin", v).apply()

    var quietEndMin: Int
        get() = sp.getInt("quietEndMin", 7 * 60)
        set(v) = sp.edit().putInt("quietEndMin", v).apply()

    var endOfDayEnabled: Boolean
        get() = sp.getBoolean("endOfDayEnabled", true)
        set(v) = sp.edit().putBoolean("endOfDayEnabled", v).apply()

    var endOfDayMin: Int
        get() = sp.getInt("endOfDayMin", 22 * 60)
        set(v) = sp.edit().putInt("endOfDayMin", v).apply()
}
