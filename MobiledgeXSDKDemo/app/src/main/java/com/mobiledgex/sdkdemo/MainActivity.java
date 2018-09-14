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
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.maps.android.SphericalUtil;
import com.mobiledgex.matchingengine.FindCloudletResponse;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.util.RequestPermissions;
import com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera.CameraActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

import distributed_match_engine.AppClient;

import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_FIND_CLOUDLET;
import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_GET_CLOUDLETS;
import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_REGISTER_CLIENT;
import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_VERIFY_LOCATION;
import static distributed_match_engine.AppClient.Match_Engine_Loc_Verify.GPS_Location_Status.LOC_VERIFIED;
import static distributed_match_engine.AppClient.Match_Engine_Loc_Verify.GPS_Location_Status.LOC_ROAMING_COUNTRY_MATCH;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
            GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener,
            SharedPreferences.OnSharedPreferenceChangeListener, GoogleMap.OnMarkerDragListener, MatchingEngineResultsListener {

    private static final String TAG = "MainActivity";
    public static final int COLOR_NEUTRAL = 0xff676798;
    public static final int COLOR_VERIFIED = 0xff009933;
    public static final int COLOR_FAILURE = 0xffff3300;
    public static final int COLOR_CAUTION = 0xff00b33c; //Amber: ffbf00;
    public static final String HOSTNAME = "mexdemo.dme.mobiledgex.net"; //TODO: Make configurable preference

    private GoogleMap mGoogleMap;
    private MatchingEngineHelper mMatchingEngineHelper;
    private Marker mUserLocationMarker;
    private Location mLastKnownLocation;
    private Location mLocationForMatching;

    private RequestPermissions mRpUtil;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private boolean mDoLocationUpdates;

    private boolean gpsInitialized = false;
    private FloatingActionButton fabFindCloudlets;
    private boolean locationVerified = false;
    private boolean locationVerificationAttempted = false;
    private double mGpsLocationAccuracyKM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate() HOSTNAME="+HOSTNAME);

        /**
         * MatchEngine APIs require special user approved permissions to READ_PHONE_STATE and
         * one of the following:
         * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION. This creates a dialog, if needed.
         */
        mRpUtil = new RequestPermissions();
        mRpUtil.requestMultiplePermissions(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mMatchingEngineHelper = new MatchingEngineHelper(this, mapFragment.getView());
        mMatchingEngineHelper.setMatchingEngineResultsListener(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Restore mex location preference, defaulting to false:
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean mexLocationAllowed = prefs.getBoolean(getResources()
                        .getString(R.string.preference_mex_location_verification),
                false);
        MatchingEngine.setMexLocationAllowed(mexLocationAllowed);

        // Watch allowed preference:
        prefs.registerOnSharedPreferenceChangeListener(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMatchingEngineHelper.doEnhancedLocationUpdateInBackground(mLocationForMatching);
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
        boolean firstTimeUse = prefs.getBoolean(getResources().getString(R.string.preference_first_time_use), true);
        if (firstTimeUse) {
            new EnhancedLocationDialog().show(this.getSupportFragmentManager(), "dialog");
            String firstTimeUseKey = getResources().getString(R.string.preference_first_time_use);
            // Disable first time use.
            prefs.edit()
                    .putBoolean(firstTimeUseKey, false)
                    .apply();
        }

        // Set, or create create an App generated UUID for use in MatchingEngine, if there isn't one:
        String uuidKey = getResources().getString(R.string.preference_mex_user_uuid);
        String currentUUID = prefs.getString(uuidKey, "");
        if (currentUUID.isEmpty()) {
            prefs.edit()
                    .putString(uuidKey, mMatchingEngineHelper.getMatchingEngine().createUUID().toString())
                    .apply();
        } else {
            mMatchingEngineHelper.getMatchingEngine().setUUID(UUID.fromString(currentUUID));
        }

        String latencyTestMethod = prefs.getString(getResources().getString(R.string.latency_method), "socket");
        CloudletListHolder.getSingleton().setLatencyTestMethod(latencyTestMethod);

    }

    /**
     * Use the MatchingEngineHelper to perform a request with the Matching Engine.
     *
     * @param reqType  The request to perform.
     */
    private void matchingEngineRequest(MatchingEngineHelper.RequestType reqType) {
        Log.i(TAG, "matchingEngineRequest("+reqType+") mLastKnownLocation="+mLastKnownLocation);
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
        if (id == R.id.action_get_cloudlet_list) {
            getCloudlets();
        }
        if (id == R.id.action_reset_location) {
            // Reset spoofed GPS
            if(mLastKnownLocation == null) {
                startLocationUpdates();
                Toast.makeText(MainActivity.this, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();
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
        //TODO: Replace these with real items.
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

        } else if (id == R.id.nav_camera) {
            // Start the face recognition Activity
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
            return true;

//        } else if (id == R.id.nav_gallery) {
//
//        } else if (id == R.id.nav_slideshow) {
//
//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {

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
            Log.i(TAG, "requestMultiplePermissions");
            mRpUtil.requestMultiplePermissions(this);
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
    public void updateLocSimLocation(double lat, double lng) {
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
        String hostName = HOSTNAME.replace("dme", "locsim");
        String url = "http://"+hostName+":8888/updateLocation";
        Log.i(TAG, "updateLocSimLocation url="+url);
        Log.i(TAG, "updateLocSimLocation body="+requestBody);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "updateLocSimLocation response="+response);
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
        mUserLocationMarker.setPosition(spoofLatLng);
        final CharSequence[] charSequence = new CharSequence[] {"Spoof GPS at this location", "Update location in GPS database"};

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setSingleChoiceItems(charSequence, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Location location = new Location("MEX");
                location.setLatitude(spoofLatLng.latitude);
                location.setLongitude(spoofLatLng.longitude);
                LatLng oldLatLng = new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude());
                switch (which) {
                    case 0:
                        Log.i(TAG, "Spoof GPS at "+location);
                        Toast.makeText(MainActivity.this, "GPS spoof enabled.", Toast.LENGTH_LONG).show();
                        double distance = SphericalUtil.computeDistanceBetween(oldLatLng, spoofLatLng)/1000;
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
    public void onVerifyLocation(final AppClient.Match_Engine_Loc_Verify.GPS_Location_Status status,
                                 final double gpsLocationAccuracyKM) {
        locationVerificationAttempted = true;
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
                if(status == LOC_VERIFIED) {
                    fabFindCloudlets.setEnabled(true);
                    mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_VERIFIED, ""));
                    message = "User Location - Verified";
                    mGpsLocationAccuracyKM = gpsLocationAccuracyKM;
                    message2 = "\n("+ mGpsLocationAccuracyKM +" km accuracy)";
                } else if(status == LOC_ROAMING_COUNTRY_MATCH) {
                    mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_CAUTION, ""));
                    message = ""+status;
                    mGpsLocationAccuracyKM = gpsLocationAccuracyKM;
                } else {
                    mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_FAILURE, ""));
                    message = ""+status;
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
    public void onFindCloudlet(final FindCloudletResponse closestCloudlet) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cloudlet cloudlet = null;
                for (int i = 0; i < CloudletListHolder.getSingleton().getCloudletList().size(); i++) {
                    cloudlet = CloudletListHolder.getSingleton().getCloudletList().valueAt(i);
                    if(cloudlet.getMarker().getPosition().latitude == closestCloudlet.loc.getLat() &&
                            cloudlet.getMarker().getPosition().longitude == closestCloudlet.loc.getLong() ) {
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
    public void onGetCloudletList(final AppClient.Match_Engine_Cloudlet_List cloudletList) {
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
                    String uri = appInstances.get(0).getUri();
                    String appName = appInstances.get(0).getAppname();
                    double distance = cloudletLocation.getDistance();
                    LatLng latLng = new LatLng(cloudletLocation.getGpsLocation().getLat(), cloudletLocation.getGpsLocation().getLong());
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(cloudletName + " Cloudlet").snippet("Click for details"));
                    marker.setTag(cloudletName);

                    Cloudlet cloudlet;
                    if(CloudletListHolder.getSingleton().getCloudletList().containsKey(cloudletName)){
                        cloudlet = CloudletListHolder.getSingleton().getCloudletList().get(cloudletName);
                    } else {
                        cloudlet = new Cloudlet(cloudletName, appName, carrierName, latLng, distance, uri, marker, speedTestBytes, speedTestPackets);
                    }
                    marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_NEUTRAL, getBadgeText(cloudlet)));
                    cloudlet.update(cloudletName, appName, carrierName, latLng, distance, uri, marker, speedTestBytes, speedTestPackets);
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
                    int padding = 240; // offset from edges of the map in pixels
                    cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                } else {
                    Log.d(TAG, "No cloudlets. Don't zoom in");
                    cu = CameraUpdateFactory.newLatLng(mUserLocationMarker.getPosition());
                }
                try {
                    mGoogleMap.moveCamera(cu);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Or replace with an app specific dialog set.
        mRpUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final String prefKeyAllowMEX = getResources().getString(R.string.preference_mex_location_verification);

        String prefKeyDownloadSize = getResources().getString(R.string.download_size);
        String prefKeyNumPackets = getResources().getString(R.string.latency_packets);
        String prefKeyLatencyMethod = getResources().getString(R.string.latency_method);

        if (key.equals(prefKeyAllowMEX)) {
            boolean mexLocationAllowed = sharedPreferences.getBoolean(prefKeyAllowMEX, false);
            MatchingEngine.setMexLocationAllowed(mexLocationAllowed);
        }

        if (key.equals(prefKeyLatencyMethod)) {
            String latencyTestMethod = sharedPreferences.getString(getResources().getString(R.string.latency_method), "socket");
            CloudletListHolder.getSingleton().setLatencyTestMethod(latencyTestMethod);
        }

        if(key.equals(prefKeyDownloadSize) || key.equals(prefKeyNumPackets)) {
            int numBytes = Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.download_size), "1048576"));
            int numPackets = Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.latency_packets), "5"));
            for (int i = 0; i < CloudletListHolder.getSingleton().getCloudletList().size(); i++) {
                Cloudlet cloudlet = CloudletListHolder.getSingleton().getCloudletList().valueAt(i);
                cloudlet.setNumBytes(numBytes);
                cloudlet.setNumPackets(numPackets);
            }
        }
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
        Log.i(TAG, "startLocationUpdates()");
        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
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

