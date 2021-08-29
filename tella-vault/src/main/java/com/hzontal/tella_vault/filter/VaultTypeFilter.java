package com.hzontal.tella_vault.filter;


import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;


import timber.log.Timber;

public class VaultTypeFilter implements Filter {

    @NonNull
    public FilterType filterType = FilterType.ALL;

    public VaultTypeFilter() {
    }

    @SuppressLint("TimberArgCount")
    @Override
    public boolean applyFilter(VaultFile vaultFile) {

        Timber.d("VaultTypeFilter ",vaultFile.mimeType);
        switch (filterType) {
            case AUDIO:
                return vaultFile.mimeType != null && MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType);
            case PHOTO:
                return vaultFile.mimeType != null && MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType);

            case VIDEO:
                return vaultFile.mimeType != null && MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType);
            default:
                return !vaultFile.id.equals("1");
        }
    }

}
