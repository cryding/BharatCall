package com.bharatcall.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService

class CallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        CallState.inCallService = this
        createCallNotificationChannel()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallState.currentCall = call
        call.registerCallback(CallState.callback)
        updateCallNotification(call)
        try {
            val launch = Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(launch)
        } catch (_: Exception) {
            // The full-screen call notification is the supported fallback for background launch limits.
        }
        InCallActivity.notifyCallChanged()
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        try {
            val launch = Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("show_dialpad", showDialpad)
            startActivity(launch)
        } catch (_: Exception) {
            // Notification remains available to bring the call UI forward.
        }
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(CallState.callback)
        if (CallState.currentCall == call) {
            CallState.currentCall = null
            getSystemService(NotificationManager::class.java).cancel(CALL_NOTIFICATION_ID)
        }
        InCallActivity.notifyCallChanged()
        super.onCallRemoved(call)
    }

    fun updateCallNotification(call: Call) {
        val manager = getSystemService(NotificationManager::class.java)
        val ringing = call.state == Call.STATE_RINGING
        val phone = call.details?.handle?.schemeSpecificPart.orEmpty().ifBlank { "Unknown caller" }
        val activityIntent = Intent(this, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val activityPending = PendingIntent.getActivity(
            this, 1, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Notification.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(com.bharatcall.app.R.drawable.ic_bharatcall)
            .setContentTitle(if (ringing) "Incoming call" else if (call.state == Call.STATE_DISCONNECTED) "Call ended" else "BharatCall")
            .setContentText(phone)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(ringing || call.state == Call.STATE_ACTIVE || call.state == Call.STATE_DIALING || call.state == Call.STATE_CONNECTING)
            .setAutoCancel(!ringing)
            .setContentIntent(activityPending)
            .setPriority(Notification.PRIORITY_MAX)
        if (ringing) {
            builder.setFullScreenIntent(activityPending, true)
            builder.addAction(android.R.drawable.sym_action_call, "Answer", actionPending("ANSWER"))
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", actionPending("DECLINE"))
        } else {
            builder.setFullScreenIntent(null, false)
        }
        manager.notify(CALL_NOTIFICATION_ID, builder.build())
    }

    private fun actionPending(action: String): PendingIntent {
        val intent = Intent(this, CallActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            this, if (action == "ANSWER") 2 else 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CALL_CHANNEL_ID) == null) {
                val channel = NotificationChannel(CALL_CHANNEL_ID, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Incoming and ongoing call controls"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 180, 250)
                    lightColor = Color.rgb(0, 190, 110)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        if (CallState.inCallService == this) CallState.inCallService = null
        super.onDestroy()
    }

    companion object {
        const val CALL_NOTIFICATION_ID = 7701
        private const val CALL_CHANNEL_ID = "bharatcall_calls"
    }
}
