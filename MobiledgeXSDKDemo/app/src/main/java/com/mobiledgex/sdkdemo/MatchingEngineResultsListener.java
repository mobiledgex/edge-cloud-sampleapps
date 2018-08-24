package com.mobiledgex.sdkdemo;

import com.mobiledgex.matchingengine.FindCloudletResponse;

public interface MatchingEngineResultsListener {
    void onVerifyLocation(boolean validated);
    void onFindCloudlet(FindCloudletResponse closestCloudlet);
}
