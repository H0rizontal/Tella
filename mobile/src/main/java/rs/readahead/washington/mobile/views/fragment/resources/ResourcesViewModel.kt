package rs.readahead.washington.mobile.views.fragment.resources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.domain.entity.reports.ResourceTemplate
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.resources.ResourcesRepository
import rs.readahead.washington.mobile.domain.usecases.reports.GetReportsServersUseCase
import rs.readahead.washington.mobile.media.MediaFileHandler
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val resourcesRepository: ResourcesRepository,
    private val dataSource: DataSource
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val _progress = MutableLiveData<Boolean>()

    private val _resources = MutableLiveData<List<ResourceTemplate>>()
    val resources: LiveData<List<ResourceTemplate>> get() = _resources
    val progress: LiveData<Boolean> get() = _progress
    private val _serversList = MutableLiveData<List<TellaReportServer>>()
    val serversList: LiveData<List<TellaReportServer>> get() = _serversList

    /* private val _mediaImported = MutableLiveData<VaultFile>()
     val mediaImported: LiveData<VaultFile> = _mediaImported*/
    private val _downloadedResource = MutableLiveData<ResourceTemplate>()
    val downloadedResource: LiveData<ResourceTemplate> = _downloadedResource
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error

    fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { result ->
            _serversList.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun getResources() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: DataSource ->
                dataSource.listTellaUploadServers().toObservable()
            }
            .flatMap { servers: List<TellaReportServer> ->
                resourcesRepository.getResourcesResult(servers).toObservable()
            }
            .doFinally {
                _progress.postValue(false)
            }
            .subscribe({
                val resourcesList = mutableListOf<ResourceTemplate>()
                it.map { instance ->
                    resourcesList.addAll(instance.resources)
                }
                _resources.postValue(resourcesList)
            })
            { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
            })
    }

    fun downloadResource(resource: ResourceTemplate) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: DataSource ->
                dataSource.listTellaUploadServers().toObservable()
            }
            .flatMap { servers: List<TellaReportServer> ->
                resourcesRepository.downloadResource(servers[0], resource.fileName).toObservable()
            }
            .flatMap {
                MediaFileHandler.downloadPdfInputstream(
                    it.byteStream(),
                    resource.fileName,
                    null
                ).toObservable()
            }
            .doFinally {
                _progress.postValue(false)
            }
            .subscribe({
                _downloadedResource.postValue(resource)
            })
            { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
            })
    }

    fun dispose() {
        disposables.dispose()
    }

    fun clearDisposable() {
        resourcesRepository.getDisposable().clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        resourcesRepository.cleanup()
    }
}

