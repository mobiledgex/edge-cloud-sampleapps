package com.mobiledgex.sdkdemo.camera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.mobiledgex.sdkdemo.R;

public class PoseCameraActivity extends AppCompatActivity {

    private Camera2BasicFragment cameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pose_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (null == savedInstanceState) {
            cameraFragment = PoseCameraFragment.newInstance();
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
