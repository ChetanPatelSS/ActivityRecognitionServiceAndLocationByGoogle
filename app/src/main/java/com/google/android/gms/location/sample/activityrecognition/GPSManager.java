package com.google.android.gms.location.sample.activityrecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class GPSManager implements android.location.GpsStatus.Listener
{
    private static final int gpsMinTime = 500;
    private static final int gpsMinDistance = 0;
    private static LocationManager locationManager = null;
    private static LocationListener locationListener = null;
    private static GPSCallback gpsCallback = null;
    Context mContext;
    public GPSManager(Context context) {
        Log.d("GPSSpeed", "speed-2-GPSManager-1");
        mContext=context;
        locationListener = location -> {
            Log.d("GPSSpeed", "speed-5-GPSManager-2");
            if (gpsCallback != null) {
                Log.d("GPSSpeed", "speed-6-GPSManager-3");
                gpsCallback.onGPSUpdate(location);
            }
        };
    }
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
// Setting Dialog Title
        alertDialog.setTitle("GPS is settings");
// Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
// On pressing Settings button
        alertDialog.setPositiveButton("Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mContext.startActivity(intent);
        });
// on pressing cancel button
        alertDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
// Showing Alert Message
        alertDialog.show();
    }
    public GPSCallback getGPSCallback()
    {
        return gpsCallback;
    }

    public void setGPSCallback(final GPSCallback gpsCallback) {
        this.gpsCallback = gpsCallback;
    }

    public void startListening(final Context context) {
        Log.d("GPSSpeed", "speed-3-startListening-1");
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        final Criteria criteria = new Criteria();

        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        final String bestProvider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }
        if (bestProvider != null && bestProvider.length() > 0) {
            Log.d("GPSSpeed", "speed-3-startListening-2");
            locationManager.requestLocationUpdates(bestProvider, gpsMinTime,
                    gpsMinDistance, locationListener);
        }
        else {
            final List<String> providers = locationManager.getProviders(true);
            for (final String provider : providers)
            {
                locationManager.requestLocationUpdates(provider, gpsMinTime,
                        gpsMinDistance, locationListener);
            }
        }
        Log.d("GPSSpeed", "speed-3-startListening-3");
    }
    public void stopListening() {
        try
        {
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            locationManager = null;
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onGpsStatusChanged(int event) {
        int Satellites = 0;
        int SatellitesInFix = 0;
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }
        int timetofix = locationManager.getGpsStatus(null).getTimeToFirstFix();
        Log.i("GPs", "Time to first fix = "+timetofix);
        for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
            if(sat.usedInFix()) {
                SatellitesInFix++;
            }
            Satellites++;
        }
        Log.i("GPS", Satellites + " Used In Last Fix ("+SatellitesInFix+")");
    }
}
