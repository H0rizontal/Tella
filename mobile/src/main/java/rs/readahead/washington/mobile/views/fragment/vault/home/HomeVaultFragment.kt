package rs.readahead.washington.mobile.views.fragment.vault.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.utils.MediaFile
import org.hzontal.shared_ui.appbar.ToolbarComponent
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.views.activity.AudioPlayActivity
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity
import rs.readahead.washington.mobile.views.activity.VideoViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.custom.CountdownTextView
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener

const val VAULT_FILTER = "vf"

class HomeVaultFragment : BaseFragment(), VaultClickListener, IHomeVaultPresenter.IView {
    private lateinit var toolbar: ToolbarComponent
    private lateinit var vaultRecyclerView: RecyclerView
    private lateinit var panicModeView: RelativeLayout
    private lateinit var countDownTextView: CountdownTextView
    private lateinit var seekBar : SeekBar
    private lateinit var seekBarGrp : Group
    private var timerDuration = 0
    private var panicActivated = false
    private val vaultAdapter by lazy { VaultAdapter(this) }
    private lateinit var homeVaultPresenter: HomeVaultPresenter
    private val bundle by lazy { Bundle() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault, container, false)
    }
    override fun initView(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        vaultRecyclerView = view.findViewById(R.id.vaultRecyclerView)
        panicModeView = view.findViewById(R.id.panic_mode_view)
        countDownTextView = view.findViewById(R.id.countdown_timer)
        seekBar = view.findViewById(R.id.panic_seek)
        seekBarGrp = view.findViewById(R.id.seekBarGrp)
        setUpToolbar()
        initData()
        initListeners()
    }

    private fun initData() {
        homeVaultPresenter = HomeVaultPresenter(this)
        vaultRecyclerView.apply {
            adapter = vaultAdapter
            layoutManager = LinearLayoutManager(activity)
        }
        timerDuration = resources.getInteger(R.integer.panic_countdown_duration)

    }

    private fun getFiles() {
        val sort = Sort()
        sort.direction = Sort.Direction.ASC
        sort.type = Sort.Type.DATE
        val limits = Limits()
        limits.limit = 5
        homeVaultPresenter.getRecentFiles(FilterType.ALL_WITHOUT_DIRECTORY, sort, limits)
    }

    private fun initListeners() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBar.progress == 100) {
                    seekBar.progress = 0
                    showPanicScreens()
                } else {
                    seekBar.progress = 0
                    hidePanicScreens()
                }
            }
        })
        panicModeView.setOnClickListener { onPanicClicked() }
        toolbar.onLeftClickListener = {nav().navigate(R.id.main_settings)}
        toolbar.onRightClickListener = {MyApplication.exit(activity)}
    }

    private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view?.findViewById<View>(R.id.appbar)?.outlineProvider = null
        } else {
            view?.findViewById<View>(R.id.appbar)?.bringToFront()
        }
    }

    override fun onRecentFilesItemClickListener(vaultFile: VaultFile) {
        when {
            MediaFile.isImageFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, PhotoViewerActivity::class.java)
                intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile)
                startActivity(intent)
            }
            MediaFile.isAudioFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, AudioPlayActivity::class.java)
                intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, vaultFile.id)
                startActivity(intent)
            }
            MediaFile.isVideoFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, VideoViewerActivity::class.java)
                intent.putExtra(VideoViewerActivity.VIEW_VIDEO, vaultFile)
                startActivity(intent)
            }
        }
    }

    override fun onFavoriteItemClickListener(form: XFormEntity) {
    }

    override fun allFilesClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.ALL.name)
        navigateToAttachmentsList(bundle)
    }

    override fun imagesClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.PHOTO.name)
        navigateToAttachmentsList(bundle)
    }

    override fun audioClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.AUDIO.name)
        navigateToAttachmentsList(bundle)
    }

    override fun documentsClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.DOCUMENTS.name)
        navigateToAttachmentsList(bundle)
    }

    override fun othersClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.OTHERS.name)
        navigateToAttachmentsList(bundle)
    }

    override fun videoClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.VIDEO.name)
        navigateToAttachmentsList(bundle)
    }

    private fun stopPanicking() {
        countDownTextView.cancel()
        countDownTextView.setCountdownNumber(timerDuration)
        panicActivated = false
        // showMainControls()
    }

    override fun onResume() {
        super.onResume()
        setupPanicView()
        if (panicActivated) {
            showPanicScreens()
            panicActivated = false
        } else {
            maybeClosePanic()
        }
        getFiles()
    }

    private fun maybeClosePanic(): Boolean {
        if (panicModeView.visibility == View.VISIBLE) {
            stopPanicking()
            hidePanicScreens()
        }
        return false // todo: check panic state here
    }

    private fun hidePanicScreens() {
        (activity as MainActivity).showBottomNavigation()
        setupPanicView()
        panicModeView.visibility = View.GONE
    }

    private fun showPanicScreens() {
        // really show panic screen
        (activity as MainActivity).hideBottomNavigation()

        panicModeView.visibility = View.VISIBLE
        panicModeView.alpha = 1f
        countDownTextView.start(
            timerDuration
        ) {
            executePanicMode()
        }
    }

    private fun setupPanicView() {
        if (Preferences.isQuickExit()) {
            seekBarGrp.visibility = View.VISIBLE
        } else {
            seekBarGrp.visibility = View.GONE
        }
    }

    private fun executePanicMode() {
        try {
            homeVaultPresenter.executePanicMode()
        } catch (ignored: Throwable) {
            panicActivated = true
        }
    }

    private fun onPanicClicked() {
        hidePanicScreens()
        stopPanicking()
    }

    override fun onCountTUServersEnded(num: Long?) {
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {
    }

    override fun onCountCollectServersEnded(num: Long?) {
    }

    override fun onCountCollectServersFailed(throwable: Throwable?) {
    }

    override fun onGetFilesSuccess(files: List<VaultFile?>) {
        if (!files.isNullOrEmpty()) {
            vaultAdapter.addRecentFiles(files)
        }else{
            vaultAdapter.removeRecentFiles()
        }
    }

    override fun onGetFilesError(error: Throwable?) {

    }

    private fun navigateToAttachmentsList(bundle: Bundle?) {
        nav().navigate(R.id.action_homeScreen_to_attachments_screen, bundle)
    }

}