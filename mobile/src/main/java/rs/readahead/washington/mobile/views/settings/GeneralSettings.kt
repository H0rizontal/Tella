package rs.readahead.washington.mobile.views.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.app.ActivityCompat
import androidx.navigation.Navigation
import org.hzontal.shared_ui.switches.TellaSwitchWithMessage
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentGeneralSettingsBinding
import rs.readahead.washington.mobile.util.C.LOCATION_PERMISSION
import rs.readahead.washington.mobile.util.CleanInsightUtils
import rs.readahead.washington.mobile.util.LocaleManager
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActions
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import java.util.Locale


class GeneralSettings :
    BaseBindingFragment<FragmentGeneralSettingsBinding>(FragmentGeneralSettingsBinding::inflate) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        (baseActivity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_select_general)
        (baseActivity as OnFragmentSelected?)?.setToolbarHomeIcon(R.drawable.ic_arrow_back_white_24dp)

        binding.languageSettingsButton.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_general_settings_to_language_settings)
        }

        setLanguageSetting()

        initSwitch(
            binding.shareDataSwitch,
            Preferences::hasAcceptedImprovements,
            Preferences::setIsAcceptedImprovements
        ) { isChecked ->
            CleanInsightUtils.grantCampaign(isChecked)
            if (isChecked) showMessageForCleanInsightsApprove(CleanInsightsActions.YES)
            binding.shareDataSwitch.setTextAndAction(R.string.action_learn_more) { startCleanInsightActivity() }
        }

        initSwitch(
            binding.crashReportSwitch,
            Preferences::isSubmittingCrashReports,
            Preferences::setSubmittingCrashReports
        )


        binding.verificationSwitch.mSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            run {
                if (!context?.let { hasLocationPermission(it) }!!) {
                    requestLocationPermission(LOCATION_PERMISSION)
                }
                Preferences.setAnonymousMode(!isChecked)
            }
        }

        initSwitch(
            binding.favoriteFormsSwitch,
            Preferences::isShowFavoriteForms,
            Preferences::setShowFavoriteForms
        )

        initSwitch(
            binding.favoriteTemplatesSwitch,
            Preferences::isShowFavoriteTemplates,
            Preferences::setShowFavoriteTemplates
        )

        initSwitch(
            binding.recentFilesSwitch,
            Preferences::isShowRecentFiles,
            Preferences::setShowRecentFiles
        )

    }

    private fun initSwitch(
        switchView: TellaSwitchWithMessage,
        preferencesGetter: () -> Boolean,
        preferencesSetter: (Boolean) -> Unit,
        onCheckedChangeListener: (Boolean) -> Unit = {}
    ) {
        switchView.mSwitch.isChecked = preferencesGetter()
        switchView.mSwitch.setOnCheckedChangeListener { _, isChecked ->
            try {
                preferencesSetter(isChecked)
                onCheckedChangeListener(isChecked)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun updateView() {
        binding.recentFilesSwitch.mSwitch.isChecked = Preferences.isShowRecentFiles()
        binding.favoriteTemplatesSwitch.mSwitch.isChecked = Preferences.isShowFavoriteTemplates()
        binding.favoriteFormsSwitch.mSwitch.isChecked = Preferences.isShowFavoriteForms()
        binding.verificationSwitch.mSwitch.isChecked = !Preferences.isAnonymousMode()
        binding.crashReportSwitch.mSwitch.isChecked = Preferences.isSubmittingCrashReports()

    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun setLanguageSetting() {
        val language = LocaleManager.getInstance().languageSetting
        if (language != null) {
            val locale = Locale(language)
            binding.languageSetting.text = StringUtils.capitalize(locale.displayName, locale)
        } else {
            binding.languageSetting.setText(R.string.settings_lang_select_default)
        }
    }

    private fun startCleanInsightActivity() {
        val intent = Intent(context, CleanInsightsActivity::class.java)
        startActivityForResult(intent, CleanInsightsActivity.CLEAN_INSIGHTS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CleanInsightsActivity.CLEAN_INSIGHTS_REQUEST_CODE) {
            val cleanInsightsActions =
                data?.extras?.getSerializable(CleanInsightsActivity.RESULT_FOR_ACTIVITY) as CleanInsightsActions
            showMessageForCleanInsightsApprove(cleanInsightsActions)
        }
    }

    private fun showMessageForCleanInsightsApprove(cleanInsightsActions: CleanInsightsActions) {
        when (cleanInsightsActions) {
            CleanInsightsActions.YES -> {
                Preferences.setIsAcceptedImprovements(true)
                CleanInsightUtils.grantCampaign(true)
                binding.shareDataSwitch.mSwitch.isChecked = true
                DialogUtils.showBottomMessage(
                    requireActivity(), getString(R.string.clean_insights_signed_for_days), false
                )
            }

            CleanInsightsActions.NO -> {
                Preferences.setIsAcceptedImprovements(false)
                CleanInsightUtils.grantCampaign(false)
                binding.shareDataSwitch.mSwitch.isChecked = false
            }

            else -> {}
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        baseActivity.maybeChangeTemporaryTimeout()
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) return true
        return false
    }

    private fun requestLocationPermission(requestCode: Int) {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ), requestCode
        )
    }
}