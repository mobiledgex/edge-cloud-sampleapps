package com.mobiledgex.emptymatchengineapp;


// Matching Engine API:
import distributed_match_engine.AppClient;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.FindCloudletResponse;
import com.mobiledgex.matchingengine.MatchingEngineRequest;

import android.content.Context;
import android.location.Location;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestMatchEngine {

    @Test
    public void findTheCloudlet() {
        Context appContext = InstrumentationRegistry.getTargetContext();
    }
}
