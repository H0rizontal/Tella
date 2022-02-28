package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.utils.MediaFile;

import org.hzontal.shared_ui.submission.SubmittingItem;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance;
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.FileUtil;

@SuppressLint("ViewConstructor")
public class UwaziFormEndView extends FrameLayout {
    LinearLayout partsListView;
    TextView titleView;
    //TextView subTitleView;
    String title;
    private UwaziEntityInstance instance;
    private boolean previewUploaded;
    private final RequestManager.ImageModelRequest<VaultFileLoaderModel> glide;


    public UwaziFormEndView(Context context, @StringRes int titleResId) {
        super(context);
        inflate(context, R.layout.uwazi_form_end_view, this);

        title = getResources().getString(titleResId);

        titleView = findViewById(R.id.title);

        titleView.setText(title);

        MediaFileHandler mediaFileHandler = new MediaFileHandler();
        VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
        glide = Glide.with(getContext()).using(glideLoader);
    }

    public UwaziFormEndView(Context context,String title) {
        super(context);
        inflate(context, R.layout.uwazi_form_end_view, this);

        this.title = title;

        titleView = findViewById(R.id.title);

        titleView.setText(title);

        MediaFileHandler mediaFileHandler = new MediaFileHandler();
        VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
        glide = Glide.with(getContext()).using(glideLoader);
    }

    public void setInstance(@NonNull UwaziEntityInstance instance, boolean offline, boolean previewUploaded) {
        this.instance = instance;
        this.previewUploaded = previewUploaded;
        refreshInstance(offline);
    }

    public void refreshInstance(boolean offline) {
        if (this.instance == null) {
            return;
        }

        //  titleView.setText(title);

        TextView formNameView = findViewById(R.id.formName);

        formNameView.setText(Objects.requireNonNull(instance.getTitle()));

        int formElements = 1;

        long formSize = instance.getTitle().getBytes(StandardCharsets.UTF_8).length +
                instance.getMetadata().toString().getBytes(StandardCharsets.UTF_8).length +
                instance.getType().getBytes(StandardCharsets.UTF_8).length;

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        partsListView.addView(createFormSubmissionPartItemView(instance, formSize, offline));
        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            partsListView.addView(createFormMediaFileItemView(mediaFile, offline));
            formSize += mediaFile.size;
            formElements++;
        }

       // TextView formElementsView = findViewById(R.id.formElements);
        TextView formSizeView = findViewById(R.id.formSize);

    //    formElementsView.setText(getResources().getQuantityString(R.plurals.collect_end_meta_number_of_elements, formElements, formElements));
        formSizeView.setText(FileUtil.getFileSizeString(formSize));
    }

    public void showUploadProgress(String partName) {
        titleView.setText(R.string.collect_end_heading_submitting);
      //  subTitleView.setVisibility(GONE);

        SubmittingItem item = findViewWithTag(partName);
        if (item != null) {
            item.setPartUploading();
        }
    }

    public void hideUploadProgress(String partName) {
        SubmittingItem item = findViewWithTag(partName);
        if (item != null) {
            item.setPartUploaded();
        }
    }

    public void setUploadProgress(String partName, float pct) {
        if (pct < 0 || pct > 1) {
            return;
        }

        SubmittingItem item = findViewWithTag(partName);
        if (item != null) {
            item.setUploadProgress(pct);
        }
    }

    public void clearPartsProgress(UwaziEntityInstance instance) {
        setPartsCleared(instance);
    }

    private View createFormSubmissionPartItemView(@NonNull UwaziEntityInstance instance, long size, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);

        item.setTag("UWAZI_RESPONSE");

        item.setPartName(R.string.collect_end_item_form_data);
        item.setPartSize(size);
        item.setPartIcon(R.drawable.ic_assignment_white_24dp);

        if (instance.getStatus() == UwaziEntityStatus.SUBMITTED ||
                instance.getStatus() == UwaziEntityStatus.SUBMITTED || // back compatibility down
                instance.getStatus() == UwaziEntityStatus.SUBMISSION_PARTIAL_PARTS) {
            item.setPartUploaded();
        } else {
            item.setPartPrepared(offline);
        }

        if (offline || instance.getStatus() == UwaziEntityStatus.SUBMITTED) {
         //   subTitleView.setVisibility(GONE);
        } else {
           // subTitleView.setVisibility(VISIBLE);
        }

        return item;
    }

    private View createFormMediaFileItemView(@NonNull FormMediaFile mediaFile, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);
        ImageView thumbView = item.findViewById(R.id.fileThumb);
        item.setTag(mediaFile.getPartName());

        item.setPartName(mediaFile.name);
        item.setPartSize(mediaFile.size);

        if (MediaFile.INSTANCE.isImageFileType(mediaFile.mimeType) || (MediaFile.INSTANCE.isVideoFileType(mediaFile.mimeType))) {
            glide.load(new VaultFileLoaderModel(mediaFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(thumbView);
        } else if (MediaFile.INSTANCE.isAudioFileType(mediaFile.mimeType)) {
            item.setPartIcon(R.drawable.ic_headset_white_24dp);
        } else {
            item.setPartIcon(R.drawable.ic_attach_file_white_24dp);
        }

        if (mediaFile.status == FormMediaFileStatus.SUBMITTED || previewUploaded) {
            item.setPartUploaded();
        } else {
            item.setPartPrepared(offline);
        }

        return item;
    }

    private void setPartsCleared(UwaziEntityInstance instance) {
        SubmittingItem item = partsListView.findViewWithTag("UWAZI_RESPONSE");

        if (instance.getStatus() == UwaziEntityStatus.SUBMITTED ||
                instance.getStatus() == UwaziEntityStatus.SUBMISSION_PARTIAL_PARTS) {
            item.setPartUploaded();
        } else {
            item.setPartCleared();
        }

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            item = partsListView.findViewWithTag(mediaFile.getPartName());

            if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
                item.setPartUploaded();
            } else {
                item.setPartCleared();
            }
        }
    }
}

