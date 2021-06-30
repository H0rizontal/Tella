package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.RawFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.AudioRecorder;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAudioCapturePresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadSchedulePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.AudioCapturePresenter;
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadSchedulePresenter;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.util.StringUtils;


@RuntimePermissions
public class AudioRecordActivity2 extends MetaDataFragment implements
        AudioRecorder.AudioRecordInterface,
        IAudioCapturePresenterContract.IView,
        ITellaFileUploadSchedulePresenterContract.IView,
        IMetadataAttachPresenterContract.IView {
    private static final String TIME_FORMAT = "%02d:%02d:%02d";
    public static String RECORDER_MODE = "rm";

    @BindView(R.id.record_audio)
    ImageButton mRecord;
    @BindView(R.id.play_audio)
    ImageButton mPlay;
    @BindView(R.id.stop_audio)
    ImageButton mStop;
    @BindView(R.id.audio_time)
    TextView mTimer;
    @BindView(R.id.free_space)
    TextView freeSpace;
    @BindView(R.id.red_dot)
    ImageView redDot;

    private ObjectAnimator animator;

    private boolean notRecording;

    private static final long UPDATE_SPACE_TIME_MS = 60000;
    private long lastUpdateTime;

    // handling MediaFile
    private MediaFile handlingMediaFile;

    // recording
    private AudioRecorder audioRecorder;
    private TellaFileUploadSchedulePresenter uploadPresenter;
    private AudioCapturePresenter presenter;
    private MetadataAttacher metadataAttacher;
    private CompositeDisposable disposable = new CompositeDisposable();
    private AlertDialog rationaleDialog;


    public enum Mode {
        COLLECT, // todo: mode is return/stand, add another one for view msgs settings
        RETURN,
        STAND
    }

    private Mode mode;


    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @org.jetbrains.annotations.Nullable ViewGroup container, @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_audio_record, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(activity);

        Toolbar toolbar = requireView().findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.recorder_app_bar);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        presenter = new AudioCapturePresenter(this);
        uploadPresenter = new TellaFileUploadSchedulePresenter(this);
        metadataAttacher = new MetadataAttacher(this);

        notRecording = true;

        mode = Mode.STAND;
       /* if (getIntent().hasExtra(RECORDER_MODE)) {
            mode = Mode.valueOf(getIntent().getStringExtra(RECORDER_MODE));
        }*/

        animator = (ObjectAnimator) AnimatorInflater.loadAnimator(activity, R.animator.fade_in);

        mTimer.setText(timeToString(0));
        disableStop();
    }


    @Override
    public void onResume() {
        super.onResume();

        if (presenter != null) {
            presenter.checkAvailableStorage();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            activity.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.record_audio, R.id.play_audio, R.id.stop_audio})//, R.id.evidence
    public void manageClick(View view) {
        switch (view.getId()) {
            case R.id.record_audio:
                if (notRecording) {
                    AudioRecordActivity2PermissionsDispatcher.handleRecordWithPermissionCheck(this);
                } else {
                    handlePause();
                }
                break;
            case R.id.play_audio:
                openRecordings();
                break;
            case R.id.stop_audio:
                handleStop();
                break;
        }
    }

  /*  @Override
    public void onBackPressed() {
        super.onBackPressed();

        setResult(Activity.RESULT_CANCELED);
        finish();
    }*/

    @Override
    public void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    public void onStop() {
        stopLocationMetadataListening();

        if (rationaleDialog != null && rationaleDialog.isShowing()) {
            rationaleDialog.dismiss();
        }

        super.onStop();
    }

    @Override
    public void onDestroy() {
        animator.end();
        animator = null;
        disposable.dispose();
        cancelRecorder();
        stopPresenter();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AudioRecordActivity2PermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    void onRecordAudioPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.RECORD_AUDIO)
    void onRecordAudioNeverAskAgain() {
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    void handleRecord() {
        notRecording = false;

        if (audioRecorder == null) {   //first start or restart
            disablePlay();
            handlingMediaFile = null;
            cancelRecorder();

            audioRecorder = new AudioRecorder(activity, this);
            disposable.add(audioRecorder.startRecording()
                    .subscribe(this::onRecordingStopped, throwable -> onRecordingError())
            );
        } else {
            canclePauseRecorder();
        }

        disableRecord();
        enableStop();
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO)
    void showRecordAudioRationale(final PermissionRequest request) {
        rationaleDialog = PermissionUtil.showRationale(activity, request, getString(R.string.permission_dialog_expl_mic));
    }

    @Override
    public void onAddingStart() {
    }

    @Override
    public void onAddingEnd() {
    }

    @Override
    public void onAddSuccess(MediaFile mediaFile) {
        attachMediaFileMetadata(mediaFile, metadataAttacher);
        activity.showToast(String.format(getString(R.string.recorder_toast_recording_saved), getString(R.string.app_name)));
    }

    @Override
    public void onAddError(Throwable error) {
        activity.showToast(R.string.gallery_toast_fail_saving_file);
    }

    @Override
    public void onAvailableStorage(long memory) {
        updateStorageSpaceLeft(memory);
    }

    @Override
    public void onAvailableStorageFailed(Throwable throwable) {
    }

    @Override
    public void onMetadataAttached(long mediaFileId, @Nullable Metadata metadata) {
        Intent intent = new Intent();

        if (mode == Mode.COLLECT) {
            intent.putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, handlingMediaFile);
        } else {
            intent.putExtra(C.CAPTURED_MEDIA_FILE_ID, mediaFileId);
        }

        activity.setResult(Activity.RESULT_OK, intent);
        mTimer.setText(timeToString(0));

        scheduleFileUpload(handlingMediaFile);
    }

    @Override
    public void onMetadataAttachError(Throwable throwable) {
        activity.showToast(R.string.gallery_toast_fail_saving_file);
    }

    @Override
    public void onDurationUpdate(long duration) {
        activity.runOnUiThread(() -> mTimer.setText(timeToString(duration)));

        if (duration > UPDATE_SPACE_TIME_MS + lastUpdateTime) {
            lastUpdateTime += UPDATE_SPACE_TIME_MS;

            if (presenter != null) {
                presenter.checkAvailableStorage();
            }
        }
    }

    @Override
    public Context getContext() {
        return activity;
    }

    @Override
    public void onMediaFilesUploadScheduled() {
        if (mode != Mode.STAND) {
            //finish();
        }
    }

    @Override
    public void onMediaFilesUploadScheduleError(Throwable throwable) {

    }

    @Override
    public void onGetMediaFilesSuccess(List<RawFile> mediaFiles) {

    }

    @Override
    public void onGetMediaFilesError(Throwable error) {

    }

    private void handleStop() {
        notRecording = true;
        stopRecorder();
    }

    private void handlePause() {
        pauseRecorder();
        enableRecord();
        notRecording = true;
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void onRecordingStopped(MediaFile mediaFile) {
        if (MediaFile.NONE.equals(mediaFile)) {
            handlingMediaFile = null;

            disableStop();
            disablePlay();
            enableRecord();

        } else {
            handlingMediaFile = mediaFile;
            handlingMediaFile.setSize(MediaFileHandler.getSize(getContext(), mediaFile));

            disableStop();
            enablePlay();
            enableRecord();

            returnData();
        }
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void onRecordingError() {
        handlingMediaFile = null;

        disableStop();
        disablePlay();
        enableRecord();

        mTimer.setText(timeToString(0));
        activity.showToast(R.string.recorder_toast_fail_recording);
    }

    private void returnData() {
        if (handlingMediaFile != null) {
            presenter.addMediaFile(handlingMediaFile);
        }
    }

    private void disableRecord() {
        mRecord.setBackground(getContext().getResources().getDrawable(R.drawable.white_circle_background));
        mRecord.setImageResource(R.drawable.ic_pause_black_24dp);
        redDot.setVisibility(View.VISIBLE);

        animator.setTarget(redDot);
        animator.start();
    }

    private void enableRecord() {
        mRecord.setBackground(getContext().getResources().getDrawable(R.drawable.audio_record_button_background));
        mRecord.setImageResource(R.drawable.ic_mic_white);
        redDot.setVisibility(View.GONE);
        animator.end();
    }

    private void disablePlay() {
        disableButton(mPlay);
    }

    private void enablePlay() {
        enableButton(mPlay);
    }

    private void disableStop() {
        disableButton(mStop);
    }

    private void enableStop() {
        enableButton(mStop);
    }

    private void enableButton(ImageButton button) {
        button.setEnabled(true);
        button.setAlpha(1f);
    }

    private void disableButton(ImageButton button) {
        button.setEnabled(false);
        button.setAlpha(.2f);
    }

    private void openRecordings() {
        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.putExtra(GalleryActivity.GALLERY_FILTER, IMediaFileRecordRepository.Filter.AUDIO.name());
        intent.putExtra(GalleryActivity.GALLERY_ALLOWS_ADDING, false);
        startActivity(intent);
    }

    private void stopRecorder() {
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder = null;
        }
    }

    private void pauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder.pauseRecording();
        }
    }

    private void canclePauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder.cancelPause();
        }
    }

    private void cancelRecorder() {
        if (audioRecorder != null) {
            audioRecorder.cancelRecording();
            audioRecorder = null;
        }
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private String timeToString(long duration) {
        return String.format(Locale.ROOT, TIME_FORMAT,
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) -
                        TimeUnit.MINUTES.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }


    private void updateStorageSpaceLeft(long memoryLeft) {
        double timeMinutes = memoryLeft / 262144.0; // 4 minutes --> 1MB approximation/1024*256
        // todo: move this (262144.0) number to recorder to provide

        int days = (int) (timeMinutes / 1440);
        int hours = (int) ((timeMinutes - days * 1440) / 60);
        int minutes = (int) (timeMinutes - days * 1440 - hours * 60);

        String spaceLeft = StringUtils.getFileSize(memoryLeft);

        if (days < 1 && hours < 12) {
            freeSpace.setText(getString(R.string.recorder_meta_space_available_hours, hours, minutes, spaceLeft));
        } else {
            freeSpace.setText(getString(R.string.recorder_meta_space_available_days, days, hours, spaceLeft));
        }
    }

    private void scheduleFileUpload(MediaFile mediaFile) {
        if (Preferences.isAutoUploadEnabled()) {
            List<MediaFile> upload = Collections.singletonList(mediaFile);
            uploadPresenter.scheduleUploadMediaFiles(upload);
        } else {
            onMediaFilesUploadScheduled();
        }
    }
}
