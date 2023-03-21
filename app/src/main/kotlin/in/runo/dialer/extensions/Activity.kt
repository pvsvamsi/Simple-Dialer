package `in`.runo.dialer.extensions

import `in`.runo.dialer.activities.SimpleActivity
import `in`.runo.dialer.dialogs.SelectSIMDialog
import `in`.runo.dialer.helpers.CallManager
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.SimpleContact

fun SimpleActivity.startCallIntent(recipient: String) {
    if (isDefaultDialer()) {
        getHandleToUse(null, recipient) { handle ->
            launchCallIntentWrapper(recipient, handle)
        }
    } else {
        launchCallIntentWrapper(recipient, null)
    }
}

fun SimpleActivity.launchCreateNewContactIntent() {
    Intent().apply {
        action = Intent.ACTION_INSERT
        data = ContactsContract.Contacts.CONTENT_URI
        launchActivityIntent(this)
    }
}

fun BaseSimpleActivity.callContactWithSim(recipient: String, useMainSIM: Boolean) {
    handlePermission(PERMISSION_READ_PHONE_STATE) {
        val wantedSimIndex = if (useMainSIM) 0 else 1
        val handle = getAvailableSIMCardLabels().sortedBy { it.id }.getOrNull(wantedSimIndex)?.handle
        launchCallIntentWrapper(recipient, handle)
    }
}

fun BaseSimpleActivity.callContactWithSimSlot(recipient: String, slot: Int) {
    handlePermission(PERMISSION_READ_PHONE_STATE) {
        val wantedSimIndex = if (slot > 0) 0 else slot
        val handle = getAvailableSIMCardLabels().sortedBy { it.id }[wantedSimIndex].handle
        launchCallIntentWrapper(recipient, handle)
    }
}

// handle private contacts differently, only Simple Contacts Pro can open them
fun Activity.startContactDetailsIntent(contact: SimpleContact) {
    val simpleContacts = "com.simplemobiletools.contacts.pro"
    val simpleContactsDebug = "com.simplemobiletools.contacts.pro.debug"
    if (contact.rawId > 1000000 && contact.contactId > 1000000 && contact.rawId == contact.contactId &&
        (isPackageInstalled(simpleContacts) || isPackageInstalled(simpleContactsDebug))
    ) {
        Intent().apply {
            action = Intent.ACTION_VIEW
            putExtra(CONTACT_ID, contact.rawId)
            putExtra(IS_PRIVATE, true)
            `package` = if (isPackageInstalled(simpleContacts)) simpleContacts else simpleContactsDebug
            setDataAndType(ContactsContract.Contacts.CONTENT_LOOKUP_URI, "vnd.android.cursor.dir/person")
            launchActivityIntent(this)
        }
    } else {
        ensureBackgroundThread {
            val lookupKey = SimpleContactsHelper(this).getContactLookupKey((contact).rawId.toString())
            val publicUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
            runOnUiThread {
                launchViewContactIntent(publicUri)
            }
        }
    }
}

// used at devices with multiple SIM cards
@SuppressLint("MissingPermission")
fun SimpleActivity.getHandleToUse(intent: Intent?, phoneNumber: String, callback: (handle: PhoneAccountHandle?) -> Unit) {
    handlePermission(PERMISSION_READ_PHONE_STATE) {
        if (it) {
            val defaultHandle = telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
            when {
                intent?.hasExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE) == true -> callback(intent.getParcelableExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE)!!)
                config.getCustomSIM(phoneNumber)?.isNotEmpty() == true -> {
                    val storedLabel = Uri.decode(config.getCustomSIM(phoneNumber))
                    val availableSIMs = getAvailableSIMCardLabels()
                    val firstOrNull = availableSIMs.firstOrNull { it.label == storedLabel }?.handle ?: availableSIMs.first().handle
                    callback(firstOrNull)
                }
                defaultHandle != null -> callback(defaultHandle)
                else -> {
                    SelectSIMDialog(this, phoneNumber) { handle ->
                        callback(handle)
                    }
                }
            }
        }
    }
}

fun BaseSimpleActivity.launchCallIntentWrapper(recipient: String, handle: PhoneAccountHandle? = null) {
    val actualDialNumberInConfMode: String = CallManager.actualDialNumberInRunoConfMode
    if(!CallManager.isInRunoConfMode) {
        val runoConferenceNumber: String = config.getRunoConferenceNumber().toString()
        if (runoConferenceNumber != "-1") {
            val trackingSimSlot = config.getTrackingSimSlot()
            if (trackingSimSlot != -1) {
                var currentSimSlot: Int = if (areMultipleSIMsAvailable()) getCallSimCardSlot() else 1
                if (currentSimSlot == -1) currentSimSlot = handle?.let { getSimSlotIndexFromAccountId(it.id) } ?: 1
                if (trackingSimSlot == currentSimSlot) {
                    Log.d("CallActivityP", "Actual Conference number is $recipient")
                    CallManager.actualDialNumberInRunoConfMode = recipient
                    CallManager.isInRunoConfMode = true
                    launchCallIntent(runoConferenceNumber, handle)
                    return
                } else {
                    Log.d("CallActivityP", "currentSimSlot is $currentSimSlot && trackingSimSlot is $trackingSimSlot")
                }
            }
        }
        Log.d("CallActivityP", "Resetting conference number is $actualDialNumberInConfMode")
        CallManager.resetConferenceMode()
    }
    Log.d("CallActivityP", "Conference mode is already in progress")
    launchCallIntent(recipient, handle)
}

fun BaseSimpleActivity.getSimSlotIndexFromAccountId(accountIdToFind: String): Int {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        return -1;
    }
    telecomManager.callCapablePhoneAccounts.forEachIndexed { index, account: PhoneAccountHandle ->
        val phoneAccount: PhoneAccount = telecomManager.getPhoneAccount(account)
        val accountId: String = phoneAccount.accountHandle
            .id
        if (accountIdToFind == accountId) {
            return index
        }
    }
    accountIdToFind.toIntOrNull()?.let {
        if (it >= 0)
            return it
    }
    return -1
}
