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
import com.mobiledgex.sdkdemo.R;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import dt.qos.predictive.PositionKpiRequest;
import dt.qos.predictive.QoSKPIRequest;

import static com.mobiledgex.sdkdemo.qoe.PredictiveQosClient.SERVER_PORT;
import static com.mobiledgex.sdkdemo.qoe.PredictiveQosClient.SERVER_URI;

public class QoeMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener,
        GoogleMap.OnCameraIdleListener, DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    private static final String TAG = "QoeMapActivity";
    private GoogleMap mMap;
    public boolean modeRoute = true;
    private PredictiveQosClient mPredictiveQosClient = null;
    private Marker startMarker;
    private Marker endMarker;
    private String apiKey;
    private LatLng startLatLng;
    private LatLng endLatLng;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qoe_map);
        Toolbar toolbar = findViewById(R.id.toolbar_qoe);

        // Build Legend Table View programmatically, based on colors and speed description
        // values from the PredictiveQosClient.
        TableLayout tl = findViewById(R.id.table_layout_legend);
        tl.removeAllViews();
        for(int i = 0; i < PredictiveQosClient.COLORS.length; i++) {
            String color = PredictiveQosClient.COLORS[i];
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new TableRow.LayoutParams());
            TextView colorBlockTv = new TextView(this);
            colorBlockTv.setText("      "); //This many spaces is nearly square with default font.
            colorBlockTv.setBackgroundColor(Color.parseColor(color));
            TextView descriptionTv = new TextView(this);
            descriptionTv.setText(" "+PredictiveQosClient.SPEEDS[i]);
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

        try {
            mPredictiveQosClient = new PredictiveQosClient(this, SERVER_URI, SERVER_PORT);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException
                | CertificateException | KeyStoreException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
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
        routeBetweenPoints(startLatLng, endLatLng);
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
        routeBetweenPoints(startLatLng, endLatLng);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu "+menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.predictive_qoe_menu, menu);

        menu.findItem(R.id.action_pqoe_map_mode).setVisible(false); //TODO: Remove

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
            modeRoute = true;
            item.setChecked(true);
            routeBetweenPoints(startLatLng, endLatLng);
        } else if (item.getItemId() == R.id.action_pqoe_mode_grid) {
            modeRoute = false;
            item.setChecked(true);
            collectGridData();
        } else {
            // Do nothing
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

        startLatLng = new LatLng(48.1,11.39);
        endLatLng = new LatLng(48.2,11.57);

        routeBetweenPoints(startLatLng, endLatLng);

        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void collectGridData() {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Log.i(TAG, "bounds=" + bounds);
        double maxLng = bounds.northeast.longitude;
        double maxLat = bounds.northeast.latitude;
        double minLng = bounds.southwest.longitude;
        double minLat = bounds.southwest.latitude;
        mMap.clear();
        final long timeStamp = System.currentTimeMillis()/1000+10;

        mPredictiveQosClient.setRequestNum(requestNum);
        QoSKPIRequest.Builder requestBuilder = QoSKPIRequest.newBuilder();
        int positionId = 0;
        double gridSize = (maxLng - minLng) / 20;
        for(double lat = minLat; lat <= maxLat; lat+=gridSize) {
            for(double lng = minLng; lng <= maxLng; lng+=gridSize) {
                PositionKpiRequest point = PositionKpiRequest.newBuilder().setPositionid(positionId).setLatitude((float) lat).setLongitude((float) lng).setTimestamp(timeStamp+positionId).build();
                requestBuilder.addRequests(point);
                positionId++;
            }
        }
        QoSKPIRequest request = requestBuilder.build();
        getQoeData(request, mMap, 0, requestNum);
        requestNum++;
    }

    private void routeBetweenPoints(LatLng startLatLng, LatLng endLatLng) {
        this.startLatLng = startLatLng;
        this.endLatLng = endLatLng;
        mMap.clear();
        startMarker = mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start of route"));
        startMarker.setDraggable(true);
        endMarker = mMap.addMarker(new MarkerOptions().position(endLatLng).title("End of route"));
        endMarker.setDraggable(true);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        long timeStamp = System.currentTimeMillis()/1000+60; // Request data for 1 minute from now.
        //If we have picked a date or time in the future, use that instead.
        if(mCalendar.getTimeInMillis()/1000 > timeStamp) {
            timeStamp = mCalendar.getTimeInMillis()/1000;
            Log.i(TAG, "Using future timestamp: "+(mCalendar.getTimeInMillis()/1000)+" "+mCalendar);
        } else {
            Log.i(TAG, "Using current timestamp: "+timeStamp);
        }

        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();

        mPredictiveQosClient.setRequestNum(requestNum);
        try {
            DirectionsResult calculatedRoutes = DirectionsApi.newRequest(context)
                    .alternatives(true)
                    .mode(TravelMode.DRIVING)
                    .origin(new com.google.maps.model.LatLng(endLatLng.latitude, endLatLng.longitude))
                    .destination(new com.google.maps.model.LatLng(startLatLng.latitude, startLatLng.longitude))
                    .await();

            Log.d(TAG, "calculatedRoutes="+calculatedRoutes.routes.length);

            //Loop through legs and steps to get encoded polylines of each step
            if (calculatedRoutes.routes != null && calculatedRoutes.routes.length > 0) {
                Log.i(TAG, "calculatedRoutes.routes.length="+calculatedRoutes.routes.length);
                int routeNum = 0;
                for(DirectionsRoute route: calculatedRoutes.routes) {
                    QoSKPIRequest.Builder requestBuilder = QoSKPIRequest.newBuilder();
                    int positionId = 0;
                    requestBuilder.setRequestid(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
                    List<LatLng> path = new ArrayList();

                    if (route.legs !=null) {
                        for(int i=0; i<route.legs.length; i++) {
                            DirectionsLeg leg = route.legs[i];
                            if (leg.steps != null) {
                                for (int j=0; j<leg.steps.length;j++){
                                    DirectionsStep step = leg.steps[j];
                                    if (step.steps != null && step.steps.length >0) {
                                        for (int k=0; k<step.steps.length;k++){
                                            DirectionsStep step1 = step.steps[k];
                                            EncodedPolyline points1 = step1.polyline;
                                            if (points1 != null) {
                                                //Decode polyline and add points to list of route coordinates
                                                List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                                for (com.google.maps.model.LatLng coord1 : coords1) {
                                                    LatLng latLng = new LatLng(coord1.lat, coord1.lng);
                                                    path.add(latLng);
                                                    boundsBuilder.include(latLng);
                                                    PositionKpiRequest point = PositionKpiRequest.newBuilder().setPositionid(positionId).setLatitude((float) coord1.lat).setLongitude((float) coord1.lng).setTimestamp(timeStamp).build();
                                                    requestBuilder.addRequests(point);
                                                    positionId++;
                                                }
                                            }
                                        }
                                    } else {
                                        EncodedPolyline points = step.polyline;
                                        if (points != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords = points.decodePath();
                                            for (com.google.maps.model.LatLng coord : coords) {
                                                LatLng latLng = new LatLng(coord.lat, coord.lng);
                                                path.add(latLng);
                                                boundsBuilder.include(latLng);
                                                PositionKpiRequest point = PositionKpiRequest.newBuilder().setPositionid(positionId).setLatitude((float) coord.lat).setLongitude((float) coord.lng).setTimestamp(timeStamp).build();
                                                requestBuilder.addRequests(point);
                                                positionId++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Log.i(TAG, "path.size()="+path.size());
                    QoSKPIRequest request = requestBuilder.build();
                    getQoeData(request, mMap, routeNum, requestNum);

                    //Draw the polyline
                    if (path.size() > 0) {
                        PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
                        mMap.addPolyline(opts);
                    }
                    routeNum++;
                }
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

    private void getQoeData(QoSKPIRequest request, GoogleMap map, int routeNum, int requestNum) {
        Log.i(TAG, "getQoeData() request size: "+request.getRequestsList().size());
        new QoeDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request, map, routeNum, requestNum, modeRoute);
    }

    private class QoeDataTask extends AsyncTask<Object, Void, String> {

        @Override
        protected String doInBackground(Object... params) {
            QoSKPIRequest request = (QoSKPIRequest) params[0];
            GoogleMap map = (GoogleMap) params[1];
            int routeNum = (int) params[2];
            int reqNum = (int) params[3];
            boolean modeRoute = (boolean) params[4];
            mPredictiveQosClient.checkHealth();
            mPredictiveQosClient.requestQos(request, map, routeNum, reqNum, modeRoute);
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "QoeDataTask done");
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    @Override
    public void onCameraIdle() {
        Log.i(TAG, "onCameraIdle()");
        if(!modeRoute) {
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
        try {
            mPredictiveQosClient.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

}
