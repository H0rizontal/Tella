package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.switches.TellaSwitchWithMessage
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences


class GeneralSettings : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_general_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_select_general)

        val crashReportsSwitch = view.findViewById<TellaSwitchWithMessage>(R.id.crash_report_switch)
        crashReportsSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            Preferences.setSubmittingCrashReports(isChecked)
        }
        crashReportsSwitch.setChecked(Preferences.isSubmittingCrashReports())

        val verificationSwitch = view.findViewById<TellaSwitchWithMessage>(R.id.verification_switch)
        verificationSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            Preferences.setAnonymousMode(!isChecked)
        }
        verificationSwitch.setChecked(!Preferences.isAnonymousMode())

        return view
    }
}