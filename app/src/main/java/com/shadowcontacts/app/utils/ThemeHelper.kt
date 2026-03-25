package com.shadowcontacts.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val PREFS_NAME = "shadow_contacts_prefs"
    private const val KEY_THEME = "theme_mode"

    const val LIGHT = 0
    const val DARK = 1
    const val AUTO = 2

    fun applySavedTheme(context: Context) {
        val mode = getSavedTheme(context)
        applyTheme(mode)
    }

    fun applyTheme(mode: Int) {
        when (mode) {
            LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun saveTheme(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME, mode).apply()
        applyTheme(mode)
    }

    fun getSavedTheme(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME, AUTO)
    }
}
