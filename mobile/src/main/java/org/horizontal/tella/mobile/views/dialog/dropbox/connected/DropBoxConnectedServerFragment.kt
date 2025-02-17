package org.horizontal.tella.mobile.views.dialog.dropbox.connected

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import org.horizontal.tella.mobile.databinding.DropBoxConnectedServerFragmentBinding
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.SharedLiveData.createDropBoxServer

class DropBoxConnectedServerFragment :
    BaseBindingFragment<DropBoxConnectedServerFragmentBinding>(
        DropBoxConnectedServerFragmentBinding::inflate
    ) {
    private var dropBoxServer: DropBoxServer? = null

    companion object {
        val TAG = DropBoxConnectedServerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            dropBoxServer: DropBoxServer,
        ): DropBoxConnectedServerFragment {
            val frag = DropBoxConnectedServerFragment()
            val args = Bundle()
            args.putString(OBJECT_KEY, Gson().toJson(dropBoxServer))
            frag.arguments = args
            return frag
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupData()
        initListeners()
    }

    private fun setupData() {
        // Use requireArguments to avoid nullable arguments handling
        arguments?.getString(OBJECT_KEY)?.let {
            dropBoxServer = Gson().fromJson(it, DropBoxServer::class.java)
        }
    }

    private fun initListeners() {
        binding.goToBtn.setOnClickListener {
            handleServerUpdate()
            baseActivity.finish()
        }
    }

    private fun handleServerUpdate() {
        createDropBoxServer.postValue(dropBoxServer)
    }
}
