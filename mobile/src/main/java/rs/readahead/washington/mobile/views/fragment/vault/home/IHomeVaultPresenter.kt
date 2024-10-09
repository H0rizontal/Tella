package rs.readahead.washington.mobile.views.fragment.vault.home

import android.content.Context
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter

class IHomeVaultPresenter {
    interface IView {
        fun getContext(): Context?
        fun onGetFilesSuccess(files: List<VaultFile?>)
        fun onGetFilesError(error: Throwable?)
        fun onMediaExported(num: Int)
        fun onExportError(error: Throwable?)
        fun onExportStarted()
        fun onExportEnded()
        fun onGetFavoriteCollectFormsSuccess(files: List<CollectForm>)
        fun onGetFavoriteCollectFormsError(error: Throwable?)
        fun onGetFavoriteCollectTemplatesSuccess(files: List<CollectTemplate>?)
        fun onGetFavoriteCollectTemplateError(error: Throwable?)
        fun onAllServerCountsEnded(serverCounts: ServerCounts)
        fun onServerCountFailed(error: Throwable?)
    }

    interface IPresenter : IBasePresenter {
        fun executePanicMode()
        fun exportMediaFiles(vaultFiles: List<VaultFile?>)
        fun getRecentFiles(filterType: FilterType?, sort: Sort?, limits: Limits)
        fun getFavoriteCollectForms()
        fun getFavoriteCollectTemplates()
        fun countAllServers()
    }
}