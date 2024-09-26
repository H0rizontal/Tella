package rs.readahead.washington.mobile.views.fragment.googledrive.send

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.views.fragment.googledrive.GoogleDriveViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsSendFragment
import java.io.File

@AndroidEntryPoint
class GoogleDriveSendFragment : BaseReportsSendFragment() {

    override val viewModel by viewModels<GoogleDriveViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState) // Call the base class's onViewCreated

        viewModel.reportProcess.observe(viewLifecycleOwner) { progress ->
            if (progress.second.id == this@GoogleDriveSendFragment.reportInstance?.id) {
                val pct = progress.first
                val instance = progress.second

                pauseResumeLabel(instance)
                endView.setUploadProgress(instance, pct.current.toFloat() / pct.size.toFloat())
            }
        }

        viewModel.instanceProgress.observe(viewLifecycleOwner) { entity ->
            if (entity.id == this@GoogleDriveSendFragment.reportInstance?.id) {
                when (entity.status) {
                    EntityStatus.SUBMITTED -> {
                        viewModel.saveSubmitted(entity)
                    }

                    EntityStatus.FINALIZED -> {
                        viewModel.saveOutbox(entity)
                    }

                    EntityStatus.PAUSED -> {
                        pauseResumeLabel(entity)
                        viewModel.saveOutbox(entity)
                    }

                    EntityStatus.DELETED -> {
                       // viewModel.instanceProgress.postValue(null)
                        //handleBackButton()
                    }

                    else -> {
                        this@GoogleDriveSendFragment.reportInstance = entity
                    }
                }
            }
        }



        reportInstance?.let { viewModel.uploadFile(it) }

    }

    override fun navigateBack() {
        if (isFromOutbox) {
            nav().popBackStack()
        } else {
            nav().popBackStack(R.id.newReportScreen, true)
        }
    }


}