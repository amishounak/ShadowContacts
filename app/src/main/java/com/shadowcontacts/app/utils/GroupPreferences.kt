package com.shadowcontacts.app.utils

import android.content.Context

object GroupPreferences {

    private const val PREFS_NAME = "shadow_contacts_prefs"
    private const val KEY_ACTIVE_GROUP = "active_group_id"

    fun getActiveGroupId(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_ACTIVE_GROUP, 1L)
    }

    fun setActiveGroupId(context: Context, groupId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_ACTIVE_GROUP, groupId).apply()
    }
}
