package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.subjects.BehaviorSubject;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.MyLocation;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.presentation.entity.SensorData;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.LocationUtil;
import rs.readahead.washington.mobile.util.MetadataUtils;
import rs.readahead.washington.mobile.util.TelephonyUtils;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.base_ui.BaseLockFragment;

public class MetaDataFragment  extends BaseLockFragment implements
        SensorEventListener {
    private static final long LOCATION_REQUEST_INTERVAL = 5000; // aggressive

    private SensorManager mSensorManager;
    private Sensor mLight;
    private Sensor mAmbientTemperature;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanResultReceiver;

    private static Location currentBestLocation;

    private boolean locationListenerRegistered = false;
    private boolean wifiReceiverRegistered = false;

    private boolean sensorListenerRegistered = false;
    private static SensorData lightSensorData = new SensorData();
    private static SensorData ambientTemperatureSensorData = new SensorData();

    private final BehaviorSubject<List<String>> wifiSubject = BehaviorSubject.create();
    private final static BehaviorSubject<MyLocation> locationSubject = BehaviorSubject.create();
    private LocationManager locationManager;

    private AlertDialog metadataAlertDialog;
    private AlertDialog locationAlertDialog;
    private Relay<MetadataActivity.MetadataHolder> metadataCancelRelay;
    private CompositeDisposable disposables;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sensors
        mSensorManager = (SensorManager) activity.getApplicationContext().getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAmbientTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        // Location
        locationManager = (LocationManager) activity.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
        locationCallback = new MetadataLocationCallback();

        // Wifi
        wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wifiSubject.onNext(getWifiStrings(wifiManager.getScanResults()));
            }
        };

        // UI stuff
        metadataCancelRelay = PublishRelay.create();
        disposables = new CompositeDisposable();
    }

    private List<String> getWifiStrings(List<ScanResult> results) {
        List<String> wifiStrings = new ArrayList<>(results.size());

        for (ScanResult result : results) {
            wifiStrings.add(result.SSID);
        }

        return wifiStrings;
    }

    protected void startSensorListening() {
        if (Preferences.isAnonymousMode()) {
            return;
        }

        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAmbientTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        sensorListenerRegistered = true;
    }

    protected void startLocationMetadataListening() {
        if (Preferences.isAnonymousMode()) {
            return;
        }

        startLocationListening();
        startWifiListening();
    }

    @SuppressWarnings("MissingPermission") // we have check
    private synchronized void startLocationListening() {
        if (isFineLocationPermissionDenied()) {
            return;
        }

        // google services way..
        fusedLocationProviderClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
        locationListenerRegistered = true;

        // get last known location to start with..
        getLastLocation();
    }

    @SuppressWarnings("MissingPermission") // we have check
    private void getLastLocation() {
        if (Preferences.isAnonymousMode()) {
            return;
        }

        if (isFineLocationPermissionDenied()) {
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        acceptBetterLocation(location);
                    }
                });
    }

    private synchronized void startWifiListening() {
        if (isFineLocationPermissionDenied()) {
            return;
        }

        if (wifiManager == null || wifiReceiverRegistered) {
            return;
        }

        // put what you know in subject..
        wifiSubject.onNext(getWifiStrings(wifiManager.getScanResults()));

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        activity.registerReceiver(wifiScanResultReceiver, filter);
        wifiReceiverRegistered = true;
    }

    protected synchronized void startWifiScan() {
        if (Preferences.isAnonymousMode()) {
            return;
        }

        if (wifiManager != null && wifiReceiverRegistered) {
            wifiManager.startScan();
        }
    }

    private void stopSensorListening() {
        if (!sensorListenerRegistered) {
            return;
        }

        mSensorManager.unregisterListener(this);
        sensorListenerRegistered = false;
    }

    protected void stopLocationMetadataListening() {
        stopLocationListening();
        stopWifiListening();
    }

    private synchronized void stopLocationListening() {
        if (!locationListenerRegistered) {
            return;
        }

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(task -> locationListenerRegistered = false);
    }

    private synchronized void stopWifiListening() {
        if (!wifiReceiverRegistered) {
            return;
        }

        activity.unregisterReceiver(wifiScanResultReceiver);
        wifiReceiverRegistered = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        startSensorListening();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopSensorListening();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_LIGHT) {
            lightSensorData.setValue(event.timestamp, event.values[0]);
        } else if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            ambientTemperatureSensorData.setValue(event.timestamp, event.values[0]);
        }
    }

    @Override
    public void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }
        hideLocationAlertDialog();
        wifiSubject.onComplete();

        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean isFineLocationPermissionDenied() {
        return (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED);
    }

    private boolean isLocationProviderEnabled() {
        final LocationManager lm = (LocationManager) activity.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            return false;
        }

        // if we have GPS, we have location gathering..
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    interface LocationSettingsCheckDoneListener {
        void onContinue();
    }

    protected void checkLocationSettings(final int requestCode, final MetadataActivity.LocationSettingsCheckDoneListener listener) {
        if (isFineLocationPermissionDenied()) {
            listener.onContinue();
            return;
        }

        if (!Preferences.isAnonymousMode() && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsMetadataDialog(requestCode, listener);
        } else {
            listener.onContinue();
        }
    }

    protected void manageLocationSettings(final int requestCode, final MetadataActivity.LocationSettingsCheckDoneListener listener) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(createLocationRequest());

        SettingsClient client = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener((Executor) this, locationSettingsResponse -> listener.onContinue());

        task.addOnFailureListener((Executor) this, e -> {
            int statusCode = ((ApiException) e).getStatusCode();
            switch (statusCode) {
                case CommonStatusCodes.RESOLUTION_REQUIRED:
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(activity, requestCode);
                    } catch (IntentSender.SendIntentException ignored) {
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    listener.onContinue();
                    break;
            }
        });
    }

    private void showGpsMetadataDialog(final int requestCode, final MetadataActivity.LocationSettingsCheckDoneListener listener) {
        String message = getString(R.string.verification_prompt_dialog_expl);

        locationAlertDialog = DialogsUtil.showMessageOKCancelWithTitle(activity,
                message,
                getString(R.string.verification_prompt_dialog_title),
                getString(R.string.verification_prompt_action_ignore),
                getString(R.string.verification_prompt_action_enable_GPS),
                (dialog, which) -> {  //ignore
                    dialog.dismiss();
                    listener.onContinue();
                },
                (dialog, which) -> {  //turn on gps
                    manageLocationSettings(requestCode, listener);
                    dialog.dismiss();
                });
    }

    public SensorData getLightSensorData() {
        return lightSensorData;
    }

    public SensorData getAmbientTemperatureSensorData() {
        return ambientTemperatureSensorData;
    }

    public Observable<List<String>> observeWifiData() {
        return wifiSubject;
    }

    public Observable<MyLocation> observeLocationData() {
        return locationSubject;
    }

    /**
     * Will emit combined object consisting of emitted both wifi and location data
     * combined, each time one of them changes. If there is no data for one of them,
     * empty data is in MetadataHolder object.
     *
     * @return stream of metadata holder objects
     */
    public Observable<MetadataActivity.MetadataHolder> observeMetadata() {
        return Observable.combineLatest(
                observeLocationData().startWith(MyLocation.createEmpty()),
                observeWifiData().startWith(Collections.<String>emptyList()),
                MetadataActivity.MetadataHolder::new
        )
                .filter(mh -> (!mh.getWifis().isEmpty() || !mh.getLocation().isEmpty()))
                .take((5 * 60 * 1000) / LOCATION_REQUEST_INTERVAL) // approx max 5 min of trying limit
                .takeUntil(mh -> !mh.getWifis().isEmpty() && !mh.getLocation().isEmpty());
    }

    private static class MetadataLocationCallback extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location location = locationResult.getLastLocation();
            acceptBetterLocation(location);
        }
    }

    private static void acceptBetterLocation(Location location) {
        if (!LocationUtil.isBetterLocation(location, currentBestLocation)) {
            return;
        }

        currentBestLocation = location;
        locationSubject.onNext(MyLocation.fromLocation(location));
    }

    // UI stuff
    protected void attachMediaFileMetadata(final MediaFile mediaFile, final IMetadataAttachPresenterContract.IPresenter metadataAttacher) {
        // skip metadata if anonymous mode..
        if (Preferences.isAnonymousMode()) {
            metadataAttacher.attachMetadata(mediaFile.getId(), null);
            return;
        }

        startWifiScan();

        final Metadata metadata = new Metadata();

        metadata.setFileName(mediaFile.getFileName());
        metadata.setFileHashSHA256(mediaFile.getHash());
        metadata.setTimestamp(System.currentTimeMillis());
        metadata.setAmbientTemperature(getAmbientTemperatureSensorData().hasValue() ? getAmbientTemperatureSensorData().getValue() : null);
        metadata.setLight(getLightSensorData().hasValue() ? getLightSensorData().getValue() : null);

        metadata.setDeviceID(MetadataUtils.getDeviceID());
        metadata.setWifiMac(MetadataUtils.getWifiMac());
        metadata.setIPv4(MetadataUtils.getIPv4());
        metadata.setIPv6(MetadataUtils.getIPv6());

        metadata.setDataType(MetadataUtils.getDataType(activity));
        metadata.setNetwork(MetadataUtils.getNetwork(activity));

        metadata.setNetworkType(MetadataUtils.getNetworkType(activity));
        metadata.setHardware(MetadataUtils.getHardware());
        metadata.setManufacturer(MetadataUtils.getManufacturer());
        metadata.setScreenSize(MetadataUtils.getScreenSize(activity));

        metadata.setLanguage(MetadataUtils.getLanguage());
        metadata.setLocale(MetadataUtils.getLocale());

        // set cells
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            metadata.setCells(TelephonyUtils.getCellInfo(activity));
        }

        // if location gathering is not possible skip it
        if (!isLocationProviderEnabled()) {
            metadataAttacher.attachMetadata(mediaFile.getId(), metadata);
            return;
        }

        // wait for set location metadata
        disposables.add(observeMetadata()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> showMetadataProgressBarDialog())
                .takeUntil(metadataCancelRelay) // this observable emits when user press skip in dialog.
                .doFinally(this::hideMetadataProgressBarDialog)
                .subscribeWith(new DisposableObserver<MetadataActivity.MetadataHolder>() {
                    @Override
                    public void onNext(MetadataActivity.MetadataHolder value) {
                        if (!value.getWifis().isEmpty()) {
                            metadata.setWifis(value.getWifis());
                            networkGatheringChecked();
                        }

                        if (!value.getLocation().isEmpty()) {
                            metadata.setMyLocation(value.getLocation());
                            locationGahteringChecked();
                        }

                        // skip if wifi gathering not possible in airplane mode
                        if ((Settings.Global.getInt(activity.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0)
                                && !wifiManager.isWifiEnabled() && metadata.getWifis() == null) {
                            List<String> wifis = new ArrayList<>();
                            metadata.setWifis(wifis);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        onComplete();
                    }

                    @Override
                    public void onComplete() {
                        metadataAttacher.attachMetadata(mediaFile.getId(), metadata);
                    }
                })
        );
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    protected void showMetadataProgressBarDialog() {
        metadataAlertDialog = DialogsUtil.showMetadataProgressBarDialog(activity, (dialog, which) -> {
            metadataCancelRelay.accept(MetadataActivity.MetadataHolder.createEmpty()); // :)
        });
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    protected void hideMetadataProgressBarDialog() {
        if (metadataAlertDialog != null) {
            metadataAlertDialog.dismiss();
        }
    }

    private void networkGatheringChecked() {
        if (metadataAlertDialog != null) {
            //noinspection ConstantConditions
            metadataAlertDialog.findViewById(R.id.networkProgress).setVisibility(View.GONE);
            //noinspection ConstantConditions
            metadataAlertDialog.findViewById(R.id.networkCheck).setVisibility(View.VISIBLE);
        }
    }

    private void locationGahteringChecked() {
        if (metadataAlertDialog != null) {
            //noinspection ConstantConditions
            metadataAlertDialog.findViewById(R.id.locationProgress).setVisibility(View.GONE);
            //noinspection ConstantConditions
            metadataAlertDialog.findViewById(R.id.locationCheck).setVisibility(View.VISIBLE);
        }
    }

    protected void hideLocationAlertDialog() {
        if (locationAlertDialog != null) {
            locationAlertDialog.dismiss();
        }
    }

    // Helper Classes
    static class MetadataHolder {
        private MyLocation location;
        private List<String> wifis;


        MetadataHolder(MyLocation location, List<String> wifis) {
            this.location = location;
            setWifis(wifis);
        }

        MyLocation getLocation() {
            return location;
        }

        List<String> getWifis() {
            return wifis;
        }

        private void setWifis(final List<String> wifis) {
            this.wifis = new ArrayList<>();

            for (String wifi : wifis) {
                if (!this.wifis.contains(wifi)) {
                    this.wifis.add(wifi);
                }
            }
        }

        static MetadataActivity.MetadataHolder createEmpty() {
            return new MetadataActivity.MetadataHolder(MyLocation.createEmpty(), Collections.emptyList());
        }
    }
}