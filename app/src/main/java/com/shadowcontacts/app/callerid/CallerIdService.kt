package com.shadowcontacts.app.callerid

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.shadowcontacts.app.R
import com.shadowcontacts.app.data.ContactDatabase
import com.shadowcontacts.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlin.math.abs

class CallerIdService : Service() {

    companion object {
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_ACTION = "action"
        const val ACTION_SHOW = "show"
        const val ACTION_DISMISS = "dismiss"

        private const val PREFS_NAME = "shadow_contacts_prefs"
        private const val KEY_OVERLAY_Y = "overlay_y_position"
        private const val DEFAULT_Y = 100

        private const val CHANNEL_ID = "shadow_contacts_caller_id"
        private const val NOTIFICATION_ID = 1001
        private const val OVERLAY_TIMEOUT_MS = 30_000L // 30 seconds
    }

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: return START_NOT_STICKY

        when (action) {
            ACTION_SHOW -> {
                val number = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return START_NOT_STICKY
                lookupAndShow(number)
            }
            ACTION_DISMISS -> {
                dismissOverlay()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun lookupAndShow(incomingNumber: String) {
        serviceScope.launch {
            val dao = ContactDatabase.getDatabase(applicationContext).contactDao()
            val allContacts = dao.getAllContactsSync()
            val normalizedIncoming = normalizeNumber(incomingNumber)

            val match = allContacts.find { contact ->
                contact.phone.isNotBlank() && normalizeNumber(contact.phone) == normalizedIncoming
            }

            if (match != null) {
                withContext(Dispatchers.Main) {
                    showOverlay(match.displayName(), match.description, match.phone)
                    showNotification(match.displayName(), match.description, match.phone)
                }
            }
        }
    }

    private fun normalizeNumber(number: String): String {
        val digits = number.replace(Regex("[^0-9]"), "")
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    private fun getSavedY(): Int {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_OVERLAY_Y, DEFAULT_Y)
    }

    private fun saveY(y: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_OVERLAY_Y, y).apply()
    }

    // ── Notification ──

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Caller ID",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows contact info for incoming calls"
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(name: String, description: String, phone: String) {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            append(phone)
            if (description.isNotBlank()) append(" · $description")
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("📞 $name")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    // ── Overlay ──

    private fun showOverlay(name: String, description: String, phone: String) {
        dismissOverlay()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = getSavedY()
        }
        layoutParams = params

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_caller_id, null).apply {
            findViewById<TextView>(R.id.overlayAppLabel).text = "Shadow Contacts"
            findViewById<TextView>(R.id.overlayName).text = name
            findViewById<TextView>(R.id.overlayPhone).text = phone

            val descView = findViewById<TextView>(R.id.overlayDescription)
            if (description.isNotBlank()) {
                descView.text = description
                descView.visibility = View.VISIBLE
            } else {
                descView.visibility = View.GONE
            }

            setupDragListener(this)
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Auto-dismiss after 30 seconds (notification persists)
        serviceScope.launch {
            delay(OVERLAY_TIMEOUT_MS)
            withContext(Dispatchers.Main) {
                dismissOverlay()
                stopSelf()
            }
        }
    }

    private fun setupDragListener(view: View) {
        var initialY = 0
        var initialTouchY = 0f
        var hasDragged = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = layoutParams?.y ?: 0
                    initialTouchY = event.rawY
                    hasDragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialTouchY
                    if (abs(deltaY) > 10) hasDragged = true

                    layoutParams?.let { lp ->
                        lp.y = initialY + deltaY.toInt()
                        if (lp.y < 0) lp.y = 0
                        try {
                            windowManager?.updateViewLayout(overlayView, lp)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    layoutParams?.let { saveY(it.y) }
                    if (!hasDragged) {
                        dismissOverlay()
                        stopSelf()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dismissOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onDestroy() {
        dismissOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }
}
