package com.mobiledgex.emptymatchengineapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mobiledgex.matchingengine.util.RequestPermissions;


public class FirstTimeUseActivity extends AppCompatActivity {

    private final String TAG = "FirstTimeUseActivity";
    private RequestPermissions mRpUtil;
    private AppCompatActivity self;
    private SharedPreferences prefs;
    private String prefKeyAllowMEX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_time_use);

        mRpUtil = new RequestPermissions();
        self = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefKeyAllowMEX = getResources().getString(R.string.preference_mex_location_verification);

        TextView devLocationWhy = findViewById(R.id.permission_location_device_why);
        devLocationWhy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle(R.string.location_device_permission_title)
                        .setMessage(R.string.location_device_permission_explanation)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create().show();
            }
        });


        TextView carrierLocationWhy = findViewById(R.id.permission_location_carrier_why);
        carrierLocationWhy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle(R.string.location_carrier_permission_title)
                        .setMessage(R.string.location_carrier_permission_explanation)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create().show();
            }
        });

        Button ok = findViewById(R.id.okbutton);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity activity = (Activity) view.getContext();
                if (shouldFinish(activity)) {

                    // Disable first time use.
                    String firstTimeUseKey = getResources().getString(R.string.preference_first_time_use);
                    prefs.edit()
                            .putBoolean(firstTimeUseKey, false)
                            .apply();

                    activity.finish();
                } else {
                    requestPermissions(self);
                }
            }
        });

    }

    private boolean shouldFinish(Activity activity) {
        final String prefKeyAllowMEX = getResources().getString(R.string.preference_mex_location_verification);
        boolean mexLocationAllowed = prefs.getBoolean(prefKeyAllowMEX, false);

        Log.d(TAG, "mexLocationallowed: " + mexLocationAllowed);
        Log.d(TAG, "Needs More Permissions: " + mRpUtil.getNeededPermissions(self).size());
        if (mexLocationAllowed &&
            (mRpUtil.getNeededPermissions(activity).size() == 0)) {

            // Nothing to ask for. Close FirstTimeUseActivity.
            return true;
        }
        return false;
    }

    private void requestPermissions(AppCompatActivity appCompatActivity) {
        // As of Android 23, permissions can be asked for while the app is still running.
        if (mRpUtil.getNeededPermissions(appCompatActivity).size() > 0) {
            mRpUtil.requestMultiplePermissions(appCompatActivity);
        }
        if (!prefs.getBoolean(prefKeyAllowMEX, false)) {
            new EnhancedLocationDialog().show(appCompatActivity.getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (shouldFinish(this)) {
            // Nothing to ask for. Close FirstTimeUseActivity.
            finish();
        }
    }

    // From AppCompatActivity interface.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Or replace with an app specific dialog set.
        mRpUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
