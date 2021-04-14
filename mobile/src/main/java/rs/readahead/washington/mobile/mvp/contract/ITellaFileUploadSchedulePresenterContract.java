package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.RawFile;

public class ITellaFileUploadSchedulePresenterContract {
    public interface IView {
        Context getContext();
        void onMediaFilesUploadScheduled();
        void onMediaFilesUploadScheduleError(Throwable throwable);
        void onGetMediaFilesSuccess(List<RawFile> mediaFiles);
        void onGetMediaFilesError(Throwable error);
    }

    public interface IPresenter extends IBasePresenter {
        void scheduleUploadMediaFiles(List<VaultFile> files);
        void scheduleUploadMediaFilesWithPriority(List<VaultFile> files, long uploadServerId, boolean metadata);
    }
}