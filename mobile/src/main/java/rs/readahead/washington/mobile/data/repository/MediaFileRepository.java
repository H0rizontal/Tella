package rs.readahead.washington.mobile.data.repository;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;

import java.util.Collection;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.entity.mapper.EntityMapper;
import rs.readahead.washington.mobile.data.rest.ReportsApi;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRepository;


public class MediaFileRepository implements IMediaFileRepository {
    @Override
    public Completable registerFormAttachments(@NonNull Collection<VaultFile> mediaFiles) {
        return ReportsApi.getApi().registerFormAttachments(new EntityMapper().transformMediaFiles(mediaFiles))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> Completable.error(new ErrorBundle(throwable)));
    }
}
