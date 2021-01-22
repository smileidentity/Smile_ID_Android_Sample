package com.demo.smileid.sid_sdk.geoloc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;

import com.smileidentity.libsmileid.model.GeoInfos;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/*import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;*/

public class SIDGeoInfos {

    private static SIDGeoInfos INSTANCE;
    private double latitude = 0;
    private double longitude = 0;
    private double altitude = 0;
    private double accuracy = 0;
    private String lastUpdate = "0";
    private boolean mGeoPermissionGranted = false;
    private int gpsPermission;

    public static synchronized SIDGeoInfos getInstance() {
        if (INSTANCE == null) INSTANCE = new SIDGeoInfos();
        return INSTANCE;
    }

    public void init(Context context) {
        gpsPermission = ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == gpsPermission) {
            /*Awareness.getSnapshotClient(context)
                    .getLocation()
                    .addOnCompleteListener(new OnCompleteListener<LocationResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<LocationResponse> task) {
                            if (task.isComplete() && task.isSuccessful()) {
                                Location location = task.getResult().getLocation();
                                mGeoPermissionGranted = true;
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                accuracy = location.getAccuracy();
                                altitude = location.getAltitude();
                            }
                        }
                    });*/
        }

        getLastKnownLocation(context);
    }

    private void getLastKnownLocation(Context context) {
        // get Geo Info
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        gpsPermission = ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION);
        if (locationManager != null) {
            Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocationGPS != null) {
                long lastUpdateTime = lastKnownLocationGPS.getTime();
                lastUpdate = getDate(lastUpdateTime, "EEE MMM dd yyyy HH:mm:ss");
            }
            if (lastKnownLocationGPS == null) {
                Location lastKnownLocationNW = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocationNW != null) {
                    long lastUpdateTime = lastKnownLocationNW.getTime();
                    lastUpdate = getDate(lastUpdateTime, "EEE MMM dd yyyy HH:mm:ss");
                }
            }
        }
    }

    private String getDate(long milliseconds, String dateFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        return formatter.format(calendar.getTime());
    }

    public GeoInfos getGeoInformation() {
        GeoInfos infos = new GeoInfos();
        infos.setAccuracy(accuracy);
        infos.setAltitude(altitude);
        infos.setLatitude(latitude);
        infos.setLongitude(longitude);
        infos.setLastUpdate(lastUpdate);
        infos.setGeoPermissionGranted(mGeoPermissionGranted);
        return infos;
    }
}
