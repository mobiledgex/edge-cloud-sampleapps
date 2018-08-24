package com.mobiledgex.sdkdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import com.android.volley.toolbox.JsonObjectRequest;
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
import com.mobiledgex.matchingengine.FindCloudletResponse;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.util.RequestPermissions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
            GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener,
            SharedPreferences.OnSharedPreferenceChangeListener, GoogleMap.OnMarkerDragListener, MatchingEngineResultsListener {

    private static final String TAG = "MainActivity";
    public static final int COLOR_NEUTRAL = 0xff676798;
    public static final int COLOR_VERIFIED = 0xff009933;
//    public static final int COLOR_VERIFIED = 0xff00ff00;
    public static final int COLOR_FAILURE = 0xffff3300;
//    public static final String HOSTNAME = "35.199.188.102";
    public static final String HOSTNAME = "acrotopia.com";

    private GoogleMap mGoogleMap;
    private ArrayMap<String, Cloudlet> mCloudlets = new ArrayMap<>();
    private MatchingEngineHelper mMatchingEngineHelper;
    private Marker mUserLocationMarker;
    private Location mLastKnownLocation;
    private Location mLocationForMatching;

    private RequestPermissions mRpUtil;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private boolean mDoLocationUpdates;

    private boolean gpsInitialized = false;

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

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
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
                getCloudlets();
                mMatchingEngineHelper.doEnhancedLocationUpdateInBackground(mLocationForMatching);
            }
        });

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
                    .putString(uuidKey, mMatchingEngineHelper.getMatchingEngine().createUUID().toString())
                    .apply();
        } else {
            mMatchingEngineHelper.getMatchingEngine().setUUID(UUID.fromString(currentUUID));
        }

        Log.i(TAG, "HOSTNAME="+HOSTNAME);

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Open "Settings" UI
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_reset) {
            // Reset spoofed GPS
            mMatchingEngineHelper.setSpoofedLocation(null);
            mUserLocationMarker.setPosition(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
            updateLocSimLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            getCloudlets();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // two minute interval
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            Log.i(TAG, "requestMultiplePermissions");
            mRpUtil.requestMultiplePermissions(this);
            return;
        } else {
            startLocationUpdates();
        }

    }

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
        String url = "http://"+HOSTNAME+":8888/updateLocation";
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
     * Gets list of cloudlets from DME, and populate map with markers.
     *
     */
    public void getCloudlets() {
        Log.i(TAG, "getCloudlets() mLastKnownLocation="+mLastKnownLocation);
        if(mLastKnownLocation == null) {
            startLocationUpdates();
            Toast.makeText(MainActivity.this, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();
            return;
        }

        // TODO: Rework this to use an SDK call once it has been added.
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://" + HOSTNAME + ":8080/GetCloudlets";

        // Request a JSON response from the provided URL.
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "Response is: " + response);
                        try {
                            mGoogleMap.clear();
                            ArrayMap<String, Cloudlet> tempCloudlets = new ArrayMap<String, Cloudlet>();
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();

                            JSONArray cloudlets = response.getJSONArray("Cloudlets");

                            //First get the new list into an ArrayMap so we can index on the CarrierName
                            int i;
                            for (i = 0; i < cloudlets.length(); i++) {
                                JSONObject cloudlet = cloudlets.getJSONObject(i);
                                Log.i(TAG, i + " cloudlet=" + cloudlet);
                                String carrierName = cloudlet.getString("CarrierName");
                                String cloudletName = cloudlet.getString("CloudletName");
                                JSONObject gpsCoords = cloudlet.getJSONObject("GpsLocation");
                                LatLng latLng = new LatLng(gpsCoords.getDouble("lat"), gpsCoords.getDouble("long"));
                                double distance = cloudlet.getDouble("Distance");
                                Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(cloudletName + " Cloudlet").snippet("Click for details"));
                                marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_NEUTRAL));
                                marker.setTag(cloudletName);
                                tempCloudlets.put(cloudletName, new Cloudlet(cloudletName, carrierName, latLng, distance, marker));
                                builder.include(marker.getPosition());
                            }

                            //Now see if all cloudlets still exist. If removed, show as transparent.
                            for (i = 0; i < mCloudlets.size(); i++) {
                                Cloudlet cloudlet = mCloudlets.valueAt(i);
                                if (!tempCloudlets.containsKey(cloudlet.getCloudletName())) {
                                    Log.i(TAG, cloudlet.getCloudletName() + " has been removed");
                                    Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(cloudlet.getLatitude(), cloudlet.getLongitude()))
                                            .title(cloudlet.getCloudletName() + " Cloudlet").snippet("Has been removed"));
                                    marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_NEUTRAL));
                                    marker.setAlpha((float) 0.33);
                                }
                            }

                            mCloudlets = tempCloudlets;

                            if(mMatchingEngineHelper.getSpoofedLocation() == null) {
                                mLocationForMatching = mLastKnownLocation;
                            } else {
                                mLocationForMatching = mMatchingEngineHelper.getSpoofedLocation();
                            }
                            LatLng latLng = new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude());
                            mUserLocationMarker = mGoogleMap.addMarker(new MarkerOptions().position(latLng)
                                    .title("User Location - Not Verified").snippet("Drag to spoof GPS")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)).draggable(true));
                            mUserLocationMarker.setTag("user");
                            mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_NEUTRAL));
                            builder.include(mUserLocationMarker.getPosition());

                            if(mMatchingEngineHelper.getSpoofedLocation() != null) {
                                Log.i(TAG, "Leave the camera alone.");
                                return;
                            }

                            LatLngBounds bounds = builder.build();
                            int padding = 240; // offset from edges of the map in pixels
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                            mGoogleMap.moveCamera(cu);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "That didn't work! error="+error);
                        Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);

    }

    @NonNull
    private BitmapDescriptor makeMarker(int resourceId, int color) {
        Drawable iconDrawable = getResources().getDrawable(resourceId);
        iconDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY );
        return getMarkerIconFromDrawable(iconDrawable);
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void onVerifyLocation(boolean validated) {
        String message;
        if(mUserLocationMarker == null) {
            Log.w(TAG, "No marker for user location");
            return;
        }
        if(validated) {
//            mUserLocationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_VERIFIED));
            message = "User Location - Verified";
        } else {
//            mUserLocationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_FAILURE));
            message = "User Location - Failed Verify";
        }
        mUserLocationMarker.setTitle(message);
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFindCloudlet(FindCloudletResponse closestCloudlet) {
        Cloudlet cloudlet = null;
        for (int i = 0; i < mCloudlets.size(); i++) {
            cloudlet = mCloudlets.valueAt(i);
            if(cloudlet.getMarker().getPosition().latitude == closestCloudlet.loc.getLat() &&
                    cloudlet.getMarker().getPosition().longitude == closestCloudlet.loc.getLong() ) {
                Log.i(TAG, "Got a match! "+cloudlet.getCloudletName());
//                cloudlet.getMarker().setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                cloudlet.getMarker().setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_VERIFIED));
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

        if(marker.getTag() == "user") {
            Log.d(TAG, "skipping mUserLocationMarker");
            return;
        }

        String cloudletName = (String) marker.getTag();
        Cloudlet cloudlet = mCloudlets.get(cloudletName);

        Intent intent = new Intent(getApplicationContext(), CloudletDetailsActivity.class);
        intent.putExtra("cloudlet", cloudlet);
        startActivity(intent);
        Log.i(TAG, "Display Detailed Cloudlet Info");
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.i(TAG, "onMapLongClick("+latLng+"). Spoof GPS to here.");
        showSpoofGpsDialog();
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
        showSpoofGpsDialog();
    }

    /**
     * When releasing a dragged marker the user will be prompted if they want to either spoof the
     * GPS at the dropped location, or to update the
     */
    private void showSpoofGpsDialog() {
        final CharSequence[] charSequence = new CharSequence[] {"Spoof GPS at this location", "Update location in GPS database"};

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setSingleChoiceItems(charSequence, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Location location = new Location("MEX");
                location.setLatitude(mUserLocationMarker.getPosition().latitude);
                location.setLongitude(mUserLocationMarker.getPosition().longitude);
                switch (which) {
                    case 0:
                        Log.i(TAG, "Spoof");
                        Toast.makeText(MainActivity.this, "GPS spoof enabled.", Toast.LENGTH_LONG).show();
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_NEUTRAL));
                        mMatchingEngineHelper.setSpoofedLocation(location);
                        getCloudlets();
                        break;
                    case 1:
                        Log.i(TAG, "Update");
                        updateLocSimLocation(mUserLocationMarker.getPosition().latitude, mUserLocationMarker.getPosition().longitude);
                        mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_NEUTRAL));
                        mMatchingEngineHelper.setSpoofedLocation(location);
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

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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
            }
        }
    };

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
            mGoogleMap.setMyLocationEnabled(true);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper());
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.i(TAG, "App should Request location permissions during onCreate().");
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

}

