package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardHideTellaFragment : BaseFragment() {

    private lateinit var backBtn: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_hide_tella_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnBoardActivityInterface).hideProgress()

        backBtn = view.findViewById(R.id.back_btn)
        backBtn.setOnClickListener {
            activity.onBackPressed()
        }

        val btnOneDesc = view.findViewById<TextView>(R.id.subtitle_btn_one)
        btnOneDesc.setText(Html.fromHtml(getString(R.string.settings_servers_setup_change_name_icon_subtitle)))

        val btnTwoDesc = view.findViewById<TextView>(R.id.subtitle_btn_two)
        btnTwoDesc.setText(Html.fromHtml(getString(R.string.settings_servers_setup_hide_behind_calculator_subtitle)))

        val btnTwoLabel = view.findViewById<LinearLayout>(R.id.sheet_two_btn_label)
        val btnTwo = view.findViewById<LinearLayout>(R.id.sheet_two_btn)

        val hideNotPossible = view.findViewById<TextView>(R.id.hide_behind_calc_not_possible)

        if ((activity.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(activity) != UnlockRegistry.Method.TELLA_PIN) {
            hideNotPossible.visibility = View.VISIBLE
            /*hideNotPossible.setOnClickListener {
                activity.addFragment(SecuritySettings(), R.id.my_nav_host_fragment)
            }*/
            btnTwoLabel.setAlpha(0.38f)
            btnTwo.setClickable(false)
        } else {
            hideNotPossible.visibility = View.GONE
            btnTwoLabel.setAlpha(1f)
            btnTwo.setClickable(true)
            btnTwo.setOnClickListener {
                hideTellaBehindCalculator()
            }
        }

        val hideBtn = view.findViewById<LinearLayout>(R.id.sheet_one_btn)
        hideBtn.setOnClickListener {
            chooseNameAndLogo()
        }
    }

    private fun hideTellaBehindCalculator(){
        activity.addFragment(
            this,
            OnBoardCalculatorFragment(),
            R.id.rootOnboard
        )
    }

    private fun chooseNameAndLogo(){
        activity.addFragment(
            this,
            OnBoardHideNameLogoFragment(),
            R.id.rootOnboard
        )
    }
}