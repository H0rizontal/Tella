package rs.readahead.washington.mobile.views.activity;

import static com.hzontal.tella_locking_ui.ConstantsKt.IS_CAMOUFLAGE;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.util.CamouflageManager;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.settings.ChangeRemoveCamouflage;
import rs.readahead.washington.mobile.views.settings.HideTella;
import rs.readahead.washington.mobile.views.settings.OnFragmentSelected;


public class SettingsActivity extends BaseLockActivity implements OnFragmentSelected {
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private ActionBar actionBar;
    private EventCompositeDisposable disposables;
    private final CamouflageManager cm = CamouflageManager.getInstance();
    protected boolean isCamouflage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings_app_bar);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.appbar).setOutlineProvider(null);
        } else {
            findViewById(R.id.appbar).bringToFront();
        }

        if (getIntent().hasExtra(IS_CAMOUFLAGE)) {
            if (cm.isDefaultLauncherActivityAlias()) {
                addFragment(new HideTella(),R.id.my_nav_host_fragment);
            } else {
                addFragment(new ChangeRemoveCamouflage(),R.id.my_nav_host_fragment);
            }
        }

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(@NotNull LocaleChangedEvent event) {
                recreate();
            }
        });
    }

    @Override
    public void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        super.onDestroy();
    }

   @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setToolbarLabel(int labelRes) {
        actionBar.setTitle(getString(labelRes));
    }

    @Override
    public void hideAppbar() {
        actionBar.hide();
    }

    @Override
    public void showAppbar() {
        actionBar.show();
    }

    @Override
    public void setToolbarHomeIcon(int iconRes) {
        actionBar.setHomeAsUpIndicator(iconRes);
    }

    @Override
    public boolean isCamouflage() {
        return isCamouflage;
    }
}
