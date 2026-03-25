package com.shadowcontacts.app.callerid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.shadowcontacts.app.utils.CallerIdPreferences

/**
 * On Android 10+, PHONE_STATE fires TWO broadcasts on incoming call:
 *   1st: state=RINGING, number=null
 *   2nd: state=RINGING, number="1234567890" (if READ_CALL_LOG granted)
 * We track state so we show the overlay on the 2nd broadcast that has the number.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ShadowCallerID"
        private var lastState = ""
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!CallerIdPreferences.isEnabled(context)) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "State=$state  Number=$number")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Only fire on the broadcast that actually carries the number
                if (!number.isNullOrBlank()) {
                    Log.d(TAG, "Incoming call from: $number — launching overlay")
                    val serviceIntent = Intent(context, CallerIdService::class.java).apply {
                        putExtra(CallerIdService.EXTRA_PHONE_NUMBER, number)
                        putExtra(CallerIdService.EXTRA_ACTION, CallerIdService.ACTION_SHOW)
                    }
                    context.startService(serviceIntent)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                    Log.d(TAG, "Call ended/answered — dismissing overlay")
                    val serviceIntent = Intent(context, CallerIdService::class.java).apply {
                        putExtra(CallerIdService.EXTRA_ACTION, CallerIdService.ACTION_DISMISS)
                    }
                    context.startService(serviceIntent)
                }
            }
        }
        lastState = state
    }
}
