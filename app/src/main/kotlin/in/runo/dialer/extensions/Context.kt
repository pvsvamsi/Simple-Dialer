package `in`.runo.dialer.extensions

import `in`.runo.dialer.helpers.CallManager
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.PowerManager
import com.simplemobiletools.commons.extensions.telecomManager
import `in`.runo.dialer.helpers.Config
import `in`.runo.dialer.models.SIMAccount

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.audioManager: AudioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

val Context.powerManager: PowerManager get() = getSystemService(Context.POWER_SERVICE) as PowerManager

@SuppressLint("MissingPermission")
fun Context.getAvailableSIMCardLabels(): ArrayList<SIMAccount> {
    val SIMAccounts = ArrayList<SIMAccount>()
    try {
        telecomManager.callCapablePhoneAccounts.forEachIndexed { index, account ->
            val phoneAccount = telecomManager.getPhoneAccount(account)
            var label = phoneAccount.label.toString()
            var address = phoneAccount.address.toString()
            if (address.startsWith("tel:") && address.substringAfter("tel:").isNotEmpty()) {
                address = Uri.decode(address.substringAfter("tel:"))
                label += " ($address)"
            }

            val SIM = SIMAccount(index + 1, phoneAccount.accountHandle, label, address.substringAfter("tel:"))
            SIMAccounts.add(SIM)
        }
    } catch (ignored: Exception) {
    }
    return SIMAccounts
}

@SuppressLint("MissingPermission")
fun Context.areMultipleSIMsAvailable(): Boolean {
    return try {
        telecomManager.callCapablePhoneAccounts.size > 1
    } catch (ignored: Exception) {
        false
    }
}


@SuppressLint("MissingPermission")
fun Context.getCallSimCardSlot(): Int {
    var slot = if(areMultipleSIMsAvailable()) -1 else 1
    try {
        val accounts = telecomManager.callCapablePhoneAccounts
        if (accounts.size > 1) {
            accounts.forEachIndexed { index, account ->
                if (account == CallManager.getPrimaryCall()?.details?.accountHandle) {
                    slot = index + 1;
                }
            }
        } else {
            slot = 1;
        }
    } catch (ignored: Exception) {
    }
    return slot;
}
