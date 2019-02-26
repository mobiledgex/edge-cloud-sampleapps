/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.sdkdemo.camera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.mobiledgex.sdkdemo.MainActivity;
import com.mobiledgex.sdkdemo.R;

public class CameraActivity extends AppCompatActivity {

    private Camera2BasicFragment cameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_camera2_basic);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (null == savedInstanceState) {
            cameraFragment = Camera2BasicFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // Rotating the device creates a new instance of the fragment. Update reference here.
        if (fragment instanceof Camera2BasicFragment)
            cameraFragment = (Camera2BasicFragment) fragment;
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        String stats = cameraFragment.getStatsText();
        resultIntent.putExtra("STATS", stats);
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}
