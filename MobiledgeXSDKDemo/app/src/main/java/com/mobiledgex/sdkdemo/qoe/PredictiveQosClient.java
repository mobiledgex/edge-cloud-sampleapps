package com.mobiledgex.sdkdemo.qoe;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLng;
import com.mobiledgex.matchingengine.MatchingEngine;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dt.qos.predictive.HealthCheckRequest;
import dt.qos.predictive.HealthCheckResponse;
import dt.qos.predictive.HealthGrpc;
import dt.qos.predictive.PositionKpiRequest;
import dt.qos.predictive.PositionKpiResult;
import dt.qos.predictive.QoSKPIRequest;
import dt.qos.predictive.QoSKPIResponse;
import dt.qos.predictive.QueryQoSGrpc;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

public class PredictiveQosClient {
    private static final String TAG = "PredictiveQosClient";

    private static final String CERT_SERVER_BKS = "certs_pqoe/server.bks";
    private static final String CERT_CLIENT_P12 = "certs_pqoe/client.p12";

    // set the public address/port for dt.qos.predictive.api project
    public static final String SERVER_URI = "qos-predictive.all-ip.t-online.de";
    public static final int SERVER_PORT = 8001;
    public static final String[] COLORS = {"#000000", "#ff0000", "#ff8900", "#ffff00", "#bbff99", "#339933", "#006600"};
    public static final String[] SPEEDS = {"no data", "bad", "slow", "ok", "good", "fast", "very fast"};

    private final ManagedChannel channel;
    private final Context mContext;
    private MatchingEngine mMatchingEngine;
    private GoogleMap mGoogleMap;

    private int routeWidth = 20;
    private int requestNum;

    public PredictiveQosClient(Context context, String host, int port) throws IOException,
            KeyManagementException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        Log.i(TAG, "onMarkerDragEnd()");

        mContext = context;
        mMatchingEngine = new MatchingEngine(context);
        String serverBksFileName = CERT_SERVER_BKS;
        String clientKeyPairFileName = CERT_CLIENT_P12;
        char[] serverBksPassword = "".toCharArray();
        char[] clientKeyPairPassword = "".toCharArray();

        // the channel represents the communication to the server, here we give
        // the connection parameters, and enable TLS with the included certs/keys.
        channel = OkHttpChannelBuilder
                .forAddress(host, port)
                .sslSocketFactory(mMatchingEngine.getMutualAuthSSLSocketFactoryInstance(serverBksFileName,
                        clientKeyPairFileName, serverBksPassword, clientKeyPairPassword))
                .build();
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    // sends a blocking healthCheck request and prints the results
    public void checkHealth() {
        Log.i(TAG, "Checking server health state...");

        // The gPRC client implementation provides 2 methods to send requests
        // to a service on the server. One is used for sending synchronous (blocking)
        // requests, suitable for simple (and quick) request-response
        // transactions, and the other is the async (non-blocking) method.
        //
        // We use the blocking method here, for which we create a client stub
        // which can be used to send the request
        final HealthGrpc.HealthBlockingStub healthBlockingStub;
        healthBlockingStub = HealthGrpc.newBlockingStub(channel);

        try {
            // building the request object
            HealthCheckRequest request = HealthCheckRequest.newBuilder().setService("QueryQoS").build();
            //actually sending out the request, this will block until the
            //response arrives, and gives back response object, which we print out
            HealthCheckResponse response = healthBlockingStub.check(request);
            Log.i(TAG, "HealthCheck response status: " + response.getStatus());
            Log.i(TAG, "HealthCheck response model version: " + response.getModelversion());
        } catch (RuntimeException e) {
            Log.e(TAG, "HealthCheck RPC failed : " + e);
        }
    }

    //  sending out a single request with 2 points, waiting for the response
    //  stream to complete
    public void requestQos() {
        // we will use this for waiting for the async client to finish,
        // in production code this wouldn't be needed, as probably there will
        // be some sort of event loop and the application will run continuously.
        // For this demo we want just wait for the response stream to complete
        // and quit.
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final long now = System.currentTimeMillis();

        // creating ten points and filling with coordinates/timestamp
        // Normally these would be collected in a container, now for simplicity
        // we just create 10 discrete objects
        PositionKpiRequest point1 = PositionKpiRequest.newBuilder()
                .setPositionid(1).setLatitude(48.2117f).setLongitude(11.639f).setTimestamp(now / 1000 + 3).setAltitude(10)
                .build();
        PositionKpiRequest point2 = PositionKpiRequest.newBuilder()
                .setPositionid(2).setLatitude(48.2110f).setLongitude(11.649f).setTimestamp(now / 1000 + 10).setAltitude(-20)
                .build();
        PositionKpiRequest point3 = PositionKpiRequest.newBuilder()
                .setPositionid(3).setLatitude(48.3110f).setLongitude(11.630f).setTimestamp(now / 1000 + 11).setAltitude(2)
                .build();
        PositionKpiRequest point4 = PositionKpiRequest.newBuilder()
                .setPositionid(4).setLatitude(48.2217f).setLongitude(11.659f).setTimestamp(now / 1000 + 12).setAltitude(1)
                .build();
        PositionKpiRequest point5 = PositionKpiRequest.newBuilder()
                .setPositionid(5).setLatitude(48.2210f).setLongitude(11.669f).setTimestamp(now / 1000 + 13).setAltitude(-2)
                .build();
        PositionKpiRequest point6 = PositionKpiRequest.newBuilder()
                .setPositionid(6).setLatitude(48.3210f).setLongitude(11.633f).setTimestamp(now / 1000 + 24).setAltitude(0)
                .build();
        PositionKpiRequest point7 = PositionKpiRequest.newBuilder()
                .setPositionid(7).setLatitude(48.3210f).setLongitude(11.633f).setTimestamp(now / 1000 + 25).setAltitude(0)
                .build();
        PositionKpiRequest point8 = PositionKpiRequest.newBuilder()
                .setPositionid(8).setLatitude(48.2310f).setLongitude(11.689f).setTimestamp(now / 1000 + 26).setAltitude(-3)
                .build();
        PositionKpiRequest point9 = PositionKpiRequest.newBuilder()
                .setPositionid(9).setLatitude(48.3310f).setLongitude(11.690f).setTimestamp(now / 1000 + 27).setAltitude(30)
                .build();
        PositionKpiRequest point10 = PositionKpiRequest.newBuilder()
                .setPositionid(10).setLatitude(48.2417f).setLongitude(11.619f).setTimestamp(now / 1000 + 30).setAltitude(1)
                .build();

        // we create a request object, set the req ID, and add the previously
        // built to points to it
        QoSKPIRequest request = QoSKPIRequest.newBuilder().
                setRequestid(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()))
                .addRequests(point1).addRequests(point2).addRequests(point3)
                .addRequests(point4).addRequests(point5).addRequests(point6)
                .addRequests(point7).addRequests(point8).addRequests(point9).addRequests(point10)
                .build();

        // this object is observing the gPRC stream, and processing (here:
        // printing) the received values
        // When multiple request are sent (here we send only one), then for
        // each ongoing request there would be a StreamObserver instance to
        // keep track of the renewals and the expiration of the request
        StreamObserver<QoSKPIResponse> responseObserver = new StreamObserver<QoSKPIResponse>() {
            @Override
            public void onNext(QoSKPIResponse msg) {
                QoSKPIResponse.Builder resultBuilder = QoSKPIResponse.newBuilder().setRequestid(msg.getRequestid());
                QoSKPIResponse result;
                for (PositionKpiResult res : msg.getResultsList()) {
                    PositionKpiResult positionResult = PositionKpiResult.newBuilder()
                            .setPositionid(res.getPositionid())
                            .setDluserthroughputAvg(res.getDluserthroughputAvg())
                            .setDluserthroughputMin(res.getDluserthroughputMin())
                            .setDluserthroughputMax(res.getDluserthroughputMax())
                            .setUluserthroughputAvg(res.getUluserthroughputAvg())
                            .setUluserthroughputMin(res.getUluserthroughputMin())
                            .setUluserthroughputMax(res.getUluserthroughputMax())
                            .setLatencyAvg(res.getLatencyAvg())
                            .setLatencyMin(res.getLatencyMin())
                            .setLatencyMax(res.getLatencyMax())
                            .build();
                    resultBuilder.addResults(positionResult);
                }
                result = resultBuilder.build();
                Log.i(TAG, "KPI response: \n" + result);
            }

            @Override
            public void onError(Throwable t) {
                Log.i(TAG, "Received error: " + t);
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "Request completed.");
                finishLatch.countDown();
            }
        };

        // creating a client stub, which represents the connection to the server
        // This time it's an async stub, as we have several requests running
        //
        // there is also an other stub API which operates with futures, see
        // here: https://grpc.io/docs/reference/java/generated-code.html
        QueryQoSGrpc.QueryQoSStub stub = QueryQoSGrpc.newStub(channel);

        //sending out the request and registering the observer for it
        stub.queryQoSKPI(request, responseObserver);
        Log.i(TAG, "Request sent out, waiting until the response stream is complete...");

        // we use the latch to wait for the request to get ready
        try {
            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                Log.e(TAG, "Request didn't finish within 1 minute");
            }
        } catch (InterruptedException ex) {
            Log.e(TAG, "Exception during waiting for the request to be completed: " + ex);
        }
        Log.i(TAG, "Request is complete");
    }

    public void setRequestNum(int num){
        requestNum = num;
    }

    public void requestQos(final QoSKPIRequest request, GoogleMap map, final int routeNum, final int localRequestNum, final boolean modeRoute) {
        Log.i(TAG, "requestQos() routeNum="+routeNum+" localRequestNum="+localRequestNum+" modeRoute="+modeRoute);
        mGoogleMap = map;
        final List<ColoredPoint> points = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        // this object is observing the gPRC stream, and processing (here:
        // printing) the received values
        // When multiple request are sent (here we send only one), then for
        // each ongoing request there would be a StreamObserver instance to
        // keep track of the renewals and the expiration of the request
        StreamObserver<QoSKPIResponse> responseObserver = new StreamObserver<QoSKPIResponse>() {
            @Override
            public void onNext(QoSKPIResponse msg) {
                Log.i(TAG, "Stale? localRequestNum="+localRequestNum+" requestNum="+requestNum);
                if(localRequestNum != requestNum) {
                    Log.w(TAG, "Ignoring stale data received.");
                    return;
                }
                points.clear();
                for (PositionKpiResult res : msg.getResultsList()) {
                    PositionKpiRequest positionKpiRequest = request.getRequests((int) res.getPositionid());
                    LatLng coords = new LatLng(positionKpiRequest.getLatitude(), positionKpiRequest.getLongitude());
                    points.add(new ColoredPoint(coords, res.getDluserthroughputAvg(), res.getUluserthroughputAvg()));
//                    Log.i(TAG, "positionResult: "+res.getPositionid()+" "+res.getDluserthroughputAvg()+" "+res.getLatencyAvg()+" "+coords);
                }
                ((Activity)mContext).runOnUiThread(new Runnable(){
                    public void run(){
                        if(modeRoute) {
                            drawColoredPolyline(points, routeNum);
                        } else {
                            drawColoredPolyGrid(points);
                        }
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                Log.i(TAG, "Received error: " + Log.getStackTraceString(t)+"\n"+t.getCause());
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "Request completed.");
                finishLatch.countDown();
            }
        };

        // creating a client stub, which represents the connection to the server
        // This time it's an async stub, as we have several requests running
        //
        // there is also an other stub API which operates with futures, see
        // here: https://grpc.io/docs/reference/java/generated-code.html
        QueryQoSGrpc.QueryQoSStub stub = QueryQoSGrpc.newStub(channel);

        //sending out the request and registering the observer for it
        stub.queryQoSKPI(request, responseObserver);
        Log.i(TAG, "Request sent out, waiting until the response stream is complete...");

        // we use the latch to wait for the request to get ready
        try {
            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                Log.e(TAG, "Request didn't finish within 1 minute");
            }
        } catch (InterruptedException ex) {
            Log.e(TAG, "Exception during waiting for the request to be completed: " + ex);
        }
        Log.i(TAG, "Request is complete");

    }

    class ColoredPoint {
        public LatLng coords;
        public int color;
        public String colorString;

        public ColoredPoint(LatLng coords, float dlSpeed, float upSpeed) {
            this.coords = coords;
            if(dlSpeed > 60) {
                colorString = COLORS[6];
                color = Color.parseColor(COLORS[6]);
            } else if(dlSpeed <= 60 && dlSpeed > 40 ) {
                colorString = COLORS[5];
                color = Color.parseColor(COLORS[5]);
            } else if(dlSpeed <= 40 && dlSpeed > 25 ) {
                colorString = COLORS[4];
                color = Color.parseColor(COLORS[4]);
            } else if(dlSpeed <= 25 && dlSpeed > 10 ) {
                colorString = COLORS[3];
                color = Color.parseColor(COLORS[3]);
            } else if(dlSpeed <= 10 && dlSpeed > 2 ) {
                colorString = COLORS[2];
                color = Color.parseColor(COLORS[2]);
            } else if(dlSpeed <= 2 && dlSpeed > 0) {
                colorString = COLORS[1];
                color = Color.parseColor(COLORS[1]);
            } else {
                colorString = COLORS[0];
                color = Color.parseColor(COLORS[0]);
            }
        }
    }


    private void drawColoredPolyGrid(List<ColoredPoint> points) {
        Log.i(TAG, "drawColoredPolyGrid() size=" + points.size());

        if (points.size() < 2)
            return;

        int ix = 0;
        ColoredPoint currentPoint;
        while (ix < points.size()) {
            currentPoint = points.get(ix);
//            Log.i(TAG, ix+" currentPoint="+currentPoint.coords+" "+currentPoint.color);
            Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(currentPoint.coords).icon(getMarkerIcon(currentPoint.colorString)));


//            mGoogleMap.addCircle(new CircleOptions()
//                    .center(currentPoint.coords)
//                    .fillColor(currentPoint.color)
//                    .strokeColor(currentPoint.color)
//                    .radius(200));
            ix++;
        }

    }

    private void drawColoredPolyline(List<ColoredPoint> points, int routeNum) {
        Log.i(TAG, "drawColoredPolyline() size="+points.size());

        if (points.size() < 2)
            return;

        routeWidth = 20;
        if(routeNum == 0) {
            routeWidth = 30;
        }

        int ix = 0;
        ColoredPoint currentPoint  = points.get(ix);
        int currentColor = currentPoint.color;
        List<LatLng> currentSegment = new ArrayList<>();
        currentSegment.add(currentPoint.coords);
        ix++;

        while (ix < points.size()) {
            currentPoint = points.get(ix);
//            Log.i(TAG, "currentPoint="+currentPoint.coords+" "+currentPoint.color);

            if (currentPoint.color == currentColor) {
                currentSegment.add(currentPoint.coords);
            } else {
                currentSegment.add(currentPoint.coords);
//                Log.i(TAG, "1.currentSegment "+currentSegment.size()+" points. "+currentColor);
                mGoogleMap.addPolyline(new PolylineOptions()
                        .addAll(currentSegment)
                        .color(currentColor)
                        .width(routeWidth));
                currentColor = currentPoint.color;
                currentSegment.clear();
                currentSegment.add(currentPoint.coords);
            }

            ix++;
        }

//        Log.i(TAG, "2.currentSegment "+currentSegment.size()+" points. "+currentColor);
        mGoogleMap.addPolyline(new PolylineOptions()
                .addAll(currentSegment)
                .color(currentColor)
                .width(routeWidth));

    }

    public BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

}
