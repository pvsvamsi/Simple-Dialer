package `in`.runo.dialer.extensions

import android.telecom.Call
import android.telecom.Call.*
import android.util.Log
import com.simplemobiletools.commons.helpers.isQPlus
import com.simplemobiletools.commons.helpers.isSPlus

private val OUTGOING_CALL_STATES = arrayOf(STATE_CONNECTING, STATE_DIALING, STATE_SELECT_PHONE_ACCOUNT)
private val INCOMING_CALL_STATES = arrayOf(STATE_RINGING)

@Suppress("DEPRECATION")
fun Call?.getStateCompat(): Int {
    return when {
        this == null -> Call.STATE_DISCONNECTED
        isSPlus() -> details.state
        else -> state
    }
}

fun Call?.getCallDuration(): Int {
    return if (this != null) {
        val connectTimeMillis = details.connectTimeMillis
        if (connectTimeMillis == 0L) {
            return 0
        }
        ((System.currentTimeMillis() - connectTimeMillis) / 1000).toInt()
    } else {
        0
    }
}

fun Call.isOutgoing(): Boolean {
    return if (isQPlus()) {
        Log.d("CallActivityPCC", "OG : details.callDirection ${details.callDirection}")
        details.callDirection == Call.Details.DIRECTION_OUTGOING
    } else {
        Log.d("CallActivityPCC", "OG : getStateCompat() ${getStateCompat()}")
        OUTGOING_CALL_STATES.contains(getStateCompat())
    }
}

fun Call.isIncoming(): Boolean {
    return if (isQPlus()) {
        Log.d("CallActivityPCC", "IC : details.callDirection ${details.callDirection}")
        details.callDirection == Call.Details.DIRECTION_INCOMING
    } else {
        Log.d("CallActivityPCC", "IC : getStateCompat() ${getStateCompat()}")
        INCOMING_CALL_STATES.contains(getStateCompat())
    }
}

fun Call.hasCapability(capability: Int): Boolean = (details.callCapabilities and capability) != 0

fun Call?.isConference(): Boolean = this?.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true
