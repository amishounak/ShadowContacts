package com.shadowcontacts.app.utils

import android.content.Context

object CallerIdPreferences {

    private const val PREFS_NAME = "shadow_contacts_prefs"
    private const val KEY_CALLER_ID_ENABLED = "caller_id_enabled"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CALLER_ID_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CALLER_ID_ENABLED, enabled).apply()
    }
}
