package com.mobiledgex.sdkdemo;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class CloudletBuilder {
    private String cloudletName;
    private String appName;
    private String carrierName;
    private LatLng gpsLocation;
    private double distance;
    private String fqdn;
    private String fqdnPrefix;
    private boolean tls;
    private Marker marker;
    private int port;
    private Context context;

    public CloudletBuilder setCloudletName(String cloudletName) {
        this.cloudletName = cloudletName;
        return this;
    }

    public CloudletBuilder setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public CloudletBuilder setCarrierName(String carrierName) {
        this.carrierName = carrierName;
        return this;
    }

    public CloudletBuilder setGpsLocation(LatLng gpsLocation) {
        this.gpsLocation = gpsLocation;
        return this;
    }

    public CloudletBuilder setDistance(double distance) {
        this.distance = distance;
        return this;
    }

    public CloudletBuilder setFqdn(String fqdn) {
        this.fqdn = fqdn;
        return this;
    }

    public CloudletBuilder setFqdnPrefix(String fqdnPrefix) {
        this.fqdnPrefix = fqdnPrefix;
        return this;
    }

    public CloudletBuilder setTls(boolean tls) {
        this.tls = tls;
        return this;
    }

    public CloudletBuilder setMarker(Marker marker) {
        this.marker = marker;
        return this;
    }

    public CloudletBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public CloudletBuilder setContext(Context context) {
        this.context = context;
        return this;
    }

    public Cloudlet createCloudlet() {
        return new Cloudlet(cloudletName, appName, carrierName, gpsLocation, distance, fqdn, fqdnPrefix, tls, marker, port, context);
    }
}