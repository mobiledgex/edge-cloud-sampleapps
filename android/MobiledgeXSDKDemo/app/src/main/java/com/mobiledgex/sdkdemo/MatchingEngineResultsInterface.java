package com.mobiledgex.sdkdemo;

import distributed_match_engine.AppClient;

public interface MatchingEngineResultsInterface {
    void onRegister(String sessionCookie);
    void onVerifyLocation(AppClient.VerifyLocationReply.GPS_Location_Status status, double gpsLocationAccuracyKM);
    void onFindCloudlet(AppClient.FindCloudletReply closestCloudlet);
    void onGetCloudletList(AppClient.AppInstListReply cloudletList);
}
