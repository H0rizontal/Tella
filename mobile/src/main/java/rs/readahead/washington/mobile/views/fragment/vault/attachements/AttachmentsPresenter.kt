package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.media.FileWalker
import rs.readahead.washington.mobile.media.MediaFileHandler

class AttachmentsPresenter(var view: IAttachmentsPresenter.IView?) :
    IAttachmentsPresenter.IPresenter {
    private val disposables = CompositeDisposable()

    override fun getFiles(parent: String?, filterType: FilterType?, sort: Sort?) {
        MyApplication.rxVault.get(parent)
            .subscribe(
                { vaultFile: VaultFile? ->
                    disposables.add(MyApplication.rxVault.list(vaultFile, filterType, sort, null)
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe { view?.onGetFilesStart() }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally { view?.onGetFilesEnd() }
                        .subscribe(
                            { vaultFiles: List<VaultFile?> ->
                                view?.onGetFilesSuccess(
                                    vaultFiles
                                )
                            }
                        ) { throwable: Throwable? ->
                            FirebaseCrashlytics.getInstance().recordException(throwable!!)
                            view?.onGetFilesError(throwable)
                        })
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onGetRootIdError(throwable)
            }.dispose()
    }

    override fun importVaultFiles(uris: List<Uri?>, parentId: String?) {
        disposables.add(Observable.fromCallable {
            MediaFileHandler.importVaultFilesUris(
                view?.getContext(), uris, parentId
            )
        }
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { view?.onImportStarted() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view?.onImportEnded() }
            .subscribe(
                { vaultFiles ->
                    view?.onMediaImported(vaultFiles)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onImportError(throwable)
            })

    }


    override fun addNewVaultFiles() {
        view?.onMediaFilesAdded()
    }

    override fun renameVaultFile(id: String, name: String) {
        disposables.add(MyApplication.rxVault.rename(id, name)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { view?.onRenameFileStart() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { view?.onRenameFileEnd() }
            .subscribe(
                { view?.onRenameFileSuccess() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onRenameFileError(throwable)
            })
    }

    override fun deleteVaultFiles(vaultFiles: List<VaultFile?>?) {
        if (vaultFiles == null) return

        val completable: MutableList<Single<Boolean>> = ArrayList()

        val resultList = walkAllFilesWithDirectories(vaultFiles)

        for (vaultFile in resultList) {
            vaultFile?.let { deleteFile(it) }?.let { completable.add(it) }
        }

        disposables.add(
            Single.zip(
                completable
            ) { objects: Array<Any?> -> objects.size }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { num: Int? -> view?.onMediaFilesDeleted(num!!) }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    view?.onMediaFilesDeletionError(throwable)
                }
        )
    }

    private fun deleteFile(vaultFile: VaultFile): Single<Boolean> {
        return MyApplication.rxVault.delete(vaultFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun deleteVaultFile(vaultFile: VaultFile?) {
        disposables.add(MyApplication.rxVault.delete(vaultFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { view?.onMediaFileDeleted() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onMediaFileDeletionError(throwable)
            })
    }

    override fun exportMediaFiles(vaultFiles: List<VaultFile?>) {
        disposables.add(
            Single
                .fromCallable {
                    val resultList = walkAllFiles(vaultFiles)
                    for (vaultFile in resultList) {
                        vaultFile?.let { MediaFileHandler.exportMediaFile(view?.getContext(), it)  }
                    }
                    vaultFiles.size
                }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { view?.onExportStarted() }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { view?.onExportEnded() }
                .subscribe(
                    { num: Int? -> view?.onMediaExported(num!!) }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    view?.onExportError(throwable)
                }
        )
    }

    private fun walkAllFiles(vaultFiles: List<VaultFile?>): List<VaultFile?> {
        val resultList = arrayListOf<VaultFile?>()
        for (vaultFile in vaultFiles) {
            if (vaultFile?.type == VaultFile.Type.DIRECTORY) {
                resultList.addAll(getAllFiles(vaultFile))
            } else {
                resultList.add(vaultFile)
            }
        }
        return resultList.toList()
    }

    private fun walkAllFilesWithDirectories(vaultFiles: List<VaultFile?>): List<VaultFile?> {
        val resultList = arrayListOf<VaultFile?>()
        val fileWalker = FileWalker()
        for (vaultFile in vaultFiles) {
            if (vaultFile?.type == VaultFile.Type.DIRECTORY) {
                resultList.addAll(fileWalker.walkWithDirectories(vaultFile))
                resultList.add(vaultFile)
            } else {
                resultList.add(vaultFile)
            }
        }
        return resultList.toList()
    }

    private fun getAllFiles(vaultFile: VaultFile): List<VaultFile> {
        val fileWalker = FileWalker()
        return fileWalker.walk(vaultFile)
    }

    override fun createFolder(folderName: String, parent: String) {
        MyApplication.rxVault
            .builder()
            .setName(folderName)
            .setType(VaultFile.Type.DIRECTORY)
            .build(parent)
            .subscribe(
                { view?.onCreateFolderSuccess() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onCountTUServersFailed(throwable)
            }.dispose()

    }

    override fun getRootId() {
        MyApplication.rxVault.root
            .subscribe(
                { vaultFile: VaultFile? -> view?.onGetRootIdSuccess(vaultFile) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                view?.onGetRootIdError(throwable)
            }.dispose()
    }


    override fun countTUServers() {
    }


    override fun destroy() {
        disposables.dispose()
        view = null
    }
}