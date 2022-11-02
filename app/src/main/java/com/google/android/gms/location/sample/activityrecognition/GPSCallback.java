package com.google.android.gms.location.sample.activityrecognition;

import android.location.Location;

public interface GPSCallback {
    public abstract void onGPSUpdate(Location location);
}
