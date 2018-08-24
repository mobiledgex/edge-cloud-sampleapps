package com.mobiledgex.sdkdemo;


import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.Serializable;

public class Cloudlet implements Serializable {
    private String mCloudletName;
    private String mCarrierName;
    private double mLatitude;
    private double mLongitude;
    private double mDistance;
    private boolean bestMatch;
    private transient Marker mMarker;

    public Cloudlet(String cloudletName, String carrierName, LatLng gpsLocation, double distance, Marker marker) {
        mCarrierName = carrierName;
        mCloudletName = cloudletName;
        mLatitude = gpsLocation.latitude;
        mLongitude = gpsLocation.longitude;
        mDistance = distance;
        mMarker = marker;
    }

    public String toString() {
        return "mCarrierName="+mCarrierName+" mCloudletName="+mCloudletName+" mLatitude="+mLatitude+" mLongitude="+mLongitude+" mDistance="+mDistance;
    }

    public String getCloudletName() {
        return mCloudletName;
    }

    public void setCloudletName(String mCloudletName) {
        this.mCloudletName = mCloudletName;
    }

    public String getCarrierName() {
        return mCarrierName;
    }

    public void setCarrierName(String mCarrierName) {
        this.mCarrierName = mCarrierName;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double Latitude) {
        this.mLatitude = Latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public double getDistance() {
        return mDistance;
    }

    public void setDistance(double mDistance) {
        this.mDistance = mDistance;
    }

    public Marker getMarker() { return mMarker; }

    public void setMarker(Marker mMarker) { this.mMarker = mMarker; }

    public boolean isBestMatch() { return bestMatch; }

    public void setBestMatch(boolean bestMatch) { this.bestMatch = bestMatch; }
}
