package com.mobiledgex.sdkdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.util.RequestPermissions;
import com.mobiledgex.sdkdemo.cv.ImageProcessorActivity;
import com.mobiledgex.sdkdemo.cv.ImageProcessorFragment;
import com.mobiledgex.sdkdemo.cv.ImageSender;
import com.mobiledgex.sdkdemo.cv.PoseProcessorActivity;
import com.mobiledgex.sdkdemo.qoe.QoeMapActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;

import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_FIND_CLOUDLET;
import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_GET_CLOUDLETS;
import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_REGISTER_CLIENT;
import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_VERIFY_LOCATION;
import static distributed_match_engine.AppClient.VerifyLocationReply.GPS_Location_Status.LOC_ROAMING_COUNTRY_MATCH;
import static distributed_match_engine.AppClient.VerifyLocationReply.GPS_Location_Status.LOC_VERIFIED;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
            GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener,
            SharedPreferences.OnSharedPreferenceChangeListener, GoogleMap.OnMarkerDragListener, MatchingEngineResultsInterface {

    private static final String TAG = "MainActivity";
    public static final int COLOR_NEUTRAL = 0xff676798;
    public static final int COLOR_VERIFIED = 0xff009933;
    public static final int COLOR_FAILURE = 0xffff3300;
    public static final int COLOR_CAUTION = 0xff00b33c; //Amber: ffbf00;

    // For TDG verifyLocation
    public static final int COLOR_GREEN = 0xff009933;
    public static final int COLOR_AMBER = 0xffffbf00;
    public static final int COLOR_DARK_AMBER = 0xffcfbf00;
    public static final int COLOR_RED = 0xffff3300;

    private static final int RC_SIGN_IN = 1;
    public static final int RC_STATS = 2;
    private String mHostname;

    private GoogleMap mGoogleMap;
    private MatchingEngineHelper mMatchingEngineHelper;
    private Marker mUserLocationMarker;
    private Location mLastKnownLocation;
    private Location mLocationForMatching;
    private Location mLocationInSimulator;

    private RequestPermissions mRpUtil;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private boolean mDoLocationUpdates;

    private boolean gpsInitialized = false;
    private FloatingActionButton fabFindCloudlets;
    private boolean locationVerified = false;
    private boolean locationVerificationAttempted = false;
    private double mGpsLocationAccuracyKM;
    private String defaultLatencyMethod = "ping";

    private GoogleSignInClient mGoogleSignInClient;
    private MenuItem signInMenuItem;
    private MenuItem signOutMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        /**
         * MatchEngine APIs require special user approved permissions to READ_PHONE_STATE and
         * one of the following:
         * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION. This creates a dialog, if needed.
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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mHostname = prefs.getString(getResources().getString(R.string.dme_hostname), "mexdemo.dme.mobiledgex.net");
        Log.i(TAG, "mHostname="+mHostname);

        mMatchingEngineHelper = new MatchingEngineHelper(this, mHostname, mapFragment.getView());
        mMatchingEngineHelper.setMatchingEngineResultsListener(this);

        boolean networkSwitchingAllowed = prefs.getBoolean(getResources()
                        .getString(R.string.preference_net_switching_allowed),false);
        Log.i(TAG, "networkSwitchingAllowed="+networkSwitchingAllowed);
        mMatchingEngineHelper.getMatchingEngine().setNetworkSwitchingEnabled(networkSwitchingAllowed);
//        mMatchingEngineHelper.getMatchingEngine().setSSLEnabled(false);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Restore mex location preference, defaulting to false:
        boolean mexLocationAllowed = prefs.getBoolean(getResources()
                        .getString(R.string.preference_mex_location_verification),
                false);
        MatchingEngine.setMexLocationAllowed(mexLocationAllowed);

        // Watch allowed preference:
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Client side FusedLocation updates.
        mDoLocationUpdates = true;

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFloatingActionBarClicked();
            }
        });

        fabFindCloudlets = findViewById(R.id.fab2);
        fabFindCloudlets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                matchingEngineRequest(REQ_FIND_CLOUDLET);
            }
        });
        boolean allowFindBeforeVerify = prefs.getBoolean(getResources().getString(R.string.preference_allow_find_before_verify), true);
        fabFindCloudlets.setEnabled(allowFindBeforeVerify);

        // Open dialog for MEX if this is the first time the app is created:
        String firstTimeUsePrefKey = getResources().getString(R.string.preference_first_time_use);
        boolean firstTimeUse = prefs.getBoolean(firstTimeUsePrefKey, true);
        if (firstTimeUse) {
            Intent intent = new Intent(this, FirstTimeUseActivity.class);
            startActivity(intent);
        }

        // Reuse the onSharedPreferenceChanged code to initialize anything dependent on these prefs:
        String prefKeyDownloadType = getResources().getString(R.string.download_type);
        String prefKeyDownloadSize = getResources().getString(R.string.download_size);
        String prefKeyNumPackets = getResources().getString(R.string.latency_packets);
        String prefKeyLatencyMethod = getResources().getString(R.string.latency_method);
        String prefKeyLatencyAutoStart = getResources().getString(R.string.pref_latency_autostart);

        onSharedPreferenceChanged(prefs, prefKeyDownloadType);
        onSharedPreferenceChanged(prefs, prefKeyDownloadSize);
        onSharedPreferenceChanged(prefs, prefKeyNumPackets);
        onSharedPreferenceChanged(prefs, prefKeyLatencyMethod);
        onSharedPreferenceChanged(prefs, prefKeyLatencyAutoStart);
    }

    /**
     * Perform the floatingActionBar action. Currently this is to perform the multi-step
     * matching engine process.
     */
    private void onFloatingActionBarClicked() {
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            mRpUtil.requestMultiplePermissions(this);
            return;
        }
        mMatchingEngineHelper.doEnhancedLocationUpdateInBackground(mLocationForMatching);
    }

    /**
     * Use the MatchingEngineHelper to perform a request with the Matching Engine.
     *
     * @param reqType  The request to perform.
     */
    private void matchingEngineRequest(MatchingEngineHelper.RequestType reqType) {
        Log.i(TAG, "matchingEngineRequest("+reqType+") mLastKnownLocation="+mLastKnownLocation);
        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            mRpUtil.requestMultiplePermissions(this);
            return;
        }
        if(mLastKnownLocation == null) {
            startLocationUpdates();
            Toast.makeText(MainActivity.this, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();
            return;
        }
//        getCloudletList();
        mMatchingEngineHelper.doRequestInBackground(reqType, mLocationForMatching);
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
                appVersion = pi.versionName+"."+pi.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        new AlertDialog.Builder(MainActivity.this)
                .setIcon(R.mipmap.ic_launcher_foreground)
                .setTitle("About")
                .setMessage(appName+"\nVersion: "+appVersion)
                .setPositiveButton("OK", null)
                .show();
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

        if (id == R.id.action_register_client) {
            matchingEngineRequest(REQ_REGISTER_CLIENT);
        }
        if (id == R.id.action_get_app_inst_list) {
            getCloudlets();
        }
        if (id == R.id.action_reset_location) {
            // Reset spoofed GPS
            if(mLastKnownLocation == null) {
                startLocationUpdates();
                Toast.makeText(MainActivity.this, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();
                return true;
            }
            if(mUserLocationMarker == null) {
                Log.w(TAG, "No marker for user location");
                Toast.makeText(MainActivity.this, "No user location marker. Please retry in a moment.", Toast.LENGTH_LONG).show();
                return true;
            }
            mMatchingEngineHelper.setSpoofedLocation(null);
            mUserLocationMarker.setPosition(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
            mUserLocationMarker.setSnippet((String) getResources().getText(R.string.drag_to_spoof));
            updateLocSimLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            locationVerified = false;
            locationVerificationAttempted = false;
            getCloudlets();
            return true;
        }
        if (id == R.id.action_verify_location) {
            matchingEngineRequest(REQ_VERIFY_LOCATION);
        }
        if (id == R.id.action_find_cloudlet) {
            matchingEngineRequest(REQ_FIND_CLOUDLET);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            // Open "Settings" UI
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_about) {
            // Handle the About action
            showAboutDialog();
            return true;
        } else if (id == R.id.nav_camera) {
            // Start the face detection Activity
            Intent intent = new Intent(this, ImageProcessorActivity.class);
            startActivityForResult(intent, RC_STATS);
            return true;
        } else if (id == R.id.nav_face_recognition) {
            // Start the face recognition Activity
            Intent intent = new Intent(this, ImageProcessorActivity.class);
            intent.putExtra(ImageProcessorFragment.EXTRA_FACE_RECOGNITION, true);
            startActivityForResult(intent, RC_STATS);
            return true;
        } else if (id == R.id.nav_pose_detection) {
            // Start the pose detection Activity
            Intent intent = new Intent(this, PoseProcessorActivity.class);
            startActivityForResult(intent, RC_STATS);
            return true;
        } else if (id == R.id.nav_qoe_map) {
            // Start the face detection Activity in Edge benchmark mode
            Intent intent = new Intent(this, QoeMapActivity.class);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady()");
        mGoogleMap = googleMap;

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
        String hostName = mHostname.replace("dme", "locsim");
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
                            mLocationInSimulator = new Location("MEX_Loc_Sim");
                            mLocationInSimulator.setLatitude(lat);
                            mLocationInSimulator.setLongitude(lng);
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
     *
     */
    public void getCloudlets() {
        Log.i(TAG, "getCloudletList() mLastKnownLocation="+mLastKnownLocation);
        if(mLastKnownLocation == null) {
            startLocationUpdates();
            Toast.makeText(MainActivity.this, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();
            return;
        }

        if(mMatchingEngineHelper.getSpoofedLocation() == null) {
            mLocationForMatching = mLastKnownLocation;
        } else {
            mLocationForMatching = mMatchingEngineHelper.getSpoofedLocation();
        }

        mMatchingEngineHelper.doRequestInBackground(REQ_GET_CLOUDLETS, mLocationForMatching);
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
        String spoofText = "Spoof GPS at this location";
        String updateSimText = "Update location in GPS database";
        final CharSequence[] charSequence;
        //Only allow updating location simulator on the demo environment
        if(mHostname.equals("mexdemo.dme.mobiledgex.net")) {
            charSequence = new CharSequence[] {spoofText, updateSimText};
        } else {
            charSequence = new CharSequence[] {spoofText};
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setSingleChoiceItems(charSequence, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Location location = new Location("MEX");
                location.setLatitude(spoofLatLng.latitude);
                location.setLongitude(spoofLatLng.longitude);
                // If the simulator location has been updated, use that as the starting location for
                // measuring distance, otherwise use actual GPS location.
                LatLng oldLatLng;
                if(mLocationInSimulator == null) {
                    oldLatLng = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                } else {
                    oldLatLng = new LatLng(mLocationInSimulator.getLatitude(), mLocationInSimulator.getLongitude());
                }
                switch (which) {
                    case 0:
                        Log.i(TAG, "Spoof GPS at "+location);
                        Toast.makeText(MainActivity.this, "GPS spoof enabled.", Toast.LENGTH_LONG).show();
                        float[] results = new float[1];
                        Location.distanceBetween(oldLatLng.latitude, oldLatLng.longitude, spoofLatLng.latitude, spoofLatLng.longitude, results);
                        double distance = results[0]/1000;
                        mUserLocationMarker.setSnippet("Spoofed "+String.format("%.2f", distance)+" km from actual location");
                        mMatchingEngineHelper.setSpoofedLocation(location);
                        locationVerificationAttempted = locationVerified = false;
                        getCloudlets();
                        break;
                    case 1:
                        Log.i(TAG, "Update GPS in simulator to "+location);
                        mUserLocationMarker.setSnippet((String) getResources().getText(R.string.drag_to_spoof));
                        updateLocSimLocation(mUserLocationMarker.getPosition().latitude, mUserLocationMarker.getPosition().longitude);
                        mMatchingEngineHelper.setSpoofedLocation(location);
                        locationVerificationAttempted = locationVerified = false;
                        getCloudlets();
                        break;
                    default:
                        Log.i(TAG, "Unknown dialog selection.");
                }

            }
        });

        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mUserLocationMarker.setPosition(new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude()));
            }
        });

        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mUserLocationMarker.setPosition(new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude()));
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onRegister(final String sessionCookie) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Successfully registered client. sessionCookie=\n"+sessionCookie, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback for Matching Engine's verifyLocation results.
     *
     * @param status  GPS_Location_Status to determine success, fail, or caution
     * @param gpsLocationAccuracyKM  location accuracy, the location is verified to
     */
    public void onVerifyLocation(final AppClient.VerifyLocationReply.GPS_Location_Status status,
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
                // We handle this differently for the demo environment, than in real life.
                if(mHostname.equals("mexdemo.dme.mobiledgex.net")) {
                    if(status == LOC_VERIFIED) {
                        fabFindCloudlets.setEnabled(true);
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_VERIFIED, ""));
                        message = "User Location - Verified";
                        message2 = "\n("+ mGpsLocationAccuracyKM +" km accuracy)";
                    } else if(status == LOC_ROAMING_COUNTRY_MATCH) {
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_CAUTION, ""));
                        message = "User Location - Verified";
                    } else {
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_FAILURE, ""));
                        message = "User Location - Failed Verify";
                    }

                } else {
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
                }
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cloudlet cloudlet = null;
                for (int i = 0; i < CloudletListHolder.getSingleton().getCloudletList().size(); i++) {
                    cloudlet = CloudletListHolder.getSingleton().getCloudletList().valueAt(i);
                    if(cloudlet.getMarker().getPosition().latitude == closestCloudlet.getCloudletLocation().getLatitude() &&
                            cloudlet.getMarker().getPosition().longitude == closestCloudlet.getCloudletLocation().getLongitude() ) {
                        Log.i(TAG, "Got a match! "+cloudlet.getCloudletName());
                        cloudlet.getMarker().setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_VERIFIED, getBadgeText(cloudlet)));
                        cloudlet.setBestMatch(true);
                        break;
                    }
                }
                if(cloudlet != null) {
                    Polyline line = mGoogleMap.addPolyline(new PolylineOptions()
                            .add(mUserLocationMarker.getPosition(), cloudlet.getMarker().getPosition())
                            .width(8)
                            .color(COLOR_VERIFIED));
                }
            }
        });
    }

    /**
     * Callback for Matching Engine's getCloudletList results. Creates ArrayMap of cloudlets
     * keyed on the cloudlet name. A map marker is also created for each cloudlet.
     *
     * @param cloudletList  List of found cloudlet instances.
     */
    @Override
    public void onGetCloudletList(final AppClient.AppInstListReply cloudletList) {
        Log.i(TAG, "onGetCloudletList()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGoogleMap.clear();
                ArrayMap<String, Cloudlet> tempCloudlets = new ArrayMap<>();
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                if(cloudletList.getCloudletsList().size() == 0) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error")
                            .setMessage("No cloudlets available.\nContact MobiledgeX support.")
                            .setPositiveButton("OK", null)
                            .show();
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int speedTestBytes = Integer.parseInt(prefs.getString(getResources().getString(R.string.download_size), "1048576"));
                int speedTestPackets = Integer.parseInt(prefs.getString(getResources().getString(R.string.latency_packets), "5"));

                //First get the new list into an ArrayMap so we can index on the cloudletName
                for(AppClient.CloudletLocation cloudletLocation:cloudletList.getCloudletsList()) {
                    Log.i(TAG, "getCloudletName()="+cloudletLocation.getCloudletName()+" getCarrierName()="+cloudletLocation.getCarrierName());
                    String carrierName = cloudletLocation.getCarrierName();
                    String cloudletName = cloudletLocation.getCloudletName();
                    List<AppClient.Appinstance> appInstances = cloudletLocation.getAppinstancesList();
                    //TODO: What if there is more than 1 appInstance in the list?
                    //There shouldn't be since we use all of appName, appVer, and devName in the
                    //request. There should only be a single match.
                    String uri = appInstances.get(0).getFQDN();
                    String appName = appInstances.get(0).getAppName();
                    String FQDNPrefix = "";
                    int publicPort = 7777;
                    List<distributed_match_engine.Appcommon.AppPort> ports = appInstances.get(0).getPortsList();
                    String appPortFormat = "{Protocol: %d, FQDNPrefix: %s, Container Port: %d, External Port: %d, Public Path: '%s'}";
                    for (Appcommon.AppPort aPort : ports) {
                        FQDNPrefix = aPort.getFQDNPrefix();
                        publicPort = aPort.getPublicPort();
                        Log.i(TAG, String.format(Locale.getDefault(), appPortFormat,
                                    aPort.getProto().getNumber(),
                                    aPort.getFQDNPrefix(),
                                    aPort.getInternalPort(),
                                    aPort.getPublicPort(),
                                    aPort.getPublicPath()));
                    }
                    double distance = cloudletLocation.getDistance();
                    LatLng latLng = new LatLng(cloudletLocation.getGpsLocation().getLatitude(), cloudletLocation.getGpsLocation().getLongitude());
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(cloudletName + " Cloudlet").snippet("Click for details"));
                    marker.setTag(cloudletName);

                    Cloudlet cloudlet;
                    if(CloudletListHolder.getSingleton().getCloudletList().containsKey(cloudletName)){
                        cloudlet = CloudletListHolder.getSingleton().getCloudletList().get(cloudletName);
                    } else {
                        cloudlet = new Cloudlet(cloudletName, appName, carrierName, latLng, distance, uri, marker, FQDNPrefix, publicPort);
                    }
                    marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_NEUTRAL, getBadgeText(cloudlet)));
                    cloudlet.update(cloudletName, appName, carrierName, latLng, distance, uri, marker, FQDNPrefix, publicPort);
                    tempCloudlets.put(cloudletName, cloudlet);
                    builder.include(marker.getPosition());

                }

                //Now see if all cloudlets still exist. If removed, show as transparent.
                for (int i = 0; i < CloudletListHolder.getSingleton().getCloudletList().size(); i++) {
                    Cloudlet cloudlet = CloudletListHolder.getSingleton().getCloudletList().valueAt(i);
                    if (!tempCloudlets.containsKey(cloudlet.getCloudletName())) {
                        Log.i(TAG, cloudlet.getCloudletName() + " has been removed");
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(cloudlet.getLatitude(), cloudlet.getLongitude()))
                                .title(cloudlet.getCloudletName() + " Cloudlet").snippet("Has been removed"));
                        marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_FAILURE, getBadgeText(cloudlet)));
                        marker.setAlpha((float) 0.33);
                    }
                }
                CloudletListHolder.getSingleton().setCloudlets(tempCloudlets);

                // Create the marker representing the user/mobile device.
                String tag = "User";
                String title = "User Location - Not Verified";
                String snippet = (String) getResources().getText(R.string.drag_to_spoof);
                BitmapDescriptor icon = makeMarker(R.mipmap.ic_marker_mobile, COLOR_NEUTRAL, "");

                if(mUserLocationMarker != null) {
                    snippet = mUserLocationMarker.getSnippet();
                    if (locationVerificationAttempted) {
                        if (locationVerified) {
                            icon = makeMarker(R.mipmap.ic_marker_mobile, COLOR_VERIFIED, "");
                            title = "User Location - Verified";
                        } else {
                            icon = makeMarker(R.mipmap.ic_marker_mobile, COLOR_FAILURE, "");
                            title = "User Location - Failed Verify";
                        }
                    }
                }

                LatLng latLng = new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude());
                mUserLocationMarker = mGoogleMap.addMarker(new MarkerOptions().position(latLng)
                        .title(title).snippet(snippet)
                        .icon(icon).draggable(true));
                mUserLocationMarker.setTag(tag);
                builder.include(mUserLocationMarker.getPosition());

                // Update the camera view if needed.
                if(mMatchingEngineHelper.getSpoofedLocation() != null) {
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
        Cloudlet cloudlet = CloudletListHolder.getSingleton().getCloudletList().get(cloudletName);
        Log.i(TAG, "1."+cloudlet+" "+cloudlet.getCloudletName()+" "+cloudlet.getMbps());

        Intent intent = new Intent(getApplicationContext(), CloudletDetailsActivity.class);
        intent.putExtra("CloudletName", cloudlet.getCloudletName());
        startActivity(intent);
        Log.i(TAG, "Display Detailed Cloudlet Info");
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.i(TAG, "onMapLongClick("+latLng+")");
        showSpoofGpsDialog(latLng);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.i(TAG, "onMarkerDragStart("+marker+")");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.i(TAG, "onMarkerDragEnd(" + marker + ")");
        showSpoofGpsDialog(marker.getPosition());
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
            boolean showDialog = prefs.getBoolean(getResources().getString(R.string.preference_fd_show_latency_stats_dialog), false);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged("+key+")");
        String prefKeyAllowMEX = getResources().getString(R.string.preference_mex_location_verification);
        String prefKeyAllowNetSwitch = getResources().getString(R.string.preference_net_switching_allowed);
        String prefKeyDownloadType = getResources().getString(R.string.download_type);
        String prefKeyDownloadSize = getResources().getString(R.string.download_size);
        String prefKeyNumPackets = getResources().getString(R.string.latency_packets);
        String prefKeyLatencyMethod = getResources().getString(R.string.latency_method);
        String prefKeyLatencyAutoStart = getResources().getString(R.string.pref_latency_autostart);
        String prefKeyDmeHostname = getResources().getString(R.string.dme_hostname);
        String prefKeyHostCloud = getResources().getString(R.string.preference_fd_host_cloud);
        String prefKeyHostEdge = getResources().getString(R.string.preference_fd_host_edge);
        String prefKeyResetFdHosts = getResources().getString(R.string.preference_fd_reset_both_hosts);

        if (key.equals(prefKeyAllowMEX)) {
            boolean mexLocationAllowed = sharedPreferences.getBoolean(prefKeyAllowMEX, false);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mexLocationAllowed);
            MatchingEngine.setMexLocationAllowed(mexLocationAllowed);
        }

        if (key.equals(prefKeyAllowNetSwitch)) {
            boolean netSwitchingAllowed = sharedPreferences.getBoolean(prefKeyAllowNetSwitch, false);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+netSwitchingAllowed);
            mMatchingEngineHelper.getMatchingEngine().setNetworkSwitchingEnabled(netSwitchingAllowed);
        }

        if (key.equals(prefKeyLatencyMethod)) {
            String latencyTestMethod = sharedPreferences.getString(prefKeyLatencyMethod, defaultLatencyMethod);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+latencyTestMethod);
            CloudletListHolder.getSingleton().setLatencyTestMethod(latencyTestMethod);
        }

        if (key.equals(prefKeyLatencyAutoStart)) {
            boolean latencyTestAutoStart = sharedPreferences.getBoolean(prefKeyLatencyAutoStart, true);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+latencyTestAutoStart);
            CloudletListHolder.getSingleton().setLatencyTestAutoStart(latencyTestAutoStart);
        }

        if (key.equals(prefKeyDmeHostname)) {
            mHostname = sharedPreferences.getString(prefKeyDmeHostname, "mexdemo.dme.mobiledgex.net");
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mHostname);
            mMatchingEngineHelper.setHostname(mHostname);
            //Clear list so we don't show old cloudlets as transparent
            CloudletListHolder.getSingleton().getCloudletList().clear();
            getCloudlets();
        }

        if (key.equals(prefKeyDownloadType)) {
            String downloadType = sharedPreferences.getString(prefKeyDownloadType, "dynamic");
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+downloadType);
            CloudletListHolder.getSingleton().setDownloadTestType(downloadType);
        }

        if (key.equals(prefKeyDownloadSize)) {
            int numBytes = Integer.parseInt(sharedPreferences.getString(prefKeyDownloadSize, "1048576"));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+numBytes);
            CloudletListHolder.getSingleton().setNumBytes(numBytes);
        }

        if (key.equals(prefKeyNumPackets)) {
            int numPackets = Integer.parseInt(sharedPreferences.getString(prefKeyNumPackets, "5"));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+numPackets);
            CloudletListHolder.getSingleton().setNumPackets(numPackets);
        }

        if(key.equals(prefKeyHostCloud)) {
            // This call will attempt to connect to the server at prefKeyHostCloud.
            // If it fails, the default will be restored.
            new FaceServerConnectivityTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    ImageProcessorFragment.DEF_FACE_HOST_CLOUD, key);
        }
        if(key.equals(prefKeyHostEdge)) {
            // This call will attempt to connect to the server at prefKeyHostEdge.
            // If it fails, the default will be restored.
            new FaceServerConnectivityTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    ImageProcessorFragment.DEF_FACE_HOST_EDGE, key);
        }

        if(key.equals(prefKeyResetFdHosts)) {
            String value = sharedPreferences.getString(prefKeyResetFdHosts, "No");
            Log.i(TAG, prefKeyResetFdHosts+" "+value);
            if(value.startsWith("Yes")) {
                Log.i(TAG, "Resetting Face server hosts.");
                sharedPreferences.edit().putString(prefKeyHostCloud, ImageProcessorFragment.DEF_FACE_HOST_CLOUD).apply();
                sharedPreferences.edit().putString(prefKeyHostEdge, ImageProcessorFragment.DEF_FACE_HOST_EDGE).apply();
                Toast.makeText(this, "Face detection hosts reset to default.", Toast.LENGTH_SHORT).show();
            }
            //Always set the value back to something so that either clicking Yes or No in the dialog
            //will activate this "changed" call.
            sharedPreferences.edit().putString(prefKeyResetFdHosts, "XXX_garbage_value").apply();
        }

    }

    private class FaceServerConnectivityTask extends AsyncTask<String, Void, Boolean> {
        private String newHost;

        @Override
        protected Boolean doInBackground(String... params) {
            String defaultHost = params[0];
            String keyName = params[1];
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            newHost = prefs.getString(keyName, defaultHost);
            boolean reachable = ImageSender.isReachable(newHost,
                    ImageSender.getFaceDetectionServerPort(newHost), 3000);
            if(!reachable) {
                Log.e(TAG, newHost+" not reachable. Resetting "+keyName+" to default.");
                prefs.edit().putString(keyName, defaultHost).apply();
                return false;
            }
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            String message;
            if(result) {
                message = "Verified new host: "+newHost;
                Log.i(TAG, message);
            } else {
                message = "Could not reach face server at '"+newHost+"'. Resetting to default.";
            }
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        }
        @Override
        protected void onPreExecute() {}
        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() mDoLocationUpdates="+mDoLocationUpdates);

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
        Log.i(TAG, "startLocationUpdates()");
        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            Log.i(TAG, "Location permission has NOT been granted");
            return;
        }
        Log.i(TAG, "Location permission has been granted");

        if(mGoogleMap == null) {
            Log.w(TAG, "Map not ready");
            return;
        }

        try {
            mGoogleMap.setMyLocationEnabled(true);

            int interval = 5000; // Initially, 5 second interval to get the first update quickly
            Log.i(TAG, "mFusedLocationClient.getLastLocation()="+mFusedLocationClient.getLastLocation()+" interval="+interval);

            mLocationRequest = new LocationRequest();
            mLocationRequest.setSmallestDisplacement(5);
            mLocationRequest.setInterval(interval);
            mLocationRequest.setFastestInterval(interval);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            Log.i(TAG, "mFusedLocationClient.requestLocationUpdates() called");
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.i(TAG, "App should Request location permissions during onCreate().");
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i(TAG, "onLocationResult() Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastKnownLocation = locationResult.getLastLocation();
                if(!gpsInitialized) {
                    getCloudlets();
                    gpsInitialized = true;
                }

                if(mLocationRequest.getInterval() < 120000) {
                    // Now that we got our first update, make interval longer to save battery.
                    Log.i(TAG, "Slowing down location request interval");
                    mLocationRequest.setInterval(120000); // two minute interval
                    mLocationRequest.setFastestInterval(120000);
                }
            }
        }
    };

}

