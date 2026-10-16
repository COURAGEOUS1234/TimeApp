package com.gabriel.timeapp

import android.content.Context
import android.graphics.Typeface

/** Theme colours and fonts for non-Compose surfaces (alerts, standby, notifications, widgets). */
object Look {
    fun tower(c: Context) = Store.settings(c).theme == "tower"
    fun accent(c: Context) = if (tower(c)) 0xFF9FDFFF.toInt() else 0xFFFFB23E.toInt()
    fun second(c: Context) = if (tower(c)) 0xFFFF9F5A.toInt() else 0xFF7FD8E6.toInt()
    fun bg(c: Context) = if (tower(c)) 0xFF04070A.toInt() else 0xFF0A0D0B.toInt()
    fun text(c: Context) = if (tower(c)) 0xFFF2F7FA.toInt() else 0xFFE6E8DF.toInt()
    fun flash(c: Context) = if (tower(c)) 0xFF0E2433.toInt() else 0xFF2E1E00.toInt()
    fun standbyTime(c: Context) = if (tower(c)) 0xFF6FA8C4.toInt() else 0xFFB07A20.toInt()
    fun digits(c: Context): Typeface = c.resources.getFont(if (tower(c)) R.font.jura_light else R.font.black_ops_one)
    fun ui(c: Context): Typeface = c.resources.getFont(if (tower(c)) R.font.jura_semibold else R.font.rajdhani_bold)
}
