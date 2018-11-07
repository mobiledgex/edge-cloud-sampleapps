package com.mobiledgex.sdkdemo.qoe;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
import com.mobiledgex.matchingengine.MexKeyStoreException;
import com.mobiledgex.matchingengine.MexTrustStoreException;
import com.mobiledgex.sdkdemo.R;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathBuilder;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dt.qos.predictive.PositionKpiRequest;
import dt.qos.predictive.QoSKPIRequest;

import static com.mobiledgex.sdkdemo.qoe.PredictiveQosClient.SERVER_PORT;
import static com.mobiledgex.sdkdemo.qoe.PredictiveQosClient.SERVER_URI;

public class QoeMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,GoogleMap.OnMarkerDragListener {

    private static final String TAG = "QoeMapActivity";
    private GoogleMap mMap;
    private PredictiveQosClient mPredictiveQosClient = null;
    private Marker startMarker;
    private Marker endMarker;
    private String apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qoe_map);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        apiKey = getResources().getString(R.string.google_directions_api_key);

        try {
            mPredictiveQosClient = new PredictiveQosClient(this, SERVER_URI, SERVER_PORT);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | MexTrustStoreException
                | InvalidKeySpecException | KeyStoreException | MexKeyStoreException e) {
            e.printStackTrace();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);

        LatLng startLatLng = new LatLng(48.1,11.39);
        LatLng endLatLng = new LatLng(48.2,11.57);
//        LatLng startLatLng = new LatLng(48.100948333740234,11.39490032196045);
//        LatLng endLatLng = new LatLng(48.101009368896484,11.394619941711426);

        routeBetweenPoints(startLatLng, endLatLng);

        mMap.getUiSettings().setZoomControlsEnabled(true);

    }

    private void routeBetweenPoints(LatLng startLatLng, LatLng endLatLng) {
        mMap.clear();
        startMarker = mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start of route"));
        startMarker.setDraggable(true);
        endMarker = mMap.addMarker(new MarkerOptions().position(endLatLng).title("End of route"));
        endMarker.setDraggable(true);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        final long timeStamp = System.currentTimeMillis()/1000+10;

        //Define list to get all latlng for the route
//        List<LatLng> path = new ArrayList();

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

            Log.d(TAG, "calculatedRoutes="+calculatedRoutes.routes.length);



            int positionId = 0;
            //Loop through legs and steps to get encoded polylines of each step
            if (calculatedRoutes.routes != null && calculatedRoutes.routes.length > 0) {
                Log.i(TAG, "calculatedRoutes.routes.length="+calculatedRoutes.routes.length);
                for(DirectionsRoute route: calculatedRoutes.routes) {
                    QoSKPIRequest.Builder requestBuilder = QoSKPIRequest.newBuilder();
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
//                                                    path.add(latLng);
                                                    boundsBuilder.include(latLng);
                                                    PositionKpiRequest point = PositionKpiRequest.newBuilder().setPositionid(positionId).setLatitude((float) coord1.lat).setLongitude((float) coord1.lng).setTimestamp(timeStamp+positionId).build();
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
//                                                path.add(latLng);
                                                boundsBuilder.include(latLng);
                                                PositionKpiRequest point = PositionKpiRequest.newBuilder().setPositionid(positionId).setLatitude((float) coord.lat).setLongitude((float) coord.lng).setTimestamp(timeStamp+positionId).build();
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
                    getQoeData(request, mMap);

//                    //Draw the polyline
//                    if (path.size() > 0) {
//                        PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(8);
//                        mMap.addPolyline(opts);
//                    }
                }
            }

        } catch(Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage());
        }

        LatLngBounds bounds = boundsBuilder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (height * 0.12); // offset from edges of the map 12% of screen
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
        mMap.animateCamera(cu);


    }

    private void getQoeData(QoSKPIRequest request, GoogleMap map) {
        Log.i(TAG, "getQoeData() request size: "+request.getRequestsList().size());
        new QoeDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request, map);
    }

    private class QoeDataTask extends AsyncTask<Object, Void, String> {

        @Override
        protected String doInBackground(Object... params) {
            QoSKPIRequest request = (QoSKPIRequest) params[0];
            GoogleMap map = (GoogleMap) params[1];
            mPredictiveQosClient.checkHealth();
            mPredictiveQosClient.requestQos(request, map);
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
    public void onMarkerDragStart(Marker marker) {
        Log.i(TAG, "onMarkerDragStart()");

    }

    @Override
    public void onMarkerDrag(Marker marker) {
//        Log.i(TAG, "onMarkerDrag()");

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
        try {
            mPredictiveQosClient.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }


}
