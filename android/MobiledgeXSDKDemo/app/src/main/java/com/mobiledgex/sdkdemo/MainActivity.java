/**
 * Copyright 2018-2021 MobiledgeX, Inc. All rights and licenses reserved.
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

package com.mobiledgex.sdkdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TravelMode;
import com.mobiledgex.computervision.ImageProcessorActivity;
import com.mobiledgex.computervision.ImageProcessorFragment;
import com.mobiledgex.computervision.ObjectProcessorActivity;
import com.mobiledgex.computervision.PoseProcessorActivity;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.util.RequestPermissions;
import com.mobiledgex.matchingenginehelper.ConnectionTester;
import com.mobiledgex.matchingenginehelper.EventLogViewer;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelper;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelperInterface;
import com.mobiledgex.sdkdemo.qoe.QoeMapActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;
import distributed_match_engine.LocOuterClass;

import static com.mobiledgex.matchingenginehelper.MatchingEngineHelper.mEdgeEventsConfigUpdated;
import static com.mobiledgex.matchingenginehelper.MatchingEngineHelper.mEdgeEventsEnabled;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
            GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener,
            SharedPreferences.OnSharedPreferenceChangeListener, GoogleMap.OnMarkerDragListener, MatchingEngineHelperInterface {

    private static final String TAG = "MainActivity";
    public static final int COLOR_NEUTRAL = 0xff676798;
    public static final int COLOR_VERIFIED = 0xff009933;
    public static final int COLOR_FAILURE = 0xffff3300;
    public static final int COLOR_CAUTION = 0xff00b33c; //Amber: ffbf00;

    // For GDDT verifyLocation
    public static final int COLOR_GREEN = 0xff009933;
    public static final int COLOR_AMBER = 0xffffbf00;
    public static final int COLOR_DARK_AMBER = 0xffcfbf00;
    public static final int COLOR_RED = 0xffff3300;

    // HashMap of map types, and cloudlet colors for good contrast.
    Map<Integer, Integer> cloudLetColors = new HashMap<>();
    private Integer mDefaultCloudletColor;

    private static final int RC_SIGN_IN = 1;
    public static final int RC_STATS = 2;
    public static final int DEFAULT_SPEED_TEST_PORT = 8008;
    private boolean mTls;

    private GoogleMap mGoogleMap;
    private MatchingEngineHelper meHelper;
    private Marker mUserLocationMarker;

    private RequestPermissions mRpUtil;
    private SupportMapFragment mMapFragment;

    private FloatingActionButton fabFindCloudlets;
    private FloatingActionButton fabPlayRoute;
    private boolean locationVerified = false;
    private boolean locationVerificationAttempted = false;
    private double mGpsLocationAccuracyKM;
    private String defaultLatencyMethod = "ping";

    private GoogleSignInClient mGoogleSignInClient;
    private MenuItem signInMenuItem;
    private MenuItem signOutMenuItem;

    private MenuItem routeModePrevItem;

    private AlertDialog mAlertDialog;
    private boolean uiHasBeenTouched;
    private Polyline mClosestCloudletPolyLine;
    private EventLogViewer mEventLogViewer;
    private LatLng mPrevMarkPosition;

    private enum RouteMode {
        FLYING,
        DRIVING
    }
    private RouteMode mRouteMode;
    private Marker mStartMarker;
    private Marker mEndMarker;
    private List<Marker> mWaypointMarkers = new ArrayList<>();
    private MenuItem mapTypeGroupPrevItem;
    private boolean mRouteIsPlaying;
    private ValueAnimator mValueAnimator;
    private int mDrivingAnimDuration;
    private int mFlyingAnimDuration;
    private static final int DEF_DRIVING_DURATION = 20; //Seconds
    private static final int DEF_FLYING_DURATION = 15; //Seconds

    private String mApiKey = BuildConfig.GOOGLE_DIRECTIONS_API_KEY;
    private Polyline mRoutePolyLine;
    private List<LatLng> mCloudletLatLngs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

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

        setContentView(R.layout.activity_main);

        NavigationView navigationView = findViewById(R.id.nav_view);
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFloatingActionButtonClicked();
            }
        });

        fabPlayRoute = findViewById(R.id.fab_play_route);
        fabPlayRoute.setVisibility(View.GONE);
        fabPlayRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playRoute();
            }
        });

        fabFindCloudlets = findViewById(R.id.fab2);
        fabFindCloudlets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                meHelper.findCloudletInBackground();
            }
        });

        RecyclerView eventsRecyclerView = findViewById(R.id.events_recycler_view);
        FloatingActionButton logExpansionButton = findViewById(R.id.fab_log_viewer);
        mEventLogViewer = new EventLogViewer(this, logExpansionButton, eventsRecyclerView);

        boolean allowFindBeforeVerify = prefs.getBoolean(getResources().getString(R.string.pref_allow_find_before_verify), true);
        fabFindCloudlets.setEnabled(allowFindBeforeVerify);

        // Open dialog for MEX if this is the first time the app is created:
        String firstTimeUsePrefKey = getResources().getString(R.string.pref_first_time_use);
        boolean firstTimeUse = prefs.getBoolean(firstTimeUsePrefKey, true);
        if (firstTimeUse) {
            Intent intent = new Intent(this, FirstTimeUseActivity.class);
            startActivity(intent);
        }

        // Run any version-specific upgrades here.
        upgradeToVersion59(prefs);

        // Reuse the onSharedPreferenceChanged code to initialize anything dependent on these prefs:
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.pref_app_instances_limit));
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.download_size));
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.upload_size));
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.latency_packets));
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.pref_latency_method));
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.pref_latency_autostart));
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.pref_driving_time_seekbar));
        onSharedPreferenceChanged(prefs, getResources().getString(R.string.pref_flying_time_seekbar));

        // Watch for any updated preferences:
        prefs.registerOnSharedPreferenceChangeListener(this);

        cloudLetColors.put(R.id.action_map_type_normal, COLOR_NEUTRAL);
        cloudLetColors.put(R.id.action_map_type_hybrid, 0xffffffff);
        cloudLetColors.put(R.id.action_map_type_retro, 0xff000000);
        cloudLetColors.put(R.id.action_map_type_satellite, 0xffffffff);
        cloudLetColors.put(R.id.action_map_type_silver, COLOR_NEUTRAL);
        cloudLetColors.put(R.id.action_map_type_terrain, COLOR_NEUTRAL);
        mDefaultCloudletColor = COLOR_NEUTRAL;

        // TODO: If GDDT ever restores their PQoE backend, unhide this menu item
        MenuItem qoeMenuItem = navigationView.getMenu().findItem(R.id.nav_qoe_map);
        qoeMenuItem.setVisible(false);
    }

    private void playRoute() {
        Log.i(TAG, "playRoute mRouteIsPlaying="+mRouteIsPlaying);
        if (mRouteIsPlaying) {
            fabPlayRoute.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            if (mValueAnimator != null) {
                mValueAnimator.cancel();
            }
            mRouteIsPlaying = false;
        } else {
            fabPlayRoute.setImageResource(R.drawable.ic_baseline_pause_24);
            mEventLogViewer.mAutoExpand = false;
            mUserLocationMarker.setPosition(mStartMarker.getPosition());
            if (mRouteMode == RouteMode.FLYING) {
                animateMarker(mRouteMode, mUserLocationMarker, mEndMarker.getPosition(), mFlyingAnimDuration);
            } else if (mRouteMode == RouteMode.DRIVING) {
                animateMarker(mRouteMode, mUserLocationMarker, mEndMarker.getPosition(), mDrivingAnimDuration);
            }
            mRouteIsPlaying = true;
        }
    }

    /**
     * Method to animate marker to destination location.
     * @param marker marker to be animated
     * @param endPosition destination position
     * @param duration Animation duration in ms.
     */
    public void animateMarker(RouteMode routeMode, Marker marker, LatLng endPosition, long duration) {
        final long[] lastLocationUpdateTime = {0};
        LatLng startPosition = marker.getPosition();

        LatLngInterpolator latLngInterpolator = new LatLngInterpolator.LinearFixed();
        mValueAnimator = ValueAnimator.ofFloat(0, 1);
        mValueAnimator.setDuration(duration);
        mValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float v = animation.getAnimatedFraction();
                LatLng newPosition;
                if (routeMode == RouteMode.DRIVING) {
                    int index = (int) (v * mRoutePolyLine.getPoints().size());
                    if (index >= mRoutePolyLine.getPoints().size()) {
                        return;
                    }
                    newPosition = mRoutePolyLine.getPoints().get(index);
                } else if (routeMode == RouteMode.FLYING) {
                    newPosition = latLngInterpolator.interpolate(v, startPosition, endPosition);
                } else {
                    Log.e(TAG, "Unknown routeMode: "+routeMode);
                    return;
                }
                marker.setPosition(newPosition);

                drawClosestCloudletLine();
                long now = SystemClock.uptimeMillis();
                if (now - lastLocationUpdateTime[0] > 1000) { // Send every second.
                    meHelper.setSpoofedLocation(newPosition.latitude, newPosition.longitude);
                    lastLocationUpdateTime[0] = now;
                }
            }
        });

        mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRouteIsPlaying = false;
                fabPlayRoute.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            }
        });

        mValueAnimator.start();
    }

    protected void drawClosestCloudletLine() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Erase "closest cloudlet" line if it exists.
                if (mClosestCloudletPolyLine != null) {
                    mClosestCloudletPolyLine.remove();
                }
                if (getClosestCloudletPosition() != null) {
                    mClosestCloudletPolyLine = mGoogleMap.addPolyline(new PolylineOptions()
                            .add(mUserLocationMarker.getPosition(), getClosestCloudletPosition())
                            .width(8)
                            .color(COLOR_VERIFIED));
                }
            }
        });
    }

    private interface LatLngInterpolator {
        LatLng interpolate(float fraction, LatLng a, LatLng b);

        class LinearFixed implements LatLngInterpolator {
            @Override
            public LatLng interpolate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lngDelta = b.longitude - a.longitude;
                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360;
                }
                double lng = lngDelta * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        }
    }

    public LatLng getClosestCloudletPosition() {
        if (meHelper.mClosestCloudlet == null) {
            return null;
        }
        LocOuterClass.Loc location = meHelper.mClosestCloudlet.getCloudletLocation();
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        return position;
    }

    /**
     * Perform the floatingActionBar action. Currently this is to perform the multi-step
     * matching engine process.
     */
    private void onFloatingActionButtonClicked() {
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            mRpUtil.requestMultiplePermissions(this);
            return;
        }
        meHelper.doEnhancedLocationUpdateInBackground();
    }

    /**
     * Adds a informational message to the log viewer.
     *
     * @param text The message to show.
     */
    public void showMessage(String text) {
        mEventLogViewer.showMessage(text);
    }

    /**
     * Adds an error message to the log viewer.
     *
     * @param text The message to show.
     */
    public void showError(String text) {
        mEventLogViewer.showError(text);
    }

    private void showAboutDialog() {
        String appName = "";
        String appVersion = "";
        try {
            // App
            ApplicationInfo appInfo = getApplicationInfo();
            if (getPackageManager() != null) {
                CharSequence seq = appInfo.loadLabel(getPackageManager());
                if (seq != null) {
                    appName = seq.toString();
                }
                PackageInfo pi  = getPackageManager().getPackageInfo(getPackageName(), 0);
                appVersion = pi.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = getAssets().open("about_dialog.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            String htmlData = sb.toString();
            htmlData = htmlData.replace("${androidAppVersion}", appVersion)
                    .replace("${appName}", meHelper.mAppName)
                    .replace("${appVersion}", meHelper.mAppVersion)
                    .replace("${orgName}", meHelper.mOrgName)
                    .replace("${carrier}", meHelper.mCarrierName)
                    .replace("${region}", meHelper.mDmeHostname)
                    .replace(".dme.mobiledgex.net", "");
            Log.i(TAG, "htmlData=\n"+htmlData);
            // The WebView to show our HTML.
            WebView webView = new WebView(MainActivity.this);
            webView.loadData(Base64.encodeToString(htmlData.getBytes(), Base64.DEFAULT), "text/html", "base64");
            new AlertDialog.Builder(MainActivity.this)
                    .setView(webView)
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setTitle(appName)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int itemId = prefs.getInt(getResources().getString(R.string.pref_google_map_type), R.id.action_map_type_silver);
        // From one version of the app to another, the generated resource ID may have changed.
        // Check that the stored preference is a valid menu item. If not, use a default.
        if (itemId != R.id.action_map_type_hybrid
                && itemId != R.id.action_map_type_normal
                && itemId != R.id.action_map_type_retro
                && itemId != R.id.action_map_type_satellite
                && itemId != R.id.action_map_type_silver
                && itemId != R.id.action_map_type_terrain) {
            itemId = R.id.action_map_type_silver;
        }
        mDefaultCloudletColor = cloudLetColors.get(itemId);
        if (mDefaultCloudletColor == null) {
            mDefaultCloudletColor = COLOR_NEUTRAL;
        }
        mapTypeGroupPrevItem = menu.findItem(itemId);
        mapTypeGroupPrevItem.setChecked(true);
        onMapTypeGroupItemClick(mapTypeGroupPrevItem);
        Log.i(TAG, "onCreateOptionsMenu itemId="+itemId+" "+mapTypeGroupPrevItem);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        uiHasBeenTouched = true;
        int id = item.getItemId();

        if (id == R.id.action_register_client) {
            meHelper.registerClientInBackground();
        }
        if (id == R.id.action_get_app_inst_list) {
            getCloudlets(false);
        }
        if (id == R.id.action_reset_location) {
            // Reset spoofed GPS
            Location lastKnownLocation = meHelper.mLastKnownLocation;
            if(lastKnownLocation == null) {
                startLocationUpdates();
                showGpsWarning();
                return true;
            }
            if(mUserLocationMarker == null) {
                Log.w(TAG, "No marker for user location");
                Toast.makeText(MainActivity.this, "No user location marker. Please retry in a moment.", Toast.LENGTH_LONG).show();
                return true;
            }
            meHelper.setSpoofedLocation(null);
            mUserLocationMarker.setPosition(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));

            initUserMobileIcon();

            if(meHelper.mAllowLocationSimulatorUpdate) {
                updateLocSimLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            }
            meHelper.mClosestCloudletHostname = null;
            getCloudlets(true);
            return true;
        }
        if (id == R.id.action_verify_location) {
            meHelper.verifyLocationInBackground();
        }
        if (id == R.id.action_find_cloudlet) {
            meHelper.findCloudletInBackground();
        }

        return super.onOptionsItemSelected(item);
    }

    private void startLocationUpdates() {
        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            Log.i(TAG, "Location permission has NOT been granted");
            return;
        }
        Log.i(TAG, "Location permission has been granted");
        meHelper.startLocationUpdates();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        uiHasBeenTouched = true;
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            // Open "Settings" UI
            mEdgeEventsConfigUpdated = false;
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_about) {
            // Handle the About action
            showAboutDialog();
            return true;
        } else if (id == R.id.nav_face_detection) {
            // Start the face detection Activity
            Intent intent = new Intent(this, ImageProcessorActivity.class);
            intent.putExtra(ImageProcessorFragment.EXTRA_EDGE_CLOUDLET_HOSTNAME, meHelper.mClosestCloudletHostname);
            putCommonIntentExtras(intent);
            startActivityForResult(intent, RC_STATS);
            return true;
        } else if (id == R.id.nav_face_recognition) {
            // Start the face recognition Activity
            Intent intent = new Intent(this, ImageProcessorActivity.class);
            intent.putExtra(ImageProcessorFragment.EXTRA_FACE_RECOGNITION, true);
            intent.putExtra(ImageProcessorFragment.EXTRA_EDGE_CLOUDLET_HOSTNAME, meHelper.mClosestCloudletHostname);
            putCommonIntentExtras(intent);
            startActivityForResult(intent, RC_STATS);
            return true;
        } else if (id == R.id.nav_pose_detection) {
            // Start the pose detection Activity
            Intent intent = new Intent(this, PoseProcessorActivity.class);
            putCommonIntentExtras(intent);
            startActivityForResult(intent, RC_STATS);
            return true;
        } else if (id == R.id.nav_object_detection) {
            // Start the object detection Activity
            Intent intent = new Intent(this, ObjectProcessorActivity.class);
            putCommonIntentExtras(intent);
            startActivityForResult(intent, RC_STATS);
            return true;
        } else if (id == R.id.nav_qoe_map) {
            // One of the dependencies used in the PQoE activity requires API level of 26 or higher.
            if (android.os.Build.VERSION.SDK_INT < 26) {
                Toast.makeText(MainActivity.this,
                        "Predictive QoE not supported on this version of Android",
                        Toast.LENGTH_LONG).show();
                return true;
            }
            // Start the PQoE Activity
            Intent intent = new Intent(this, QoeMapActivity.class);
            Log.i(TAG, "mDmeHostname="+ meHelper. mDmeHostname);
            intent.putExtra(QoeMapActivity.EXTRA_HOSTNAME, meHelper.mDmeHostname);
            intent.putExtra(QoeMapActivity.EXTRA_CARRIER_NAME, meHelper.mCarrierName);
            startActivity(intent);
            return true;
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onRouteModeItemClick(MenuItem item) {
        Log.i(TAG, "onRouteModeItemClick "+item+" "+item.isChecked());
        // Remove any existing driving route.
        if (mRoutePolyLine != null) {
            mRoutePolyLine.remove();
        }

        // Turn off everything route related. If a menu item is being unchecked,
        // then we're done after this. Otherwise we will rebuild everything below.
        if (mStartMarker != null) {
            mStartMarker.setVisible(false);
        }
        if (mEndMarker != null) {
            mEndMarker.setVisible(false);
        }
        mStartMarker = null;
        mEndMarker = null;
        for (Marker marker: mWaypointMarkers) {
            marker.setVisible(false);
        }
        mWaypointMarkers.clear();
        fabPlayRoute.setVisibility(View.GONE);
        /////////////////////////////////////////////////////////////////////

        if (item.isChecked()) {
            if (routeModePrevItem == item) {
                routeModePrevItem.setChecked(false);
                return;
            }
        }
        if (routeModePrevItem != null) {
            routeModePrevItem.setChecked(false);
        }
        routeModePrevItem = item;

        if (item.getItemId() == R.id.action_route_mode_flying) {
            mRouteMode = RouteMode.FLYING;
            initRouteMarkers(item.getItemId());
            item.setChecked(true);
            fabPlayRoute.setVisibility(View.VISIBLE);
        }
        if (item.getItemId() == R.id.action_route_mode_driving) {
            mRouteMode = RouteMode.DRIVING;
            initRouteMarkers(item.getItemId());
            item.setChecked(true);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    routeBetweenPoints(mStartMarker.getPosition(), mEndMarker.getPosition(), mWaypointMarkers);
                }
            });
        }
    }

    /**
     * Place start and end markers in diagonal corners of the current map view.
     * @param itemId
     */
    private void initRouteMarkers(int itemId) {
        if (mStartMarker != null) {
            mStartMarker.setVisible(true);
            mEndMarker.setVisible(true);
            return;
        }
        LatLngBounds bounds = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;
        double maxLng = bounds.northeast.longitude;
        double maxLat = bounds.northeast.latitude;
        double minLng = bounds.southwest.longitude;
        double minLat = bounds.southwest.latitude;
        double width = maxLng - minLng;
        double height = maxLat - minLat;

        double startLng = minLng + width/5;
        double startLat = maxLat - height/5;
        double endLng = maxLng - width/5;
        double endLat = minLat + height/5;

        mStartMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(new LatLng(startLat, startLng))
                .title("Start of route")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        mStartMarker.setDraggable(true);
        mEndMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(new LatLng(endLat, endLng))
                .title("End of route")); //"End" or "Stop" -- Red by default.
        mEndMarker.setDraggable(true);
    }

    public void onMapTypeGroupItemClick(MenuItem item) {
        mapTypeGroupPrevItem.setChecked(false);
        mapTypeGroupPrevItem = item;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(getResources().getString(R.string.pref_google_map_type), item.getItemId()).apply();
        Log.i(TAG, "onMapTypeGroupItemClick itemId="+item.getItemId()+" "+item);
        int oldColor = mDefaultCloudletColor;
        mDefaultCloudletColor = cloudLetColors.get(item.getItemId());
        if (mDefaultCloudletColor != oldColor) {
            initAllCloudletMarkers();
            if (mUserLocationMarker != null) {
                mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, mDefaultCloudletColor, ""));
            }
        }

        if (mGoogleMap == null) {
            Log.w(TAG, "Map not ready. Will not modify.");
            return;
        }

        if (item.getItemId() == R.id.action_map_type_normal) {
            item.setChecked(true);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            setCustomMapStyle(mGoogleMap, R.raw.map_style_default);
        } else if (item.getItemId() == R.id.action_map_type_silver) {
            item.setChecked(true);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            setCustomMapStyle(mGoogleMap, R.raw.map_style_silver);
        } else if (item.getItemId() == R.id.action_map_type_retro) {
            item.setChecked(true);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            setCustomMapStyle(mGoogleMap, R.raw.map_style_retro);
        } else if (item.getItemId() == R.id.action_map_type_satellite) {
            item.setChecked(true);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (item.getItemId() == R.id.action_map_type_hybrid) {
            item.setChecked(true);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (item.getItemId() == R.id.action_map_type_terrain) {
            item.setChecked(true);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
    }

    private void setCustomMapStyle(GoogleMap googleMap, int mapStyleResource) {
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, mapStyleResource));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    public void putCommonIntentExtras(Intent intent) {
        if(meHelper.getSpoofedLocation() != null) {
            showMessage("Using spoofed location.");
            intent.putExtra(ImageProcessorFragment.EXTRA_SPOOF_GPS, true);
            intent.putExtra(ImageProcessorFragment.EXTRA_LATITUDE,
                    meHelper.getSpoofedLocation().getLatitude());
            intent.putExtra(ImageProcessorFragment.EXTRA_LONGITUDE,
                    meHelper.getSpoofedLocation().getLongitude());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady()");
        mGoogleMap = googleMap;

        try {
            mGoogleMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.i(TAG, "App should Request location permissions during onResume().");
        }

        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnMarkerDragListener(this);

        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            return;
        } else {
            startLocationUpdates();
        }

    }

    /**
     * This makes a web service call to the location simulator to update the current IP address
     * entry in the database with the given latitude/longitude.
     *
     * @param lat
     * @param lng
     */
    public void updateLocSimLocation(final double lat, final double lng) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("latitude", lat);
            jsonBody.put("longitude", lng);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        final String requestBody = jsonBody.toString();

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String hostName = meHelper.mDmeHostname.replace("dme", "locsim");
        String url = "http://"+hostName+":8888/updateLocation";
        Log.i(TAG, "updateLocSimLocation url="+url);
        Log.i(TAG, "updateLocSimLocation body="+requestBody);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "updateLocSimLocation response="+response);
                        if(response.startsWith("Location DB Updated OK")) {
                            meHelper.mLocationInSimulator = new Location("MobiledgeX_Loc_Sim");
                            meHelper.mLocationInSimulator.setLatitude(lat);
                            meHelper.mLocationInSimulator.setLongitude(lng);
                        }
                        Snackbar.make(findViewById(android.R.id.content), response, Snackbar.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "That didn't work! error="+error);
                Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.wtf(TAG, "Unsupported Encoding while trying to get the body bytes");
                    return null;
                }
            }
        };

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    /**
     * Gets list of cloudlets from DME, and populates map with markers.
     * @param clearExisting
     */
    public void getCloudlets(boolean clearExisting) {
        Log.i(TAG, "getCloudlets()  meHelper="+ meHelper);
        if (meHelper == null) {
            Log.i(TAG, "getCloudlets() meHelper not yet initialized");
            return;
        }
        Log.i(TAG, "getCloudlets() mLastKnownLocation="+meHelper.mLastKnownLocation);
        if(meHelper.mLastKnownLocation == null) {
            startLocationUpdates();
            showGpsWarning();
            return;
        }

        if (clearExisting) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Clear list so we don't show old cloudlets as transparent
                    //First, remove all cloudlet markers.
                    for (int i = 0; i < CloudletListHolder.getCloudletList().size(); i++) {
                        Cloudlet cloudlet = CloudletListHolder.getCloudletList().valueAt(i);
                        cloudlet.getMarker().remove();
                    }
                    //Then clear the list.
                    CloudletListHolder.getCloudletList().clear();
                }
            });
        }

        showMessage("Performing getAppInstList");
        try {
            meHelper.getAppInstList();
        } catch (InterruptedException | ExecutionException e) {
            String message = "Error during getAppInstList: " + e;
            Log.e(TAG, message);
            showError(message);
        }
    }

    @Override
    public ConnectionTester makeConnectionTester(boolean tls) {
        int testConnectionPort = DEFAULT_SPEED_TEST_PORT;
        String testUrl = "/test/";
        String expectedResponse = "Valid GET Request to server";
        if (meHelper.mAppName.equals("sdktest") || meHelper.mAppName.equals("automation-sdk-porttest")) {
            testUrl = "/automation.html";
            expectedResponse = "test server is running";
            testConnectionPort = 8085;
            tls = false;
        }
        String scheme = tls ? "https" : "http";
        String appInstUrl = scheme+"://"+meHelper.mClosestCloudlet.getFqdn()+":"+testConnectionPort+testUrl;

        return new ConnectionTester(appInstUrl, expectedResponse);
    }

    public void showGpsWarning() {
        if (uiHasBeenTouched) {
            Toast.makeText(MainActivity.this, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private BitmapDescriptor makeMarker(int resourceId, int color, String badgeText) {
        Drawable iconDrawable = getResources().getDrawable(resourceId);
        iconDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY );
        return getMarkerIconFromDrawable(iconDrawable, color, badgeText);
    }

    /**
     * Create map marker icon, based on given drawable, color, and add badge text if non-empty.
     *
     * @param drawable
     * @param color
     * @param badgeText
     * @return
     */
    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable, int color, String badgeText) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        if(badgeText != null && badgeText.length() != 0) {
            float scale = getResources().getDisplayMetrics().density;
            Log.d(TAG, "scale=" + scale + " x,y=" + drawable.getIntrinsicWidth() + "," + drawable.getIntrinsicHeight());
            Paint paint = new Paint();
            paint.setStrokeWidth(5);
            paint.setTextAlign(Paint.Align.CENTER);
            float textSize = 22 * scale;
            float badgeWidth = paint.measureText(badgeText);
            paint.setTextSize(textSize);
            paint.setColor(color);
            canvas.drawText(badgeText, drawable.getIntrinsicWidth() / 2, drawable.getIntrinsicHeight() / 2 + textSize / 2, paint);
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * When releasing a dragged marker or long-clicking on the map, the user will be prompted
     * if they want to either spoof the GPS at the dropped location, or to update the GPS location
     * for their IP address in the simulator.
     *
     * @param spoofLatLng  The location to use.
     */
    private void showSpoofGpsDialog(final LatLng spoofLatLng) {
        if(mUserLocationMarker == null) {
            Log.e(TAG, "No mUserLocationMarker.");
            return;
        }
        mUserLocationMarker.setPosition(spoofLatLng);
        final boolean[] resetPosition = {true}; //Has to be final. Use array so we can update the value.
        String spoofText = "Spoof GPS at this location";
        String addWaypointText = "Add waypoint to route";
        String updateSimText = "Update location in GPS database";
        List<String> items = new ArrayList<>();
        items.add(spoofText);
        if (mRoutePolyLine != null) {
            items.add(addWaypointText);
        }
        // Only allow updating location simulator on supported environments
        if (meHelper.mAllowLocationSimulatorUpdate) {
            items.add(updateSimText);
        }
        Log.i(TAG, "items="+items);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setSingleChoiceItems(items.toArray(new CharSequence[items.size()]), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Location location = new Location("MobiledgeX_Loc_Sim");
                location.setLatitude(spoofLatLng.latitude);
                location.setLongitude(spoofLatLng.longitude);
                // If the simulator location has been updated, use that as the starting location for
                // measuring distance, otherwise use actual GPS location.
                LatLng oldLatLng;
                if(meHelper.mLocationInSimulator == null) {
                    oldLatLng = new LatLng(meHelper.mLastKnownLocation.getLatitude(), meHelper.mLastKnownLocation.getLongitude());
                } else {
                    oldLatLng = new LatLng(meHelper.mLocationInSimulator.getLatitude(), meHelper.mLocationInSimulator.getLongitude());
                }

                String selectedItemText = items.get(which);

                if (selectedItemText.equals(spoofText)) {
                    Log.i(TAG, "Spoofing GPS at " + location);
                    showMessage("GPS spoofing activated.");
                    float[] results = new float[1];
                    Location.distanceBetween(oldLatLng.latitude, oldLatLng.longitude, spoofLatLng.latitude, spoofLatLng.longitude, results);
                    double distance = results[0] / 1000;
                    initUserMobileIcon();
                    mUserLocationMarker.setSnippet("Spoofed " + String.format("%.2f", distance) + " km from actual location");
                    meHelper.setSpoofedLocation(location);
                    resetPosition[0] = false;
                } else if (selectedItemText.equals(updateSimText)) {
                    Log.i(TAG, "Update GPS in simulator to " + location);
                    initUserMobileIcon();
                    mUserLocationMarker.setSnippet((String) getResources().getText(R.string.drag_to_spoof));
                    updateLocSimLocation(mUserLocationMarker.getPosition().latitude, mUserLocationMarker.getPosition().longitude);
                    meHelper.setSpoofedLocation(location);
                    resetPosition[0] = false;
                } else if (selectedItemText.equals(addWaypointText)) {
                    Log.i(TAG, "Add new waypoint at " + location);
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                            .position(spoofLatLng)
                            .alpha(0.6f)
                            .title("Waypoint " + mWaypointMarkers.size()+1)
                            .icon(BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                    marker.setDraggable(true);
                    mWaypointMarkers.add(marker);
                    routeBetweenPoints(mStartMarker.getPosition(), mEndMarker.getPosition(), mWaypointMarkers);
                } else {
                    Log.i(TAG, "Unknown dialog selection.");
                }
                dialog.dismiss();
            }
        });

        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (resetPosition[0]) {
                    mUserLocationMarker.setPosition(mPrevMarkPosition);
                }
            }
        });

        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (resetPosition[0]) {
                    mUserLocationMarker.setPosition(mPrevMarkPosition);
                }
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    /**
     * Set user location marker's icon, title, and snippet to default values.
     */
    protected void initUserMobileIcon() {
        Log.d(TAG, "initUserMobileIcon()");
        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, mDefaultCloudletColor, ""));
        mUserLocationMarker.setTitle(getString(R.string.location_not_verified));
        mUserLocationMarker.setSnippet((String) getResources().getText(R.string.drag_to_spoof));
        mUserLocationMarker.setTag("User");
        // Displayed text doesn't update if the InfoWindow is currently showing, so we cycle it.
        if (mUserLocationMarker.isInfoWindowShown()) {
            mUserLocationMarker.hideInfoWindow();
            mUserLocationMarker.showInfoWindow();
        }
        locationVerificationAttempted = locationVerified = false;

        // This will also erase any existing line, and will only
        // draw the line if we actually have a closest cloudlet.
        drawClosestCloudletLine();
    }

    /**
     * Reset all existing cloudlet markers to default state.
     */
    private void initAllCloudletMarkers() {
        for (int i = 0; i < CloudletListHolder.getCloudletList().size(); i++) {
            initCloudletMarker(CloudletListHolder.getCloudletList().valueAt(i));
        }
    }

    /**
     * Set cloudlet marker's icon, title, and snippet to default values.
     * @param cloudlet
     */
    private void initCloudletMarker(Cloudlet cloudlet) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Marker marker = cloudlet.getMarker();
                String cloudletName = cloudlet.getCloudletName();
                marker.setTitle(cloudletName + " Cloudlet");
                marker.setSnippet("Click for details");
                marker.setTag(cloudletName); // This is used by automation testing.
                marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, mDefaultCloudletColor, getBadgeText(cloudlet)));
            }
        });
    }

    @Override
    public void onRegister() {
        showMessage("Successfully registered client.");
    }

    /**
     * Callback for Matching Engine's verifyLocation results.
     *
     * @param status  GPS_Location_Status to determine success, fail, or caution
     * @param gpsLocationAccuracyKM  location accuracy, the location is verified to
     */
    public void onVerifyLocation(final AppClient.VerifyLocationReply.GPSLocationStatus status,
                                 final double gpsLocationAccuracyKM) {
        locationVerificationAttempted = true;
        mGpsLocationAccuracyKM = gpsLocationAccuracyKM;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String message;
                String message2="";
                if(mUserLocationMarker == null) {
                    Log.w(TAG, "No marker for user location");
                    return;
                }

                mUserLocationMarker.hideInfoWindow();
                switch (status) {
                    case LOC_VERIFIED:
                        message2 = "\n("+ mGpsLocationAccuracyKM +" km accuracy)";
                        if(mGpsLocationAccuracyKM <= 2) {
                            //Cat 1
                            mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_GREEN, ""));
                        } else if (mGpsLocationAccuracyKM <= 10) {
                            //Cat 2
                            mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_AMBER, ""));
                        } else {
                            //Cat 3
                            mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_DARK_AMBER, ""));
                        }
                        break;
                    case LOC_MISMATCH_SAME_COUNTRY:
                        //Cat 4
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_RED, ""));
                        break;
                    case LOC_ROAMING_COUNTRY_MATCH:
                        //Cat 6
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_GREEN, ""));
                        break;
                    default:
                        //Cat 5, 7, other
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_RED, ""));
                        break;
                }
                message = "User Location - "+status;
                mUserLocationMarker.setTitle(message);
                Toast.makeText(MainActivity.this, message+message2, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Determine what if any badge text should be shown for the given cloudlet.
     *
     * @param cloudlet
     * @return
     */
    private String getBadgeText(Cloudlet cloudlet) {
        String badgeText = "";
        if(cloudlet.getCarrierName().equalsIgnoreCase("gcp")) {
            badgeText = "G";
        } else if(cloudlet.getCarrierName().equalsIgnoreCase("azure")) {
            badgeText = "A";
        }
        return badgeText;
    }

    /**
     * Callback for Matching Engine's findCloudlet results. Looks through cloudlet list
     * and marks this one as closest by setting the color.
     *
     * @param closestCloudlet  Object encapsulating the closest cloudlet characteristics.
     */
    @Override
    public void onFindCloudlet(final AppClient.FindCloudletReply closestCloudlet) {
        // Get full list of cloudlets again in case any have been added or removed.
        getCloudlets(false);
        initAllCloudletMarkers();
        Cloudlet cloudlet = null;
        for (int i = 0; i < CloudletListHolder.getCloudletList().size(); i++) {
            cloudlet = CloudletListHolder.getCloudletList().valueAt(i);
            Log.i(TAG, "Checking: "+closestCloudlet.getFqdn()+" "+cloudlet.getFqdn());
            if(cloudlet.getFqdn().equals(closestCloudlet.getFqdn()) ) {
                Log.i(TAG, "Got a match! "+cloudlet.getCloudletName());
                Marker marker = cloudlet.getMarker();
                String badgeText = getBadgeText(cloudlet);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_VERIFIED, badgeText));
                    }
                });
                cloudlet.setBestMatch(true);
                break;
            }
        }
        if (cloudlet != null) {
            drawClosestCloudletLine();
            //Save the hostname for use by the Computer Vision Activity.
            meHelper.mClosestCloudletHostname = cloudlet.getHostName();
            Log.i(TAG, "mClosestCloudletHostname: "+ meHelper.mClosestCloudletHostname);
            showMessage("Closest Cloudlet is now: " + meHelper.mClosestCloudletHostname);
        }
    }

    /**
     * Callback for Matching Engine's getCloudletList results. Creates ArrayMap of cloudlets
     * keyed on the cloudlet name. A map marker is also created for each cloudlet.
     * @param cloudletList  List of found cloudlet instances.
     *
     */
    @Override
    public void onGetCloudletList(final AppClient.AppInstListReply cloudletList) {
        meHelper.mAppInstanceReplyList = cloudletList;
        Log.i(TAG, "onGetCloudletList()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayMap<String, Cloudlet> tempCloudlets = new ArrayMap<>();
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                mCloudletLatLngs.clear();

                // If you get an empty list because you have changed Region, but not yet Operator
                // (or vice versa), and then you set a proper combination that does return a list,
                // we don't want the dialog from the previous attempt to still be hanging around.
                if(mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }

                if(cloudletList.getCloudletsList().size() == 0) {
                    String message = "No cloudlets available.\nPlease update Region and Operator settings.";
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error")
                            .setMessage(message)
                            .setPositiveButton("OK", null);
                    mAlertDialog = dialogBuilder.show();
                    showError(message);
                }

                showMessage("Got "+cloudletList.getCloudletsList().size()+" cloudlets");
                int num = 1;
                //First get the new list into an ArrayMap so we can index on the cloudletName
                for(AppClient.CloudletLocation cloudletLocation : cloudletList.getCloudletsList()) {
                    Log.i(TAG, "getCloudletName()="+cloudletLocation.getCloudletName()+" getCarrierName()="+cloudletLocation.getCarrierName());
                    showMessage(" "+num+". "+cloudletLocation.getCloudletName());
                    num++;
                    Cloudlet cloudlet = makeCloudlet(cloudletLocation);
                    tempCloudlets.put(cloudlet.getCloudletName(), cloudlet);
                    builder.include(cloudlet.getMarker().getPosition());
                    mCloudletLatLngs.add(cloudlet.getMarker().getPosition());
                }

                // Hide the log viewer after a short delay.
                mEventLogViewer.initialLogsComplete();

                // Reset all cloudlet markers to default state.
                initAllCloudletMarkers();

                //Now see if all cloudlets still exist. If removed, show as semi-transparent.
                for (int i = 0; i < CloudletListHolder.getCloudletList().size(); i++) {
                    Cloudlet cloudlet = CloudletListHolder.getCloudletList().valueAt(i);
                    if (!tempCloudlets.containsKey(cloudlet.getCloudletName())) {
                        Log.i(TAG, cloudlet.getCloudletName() + " has been removed");
                        showMessage(cloudlet.getCloudletName() + " has been removed");
                        Marker marker = cloudlet.getMarker();
                        marker.setSnippet("Has been removed");
                        marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_FAILURE, getBadgeText(cloudlet)));
                        marker.setAlpha((float) 0.33);
                    }
                }
                CloudletListHolder.setCloudlets(tempCloudlets);

                // Erase "closest cloudlet" line if it exists.
                if (mClosestCloudletPolyLine != null) {
                    mClosestCloudletPolyLine.remove();
                }

                Log.d(TAG, "mUserLocationMarker="+mUserLocationMarker+" locationVerificationAttempted="+locationVerificationAttempted+" locationVerified="+locationVerified);
                if(mUserLocationMarker == null) {
                    // Create the marker representing the user/mobile device.
                    Location location = meHelper.getLocationForMatching();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.i(TAG, "addMarker for user location");
                    mUserLocationMarker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                    mUserLocationMarker.setZIndex(1); //Default is 0, so this will be drawn on top.
                    initUserMobileIcon();
                }
                builder.include(mUserLocationMarker.getPosition());

                // Update the camera view if needed.
                if(meHelper.getSpoofedLocation() != null) {
                    Log.i(TAG, "Leave the camera alone.");
                    return;
                }
                LatLngBounds bounds = builder.build();
                Log.i(TAG, "bounds.northeast="+bounds.northeast+" bounds.southwest="+bounds.southwest);

                //If there are no cloudlets, don't use the bounds, as it will zoom in super close.
                CameraUpdate cu;
                if(!bounds.southwest.equals(bounds.northeast)) {
                    Log.d(TAG, "Using cloudlet boundaries");
                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen
                    cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                } else {
                    Log.d(TAG, "No cloudlets. Don't zoom in");
                    cu = CameraUpdateFactory.newLatLng(mUserLocationMarker.getPosition());
                }
                try {
                    mGoogleMap.animateCamera(cu);
                } catch (Exception e) {
                    Log.e(TAG, "Map wasn't ready.", e);
                }
            }
        });
    }

    protected Cloudlet makeCloudlet(AppClient.CloudletLocation cloudletLocation) {
        String carrierName = cloudletLocation.getCarrierName();
        String cloudletName = cloudletLocation.getCloudletName();
        List<AppClient.Appinstance> appInstances = cloudletLocation.getAppinstancesList();
        // There will only be a single match because we supply all of appName, appVer,
        // and orgName in the request. So we just get the first item.
        String fqdn = appInstances.get(0).getFqdn();
        String appName = appInstances.get(0).getAppName();
        String FQDNPrefix = "";
        int publicPort = 0;
        mTls = false;
        List<distributed_match_engine.Appcommon.AppPort> ports = appInstances.get(0).getPortsList();
        String appPortFormat = "{Protocol: %d, FQDNPrefix: %s, TLS: %b, Container Port: %d, External Port: %d}";
        for (Appcommon.AppPort aPort : ports) {
            FQDNPrefix = aPort.getFqdnPrefix();
            Log.i(TAG, String.format(Locale.getDefault(), appPortFormat,
                    aPort.getProto().getNumber(),
                    aPort.getFqdnPrefix(),
                    aPort.getTls(),
                    aPort.getInternalPort(),
                    aPort.getPublicPort()));

            // For our app, the first port is for an http/ws server and can be TLS.
            // This server provides both CV and speedtest capabilities.
            // The second port is for a generic socket server.
            // Only choose the first port.
            if (publicPort == 0) {
                publicPort = aPort.getPublicPort();
                mTls = aPort.getTls();
                meHelper.setTestPort(publicPort);
                Log.i(TAG, "Using publicPort="+publicPort+" TLS="+mTls);

                if (publicPort != DEFAULT_SPEED_TEST_PORT) {
                    String message = "WARNING: appInst first port " + publicPort + " does not match " + DEFAULT_SPEED_TEST_PORT;
                    Log.w(TAG, message);
                }
            }
        }
        double distance = cloudletLocation.getDistance();
        LatLng latLng = new LatLng(cloudletLocation.getGpsLocation().getLatitude(), cloudletLocation.getGpsLocation().getLongitude());
        Cloudlet cloudlet;
        if(CloudletListHolder.getCloudletList().containsKey(cloudletName)){
            Log.i(TAG, "Reusing existing marker for "+cloudletName);
            cloudlet = CloudletListHolder.getCloudletList().get(cloudletName);
        } else {
            Log.i(TAG, "addMarker for "+cloudletName);
            Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng));
            cloudlet = new CloudletBuilder()
                    .setCloudletName(cloudletName)
                    .setAppName(appName)
                    .setCarrierName(carrierName)
                    .setGpsLocation(latLng)
                    .setDistance(distance).setFqdn(fqdn)
                    .setFqdnPrefix(FQDNPrefix)
                    .setTls(mTls)
                    .setMarker(marker)
                    .setPort(publicPort)
                    .createCloudlet();
            initCloudletMarker(cloudlet);
        }
        return cloudlet;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i(TAG, "onMarkerClick("+marker+"). Draggable="+marker.isDraggable());
        marker.showInfoWindow();
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.i(TAG, "onMapClick("+latLng+")");
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        if(marker.getTag().toString().equalsIgnoreCase("user")) {
            Log.d(TAG, "skipping mUserLocationMarker");
            return;
        }

        String cloudletName = (String) marker.getTag();
        Cloudlet cloudlet = CloudletListHolder.getCloudletList().get(cloudletName);
        Log.i(TAG, "1."+cloudlet+" "+cloudlet.getCloudletName()+" "+cloudlet.getSpeedTestDownloadResult());

        Intent intent = new Intent(getApplicationContext(), CloudletDetailsActivity.class);
        intent.putExtra("CloudletName", cloudlet.getCloudletName());
        startActivity(intent);
        Log.i(TAG, "Display Detailed Cloudlet Info");
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.i(TAG, "onMapLongClick("+latLng+")");
        if (mUserLocationMarker == null) {
            showError("User marker doesn't exist");
            return;
        }
        mPrevMarkPosition = mUserLocationMarker.getPosition();
        showSpoofGpsDialog(latLng);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Location location = meHelper.getLocationForMatching();
        mPrevMarkPosition = new LatLng(location.getLatitude(), location.getLongitude());
        Log.i(TAG, "onMarkerDragStart("+marker+")");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.i(TAG, "onMarkerDragEnd(" + marker + ")");
        if (marker.equals(mUserLocationMarker)) {
            showSpoofGpsDialog(marker.getPosition());
        }

        if (marker.equals(mStartMarker) || marker.equals(mEndMarker)) {
            Log.i(TAG, "Find route with moved start or end point");
            if (mRouteMode == RouteMode.DRIVING) {
                routeBetweenPoints(mStartMarker.getPosition(), mEndMarker.getPosition(), mWaypointMarkers);
            }
        }

        if (mWaypointMarkers.contains(marker)) {
            Log.i(TAG, "Find route with moved waypoint");
            routeBetweenPoints(mStartMarker.getPosition(), mEndMarker.getPosition(), mWaypointMarkers);
        }
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
            boolean showDialog = prefs.getBoolean(getResources().getString(R.string.pref_cv_show_latency_stats_dialog), false);
            if(!showDialog) {
                Log.d(TAG, "Preference is to not show latency stats dialog");
                return;
            }

            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, Locale.getDefault());
            String currentDateTime = format.format(new Date());

            String stats = data.getExtras().getString("STATS");
            // The TextView to show your Text
            TextView showText = new TextView(MainActivity.this);
            showText.setBackgroundColor(Color.parseColor("#EEEEEE"));
            showText.setText(currentDateTime + "\n" + stats);
            showText.setTextIsSelectable(true);
            int padding = (int) (15 * getResources().getDisplayMetrics().density);
            showText.setPadding(padding, padding, padding, padding);
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

    /**
     * In version 59 of this client app, the backend app name changed from "MobiledgeX SDK Demo"
     * to "ComputerVision". This method will update the General Settings custom app name Preference
     * value accordingly.
     *
     * However, we need to make sure this is only done once, in case the user specifically needs to
     * connect to a "MobiledgeX SDK Demo" backend app and has updated their preferences manually.
     * Because of this, we store a flag indicating that the upgrade has already happened so we can
     * check the value and not perform it again.
     *
     * @param sharedPreferences
     */
    private void upgradeToVersion59(SharedPreferences sharedPreferences) {
        String upgradedToVersion59Flag = "upgradedToVersion59Flag";
        boolean upgraded = sharedPreferences.getBoolean(upgradedToVersion59Flag, false);
        if (upgraded) {
            Log.i(TAG, "upgradeToVersion59 previously performed");
            return;
        }
        String prefKeyAppName = getResources().getString(R.string.pref_app_name);
        String oldDefaultAppName = "MobiledgeX SDK Demo";
        String newDefaultAppName = getResources().getString(R.string.dme_app_name);
        String appName = sharedPreferences.getString(prefKeyAppName, newDefaultAppName);
        if (appName.equals(oldDefaultAppName)) {
            Log.i(TAG, "upgradeToVersion59 changing "+prefKeyAppName+" from '"+oldDefaultAppName+"' to '"+newDefaultAppName+"'");
            sharedPreferences.edit().putString(prefKeyAppName, newDefaultAppName).apply();
        }
        sharedPreferences.edit().putBoolean(upgradedToVersion59Flag, true).apply();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged("+key+")");
        String prefKeyDownloadSize = getResources().getString(R.string.download_size);
        String prefKeyUploadSize = getResources().getString(R.string.upload_size);
        String prefKeyNumPackets = getResources().getString(R.string.latency_packets);
        String prefKeyLatencyMethod = getResources().getString(R.string.pref_latency_method);
        String prefKeyLatencyAutoStart = getResources().getString(R.string.pref_latency_autostart);
        String prefKeyDrivingAnimDuration = getResources().getString(R.string.pref_driving_time_seekbar);
        String prefKeyFlyingAnimDuration = getResources().getString(R.string.pref_flying_time_seekbar);

        if (key.equals(prefKeyLatencyMethod)) {
            String latencyTestMethod = sharedPreferences.getString(prefKeyLatencyMethod, defaultLatencyMethod);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+latencyTestMethod);
            CloudletListHolder.setLatencyTestMethod(latencyTestMethod);
        }

        if (key.equals(prefKeyLatencyAutoStart)) {
            boolean latencyTestAutoStart = sharedPreferences.getBoolean(prefKeyLatencyAutoStart, true);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+latencyTestAutoStart);
            CloudletListHolder.setLatencyTestAutoStart(latencyTestAutoStart);
        }

        if (key.equals(prefKeyDownloadSize)) {
            int numBytes = Integer.parseInt(sharedPreferences.getString(prefKeyDownloadSize, "10485760"));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+numBytes);
            CloudletListHolder.setNumBytesDownload(numBytes);
        }

        if (key.equals(prefKeyUploadSize)) {
            int numBytes = Integer.parseInt(sharedPreferences.getString(prefKeyUploadSize, "5242880"));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+numBytes);
            CloudletListHolder.setNumBytesUpload(numBytes);
        }

        if (key.equals(prefKeyNumPackets)) {
            int numPackets = Integer.parseInt(sharedPreferences.getString(prefKeyNumPackets, "5"));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+numPackets);
            CloudletListHolder.setNumPackets(numPackets);
        }

        if (key.equals(prefKeyDrivingAnimDuration)) {
            mDrivingAnimDuration = 1000 * sharedPreferences.getInt(prefKeyDrivingAnimDuration, DEF_DRIVING_DURATION);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mDrivingAnimDuration);
        }

        if (key.equals(prefKeyFlyingAnimDuration)) {
            mFlyingAnimDuration = 1000 * sharedPreferences.getInt(prefKeyFlyingAnimDuration, DEF_FLYING_DURATION);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mFlyingAnimDuration);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check permissions here, as the user has the ability to change them on the fly through
        // system settings.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            // Opens a UI. When it returns, onResume() is called again.
            mRpUtil.requestMultiplePermissions(this);
            return;
        }

        if (meHelper == null) {
            // Permissions available. Create a MobiledgeX MatchingEngineHelper instance (could also use Application wide instance).
            meHelper = new MatchingEngineHelper.Builder()
                    .setActivity(this)
                    .setMeHelperInterface(this)
                    .setView(mMapFragment.getView())
                    .setTestPort(DEFAULT_SPEED_TEST_PORT)
                    .build();
        }

        Log.i(TAG, "onResume() mEdgeEventsConfigUpdated="+mEdgeEventsConfigUpdated);
        if (mEdgeEventsEnabled && mEdgeEventsConfigUpdated) {
            meHelper.startEdgeEvents();
        }

        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (meHelper != null) {
            meHelper.stopLocationUpdates();
        }
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        meHelper.onDestroy();
    }

    /**
     * Based on a starting and ending point, get route from the Google Directions API, and use
     * the returned data to build paths to draw on the map, and build a list of points to
     * collect QOS data for.
     *  @param startLatLng  The route's starting point.
     * @param endLatLng  The route's ending point.
     * @param waypointMarkers  List of Waypoint markers to added to the route.
     */
    private void routeBetweenPoints(LatLng startLatLng, LatLng endLatLng, List<Marker> waypointMarkers) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(mApiKey)
                .build();

        if (mRoutePolyLine != null) {
            mRoutePolyLine.remove();
        }
        DirectionsApiRequest.Waypoint[] waypoints = new DirectionsApiRequest.Waypoint[waypointMarkers.size()];
        for (int i = 0; i < waypointMarkers.size(); i++) {
            Marker marker = waypointMarkers.get(i);
            // Converting between com.google.maps.model.LatLng
            // and com.google.android.gms.maps.model.Latlng is a pain!
            waypoints[i] = new DirectionsApiRequest.Waypoint(new com.google.maps.model.LatLng(marker.getPosition().latitude, marker.getPosition().longitude));
        }
        try {
            DirectionsResult calculatedRoutes = DirectionsApi.newRequest(context)
                    .alternatives(false)
                    .mode(TravelMode.DRIVING)
                    .origin(new com.google.maps.model.LatLng(startLatLng.latitude, startLatLng.longitude))
                    .waypoints(waypoints)
                    .destination(new com.google.maps.model.LatLng(endLatLng.latitude, endLatLng.longitude))
                    .await();

            if (calculatedRoutes == null || calculatedRoutes.routes.length == 0) {
                Log.w(TAG, "calculatedRoutes has no content");
                showError("No driving route found.");
                fabPlayRoute.setVisibility(View.GONE);
                return;
            }
            Log.d(TAG, "calculatedRoutes="+calculatedRoutes.routes.length);
            fabPlayRoute.setVisibility(View.VISIBLE);

            //Loop through legs and steps to get encoded polylines of each step
            for (DirectionsRoute route: calculatedRoutes.routes) {
                List<LatLng> path = new ArrayList<>();

                if (route.legs !=null) {
                    for (int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++) {
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points1.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            LatLng latLng = new LatLng(coord.lat, coord.lng);
                                            path.add(latLng);
                                            boundsBuilder.include(latLng);
                                        }
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    //Decode polyline and add points to list of route coordinates
                                    List<com.google.maps.model.LatLng> coords = points.decodePath();
                                    for (com.google.maps.model.LatLng coord : coords) {
                                        LatLng latLng = new LatLng(coord.lat, coord.lng);
                                        path.add(latLng);
                                        boundsBuilder.include(latLng);
                                    }
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "path.size()="+path.size());

                //Draw the polyline
                if (path.size() > 0) {
                    PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(8);
                    mRoutePolyLine = mGoogleMap.addPolyline(opts);
                }
            }

            // Include cloudlet locations in bounds we are going to zoom to.
            for (LatLng latLng : mCloudletLatLngs) {
                boundsBuilder.include(latLng);
            }
            LatLngBounds bounds = boundsBuilder.build();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (height * 0.10); // offset from edges of the map 10% of screen
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mGoogleMap.animateCamera(cu);

        } catch(Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "Error during routeBetweenPoints: "+ex.getLocalizedMessage());
            showError("Error: "+ex.getLocalizedMessage());
        }
    }
}
