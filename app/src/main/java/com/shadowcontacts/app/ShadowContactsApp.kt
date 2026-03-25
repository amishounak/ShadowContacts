package com.shadowcontacts.app

import android.app.Application
import com.shadowcontacts.app.utils.ThemeHelper

class ShadowContactsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeHelper.applySavedTheme(this)
    }
}
