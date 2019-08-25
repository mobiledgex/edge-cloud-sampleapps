package com.mobiledgex.sdkdemo.qoe;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TravelMode;
import com.mobiledgex.matchingengine.ChannelIterator;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.sdkdemo.MainActivity;
import com.mobiledgex.sdkdemo.MatchingEngineHelper;
import com.mobiledgex.sdkdemo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import distributed_match_engine.LocOuterClass;

public class QoeMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener,
        GoogleMap.OnCameraIdleListener, DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    private static final String TAG = "QoeMapActivity";
    public static final String EXTRA_HOSTNAME = "EXTRA_HOSTNAME";
    public static final String EXTRA_CARRIER_NAME = "EXTRA_CARRIER_NAME";
    public static final String[] COLORS = {"#000000", "#ff0000", "#ff8900", "#ffff00", "#bbff99", "#339933", "#006600"};
    public static final String[] SPEEDS = {"no data", "bad", "slow", "ok", "good", "fast", "very fast"};
    public static final double[] DL_THRESHOLDS = {0, 35, 45, 55, 74, 100, Double.POSITIVE_INFINITY};
    public static final double[] UL_THRESHOLDS = {0, 0.04, 0.12, 0.4, 2, 4, Double.POSITIVE_INFINITY};
    public static final int GRID_POINT_ALPHA = 128;
    private String mHostname;
    private GoogleMap mMap;
    private int routeWidth = 20;
    private Marker startMarker;
    private Marker endMarker;
    private String apiKey;
    private LatLng originLatLng;
    private LatLng destinationLatLng;
    private int requestNum = 0;
    private MenuItem modeGroupPrevItem;
    private MenuItem mapTypeGroupPrevItem;
    private Button buttonTime;
    private Button buttonDate;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateTimeFormat;
    private Calendar mCalendar;
    private BroadcastReceiver mBroadcastReceiver;
    private MatchingEngineHelper mMatchingEngineHelper;
    private double gridSize;

    private enum pointMode {
        ROUTE,
        GRID
    }
    private pointMode mPointMode = pointMode.ROUTE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qoe_map);
        Toolbar toolbar = findViewById(R.id.toolbar_qoe);

        Intent intent = getIntent();
        mHostname = intent.getStringExtra(EXTRA_HOSTNAME);
        Log.i(TAG, "mHostname="+mHostname);
        if(mHostname == null) {
            mHostname = MainActivity.DEFAULT_DME_HOSTNAME;
        }
        String carrierName = intent.getStringExtra(EXTRA_CARRIER_NAME);
        if(carrierName == null) {
            carrierName = MainActivity.DEFAULT_CARRIER_NAME;
        }

        View mapView = findViewById(R.id.map);
        mMatchingEngineHelper = new MatchingEngineHelper(this, mHostname, carrierName, mapView);
        mMatchingEngineHelper.registerClientInBackground();

        // Build Legend Table View programmatically, based on colors and speed description values.
        TableLayout tl = findViewById(R.id.table_layout_legend);
        tl.removeAllViews();
        for(int i = COLORS.length - 1; i >=0 ; i--) {
            String color = COLORS[i];
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new TableRow.LayoutParams());
            TextView colorBlockTv = new TextView(this);
            colorBlockTv.setText("      "); //This many spaces is nearly square with default font.
            colorBlockTv.setBackgroundColor(Color.parseColor(color));
            TextView descriptionTv = new TextView(this);
            descriptionTv.setText(" "+SPEEDS[i]);
            descriptionTv.setBackgroundColor(Color.WHITE);
            tr.addView(colorBlockTv);
            tr.addView(descriptionTv);
            tl.addView(tr, new TableLayout.LayoutParams());
        }

        buttonDate = findViewById(R.id.buttonDate);
        buttonTime = findViewById(R.id.buttonTime);
        //Set Date and Time buttons to current
        if(DateFormat.is24HourFormat(this)) {
            timeFormat = new SimpleDateFormat("HH:mm:00");
            dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        } else {
            timeFormat = new SimpleDateFormat("hh:mm:00 a");
            dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:00 a");
        }
        dateFormat = new SimpleDateFormat("E, yyyy-MM-dd");

        mCalendar = Calendar.getInstance(TimeZone.getDefault());
        buttonDate.setText(dateFormat.format(mCalendar.getTime()));
        buttonTime.setText(timeFormat.format(mCalendar.getTime()));

        //If we haven't set a future date/time, update our Calendar instance every minute.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    if(mCalendar.getTimeInMillis() < System.currentTimeMillis()) {
                        mCalendar = Calendar.getInstance(TimeZone.getDefault());
                        buttonTime.setText(timeFormat.format(mCalendar.getTime()));
                    }
                }
            }
        };

        registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        apiKey = getResources().getString(R.string.google_directions_api_key);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void selectDate(View view) {
        // Create a new instance of DatePickerDialog and show it
        DatePickerDialog dialog = new DatePickerDialog(this, this,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    public void selectTime(View view) {
        // Create a new instance of TimePickerDialog and show it
        TimePickerDialog dialog = new TimePickerDialog(this, this,
                mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this));
        dialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        //Make sure date/time is not in the past.
        Calendar newCal = (Calendar) mCalendar.clone();
        newCal.set(year, month, dayOfMonth);
        if(newCal.getTimeInMillis() < System.currentTimeMillis()) {
            String message = "Error: "+dateTimeFormat.format(newCal.getTime())+" is in the past";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        mCalendar.set(year, month, dayOfMonth);
        buttonDate.setText(dateFormat.format(mCalendar.getTime()));
        routeBetweenPoints(originLatLng, destinationLatLng);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        //Make sure date/time is not in the past.
        Calendar newCal = (Calendar) mCalendar.clone();
        newCal.set(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
        if(newCal.getTimeInMillis() < System.currentTimeMillis()) {
            String message = "Error: "+dateTimeFormat.format(newCal.getTime())+" is in the past";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }
        mCalendar.set(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
        buttonTime.setText(timeFormat.format(mCalendar.getTime()));
        routeBetweenPoints(originLatLng, destinationLatLng);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu "+menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.predictive_qoe_menu, menu);

        // We should be able to use <group android:checkableBehavior="single">
        // to automatically allow only a single item to be checked at a time,
        // but that is broken for submenus, so we need to track the previously
        // checked items for our group, and manually uncheck it each time.
        modeGroupPrevItem = menu.findItem(R.id.action_pqoe_mode_route);
        modeGroupPrevItem.setChecked(true);
        mapTypeGroupPrevItem = menu.findItem(R.id.action_pqoe_map_type_silver);
        mapTypeGroupPrevItem.setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public void onModeGroupItemClick(MenuItem item) {
        modeGroupPrevItem.setChecked(false);
        modeGroupPrevItem = item;
        if (item.getItemId() == R.id.action_pqoe_mode_route) {
            mPointMode = pointMode.ROUTE;
            item.setChecked(true);
            routeBetweenPoints(originLatLng, destinationLatLng);
        } else if (item.getItemId() == R.id.action_pqoe_mode_grid) {
            mPointMode = pointMode.GRID;
            item.setChecked(true);
            collectGridData();
        }
    }

    public void onTypeGroupItemClick(MenuItem item) {
        mapTypeGroupPrevItem.setChecked(false);
        mapTypeGroupPrevItem = item;
        if (item.getItemId() == R.id.action_pqoe_map_type_normal) {
            item.setChecked(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            setCustomMapStyle(mMap, R.raw.map_style_default);
        } else if(item.getItemId() == R.id.action_pqoe_map_type_silver) {
            item.setChecked(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            setCustomMapStyle(mMap, R.raw.map_style_silver);
        } else if(item.getItemId() == R.id.action_pqoe_map_type_satellite) {
            item.setChecked(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if(item.getItemId() == R.id.action_pqoe_map_type_hybrid) {
            item.setChecked(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if(item.getItemId() == R.id.action_pqoe_map_type_terrain) {
            item.setChecked(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
    }

    public void onTestDataItemClick(MenuItem item) {
        loadTestData();
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnCameraIdleListener(this);

        setCustomMapStyle(mMap, R.raw.map_style_silver);

        originLatLng = new LatLng(48.2,11.57);
        destinationLatLng = new LatLng(48.1,11.4);

        routeBetweenPoints(originLatLng, destinationLatLng);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    /**
     * Based on the visible map, builds a list of points on a grid for collecting QOS numbers.
     */
    private void collectGridData() {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Log.i(TAG, "bounds=" + bounds);
        double maxLng = bounds.northeast.longitude;
        double maxLat = bounds.northeast.latitude;
        double minLng = bounds.southwest.longitude;
        double minLat = bounds.southwest.latitude;
        mMap.clear();
        LocOuterClass.Timestamp timeStamp = getTimestamp();

        ArrayList<AppClient.QosPosition> positions = new ArrayList<>();
        requestNum = 0;
        int positionId = 0;
        gridSize = (maxLng - minLng) / 20;
        Log.i(TAG, "gridSize="+gridSize);
        for(double lat = minLat; lat <= maxLat; lat+=gridSize) {
            for(double lng = minLng; lng <= maxLng; lng+=gridSize) {
                LatLng latLng = new LatLng(lat, lng);

                LocOuterClass.Loc loc = LocOuterClass.Loc.newBuilder()
                        .setLatitude((float) latLng.latitude)
                        .setLongitude((float) latLng.longitude)
                        .setTimestamp(timeStamp).build();
                AppClient.QosPosition np = AppClient.QosPosition.newBuilder()
                        .setPositionid(positionId)
                        .setGpsLocation(loc)
                        .build();
                positions.add(np);

                positionId++;
            }
        }
        getQoeData(positions, 0, requestNum);

        requestNum++;
    }

    private void loadTestData() {
        Log.i(TAG, "loadTestData()");
        originLatLng = null;
        destinationLatLng = null;
        mMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        String testDataJson;
        try {
            StringBuilder sb = new StringBuilder();
            InputStream is = getAssets().open("positions.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            testDataJson = sb.toString();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(testDataJson);
            JSONArray jsonArray = jsonObject.getJSONArray("requests");
            List<LatLng> path = new ArrayList();
            ArrayList<AppClient.QosPosition> positions = new ArrayList<>();

            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject position = jsonArray.getJSONObject(i);
                double lat = position.getDouble("latitude");
                double lng = position.getDouble("longitude");
                double seconds = position.getDouble("timestamp");
                int positionId = position.getInt("positionid");
                distributed_match_engine.LocOuterClass.Timestamp timeStamp = LocOuterClass.Timestamp.newBuilder()
                        .setSeconds((long) seconds)
                        .build();
                LatLng latLng = new LatLng(lat, lng);
                path.add(latLng);
                boundsBuilder.include(latLng);

                LocOuterClass.Loc loc = LocOuterClass.Loc.newBuilder()
                        .setLatitude((float) latLng.latitude)
                        .setLongitude((float) latLng.longitude)
                        .setTimestamp(timeStamp).build();
                AppClient.QosPosition np = AppClient.QosPosition.newBuilder()
                        .setPositionid(positionId)
                        .setGpsLocation(loc)
                        .build();
                positions.add(np);

                if(originLatLng == null) {
                    originLatLng = latLng;
                }
                destinationLatLng = latLng;
            }
            int routeNum = 0;
            getQoeData(positions, routeNum, requestNum);

            //Draw the polyline
            if (path.size() > 0) {
                PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
                mMap.addPolyline(opts);
            }

            startMarker = mMap.addMarker(new MarkerOptions().position(originLatLng).title("Start of route"));
            startMarker.setDraggable(true);
            endMarker = mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("End of route"));
            endMarker.setDraggable(true);

            LatLngBounds bounds = boundsBuilder.build();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (height * 0.10); // offset from edges of the map 10% of screen
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.animateCamera(cu);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Based on a starting and ending point, get route from the Google Directions API, and use
     * the returned data to build paths the draw on the map, and build a list of points to
     * collect QOS data for.
     *
     * @param startLatLng  The route's starting point.
     * @param endLatLng  The route's ending point.
     */
    private void routeBetweenPoints(LatLng startLatLng, LatLng endLatLng) {
        originLatLng = startLatLng;
        destinationLatLng = endLatLng;
        mMap.clear();
        startMarker = mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start of route"));
        startMarker.setDraggable(true);
        endMarker = mMap.addMarker(new MarkerOptions().position(endLatLng).title("End of route"));
        endMarker.setDraggable(true);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        LocOuterClass.Timestamp timeStamp = getTimestamp();

        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();

        try {
            DirectionsResult calculatedRoutes = DirectionsApi.newRequest(context)
                    .alternatives(true)
                    .mode(TravelMode.DRIVING)
                    .origin(new com.google.maps.model.LatLng(endLatLng.latitude, endLatLng.longitude))
                    .destination(new com.google.maps.model.LatLng(startLatLng.latitude, startLatLng.longitude))
                    .await();

            if(calculatedRoutes == null || calculatedRoutes.routes.length == 0) {
                Log.w(TAG, "calculatedRoutes has no content");
                return;
            }
            Log.d(TAG, "calculatedRoutes="+calculatedRoutes.routes.length);

            //Loop through legs and steps to get encoded polylines of each step
            int routeNum = 0;
            for(DirectionsRoute route: calculatedRoutes.routes) {
                int positionId = 0;
                List<LatLng> path = new ArrayList();
                ArrayList<AppClient.QosPosition> positions = new ArrayList<>();

                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++) {
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        positionId = addPositionsFromPath(points1, positions, path, positionId, boundsBuilder, timeStamp);
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    positionId = addPositionsFromPath(points, positions, path, positionId, boundsBuilder, timeStamp);
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "path.size()="+path.size());

                getQoeData(positions, routeNum, requestNum);

                //Draw the polyline
                if (path.size() > 0) {
                    PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
                    mMap.addPolyline(opts);
                }
                routeNum++;
            }

        } catch(Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage());
        }
        requestNum++;

        LatLngBounds bounds = boundsBuilder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (height * 0.10); // offset from edges of the map 10% of screen
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
        mMap.animateCamera(cu);

    }

    /**
     * Calculate a timestamp to be used in the QOS request. Default is 1 minute from now, but if
     * the date or time have been modified by the user, that will be used instead.
     *
     * @return  The timestamp.
     */
    private LocOuterClass.Timestamp getTimestamp() {
        long seconds = System.currentTimeMillis()/1000+60; // Request data for 1 minute from now.
        //If we have picked a date or time in the future, use that instead.
        if(mCalendar.getTimeInMillis()/1000 > seconds) {
            seconds = mCalendar.getTimeInMillis()/1000;
            Log.i(TAG, "Using future timestamp: "+(mCalendar.getTimeInMillis()/1000)+" "+mCalendar);
        } else {
            Log.i(TAG, "Using current timestamp: "+seconds);
        }
        return LocOuterClass.Timestamp.newBuilder()
                .setSeconds(seconds)
                .build();
    }

    /**
     * From an EncodedPolyline of points on a route, iterate through the points and create a
     * QosPosition from each and add it to the given ArrayList, and create a LatLng and add it to
     * the given "path" List.
     *
     * @param points  The EncodedPolyline of points along the route.
     * @param positions  The ArrayList of QosPositions to all the points to.
     * @param path  List of latitude and longitude values used to draw the route on the map.
     * @param positionId  Position ID which will be incremented with each point added.
     * @param boundsBuilder  Used to determine pan and zoom on the map to display all included data.
     * @param timeStamp  Timestamp to be added to each QosPosition.
     * @return  The final position ID.
     */
    private int addPositionsFromPath(EncodedPolyline points,
                                     ArrayList<AppClient.QosPosition> positions, List<LatLng> path,
                                     int positionId, LatLngBounds.Builder boundsBuilder,
                                     LocOuterClass.Timestamp timeStamp) {
        if (points != null) {
            //Decode polyline and add points to list of route coordinates
            List<com.google.maps.model.LatLng> coords = points.decodePath();
            for (com.google.maps.model.LatLng coord : coords) {
                LatLng latLng = new LatLng(coord.lat, coord.lng);
                path.add(latLng);
                boundsBuilder.include(latLng);

                LocOuterClass.Loc loc = LocOuterClass.Loc.newBuilder()
                        .setLatitude((float) latLng.latitude)
                        .setLongitude((float) latLng.longitude)
                        .setTimestamp(timeStamp).build();
                AppClient.QosPosition np = AppClient.QosPosition.newBuilder()
                        .setPositionid(positionId)
                        .setGpsLocation(loc)
                        .build();
                positions.add(np);

                positionId++;
            }
        }
        return positionId;
    }

    /**
     * Instantiates a QoeDataTask to make a background call to getQosPositionKpi.
     * @param positions  ArrayList of QosPositions.
     * @param routeNum  The route number, used to differentiate the first route returned by the
     *                  Directions API.
     * @param requestNum  The request number, which is incremented with each QOS request made.
     */
    private void getQoeData(ArrayList<AppClient.QosPosition> positions, int routeNum, int requestNum) {
        Log.i(TAG, "getQoeData() request size: "+positions.size());
        new QoeDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, positions, routeNum, requestNum);
    }

    /**
     * Makes a background call to getQosPositionKpi with the positions array as the parameter, waits
     * for results, and then draws the received data.
     */
    private class QoeDataTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            ArrayList<AppClient.QosPosition> positions = (ArrayList<AppClient.QosPosition>) params[0];
            final int routeNum = (int) params[1];
            int reqNum = (int) params[2];
            Log.i(TAG, positions.size()+" positions routeNum="+routeNum+" reqNum="+reqNum);
            MatchingEngine me = mMatchingEngineHelper.getMatchingEngine();
            Log.i(TAG, "me="+me+" host="+me.getHost()+" port="+me.getPort());
            final List<ColoredPoint> points = new ArrayList<>();

            try {
                AppClient.QosPositionRequest request = me.createQoSPositionRequest(positions);
                ChannelIterator<AppClient.QosPositionKpiReply> responseIterator = me.getQosPositionKpi(request,
                        mHostname, me.getPort(), 15000);
                // A stream of QosPositionKpiReply(s), with a non-stream block of responses.
                long total = 0;
                points.clear();

                //TODO: This would normally be a while(hasNext()), but DT's current implementation
                // keeps the stream open and sends a repeat of the same data after 10 seconds.
                // Using "if" instead avoids waiting for the duplicate data and redrawing unnecessarily.
                if (responseIterator.hasNext()) {
                    AppClient.QosPositionKpiReply aR = responseIterator.next();
                    String csv = "";
                    for (int i = 0; i < aR.getPositionResultsCount(); i++) {
                        AppClient.QosPositionKpiResult result = aR.getPositionResults(i);
                        Log.i(TAG, i+" dluserthroughput_avg="+result.getDluserthroughputAvg()+" uluserthroughput_avg="+result.getUluserthroughputAvg());
                        LatLng coords = new LatLng(result.getGpsLocation().getLatitude(), result.getGpsLocation().getLongitude());
                        points.add(new ColoredPoint(coords, result.getDluserthroughputAvg(), result.getUluserthroughputAvg()));
                        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                        cal.setTimeInMillis(result.getGpsLocation().getTimestamp().getSeconds() * 1000L);
                        csv += DateFormat.format("HH:mm:ss,MM/dd/yyyy", cal).toString()
                                + "," + result.getGpsLocation().getLatitude()
                                + "," + result.getGpsLocation().getLongitude() + "\n";
                    }
                    total += aR.getPositionResultsCount();
                    System.out.println("==========================");
                    System.out.println(csv);
                    System.out.println("==========================");

                    //We've got a list of points. Draw the results.
                    runOnUiThread(new Runnable(){
                        public void run(){
                            if(mPointMode == pointMode.ROUTE) {
                                drawColoredPolyline(points, routeNum);
                            } else if (mPointMode == pointMode.GRID) {
                                drawColoredGrid(points);
                            }
                            points.clear();
                        }
                    });
                }
                //Shut down the channel.
                responseIterator.shutdown();

            } catch (InterruptedException | ExecutionException | RuntimeException e ) {
                e.printStackTrace();
                toastOnUiThread(e.getMessage(), Toast.LENGTH_LONG);
            }
            return null;
        }
    }

    @Override
    public void onCameraIdle() {
        Log.i(TAG, "onCameraIdle()");
        if(mPointMode == pointMode.GRID) {
            collectGridData();
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.i(TAG, "onMarkerDragStart()");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        //do nothing
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.i(TAG, "onMarkerDragEnd()");
        routeBetweenPoints(startMarker.getPosition(), endMarker.getPosition());
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.i(TAG, "onMapLongClick()");
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop()");
        unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    public void toastOnUiThread(final String message, final int length) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(QoeMapActivity.this, message, length).show();
            }
        });
    }

    /**
     * Draw a grid of circles where the color is based on the received QOS data for that point.
     *
     * @param points  The list of ColoredPoints on the grid.
     */
    private void drawColoredGrid(List<ColoredPoint> points) {
        Log.i(TAG, "drawColoredGrid() size=" + points.size());

        if (points.size() < 2)
            return;

        int i = 0;
        ColoredPoint currentPoint;
        while (i < points.size()) {
            currentPoint = points.get(i);
            mMap.addCircle(new CircleOptions()
                    .center(currentPoint.coords)
                    .fillColor(ColorUtils.setAlphaComponent(currentPoint.color, GRID_POINT_ALPHA))
                    .strokeColor(ColorUtils.setAlphaComponent(currentPoint.color, GRID_POINT_ALPHA))
                    .radius(gridSize*10000));
            i++;
        }
    }

    /**
     * Draws a route on the map, where the color of each point is based on received QOS data
     * for that point.
     *
     * @param points  The list of ColoredPoints on the route.
     * @param routeNum  The route number, used to differentiate the primary route by drawing
     *                  it wider.
     */
    private void drawColoredPolyline(List<ColoredPoint> points, int routeNum) {
        Log.i(TAG, "drawColoredPolyline() size="+points.size());

        if (points.size() < 2) {
            return;
        }

        routeWidth = 20;
        if(routeNum == 0) {
            routeWidth = 30;
        }

        int i = 0;
        ColoredPoint currentPoint  = points.get(i);
        int currentColor = currentPoint.color;
        List<LatLng> currentSegment = new ArrayList<>();
        currentSegment.add(currentPoint.coords);
        i++;

        while (i < points.size()) {
            currentPoint = points.get(i);

            if (currentPoint.color == currentColor) {
                currentSegment.add(currentPoint.coords);
            } else {
                currentSegment.add(currentPoint.coords);
                mMap.addPolyline(new PolylineOptions()
                        .addAll(currentSegment)
                        .color(currentColor)
                        .width(routeWidth));
                currentColor = currentPoint.color;
                currentSegment.clear();
                currentSegment.add(currentPoint.coords);
            }

            i++;
        }

        mMap.addPolyline(new PolylineOptions()
                .addAll(currentSegment)
                .color(currentColor)
                .width(routeWidth));
    }

    /**
     * Class for calculating a score and assigning a color index based on KPI values.
     */
    private class ColoredPoint {
        public LatLng coords;
        public int color;

        public ColoredPoint(LatLng coords, float dlSpeed, float upSpeed) {
            this.coords = coords;
            //The calculated score will be used as the index into the COLORS array.
            int score = DL_THRESHOLDS.length -1;
            int dlScore = 0;
            int upScore = 0;

            for(int i = 0; i < DL_THRESHOLDS.length-1; i++) {
                if(dlSpeed > DL_THRESHOLDS[i] && dlSpeed <= DL_THRESHOLDS[i+1]) {
                    dlScore = i+1;
                    break;
                }
            }
            for(int i = 0; i < UL_THRESHOLDS.length-1; i++) {
                if(upSpeed > UL_THRESHOLDS[i] && upSpeed <= UL_THRESHOLDS[i+1]) {
                    upScore = i+1;
                    break;
                }
            }

            if(dlScore < score) {
                score = dlScore;
            }

            if(upScore < score) {
                score = upScore;
            }
            Log.i(TAG, "getDluserthroughputAvg "+dlSpeed+" getUluserthroughputAvg="+upSpeed+" score="+score);

            color = Color.parseColor(COLORS[score]);
        }
    }
}
