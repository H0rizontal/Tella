package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.ViewUtil;

public class UploadSection extends Section {
    private List<FileUploadInstance> instances;
    private int numberOfUploads;
    private boolean isUploadFinished;
    private boolean expanded = !Preferences.isAutoDeleteEnabled();
    private VaultFileUrlLoader glideLoader;
    private long started;
    private long set;
    private UploadSectionListener uploadSectionListener;
    private Context context;

    public UploadSection(Context context, MediaFileHandler mediaFileHandler, @NonNull final List<FileUploadInstance> instances, @NonNull UploadSectionListener uploadSectionListener, Long set) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.upload_section_item)
                .headerResourceId(R.layout.upload_section_header)
                .footerResourceId(R.layout.upload_section_footer)
                .emptyResourceId(R.layout.upload_empty_layout)
                .failedResourceId(R.layout.upload_empty_layout)
                .build());
        this.context = context;
        this.glideLoader = new VaultFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.uploadSectionListener = uploadSectionListener;
        this.instances = instances;
        this.set = set;
        this.numberOfUploads = instances.size();
        this.isUploadFinished = true;
        this.started = instances.get(0).getStarted();
        for (FileUploadInstance instance : instances) {
            if (instance.getStatus() != ITellaUploadsRepository.UploadStatus.UPLOADED) {
                this.isUploadFinished = false;
                this.expanded = true;
            }
            if (instance.getStarted() < this.started) {
                this.started = instance.getStarted();
            }
        }
    }

    @Override
    public int getContentItemsTotal() {
        return expanded ? instances.size() : 0;
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new UploadViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(final RecyclerView.ViewHolder vholder, int position) {
        final VaultFile vaultFile = instances.get(position).getMediaFile();
        UploadViewHolder itemHolder = (UploadViewHolder) vholder;

        if (vaultFile == null) {
            itemHolder.mediaView.setImageDrawable(context.getResources().getDrawable(R.drawable.uploaded_empty_file));
            itemHolder.mediaView.setAlpha((float) 0.5);
        } else if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType) || (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType))) {
            Glide.with(itemHolder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(itemHolder.mediaView);
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            Drawable drawable = VectorDrawableCompat.create(context.getResources(),
                    R.drawable.ic_mic_gray, null);
            itemHolder.mediaView.setImageDrawable(drawable);
        }

        if (instances.get(position).getStatus() != ITellaUploadsRepository.UploadStatus.UPLOADED) {
            ViewUtil.setGrayScale(itemHolder.mediaView);
        } else {
            ViewUtil.setColored(itemHolder.mediaView);
        }

        itemHolder.itemView.setOnClickListener(v ->
                uploadSectionListener.onItemRootViewClicked(vaultFile)
        );
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(final View view) {
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(final RecyclerView.ViewHolder holder) {
        final HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

        headerHolder.startedText.setText(String.format("%s %s", context.getResources().getString(R.string.upload_meta_date_started), Util.getDateTimeString(this.started, "dd/MM/yyyy h:mm a")));

        if (isUploadFinished) {
            headerHolder.title.setText(context.getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files_uploaded, numberOfUploads, numberOfUploads));
            headerHolder.title.setVisibility(View.VISIBLE);
            headerHolder.startedText.setVisibility(View.VISIBLE);
        } else {
            headerHolder.title.setVisibility(View.GONE);
            headerHolder.startedText.setVisibility(View.GONE);
        }
    }

    @Override
    public RecyclerView.ViewHolder getFooterViewHolder(final View view) {
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindFooterViewHolder(final RecyclerView.ViewHolder holder) {
        final FooterViewHolder footerHolder = (FooterViewHolder) holder;
        footerHolder.fTitle.setText(context.getResources().getString(R.string.upload_main_action_more_details));
        footerHolder.fTitle.setOnClickListener(v -> uploadSectionListener.showUploadInformation(this.set));

        if (expanded) {
            footerHolder.fTitle.setVisibility(View.VISIBLE);
        } else {
            footerHolder.fTitle.setVisibility(View.GONE);
        }
    }

    static class UploadViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaView;

        UploadViewHolder(View itemView) {
            super(itemView);
            mediaView = itemView.findViewById(R.id.mediaView);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView startedText;

        HeaderViewHolder(@NonNull final View view) {
            super(view);

            title = view.findViewById(R.id.header_text);
            startedText = view.findViewById(R.id.started_text);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        final TextView fTitle;

        FooterViewHolder(@NonNull final View view) {
            super(view);

            this.fTitle = view.findViewById(R.id.footer_text);
        }
    }

    public interface UploadSectionListener {
        void showUploadInformation(final long set);
        void onItemRootViewClicked(VaultFile vaultFile);
    }
}
