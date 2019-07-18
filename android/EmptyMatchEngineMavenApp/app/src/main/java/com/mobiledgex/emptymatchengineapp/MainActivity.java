package com.mobiledgex.emptymatchengineapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import android.content.Intent;

// Matching Engine API:
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.NetworkRequestTimeoutException;
import com.mobiledgex.matchingengine.util.RequestPermissions;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;
import io.grpc.StatusRuntimeException;


// Location API:
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";
    private MatchingEngine mMatchingEngine;
    private String someText = null;

    private RequestPermissions mRpUtil;
    private FusedLocationProviderClient mFusedLocationClient;

    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private boolean mDoLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * MatchingEngine APIs require special user approved permissions to READ_PHONE_STATE and
         * one of the following:
         * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION.
         *
         * The example RequestPermissions utility creates a UI dialog, if needed.
         *
         * You can do this anywhere, MainApplication.onActivityResumed(), or a subset of permissions
         * onResume() on each Activity.
         *
         * Permissions must exist prior to API usage to avoid SecurityExceptions.
         */
        mRpUtil = new RequestPermissions();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();

        // Restore app's matching engine location preference, defaulting to false:
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.location_preferences, false);

        boolean matchingEngineLocationAllowed = prefs.getBoolean(getResources()
                        .getString(R.string.preference_matching_engine_location_verification),
                false);
        MatchingEngine.setMatchingEngineLocationAllowed(matchingEngineLocationAllowed);

        // Watch allowed preference:
        prefs.registerOnSharedPreferenceChangeListener(this);


        // Client side FusedLocation updates.
        mDoLocationUpdates = true;

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                String clientLocText = "";
                for (Location location : locationResult.getLocations()) {
                    // Update UI with client location data
                    clientLocText += "[" + location.toString() + "]";
                }
                TextView tv = findViewById(R.id.client_location_content);
                tv.setText(clientLocText);
            };
        };

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doEnhancedLocationVerification();
            }
        });

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // Open dialog for MobiledgeX MatchingEngine if this is the first time the app is created:
        String firstTimeUsePrefKey = getResources().getString(R.string.preference_first_time_use);
        boolean firstTimeUse = prefs.getBoolean(firstTimeUsePrefKey, true);

        if (firstTimeUse) {
            Intent intent = new Intent(this, FirstTimeUseActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {

            // Open "Settings" UI
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            // Opens a UI. When it returns, onResume() is called again.
            mRpUtil.requestMultiplePermissions(this);
            return;
        }

        if (mMatchingEngine == null) {
            // Permissions available. Create a MobiledgeX MatchingEngine instance (could also use Application wide instance).
            mMatchingEngine = new MatchingEngine(this);
        }

        if (mDoLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(MatchingEngine.MATCHING_ENGINE_LOCATION_PERMISSION, MatchingEngine.isMatchingEngineLocationAllowed());
    }

    @Override
    public void onRestoreInstanceState(Bundle restoreInstanceState) {
        super.onRestoreInstanceState(restoreInstanceState);
        if (restoreInstanceState != null) {
            MatchingEngine.setMatchingEngineLocationAllowed(restoreInstanceState.getBoolean(MatchingEngine.MATCHING_ENGINE_LOCATION_PERMISSION));
        }
    }

    /**
     * See documentation for Google's FusedLocationProviderClient for additional usage information.
     */
    private void startLocationUpdates() {
        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            return;
        }

        try {
            if (mFusedLocationClient == null) {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            }
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback,
                    null /* Looper */);
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.i(TAG, "App should Request location permissions during onCreate().");
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final String prefKeyAllowMEX = getResources().getString(R.string.preference_matching_engine_location_verification);

        if (key.equals(prefKeyAllowMEX)) {
            boolean mexLocationAllowed = sharedPreferences.getBoolean(prefKeyAllowMEX, false);
            MatchingEngine.setMatchingEngineLocationAllowed(mexLocationAllowed);
        }
    }

    public void doEnhancedLocationVerification() throws SecurityException {
        final Activity ctx = this;

        // Run in the background and post text results to the UI thread.
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    doEnhancedLocationUpdateInBackground(task, ctx);
                } else {
                    Log.w(TAG, "getLastLocation:exception", task.getException());
                    someText = "Last location not found, or has never been used. Location cannot be verified using 'getLastLocation()'. " +
                            "Use the requestLocationUpdates() instead if applicable for location verification.";
                    TextView tv = findViewById(R.id.mobiledgex_verified_location_content);
                    tv.setText(someText);
                }
            }
        });
    }

    private void doEnhancedLocationUpdateInBackground(final Task<Location> aTask, final Activity ctx) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Location location = aTask.getResult();
                // Location found. Create a request:
                try {
                    someText = "";
                    // If no carrierName, or active Subscription networks, the app should use the public cloud instead.
                    List<SubscriptionInfo> subList = mMatchingEngine.getActiveSubscriptionInfoList();
                    if (subList != null && subList.size() > 0) {
                        for(SubscriptionInfo info: subList) {
                            if (info.getCarrierName().equals("Android")) {
                                someText += "Emulator Active Subscription Network: " + info.toString() + "\n";
                            } else {
                                someText += "Active Subscription network: " + info.toString() + "\n";
                            }
                        }
                        mMatchingEngine.setNetworkSwitchingEnabled(true);
                    } else {
                        // This example will continue to execute anyway, as Demo DME may still be reachable to discover nearby edge cloudlets.
                        someText += "No active cellular networks: app should use public cloud instead of the edgecloudlet at this time.\n";
                        mMatchingEngine.setNetworkSwitchingEnabled(false);
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                    boolean locationVerificationAllowed = prefs.getBoolean(getResources().getString(R.string.preference_matching_engine_location_verification), false);

                    //String carrierName = mMatchingEngine.retrieveNetworkCarrierName(ctx); // Regular use case
                    String carrierName = "mexdemo";                                         // Override carrierName
                    if (carrierName == null) {
                        someText += "No carrier Info!\n";
                    }
                    String host = mMatchingEngine.generateDmeHostAddress(carrierName);      // Override carrier specific host name
                    int port = mMatchingEngine.getPort(); // Keep same port.

                    String devName = "EmptyMatchEngineApp"; // Always supplied by application.
                    String appName = mMatchingEngine.getAppName(ctx);
                    appName = "EmptyMatchEngineApp"; // Choosing a known registered app name instead of engine discovered name.

                    AppClient.RegisterClientRequest registerClientRequest =
                            mMatchingEngine.createRegisterClientRequest(ctx,
                                    devName, appName,
                                    null, carrierName, null);

                    AppClient.RegisterClientReply registerStatus =
                            mMatchingEngine.registerClient(registerClientRequest,
                                    host, port, 10000);

                    if (registerStatus.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
                        someText += "Registration Failed. Error: " + registerStatus.getStatus();
                        TextView tv = findViewById(R.id.mobiledgex_verified_location_content);
                        tv.setText(someText);
                        return;
                    }

                    AppClient.VerifyLocationRequest verifyRequest =
                            mMatchingEngine.createVerifyLocationRequest(ctx, carrierName, location);
                    if (verifyRequest != null) {
                        // Location Verification (Blocking, or use verifyLocationFuture):
                        AppClient.VerifyLocationReply verifiedLocation =
                                mMatchingEngine.verifyLocation(verifyRequest, host, port, 10000);

                        someText += "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                                ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                                ", Location Accuracy: " + verifiedLocation.getGpsLocationAccuracyKm() + " ]\n";

                        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
                        AppClient.FindCloudletRequest findCloudletRequest =
                                mMatchingEngine.createFindCloudletRequest(ctx, carrierName, location);
                        AppClient.FindCloudletReply closestCloudlet = mMatchingEngine.findCloudlet(findCloudletRequest,
                                host, port, 10000);

                        List<distributed_match_engine.Appcommon.AppPort> ports = closestCloudlet.getPortsList();
                        String portListStr = "";
                        boolean first = true;
                        String appPortFormat = "{Protocol: %d, Container Port: %d, External Port: %d, Path Prefix: '%s'}";
                        for (Appcommon.AppPort aPort : ports) {
                            if (!first) {
                                portListStr += ", ";

                            }
                            portListStr += String.format(Locale.getDefault(), appPortFormat,
                                    aPort.getProto().getNumber(),
                                    aPort.getInternalPort(),
                                    aPort.getPublicPort(),
                                    aPort.getPathPrefix());
                        }

                        someText += "[Cloudlet App Ports: [" + portListStr + "]\n";

                        String appInstListText = "";
                        AppClient.AppInstListRequest appInstListRequest = mMatchingEngine.createAppInstListRequest(ctx, carrierName, location);
                        AppClient.AppInstListReply appInstListReply = mMatchingEngine.getAppInstList(appInstListRequest,10000);
                        for (AppClient.CloudletLocation cloudletLocation : appInstListReply.getCloudletsList()) {
                            String location_carrierName = cloudletLocation.getCarrierName();
                            String location_cloudletName = cloudletLocation.getCloudletName();
                            double location_distance = cloudletLocation.getDistance();

                            appInstListText += "[CloudletLocation: CarrierName: " + location_carrierName;
                            appInstListText += ", CloudletName: " + location_cloudletName;
                            appInstListText += ", Distance: " + location_distance;
                            appInstListText += " , AppInstances: [";
                            for (AppClient.Appinstance appinstance : cloudletLocation.getAppinstancesList()) {
                                appInstListText += "Name: " + appinstance.getAppName()
                                        + ", Version: " + appinstance.getAppVers()
                                        + ", FQDN: " + appinstance.getFqdn()
                                        + ", Ports: " + appinstance.getPortsList().toString();

                            }
                            appInstListText += "]]";
                        }
                        if (!appInstListText.isEmpty()) {
                            someText += appInstListText;
                        }
                    } else {
                        someText = "Cannot create request object.\n";
                        if (!locationVerificationAllowed) {
                            someText += " Reason: Enhanced location is disabled.\n";
                        }
                    }

                    someText += "[Is WiFi Enabled: " + mMatchingEngine.isWiFiEnabled(ctx) + "]\n";

                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        someText += "[Is Roaming Data Enabled: " + mMatchingEngine.isRoamingData() + "]\n";
                    } else {
                        someText += "[Roaming Data status unknown.]\n";
                    }

                    CarrierConfigManager carrierConfigManager = ctx.getSystemService(CarrierConfigManager.class);
                    someText += "[Enabling WiFi Calling could disable Cellular Data if on a Roaming Network!\nWiFi Calling  Support Status: "
                            + mMatchingEngine.isWiFiCallingSupported(carrierConfigManager) + "]\n";


                    // Background thread. Post update to the UI thread:
                    ctx.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView tv = findViewById(R.id.mobiledgex_verified_location_content);
                            tv.setText(someText);
                        }
                    });
                } catch (ExecutionException ee) {
                    ee.printStackTrace();
                    if (ee.getCause() instanceof NetworkRequestTimeoutException) {
                        String causeMessage = ee.getCause().getMessage();
                        someText = "Network connection failed: " + causeMessage;
                        Log.e(TAG, someText);
                        // Handle network error with failover logic. MobiledgeX MatchingEngine requests over cellular is needed to talk to the DME.
                        ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView tv = findViewById(R.id.mobiledgex_verified_location_content);
                                tv.setText(someText);
                            }
                        });
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } catch (StatusRuntimeException sre) {
                    sre.printStackTrace();
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                } catch (Resources.NotFoundException nfe) {
                    nfe.printStackTrace();
                }
            }
        });
    }
}
