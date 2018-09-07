package com.mobiledgex.sdkdemo;

import com.mobiledgex.matchingengine.FindCloudletResponse;

import distributed_match_engine.AppClient;

public interface MatchingEngineResultsListener {
    void onRegister(String sessionCookie);
    void onVerifyLocation(AppClient.Match_Engine_Loc_Verify.GPS_Location_Status status, double gpsLocationAccuracyKM);
    void onFindCloudlet(FindCloudletResponse closestCloudlet);
    void onGetCloudletList(AppClient.Match_Engine_Cloudlet_List cloudletList);
}
