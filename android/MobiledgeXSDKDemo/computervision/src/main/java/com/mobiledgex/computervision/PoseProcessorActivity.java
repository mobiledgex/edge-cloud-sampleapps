package com.mobiledgex.computervision;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class PoseProcessorActivity extends AppCompatActivity {

    private ImageProcessorFragment imageProcessorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pose_processor);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (null == savedInstanceState) {
            imageProcessorFragment = PoseProcessorFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, imageProcessorFragment)
                    .commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // Rotating the device creates a new instance of the fragment. Update reference here.
        if (fragment instanceof ImageProcessorFragment)
            imageProcessorFragment = (ImageProcessorFragment) fragment;
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        String stats = imageProcessorFragment.getStatsText();
        resultIntent.putExtra("STATS", stats);
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }

}
