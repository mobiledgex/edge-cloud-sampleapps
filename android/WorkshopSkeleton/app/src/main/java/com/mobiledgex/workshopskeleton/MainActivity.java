package com.mobiledgex.workshopskeleton;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

// Matching Engine API:
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.mobiledgex.matchingengine.MatchingEngine;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private MatchingEngine matchingEngine;
    private String someText = null;
    private AppClient.FindCloudletReply mClosestCloudlet;
    private Activity ctx;
    private String host;
    private int port;
    private String carrierName;
    private String appName;
    private String devName;

    private TextView cloudletNameTv;
    private TextView appNameTv;
    private TextView fqdnTv;
    private TextView portNumberTv;
    private TextView carrierNameTv;
    private TextView distanceTv;
    private TextView latitudeTv;
    private TextView longitudeTv;
    private CheckBox checkboxRegistered;
    private CheckBox checkboxCloudletFound;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        progressBar = findViewById(R.id.progressBar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(registerClient()) {
                        // Now that we are registered, let's find the closest cloudlet
                        findCloudlet();
                    }
                } catch (ExecutionException | InterruptedException | io.grpc.StatusRuntimeException e) {
                    e.printStackTrace();
                    someText = "Registration Failed. Exception="+e.getLocalizedMessage();
                    showErrorMsg(someText);
                }
            }
        });
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

        if (id == R.id.action_reset) {
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean registerClient() throws ExecutionException, InterruptedException, io.grpc.StatusRuntimeException {
        // NOTICE: In a real app, these values would be determined by the SDK, but we are reusing
        // an existing app so we don't have to create new app provisioning data for this workshop.
        appName = "MobiledgeX SDK Demo";
        devName = "MobiledgeX SDK Demo"; // NOTICE: In the current demo config, this matches the appName.
        carrierName = "TDG";

        //NOTICE: A real app would request permission to enable this.
        MatchingEngine.setMexLocationAllowed(true);

        /////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to register the client. Replace all "= null" lines here.
        matchingEngine = null;
        AppClient.RegisterClientRequest registerClientRequest = null;
        AppClient.RegisterClientReply registerStatus = null;
        /////////////////////////////////////////////////////////////////////////////////////

        if(matchingEngine == null) {
            someText = "registerClient call is not successfully coded. Search for TODO in code.";
            Log.e(TAG, someText);
            showErrorMsg(someText);
            return false;
        }

        Log.i(TAG, "registerClientRequest="+registerClientRequest);
        Log.i(TAG, "registerStatus.getStatus()="+registerStatus.getStatus());

        if (registerStatus.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
            someText = "Registration Failed. Error: " + registerStatus.getStatus();
            Log.e(TAG, someText);
            showErrorMsg(someText);
            return false;
        }

        Log.i(TAG, "SessionCookie:" + registerStatus.getSessionCookie());
        checkboxRegistered.setChecked(true);
        checkboxRegistered.setText(R.string.client_registered);
        // Populate app details.
        carrierNameTv.setText(carrierName);
        appNameTv.setText(appName);

        return true;
    }

    public boolean findCloudlet() throws ExecutionException, InterruptedException {
        //(Blocking call, or use findCloudletFuture):
        Location location = new Location("MEX");
        ////////////////////////////////////////////////////////////
        // TODO: Change these coordinates to where you're actually located.
        // Of course a real app would use GPS to acquire the exact location.
        location.setLatitude(52.5157236);
        location.setLongitude(13.2975664);

        ////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to find the cloudlet closest to you. Replace "= null" here.
        AppClient.FindCloudletRequest findCloudletRequest = null;
        mClosestCloudlet = null;
        ////////////////////////////////////////////////////////////////////////////////////////////

        Log.i(TAG, "mClosestCloudlet="+mClosestCloudlet);
        if(mClosestCloudlet == null) {
            someText = "findCloudlet call is not successfully coded. Search for TODO in code.";
            Log.e(TAG, someText);
            showErrorMsg(someText);
            return false;
        }
        if(mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
            someText = "findCloudlet Failed. Error: " + mClosestCloudlet.getStatus();
            Log.e(TAG, someText);
            showErrorMsg(someText);
            return false;
        }
        Log.i(TAG, "REQ_FIND_CLOUDLET mClosestCloudlet.uri=" + mClosestCloudlet.getFQDN());
        checkboxCloudletFound.setChecked(true);
        checkboxCloudletFound.setText(R.string.cloudlet_found);

        // Populate cloudlet details.
        latitudeTv.setText(""+mClosestCloudlet.getCloudletLocation().getLat());
        longitudeTv.setText(""+mClosestCloudlet.getCloudletLocation().getLong());
        fqdnTv.setText(mClosestCloudlet.getFQDN());
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        LatLng cloudletLatLng = new LatLng(mClosestCloudlet.getCloudletLocation().getLat(),
                mClosestCloudlet.getCloudletLocation().getLong());
        double distance = SphericalUtil.computeDistanceBetween(userLatLng, cloudletLatLng)/1000;
        distanceTv.setText(String.format("%.2f", distance)+" km");
        //Extract cloudlet name from FQDN
        String[] parts = mClosestCloudlet.getFQDN().split("\\.");
        cloudletNameTv.setText(parts[1]);
        List<Appcommon.AppPort> ports = mClosestCloudlet.getPortsList();
        String appPortFormat = "{Protocol: %d, Container Port: %d, External Port: %d, Public Path: '%s'}";
        for (Appcommon.AppPort aPort : ports) {
            portNumberTv.setText(""+aPort.getPublicPort());
            Log.i(TAG, String.format(Locale.getDefault(), appPortFormat,
                    aPort.getProto().getNumber(),
                    aPort.getInternalPort(),
                    aPort.getPublicPort(),
                    aPort.getPublicPath()));
        }

        // TODO: Copy/paste the output of this log into a terminal to test latency.
        Log.i("COPY_PASTE", "ping -c 4 "+mClosestCloudlet.getFQDN());

        return true;
    }

    public void showErrorMsg(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

}
