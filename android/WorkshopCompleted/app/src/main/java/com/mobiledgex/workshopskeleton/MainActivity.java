/**
 * Copyright 2019-2021 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.workshopskeleton;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.eventbus.Subscribe;
import com.google.maps.android.SphericalUtil;


// Matching Engine API:
import com.mobiledgex.matchingengine.EdgeEventsConnection;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.ChannelIterator;
import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.edgeeventsconfig.EdgeEventsConfig;
import com.mobiledgex.matchingengine.edgeeventsconfig.FindCloudletEvent;
import com.mobiledgex.matchingengine.performancemetrics.NetTest;
import com.mobiledgex.matchingengine.performancemetrics.Site;
import com.mobiledgex.matchingengine.util.RequestPermissions;
import com.mobiledgex.matchingenginehelper.EventLogViewer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;
import distributed_match_engine.LocOuterClass;
import io.grpc.StatusRuntimeException;

import static distributed_match_engine.AppClient.ServerEdgeEvent.ServerEventType.EVENT_APPINST_HEALTH;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 1;
    public static final int RC_STATS = 2;
    private MatchingEngine matchingEngine;
    private RequestPermissions mRpUtil;
    private AppClient.FindCloudletReply mClosestCloudlet;
    private Activity ctx;
    private String host;
    private int port;
    private String carrierName;
    private String appName;
    private String orgName;
    private String appVersion;

    private TextView cloudletNameTv;
    private String cloudletNameTvStr = "";
    private TextView appNameTv;
    private String appNameTvStr = "";
    private TextView fqdnTv;
    private String fqdnTvStr = "";
    private TextView portNumberTv;
    private String portNumberTvStr = "";
    private TextView carrierNameTv;
    private TextView distanceTv;
    private String distanceTvStr = "";
    private TextView latitudeTv;
    private String latitudeTvStr = "";
    private TextView longitudeTv;
    private String longitudeTvStr = "";
    private String mClosestCloudletHostname;

    private CheckBox checkboxRegistered;
    private CheckBox checkboxCloudletFound;
    private CheckBox checkboxLocationVerified;
    private ProgressBar progressBar;

    private GoogleSignInClient mGoogleSignInClient;
    private MenuItem signInMenuItem;
    private MenuItem signOutMenuItem;

    private String registerStatusText;
    private String findCloudletStatusText;
    private String verifyLocStatusText;
    private String getQosPosStatusText;
    private EventLogViewer mEventLogViewer;
    private EdgeEventsSubscriber mEdgeEventsSubscriber;
    private Location mLastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this;

        setContentView(R.layout.activity_main);
        cloudletNameTv = findViewById(R.id.cloudletName);
        fqdnTv = findViewById(R.id.fqdn);
        portNumberTv = findViewById(R.id.portNumber);
        appNameTv = findViewById(R.id.appName);
        carrierNameTv = findViewById(R.id.carrierName);
        distanceTv = findViewById(R.id.distance);
        latitudeTv = findViewById(R.id.latitude);
        longitudeTv = findViewById(R.id.longitude);
        checkboxRegistered = findViewById(R.id.checkBoxRegistered);
        checkboxCloudletFound = findViewById(R.id.checkBoxCloudletFound);
        checkboxLocationVerified = findViewById(R.id.checkBoxLocationVerified);
        progressBar = findViewById(R.id.progressBar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerClientInBackground();
            }
        });

        RecyclerView eventsRecyclerView = findViewById(R.id.events_recycler_view);
        FloatingActionButton logExpansionButton = findViewById(R.id.fabEventLogs);
        mEventLogViewer = new EventLogViewer(this, logExpansionButton, eventsRecyclerView);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        signInMenuItem = navigationView.getMenu().findItem(R.id.nav_google_signin);
        signOutMenuItem = navigationView.getMenu().findItem(R.id.nav_google_signout);

        if(account != null) {
            //This means we're already signed in.
            signInMenuItem.setVisible(false);
            signOutMenuItem.setVisible(true);
        } else {
            signInMenuItem.setVisible(true);
            signOutMenuItem.setVisible(false);
        }

        /**
         * MatchingEngine APIs require special user approved permissions to READ_PHONE_STATE and
         * one of the following:
         * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION.
         *
         * The RequestPermissions utility creates a UI dialog, if needed.
         *
         * You can do this anywhere, MainApplication.onActivityResumed(), or a subset of permissions
         * onResume() on each Activity.
         *
         * Permissions must exist prior to API usage to avoid SecurityExceptions.
         */
        mRpUtil = new RequestPermissions();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_reset) {
            mClosestCloudlet = null;
            mClosestCloudletHostname = null;
            carrierNameTv.setText("none");
            appNameTv.setText("none");
            latitudeTv.setText("none");
            longitudeTv.setText("none");
            fqdnTv.setText("none");
            portNumberTv.setText("none");
            distanceTv.setText("none");
            cloudletNameTv.setText("none");
            checkboxRegistered.setChecked(false);
            checkboxRegistered.setText(R.string.client_registered_question);
            checkboxCloudletFound.setChecked(false);
            checkboxCloudletFound.setText(R.string.cloudlet_found_question);
            checkboxLocationVerified.setChecked(false);
            checkboxLocationVerified.setText(R.string.location_verified_question);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_face_detection) {
            // Handle the camera action
            Intent intent = new Intent(this, FaceProcessorActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_google_signin) {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } else if (id == R.id.nav_google_signout) {
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this, "Sign out successful.", Toast.LENGTH_LONG).show();
                            signInMenuItem.setVisible(true);
                            signOutMenuItem.setVisible(false);
                        }
                    });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean registerClient() throws ExecutionException, InterruptedException,
            io.grpc.StatusRuntimeException, DmeDnsException, PackageManager.NameNotFoundException {
        // NOTICE: In a real app, these values could be determined by the SDK, but we are reusing
        // an existing app so we don't have to create new app provisioning data for this workshop.
        appName = "ComputerVision";
        orgName = "MobiledgeX-Samples";
        carrierName = "GDDT";
        appVersion = "2.2";

        //NOTICE: A real app would request permission to enable this.
        MatchingEngine.setMatchingEngineLocationAllowed(true);

        /////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to register the client. Replace all "= null" lines here.
        host = matchingEngine.generateDmeHostAddress();
        if(host == null) {
            Log.e(TAG, "Could not generate host");
            host = "wifi.dme.mobiledgex.net";   //fallback host
        }
        port = matchingEngine.getPort(); // Keep same port.
        AppClient.RegisterClientRequest registerClientRequest;
        registerClientRequest = matchingEngine.createDefaultRegisterClientRequest(ctx, orgName)
                .setAppName(appName).setAppVers(appVersion).setCarrierName(carrierName).build();
        Log.i(TAG, "registerClientRequest: host="+host+" port="+port
                +" getAppName()="+registerClientRequest.getAppName()
                +" getAppVers()="+registerClientRequest.getAppVers()
                +" getOrgName()="+registerClientRequest.getOrgName()
                +" getCarrierName()="+registerClientRequest.getCarrierName());
        AppClient.RegisterClientReply registerStatus
                = matchingEngine.registerClient (registerClientRequest, host, port, 10000);
        /////////////////////////////////////////////////////////////////////////////////////

        if(matchingEngine == null) {
            registerStatusText = "matchingEngine uninitialized";
            Log.e(TAG, registerStatusText);
            return false;
        }

        Log.i(TAG, "registerClientRequest="+registerClientRequest);
        Log.i(TAG, "registerStatus.getStatus()="+registerStatus.getStatus());

        if (registerStatus.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
            registerStatusText = "Registration Failed. Error: " + registerStatus.getStatus();
            Log.e(TAG, registerStatusText);
            return false;
        }
        Log.i(TAG, "SessionCookie:" + registerStatus.getSessionCookie());
        mEventLogViewer.showMessage("RegisterClient successful");
        HashMap<String, String> deviceDetails = matchingEngine.getDeviceInfo();
        Log.i(TAG, "deviceDetails="+deviceDetails);
        mEventLogViewer.showMessage("deviceDetails="+deviceDetails);

        return true;
    }

    public boolean findCloudlet() throws ExecutionException, InterruptedException, PackageManager.NameNotFoundException {
        //(Blocking call, or use findCloudletFuture):
        mLastKnownLocation = new Location("MobiledgeX");
        ////////////////////////////////////////////////////////////
        // TODO: Change these coordinates to where you're actually located.
        // Of course a real app would use GPS to acquire the exact location.
        mLastKnownLocation.setLatitude(52.52);
        mLastKnownLocation.setLongitude(13.4040);    //Beacon

        ////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to find the cloudlet closest to you. Replace "= null" here.
        AppClient.FindCloudletRequest findCloudletRequest;
        findCloudletRequest = matchingEngine.createDefaultFindCloudletRequest(ctx, mLastKnownLocation)
                .setCarrierName(carrierName).build();
        mClosestCloudlet = matchingEngine.findCloudlet(findCloudletRequest, host, port, 10000);
        ////////////////////////////////////////////////////////////////////////////////////////////

        Log.i(TAG, "mClosestCloudlet="+mClosestCloudlet);
        if(mClosestCloudlet == null) {
            findCloudletStatusText = "findCloudlet call is not successfully coded. Search for TODO in code.";
            Log.e(TAG, findCloudletStatusText);
            return false;
        }
        if(mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
            findCloudletStatusText = "findCloudlet Failed. Error: " + mClosestCloudlet.getStatus();
            Log.e(TAG, findCloudletStatusText);
            return false;
        }
        Log.i(TAG, "REQ_FIND_CLOUDLET mClosestCloudlet.uri=" + mClosestCloudlet.getFqdn());

        startEdgeEvents();

        // Populate cloudlet details.
        populateCloudletDetails();

        // TODO: Copy/paste the output of this log into a terminal to test latency.
        Log.i("COPY_PASTE", "ping -c 4 "+mClosestCloudletHostname);

        verifyLocationInBackground(mLastKnownLocation);

        getQoSPositionKpiInBackground(mLastKnownLocation);

        return true;
    }

    private void populateCloudletDetails() {
        Log.i(TAG, "populateCloudletDetails() mClosestCloudlet="+mClosestCloudlet);
        latitudeTvStr = ""+mClosestCloudlet.getCloudletLocation().getLatitude();
        longitudeTvStr = ""+mClosestCloudlet.getCloudletLocation().getLongitude();
        fqdnTvStr = mClosestCloudlet.getFqdn();
        LatLng userLatLng = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
        LatLng cloudletLatLng = new LatLng(mClosestCloudlet.getCloudletLocation().getLatitude(),
                mClosestCloudlet.getCloudletLocation().getLongitude());
        double distance = SphericalUtil.computeDistanceBetween(userLatLng, cloudletLatLng)/1000;
        distanceTvStr = String.format("%.2f", distance)+" km";

        // Extract cloudlet name from FQDN
        String[] parts = mClosestCloudlet.getFqdn().split("\\.");
        cloudletNameTvStr = parts[1];

        //Find FqdnPrefix from Port structure.
        String FqdnPrefix = "";
        List<Appcommon.AppPort> ports = mClosestCloudlet.getPortsList();
        String appPortFormat = "{Protocol: %d, Container Port: %d, External Port: %d, Path Prefix: '%s'}";
        for (Appcommon.AppPort aPort : ports) {
            FqdnPrefix = aPort.getFqdnPrefix();
            // assign first port number to portNumberTvStr
            if (portNumberTvStr == "") {
                portNumberTvStr = String.valueOf(aPort.getPublicPort());
            }
            Log.i(TAG, String.format(Locale.getDefault(), appPortFormat,
                    aPort.getProto().getNumber(),
                    aPort.getInternalPort(),
                    aPort.getPublicPort(),
                    aPort.getEndPort()));
        }
        // Build full hostname.
        mClosestCloudletHostname = FqdnPrefix+mClosestCloudlet.getFqdn();

        checkboxCloudletFound.setChecked(true);
        checkboxCloudletFound.setText(R.string.cloudlet_found);
        cloudletNameTv.setText(cloudletNameTvStr);
        fqdnTv.setText(fqdnTvStr);
        portNumberTv.setText(portNumberTvStr);
        latitudeTv.setText(latitudeTvStr);
        longitudeTv.setText(longitudeTvStr);
        distanceTv.setText(distanceTvStr);

        mEventLogViewer.showMessage("Closest cloudlet is now "+mClosestCloudletHostname);
        // Hide the log viewer after a short delay.
        mEventLogViewer.initialLogsComplete();
    }

    private boolean verifyLocation(Location loc) throws InterruptedException, IOException, ExecutionException {
        AppClient.VerifyLocationRequest verifyLocationRequest
                = matchingEngine.createDefaultVerifyLocationRequest(ctx, loc)
                .setCarrierName(carrierName).build();
        String TAG = "verifyLocation";
        Log.i(TAG, "verifyLocationRequest="+verifyLocationRequest);
        if (verifyLocationRequest != null) {
            try {
                AppClient.VerifyLocationReply verifyLocationReply = matchingEngine.verifyLocation(verifyLocationRequest, host, port, 10000);
                String message = "GPS LocationStatus: " + verifyLocationReply.getGpsLocationStatus() +
                        ", Location Accuracy: " + verifyLocationReply.getGpsLocationAccuracyKm() +
                        ", Location Verified: Tower: " + verifyLocationReply.getTowerStatus();
                Log.i(TAG, message);
                mEventLogViewer.showMessage(message);
                return true;
            } catch(NetworkOnMainThreadException ne) {
                verifyLocStatusText = "Network thread exception";
                Log.e(TAG, verifyLocStatusText);
                return false;
            }
        } else {
            verifyLocStatusText = "Cannot verify location";
            Log.e(TAG, verifyLocStatusText);
            return false;
        }
    }

    private boolean getQoSPositionKpi(LocOuterClass.Loc loc) throws InterruptedException, ExecutionException{
        List<AppClient.QosPosition> requests = createPositionList(loc, 45, 200, 1);
        if (requests.isEmpty()) {
            getQosPosStatusText = "No items added to the position list";
            Log.e(TAG, getQosPosStatusText);
            return false;
        }
        AppClient.QosPositionRequest qosPositionRequest = matchingEngine.createDefaultQosPositionRequest(requests, 0, null).build();

        if(qosPositionRequest != null) {
            try {
                ChannelIterator<AppClient.QosPositionKpiReply> qosPositionKpiReplies = matchingEngine.getQosPositionKpi(qosPositionRequest, host, port, 20000);
                if (!qosPositionKpiReplies.hasNext()) {
                    getQosPosStatusText = "Replies is empty";
                    Log.e(TAG, "Replies is empty");
                    return false;
                }
                while (qosPositionKpiReplies.hasNext()) {
                    Log.i(TAG, "Position results are " + qosPositionKpiReplies.next().getPositionResultsList());
                }
                return true;
            } catch (NetworkOnMainThreadException ne) {
                getQosPosStatusText = "Network thread exception";
                Log.e(TAG, getQosPosStatusText);
                return false;
            }
        } else {
            getQosPosStatusText = "QoS request is null";
            Log.e(TAG, getQosPosStatusText);
            return false;
        }
    }

    private void startEdgeEvents() {
        mEdgeEventsSubscriber = new EdgeEventsSubscriber();
        matchingEngine.getEdgeEventsBus().register(mEdgeEventsSubscriber);

        // set a default config.
        // There is also a parameterized version to further customize.
        EdgeEventsConfig backgroundEdgeEventsConfig = matchingEngine.createDefaultEdgeEventsConfig();

        // Modify the default config with a few custom attribute values:
        backgroundEdgeEventsConfig.latencyTestType = NetTest.TestType.CONNECT;
        // This is the internal port, that has not been remapped to a public port for a particular appInst.
        backgroundEdgeEventsConfig.latencyInternalPort = 8008; // 0 will grab first UDP port but will favor the first TCP port if found.
        // Latency config. There is also a very similar location update config.
        backgroundEdgeEventsConfig.latencyUpdateConfig.maxNumberOfUpdates = 0; // Default is 0, which means test forever.
        backgroundEdgeEventsConfig.latencyUpdateConfig.updateIntervalSeconds = 7; // The default is 30.
        backgroundEdgeEventsConfig.latencyThresholdTrigger = 186;

        String message = "Subscribed to ServerEdgeEvents";
        Log.i(TAG, message);
        mEventLogViewer.showMessage(message);
    }

    private void registerClientInBackground() {
        // Creates new BackgroundRequest object which will call registerClient (then findCloudlet if registerClient is successful) to run on background thread
        new RegisterClientBackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void findCloudletInBackground() {
        // Creates new BackgroundRequest object which will call findCloudlet
        new FindCloudletBackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void verifyLocationInBackground(Location loc) {
        // Creates new BackgroundRequest object which will call verifyLocation to run on background thread
        new VerifyLocBackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, loc);
    }

    private void getQoSPositionKpiInBackground(Location loc) {
        // Creates new BackgroundRequest object which will call getQoSPositionKpi to run on background thread
        new QoSPosBackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, loc);
    }

    private List<AppClient.QosPosition> createPositionList(LocOuterClass.Loc loc, double direction_degrees, double totalDistanceKm, double increment) {
        List<AppClient.QosPosition> positions = new ArrayList<>();
        long positionId = 1;

        AppClient.QosPosition firstPosition = AppClient.QosPosition.newBuilder()
                .setPositionid(positionId)
                .setGpsLocation(loc)
                .build();
        positions.add(firstPosition);

        LocOuterClass.Loc lastLocation = LocOuterClass.Loc.newBuilder()
                .setLongitude(loc.getLongitude())
                .setLatitude(loc.getLatitude())
                .build();

        double kmPerDegreeLat = 110.57; //at Equator
        double kmPerDegreeLong = 111.32; //at Equator
        double addLatitude = (Math.sin(direction_degrees * (Math.PI/180)) * increment)/kmPerDegreeLat;
        double addLongitude = (Math.cos(direction_degrees * (Math.PI/180)) * increment)/kmPerDegreeLong;
        for (double traverse = 0; traverse + increment < totalDistanceKm; traverse += increment) {
            LocOuterClass.Loc next = LocOuterClass.Loc.newBuilder()
                    .setLongitude(lastLocation.getLongitude() + addLongitude)
                    .setLatitude(lastLocation.getLatitude() + addLatitude)
                    .build();
            AppClient.QosPosition nextPosition = AppClient.QosPosition.newBuilder()
                    .setPositionid(++positionId)
                    .setGpsLocation(next)
                    .build();

            positions.add(nextPosition);
            Log.i(TAG, "Latitude is " + nextPosition.getGpsLocation().getLatitude() + " and Longitude is " + nextPosition.getGpsLocation().getLongitude());
            lastLocation = next;
        }
        return positions;
    }

    public void showErrorMsg(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult requestCode="+requestCode+" resultCode="+resultCode+" data="+data);
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        } else if (requestCode == RC_STATS && resultCode == RESULT_OK) {
            //Get preference
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean showDialog = prefs.getBoolean("fd_show_latency_stats_dialog", false);
            if(!showDialog) {
                Log.d(TAG, "Preference is to not show latency stats dialog");
                return;
            }

            String stats = data.getExtras().getString("STATS");
            // The TextView to show your Text
            TextView showText = new TextView(MainActivity.this);
            showText.setText(stats);
            showText.setTextIsSelectable(true);
            int horzPadding = (int) (25 * getResources().getDisplayMetrics().density);
            showText.setPadding(horzPadding, 0,horzPadding,0);
            new AlertDialog.Builder(MainActivity.this)
                    .setView(showText)
                    .setTitle("Stats")
                    .setCancelable(true)
                    .setPositiveButton("OK", null)
                    .show();
        }

    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            signInMenuItem.setVisible(false);
            signOutMenuItem.setVisible(true);
            Toast.makeText(MainActivity.this, "Sign in successful. Welcome, "+account.getDisplayName(), Toast.LENGTH_LONG).show();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error")
                    .setMessage("signInResult:failed code=" + e.getStatusCode())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * Check permissions here, as the user has the ability to change them on the fly through
         * system settings
         */
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            // Opens a UI. When it returns, onResume() is called again.
            mRpUtil.requestMultiplePermissions(this);
            return;
        }
        // Ensures that user can switch from wifi to cellular network data connection (required to verifyLocation)
        matchingEngine = new MatchingEngine(ctx);
        matchingEngine.setNetworkSwitchingEnabled(false);  //false-> wifi (Use wifi for demo)
    }

    public class RegisterClientBackgroundRequest extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                return registerClient();
            } catch (ExecutionException | InterruptedException | StatusRuntimeException | DmeDnsException | PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                registerStatusText = "Registration Failed. Exception="+e.getLocalizedMessage();
                showErrorMsg(registerStatusText);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean clientRegistered) {
            if(clientRegistered) {
                checkboxRegistered.setChecked(true);
                checkboxRegistered.setText(R.string.client_registered);
                // Populate app details.
                carrierNameTv.setText(carrierName);
                appNameTv.setText(appName);
                findCloudletInBackground();
            } else {
                registerStatusText = "Failed to register client. " + registerStatusText;
                Log.e(TAG, registerStatusText);
                showErrorMsg(registerStatusText);
            }
        }
    }

    public class FindCloudletBackgroundRequest extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                return findCloudlet();
            } catch (ExecutionException | InterruptedException | StatusRuntimeException | PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                findCloudletStatusText = ". Exception="+e.getLocalizedMessage();
                showErrorMsg(findCloudletStatusText);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean cloudletFound) {
            if(!cloudletFound) {
                findCloudletStatusText = "Failed to find cloudlet. " + findCloudletStatusText;
                Log.e(TAG, findCloudletStatusText);
                showErrorMsg(findCloudletStatusText);
            }
        }
    }

    public class VerifyLocBackgroundRequest extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                if (verifyLocation((Location) params[0])) {
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (IOException ioe) {
                verifyLocStatusText = ioe.getMessage();
                Log.e(TAG, verifyLocStatusText);
                showErrorMsg(verifyLocStatusText);
            } catch (ExecutionException ee) {
                verifyLocStatusText = ee.getMessage();
                Log.e(TAG, verifyLocStatusText);
                showErrorMsg(verifyLocStatusText);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean locationVerified) {
            if (locationVerified) {
                checkboxLocationVerified.setChecked(true);
                checkboxLocationVerified.setText(R.string.location_verified);
            } else {
                verifyLocStatusText = "Failed to verify location. " + verifyLocStatusText;
                Log.e(TAG, verifyLocStatusText);
                showErrorMsg(verifyLocStatusText);
            }
        }
    }

    public class QoSPosBackgroundRequest extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            Location location = (Location) params[0];
            LocOuterClass.Loc loc = LocOuterClass.Loc.newBuilder()
                    .setLongitude(location.getLongitude())
                    .setLatitude(location.getLatitude())
                    .build();

            try {
                if (getQoSPositionKpi(loc)) {
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (ExecutionException ee) {
                getQosPosStatusText = ee.getMessage();
                Log.e(TAG, getQosPosStatusText);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean gotQoSPositions) {
            if (!gotQoSPositions) {
                getQosPosStatusText = "Failed to get qosPositions. " + getQosPosStatusText;
                Log.e(TAG, getQosPosStatusText);
                showErrorMsg(getQosPosStatusText);
            }
        }
    }

    // (Guava EventBus Interface)
    // This class encapsulates what an app might implement to watch for edge events. Not every event
    // needs to be implemented. If you just want FindCloudlet, just @Subscribe to FindCloudletEvent.
    //
    class EdgeEventsSubscriber {

        // Subscribe to error handlers!
        @Subscribe
        public void onMessageEvent(EdgeEventsConnection.EdgeEventsError error) {
            Log.d(TAG, "EdgeEvents error reported, reason: " + error);
            if (error.toString().equals("eventTriggeredButCurrentCloudletIsBest")) {
                return;
            }
            mEventLogViewer.showError(error.toString());
        }

        // Subscribe to FindCloudletEvent updates to appInst. Reasons to do so include
        // the AppInst Health and Latency spec for this application.
        @Subscribe
        public void onMessageEvent(FindCloudletEvent findCloudletEvent) {
            Log.i(TAG, "Cloudlet update, reason: " + findCloudletEvent.trigger);

            // Connect to new Cloudlet in the event here, preferably in a background task.
            Log.i(TAG, "FindCloudletEvent. Cloudlet: " + findCloudletEvent.newCloudlet);

            String someText = findCloudletEvent.newCloudlet.getFqdn();
            mEventLogViewer.showMessage("FindCloudletEvent. FQDN: "+someText);

            Log.i(TAG, "Received: Server pushed a new FindCloudletReply to switch to: " + findCloudletEvent);
            handleFindCloudletServerPush(findCloudletEvent);
        }

        void handleFindCloudletServerPush(FindCloudletEvent event) {
            mEventLogViewer.showMessage("Received: FindCloudletEvent");

            // In this demo case, use our existing interface to display the newly selected cloudlet on the map.
            mClosestCloudlet = event.newCloudlet;
            populateCloudletDetails();
        }
    }
}
