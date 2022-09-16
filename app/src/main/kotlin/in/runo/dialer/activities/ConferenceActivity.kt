package `in`.runo.dialer.activities

import android.os.Bundle
import `in`.runo.dialer.R
import `in`.runo.dialer.adapters.ConferenceCallsAdapter
import `in`.runo.dialer.helpers.CallManager
import kotlinx.android.synthetic.main.activity_conference.*

class ConferenceActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conference)

        conference_calls_list.adapter = ConferenceCallsAdapter(this, conference_calls_list, ArrayList(CallManager.getConferenceCalls())) {}
    }
}
