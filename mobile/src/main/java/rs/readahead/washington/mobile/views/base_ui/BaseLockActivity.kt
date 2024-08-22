package rs.readahead.washington.mobile.views.base_ui

import android.content.Intent
import android.view.WindowManager
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS_BLUE_SKIN
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS_ORANGE_SKIN
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS_YELLOW_SKIN
import com.hzontal.tella_locking_ui.common.util.DivviupUtils
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.calculator.CalculatorActivity
import info.guardianproject.cacheword.SecretsManager
import org.hzontal.shared_ui.data.CommonPreferences
import org.hzontal.shared_ui.utils.CALCULATOR_THEME
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.MyApplication.isConnectedToInternet
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.views.activity.PatternUpgradeActivity
import timber.log.Timber

abstract class BaseLockActivity : BaseActivity() {

    private val holder by lazy { applicationContext as IUnlockRegistryHolder }
    var isLocked = false
        private set

    fun restrictActivity() {
        if (!MyApplication.getMainKeyStore().isStored) {
            startKeySetup()
        } else {
            isLocked = !MyApplication.getMainKeyHolder().exists()
            if (isLocked) {
                sendTimeSpentAnalytics()
                startUnlockingMainKey()
            } else {
                Timber.d("+++++ unlocked")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("+++++ onPause()")
        val lastTimeSpent = CommonPreferences.getTimeSpent()
        CommonPreferences.setTimeSpent(lastTimeSpent + (System.currentTimeMillis() - CommonPreferences.getUnlockTime()))
        Timber.d(
            "+++++ time spent %s",
            lastTimeSpent + (System.currentTimeMillis() - CommonPreferences.getUnlockTime())
        )
    }

    private fun startKeySetup() {
        val intent = Intent(
            this,
            if (SecretsManager.isInitialized(this)) PatternUpgradeActivity::class.java else PatternSetActivity::class.java
        )
        this.startActivity(intent)
    }

    private fun startUnlockingMainKey() {
        val intent = when (holder.unlockRegistry.getActiveMethod(this)) {
            UnlockRegistry.Method.TELLA_PIN -> {
                //temp switch
                when (Preferences.getAppAlias()) {
                    CALCULATOR_ALIAS, CALCULATOR_ALIAS_BLUE_SKIN, CALCULATOR_ALIAS_ORANGE_SKIN, CALCULATOR_ALIAS_YELLOW_SKIN
                    -> Intent(this, CalculatorActivity::class.java).putExtra(
                        CALCULATOR_THEME,
                        Preferences.getCalculatorTheme()
                    )

                    else -> Intent(this, PinUnlockActivity::class.java)
                }

            }

            UnlockRegistry.Method.TELLA_PATTERN -> {
                Intent(this, PatternUnlockActivity::class.java)
            }

            UnlockRegistry.Method.TELLA_PASSWORD -> {
                Intent(this, PasswordUnlockActivity::class.java)
            }

            else -> {
                Intent(this, PatternUnlockActivity::class.java)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        this.startActivity(intent)
        finish()
    }

    override fun onResume() {
        CommonPreferences.setUnlockTime(System.currentTimeMillis())
        restrictActivity()
        maybeEnableSecurityScreen()
        super.onResume()
    }

    private fun maybeRestoreTimeout() {
        if (Preferences.isTempTimeout()) {
            MyApplication.getMainKeyHolder().timeout = LockTimeoutManager.IMMEDIATE_SHUTDOWN
            Preferences.setTempTimeout(false)
        }
    }

    private fun maybeEnableSecurityScreen() {
        if (Preferences.isSecurityScreenEnabled()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    override fun onDestroy() {
        maybeRestoreTimeout()
        super.onDestroy()
    }

    private fun sendTimeSpentAnalytics() {
        if (isConnectedToInternet(baseContext)) {
            DivviupUtils.runTimeSpentEvent(baseContext, CommonPreferences.getTimeSpent())
            CommonPreferences.setTimeSpent(0L)
        }
    }
}