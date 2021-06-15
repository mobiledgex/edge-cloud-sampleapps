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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private String prefKeyAllowMatchingEngineLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_time_use);

        mRpUtil = new RequestPermissions();
        self = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefKeyAllowMatchingEngineLocation = getResources().getString(R.string.pref_matching_engine_location_verification);

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
                                /*
                                From Jesse.Bijl@detecon.com:
                                In the DT flow the user presses OK at the text message (2nd screen)
                                and therewith is opted in automatically. User can go to settings
                                menu in order to opt-out (though does not need to go there for opt-in).
                                */
                                Log.i(TAG, "OK Clicked. Automatic opt-in");
                                prefs.edit().putBoolean(prefKeyAllowMatchingEngineLocation, true).apply();
                                boolean matchingEngineLocationAllowed = prefs.getBoolean(prefKeyAllowMatchingEngineLocation, false);

                                Log.d(TAG, "matchingEngineLocationAllowed: " + matchingEngineLocationAllowed);

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
                    String firstTimeUseKey = getResources().getString(R.string.pref_first_time_use);
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
        final String prefKeyAllowMatchingEngineLocation = getResources().getString(R.string.pref_matching_engine_location_verification);
        boolean mobiledgeXLocationAllowed = prefs.getBoolean(prefKeyAllowMatchingEngineLocation, false);

        Log.d(TAG, "mobiledgeXLocationAllowed: " + mobiledgeXLocationAllowed);
        Log.d(TAG, "Needs More Permissions: " + mRpUtil.getNeededPermissions(self).size());
        if (mobiledgeXLocationAllowed &&
            (mRpUtil.getNeededPermissions((AppCompatActivity) activity).size() == 0)) {

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
        if (!prefs.getBoolean(prefKeyAllowMatchingEngineLocation, false)) {
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
