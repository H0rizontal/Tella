package rs.readahead.washington.mobile.views.dialog.reports.step6

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.FragmentSuccessfulSetServerBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.step5.ServerAdvancedSettingsFragment

class SuccessfulSetServerFragment :
    BaseBindingFragment<FragmentSuccessfulSetServerBinding>(
        FragmentSuccessfulSetServerBinding::inflate
    ) {
    private var isUpdate = false
    private lateinit var server: TellaReportServer

    interface TellaUploadServerDialogHandler {
        fun onTellaUploadServerDialogCreate(server: TellaReportServer?)
        fun onTellaUploadServerDialogUpdate(server: TellaReportServer?)
    }

    companion object {
        val TAG: String = ServerAdvancedSettingsFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: TellaReportServer,
            isUpdate: Boolean
        ): ServerAdvancedSettingsFragment {
            val frag = ServerAdvancedSettingsFragment()
            val args = Bundle()
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            args.putBoolean(IS_UPDATE_SERVER, isUpdate)
            frag.arguments = args
            return frag
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
    }

    private fun initView() {

        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
        arguments?.getBoolean(IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }
    }

    private fun initListeners() {
        binding?.okBtn?.setOnClickListener {
            save(server)
        }

    }

    private fun save(server: TellaReportServer) {
        val activity = activity as TellaUploadServerDialogHandler? ?: return
        if (server.id == 0L) {
            activity.onTellaUploadServerDialogCreate(server)
        } else {
            activity.onTellaUploadServerDialogUpdate(server)
        }
        baseActivity.finish()
    }

}