package `in`.runo.dialer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.runo.dialer.activities.CallActivity
import `in`.runo.dialer.helpers.ACCEPT_CALL
import `in`.runo.dialer.helpers.CallManager
import `in`.runo.dialer.helpers.DECLINE_CALL

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACCEPT_CALL -> {
                context.startActivity(CallActivity.getStartIntent(context))
                CallManager.accept()
            }
            DECLINE_CALL -> CallManager.reject()
        }
    }
}
