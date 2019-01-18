package com.mobiledgex.sdkdemo.camera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.mobiledgex.sdkdemo.R;

public class PoseCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("BDA9 PoseCameraActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pose_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, PoseCameraFragment.newInstance())
                    .commit();
        }
    }
}
