package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.hzontal.shared_ui.buttons.PanelToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.uwazi.SelectValue;
import rs.readahead.washington.mobile.domain.entity.uwazi.Value;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziRelationShipEntity;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;
import rs.readahead.washington.mobile.views.custom.CollectRelationShipPreviewView;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.searchable_multi_select.SearchableItem;


@SuppressLint("ViewConstructor")
public class UwaziRelationShipWidget extends UwaziQuestionWidget {
    private final HashMap<String, FormMediaFile> formFiles = new HashMap<>();
    private ArrayList<String> relationShipFiles = new ArrayList<>();

    Context context;
    Button addFiles;
    ImageButton clearButton;
    ViewGroup filesPanel;
    PanelToggleButton filesToggle;
    TextView numberOfFiles;
    Boolean isPdf;
    View infoFilePanel;
    private final List<UwaziRelationShipEntity> items;

    private final ArrayList<AppCompatCheckBox> checkBoxes;
    // counter of all checkboxes which can be in result set
    // List of all Ids which can be in result set (Ids from all checkboxes without header checkboxes)
    private final ArrayList<String> checkIds = new ArrayList<>();

    public UwaziRelationShipWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt, boolean isPdf) {
        super(context, formEntryPrompt);
        checkBoxes = new ArrayList<>();
        items = formEntryPrompt.getEntities();

        this.context = context;
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);


        addImageWidgetViews(linearLayout);
        addAnswerView(linearLayout);
        setHelpTextView(getContext().getString(R.string.Uwazi_RelationShipWidget_Select_Entities));
        clearAnswer();
        if(items != null) showPreviewFromDraft();

    }


    @Override
    public ArrayList<String> getAnswer() {
//        List<String> vc = new ArrayList<>();
//        List<UwaziValue> selection = new ArrayList<>();
//
//        for (int i = 0; i < checkBoxes.size(); ++i) {
//            AppCompatCheckBox c = checkBoxes.get(i);
//            if (c.isChecked()) {
//                vc.add(checkIds.get(i));
//            }
//        }
//
//        if (vc.size() == 0) {
//            return null;
//        } else {
//            for (String answer : vc) {
//                UwaziValue uValue = new UwaziValue(answer);
//                selection.add(uValue);
//            }
            return relationShipFiles;
        //}
    }

    @Override
    public void setFocus(Context context) {

    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        List<SearchableItem> files;
        List<String> result;
        if (data instanceof ArrayList) {
            result = ((List<String>) data);

        } else {
            result = new Gson().fromJson((String) data, new TypeToken<List<String>>() {
            }.getType());
        }
        if (result != null && !result.isEmpty()) {

            String[] ids = Arrays.copyOf(
                    result.toArray(), result.size(),
                    String[].class);
            relationShipFiles = (ArrayList<String>) result;
            if (!relationShipFiles.isEmpty()) {
                for (String relation : relationShipFiles) {
                    if (!relationShipFiles.contains(relation)) {
                        relationShipFiles.add(relation);
                    }
                }
                showPreview();
                return getFilenames().toString();
            }
        }

        return null;
    }


    protected List<String> getFilenames() {
        if (relationShipFiles != null) {
            return new ArrayList<>(relationShipFiles);
        } else {
            return null;
        }
    }

    private void showPreview() {
        filesPanel.removeAllViews();
        infoFilePanel.setVisibility(VISIBLE);
        clearButton.setVisibility(VISIBLE);
        for (String file : relationShipFiles) {
            CollectRelationShipPreviewView previewView = new CollectRelationShipPreviewView(context, null,file);
            filesPanel.addView(previewView);
        }
        numberOfFiles.setText(context.getResources().getQuantityString(R.plurals.Uwazi_RelationShipWidget_EntitiesAttached, relationShipFiles.size(), relationShipFiles.size()));
        addFiles.setText("Add more entities");
        filesToggle.setOpen();
    }
    private void showPreviewFromDraft() {
         filesPanel.removeAllViews();
        infoFilePanel.setVisibility(VISIBLE);
        clearButton.setVisibility(VISIBLE);
        for (UwaziRelationShipEntity entry : items) {
            CollectRelationShipPreviewView previewView = new CollectRelationShipPreviewView(context, null,entry.getLabel());
            filesPanel.addView(previewView);
        }
        numberOfFiles.setText(context.getResources().getQuantityString(R.plurals.Uwazi_RelationShipWidget_EntitiesAttached, relationShipFiles.size(), relationShipFiles.size()));
        addFiles.setText("Add more entities");
        filesToggle.setOpen();
    }
    private void addImageWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.uwazi_widget_multifile, linearLayout, true);

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));
        clearButton.setOnClickListener(v -> clearAnswer());

        infoFilePanel = view.findViewById(R.id.infoFilePanel);
        filesPanel = view.findViewById(R.id.files);
        filesToggle = view.findViewById(R.id.toggle_button);
        filesToggle.setVisibility(GONE);
        numberOfFiles = view.findViewById(R.id.numOfFiles);
        addFiles = view.findViewById(R.id.addText);
        addFiles.setText(R.string.select_entities);
        addFiles.setOnClickListener(v -> showSelectEntitiesScreen());
    }

    private void showSelectEntitiesScreen() {
        waitingForAData = true;
        ((OnSelectEntitiesClickListener) getContext()).onSelectEntitiesClicked(formEntryPrompt,getFilenames());
    }

    @Override
    public void clearAnswer() {
        relationShipFiles.clear();
        infoFilePanel.setVisibility(GONE);
        clearButton.setVisibility(View.GONE);
        filesPanel.removeAllViews();
        addFiles.setText(R.string.select_entities);
    }

}
