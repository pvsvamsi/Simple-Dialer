package `in`.runo.dialer.activities

import android.os.Bundle
import `in`.runo.dialer.R
import `in`.runo.dialer.adapters.ConferenceCallsAdapter
import `in`.runo.dialer.helpers.CallManager
import com.simplemobiletools.commons.helpers.NavigationIcon
import kotlinx.android.synthetic.main.activity_conference.*

class ConferenceActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conference)
        updateMaterialActivityViews(conference_coordinator, conference_list, true)
        setupMaterialScrollListener(conference_list, conference_toolbar)
        conference_list.adapter = ConferenceCallsAdapter(this, conference_list, ArrayList(CallManager.getConferenceCalls())) {}
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(conference_toolbar, NavigationIcon.Arrow)
    }
}
