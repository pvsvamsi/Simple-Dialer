package `in`.runo.dialer.services

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.telecom.Call
import android.telecom.InCallService
import `in`.runo.dialer.activities.CallActivity
import `in`.runo.dialer.extensions.getStateCompat
import `in`.runo.dialer.extensions.isOutgoing
import `in`.runo.dialer.extensions.powerManager
import `in`.runo.dialer.helpers.CallManager
import `in`.runo.dialer.helpers.CallNotificationManager
import `in`.runo.dialer.helpers.NoCall

class CallService : InCallService() {
    private val callNotificationManager by lazy { CallNotificationManager(this) }

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state != Call.STATE_DISCONNECTED) {
                callNotificationManager.setupNotification()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(call)
        CallManager.inCallService = this
        call.registerCallback(callListener)

        val isScreenLocked = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
        if (!powerManager.isInteractive || call.isOutgoing() || isScreenLocked) {
            try {
                startActivity(CallActivity.getStartIntent(this))
                if (call.getStateCompat() != Call.STATE_RINGING) {
                    callNotificationManager.setupNotification()
                }
            } catch (e: ActivityNotFoundException) {
                // seems like startActivity can thrown AndroidRuntimeException and ActivityNotFoundException, not yet sure when and why, lets show a notification
                callNotificationManager.setupNotification()
            }
        } else {
            callNotificationManager.setupNotification()
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callListener)
        val wasPrimaryCall = call == CallManager.getPrimaryCall()
        CallManager.onCallRemoved(call)
        if (CallManager.getPhoneState() == NoCall) {
            CallManager.inCallService = null
            callNotificationManager.cancelNotification()
        } else {
            callNotificationManager.setupNotification()
            if (wasPrimaryCall) {
                startActivity(CallActivity.getStartIntent(this))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callNotificationManager.cancelNotification()
    }
}
