package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import com.hzontal.tella_vault.VaultFile;

import java.io.File;


public class ICameraPresenterContract {
    public interface IView {
        void onAddingStart();
        void onAddingEnd();
        void onAddSuccess(VaultFile vaultFile);
        void onAddError(Throwable error);
        void rotateViews(int rotation);
        void onLastMediaFileSuccess(VaultFile mediaFile);
        void onLastMediaFileError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addJpegPhoto(byte[] jpeg);
        void addMp4Video(File file);
        void handleRotation(int orientation);
        void getLastMediaFile();
    }
}
