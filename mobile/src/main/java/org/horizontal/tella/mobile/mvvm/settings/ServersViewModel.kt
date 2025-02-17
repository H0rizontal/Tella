package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import javax.inject.Inject

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _serversDeleted = SingleLiveEvent<Boolean>()
    val serversDeleted: LiveData<Boolean> get() = _serversDeleted

    private val _error = SingleLiveEvent<Throwable>()
    val error: LiveData<Throwable> get() = _error

    fun deleteServers() {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable { dataSource: DataSource ->
                    dataSource.deleteAllServers()
                }
                .subscribe(
                    {
                        _serversDeleted.postValue(true)
                    },
                    { throwable: Throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(throwable)
                    }
                )
        )
    }

    fun removeAutoUploadServersSettings() {
        Preferences.setAutoUpload(false)
        Preferences.setAutoUploadServerId(-1)
    }

    fun getAutoUploadServerId(): Long {
        return Preferences.getAutoUploadServerId()
    }

    fun setAutoUploadServerId(id: Long) {
        Preferences.setAutoUploadServerId(id)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose() // Clean up Rx subscriptions
    }
}