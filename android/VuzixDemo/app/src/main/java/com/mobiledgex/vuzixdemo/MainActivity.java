/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
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

package com.mobiledgex.vuzixdemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.mobiledgex.computervision.ImageProcessorActivity;
import com.mobiledgex.computervision.ImageProcessorFragment;
import com.mobiledgex.computervision.PoseProcessorActivity;
import com.mobiledgex.computervision.PoseProcessorFragment;
import com.mobiledgex.computervision.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button buttonFaceDetection = findViewById(R.id.button_face_detection);
        buttonFaceDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFaceDetection(getApplicationContext());
            }
        });

        Button buttonFaceRecognition = findViewById(R.id.button_face_recognition);
        buttonFaceRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFaceRecognition(getApplicationContext());
            }
        });

        Button buttonPoseDetection = findViewById(R.id.button_pose_detection);
        buttonPoseDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPoseDetection(getApplicationContext());
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
        prefs.edit().putInt(prefKeyFrontCamera, CameraCharacteristics.LENS_FACING_BACK).apply();

    }

    private void launchFaceDetection(Context context) {
        Intent intent = new Intent(context, ImageProcessorActivity.class);
        intent.putExtra(ImageProcessorFragment.EXTRA_FACE_STROKE_WIDTH, 4);
        startActivity(intent);
    }

    private void launchFaceRecognition(Context context) {
        Intent intent = new Intent(context, ImageProcessorActivity.class);
        intent.putExtra(ImageProcessorFragment.EXTRA_FACE_STROKE_WIDTH, 4);
        intent.putExtra(ImageProcessorFragment.EXTRA_FACE_RECOGNITION, true);
        startActivity(intent);
    }

    private void launchPoseDetection(Context context) {
        Intent intent = new Intent(context, PoseProcessorActivity.class);
        intent.putExtra(PoseProcessorFragment.EXTRA_POSE_JOINT_RADIUS, 4);
        intent.putExtra(PoseProcessorFragment.EXTRA_POSE_STROKE_WIDTH, 4);
        intent.putExtra(PoseProcessorFragment.EXTRA_EDGE_CLOUDLET_HOSTNAME, "136.144.61.37"); //openpose.chicago-packet.mobiledgex.net
        startActivity(intent);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.FaceDetectionSettingsFragment.class.getName() );
            intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
