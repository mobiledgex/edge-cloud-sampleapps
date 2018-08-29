package com.mobiledgex.emptymatchengineapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CarrierConfigManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import android.content.Intent;

// Matching Engine API:
import com.mobiledgex.matchingengine.FindCloudletResponse;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.MatchingEngineRequest;
import com.mobiledgex.matchingengine.util.RequestPermissions;

import distributed_match_engine.AppClient;
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
         * MatchEngine APIs require special user approved permissions to READ_PHONE_STATE and
         * one of the following:
         * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION. This creates a dialog, if needed.
         */
        mRpUtil = new RequestPermissions();
        mRpUtil.requestMultiplePermissions(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();

        mMatchingEngine = new MatchingEngine(this);

        // Restore mex location preference, defaulting to false:
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean mexLocationAllowed = prefs.getBoolean(getResources()
                .getString(R.string.preference_mex_location_verification),
                false);
        MatchingEngine.setMexLocationAllowed(mexLocationAllowed);

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

        // Open dialog for MEX if this is the first time the app is created:
        boolean firstTimeUse = prefs.getBoolean(getResources().getString(R.string.perference_first_time_use), true);
        if (firstTimeUse) {
            new EnhancedLocationDialog().show(this.getSupportFragmentManager(), "dialog");
            String firstTimeUseKey = getResources().getString(R.string.perference_first_time_use);
            // Disable first time use.
            prefs.edit()
                    .putBoolean(firstTimeUseKey, false)
                    .apply();
        }

        // Set, or create create an App generated UUID for use in MatchingEngine, if there isn't one:
        String uuidKey = getResources().getString(R.string.perference_mex_user_uuid);
        String currentUUID = prefs.getString(uuidKey, "");
        if (currentUUID.isEmpty()) {
            prefs.edit()
                    .putString(uuidKey, mMatchingEngine.createUUID().toString())
                    .apply();
        } else {
            mMatchingEngine.setUUID(UUID.fromString(currentUUID));
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

        //noinspection SimplifiableIfStatement
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
        savedInstanceState.putBoolean(MatchingEngine.MEX_LOCATION_PERMISSION, MatchingEngine.isMexLocationAllowed());
    }

    @Override
    public void onRestoreInstanceState(Bundle restoreInstanceState) {
        super.onRestoreInstanceState(restoreInstanceState);
        if (restoreInstanceState != null) {
            MatchingEngine.setMexLocationAllowed(restoreInstanceState.getBoolean(MatchingEngine.MEX_LOCATION_PERMISSION));
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Or replace with an app specific dialog set.
        mRpUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final String prefKeyAllowMEX = getResources().getString(R.string.preference_mex_location_verification);

        if (key.equals(prefKeyAllowMEX)) {
            boolean mexLocationAllowed = sharedPreferences.getBoolean(prefKeyAllowMEX, false);
            MatchingEngine.setMexLocationAllowed(mexLocationAllowed);
        }
    }



    public void doEnhancedLocationVerification() throws SecurityException {
        final Activity ctx = this;

        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            mRpUtil.requestMultiplePermissions(this);
            return;
        }

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
                    TextView tv = findViewById(R.id.mex_verified_location_content);
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
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                    boolean mexAllowed = prefs.getBoolean(getResources().getString(R.string.preference_mex_location_verification), false);

                    //MatchingEngineRequest req = mMatchingEngine.createRequest(ctx, location); // Regular use case.
                    String host = "tdg.dme.mobiledgex.net"; // Override host.
                    int port = mMatchingEngine.getPort(); // Keep same port.
                    String carrierName = "TDG";
                    String devName = "EmptyMatchEngineMavenApp";

                    MatchingEngineRequest req = mMatchingEngine.createRequest(ctx, host, port, carrierName, devName, location);

                    AppClient.Match_Engine_Status registerStatus = mMatchingEngine.registerClient(req, 10000);
                    if (registerStatus.getStatus() != AppClient.Match_Engine_Status.ME_Status.ME_SUCCESS) {
                        someText = "Registration Failed. Error: " + registerStatus.getStatus();
                        TextView tv = findViewById(R.id.mex_verified_location_content);
                        tv.setText(someText);
                        return;
                    }

                    req = mMatchingEngine.createRequest(ctx, host, port, carrierName, devName, location);
                    if (req != null) {
                        // Location Verification (Blocking, or use verifyLocationFuture):
                        AppClient.Match_Engine_Loc_Verify verifiedLocation = mMatchingEngine.verifyLocation(req, 10000);
                        someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                                ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                                ", Location Accuracy: " + verifiedLocation.getGPSLocationAccuracyKM() + " ]\n";

                        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
                        FindCloudletResponse closestCloudlet = mMatchingEngine.findCloudlet(req, 10000);
                        // FIXME: It's not possible to get a complete http(s) URI on just a service IP + port!
                        String serverip = null;
                        if (closestCloudlet.service_ip != null && closestCloudlet.service_ip.length > 0) {
                            serverip = closestCloudlet.service_ip[0] + ", ";
                            for (int i = 1; i < closestCloudlet.service_ip.length - 1; i++) {
                                serverip += closestCloudlet.service_ip[i] + ", ";
                            }
                            serverip += closestCloudlet.service_ip[closestCloudlet.service_ip.length - 1];
                        }
                        someText += "[Cloudlet Server: URI: [" + closestCloudlet.uri + "], Serverip: [" + serverip + "], Port: " + closestCloudlet.port + "]\n";
                    } else {
                        someText = "Cannot create request object.\n";
                        if (!mexAllowed) {
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
                            TextView tv = findViewById(R.id.mex_verified_location_content);
                            tv.setText(someText);
                        }
                    });
                } catch (ExecutionException ee) {
                    ee.printStackTrace();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } catch (StatusRuntimeException sre) {
                    sre.printStackTrace();
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
            }
        });
    }
}
