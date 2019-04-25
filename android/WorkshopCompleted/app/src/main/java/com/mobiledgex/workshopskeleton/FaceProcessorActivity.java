package com.mobiledgex.workshopskeleton;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class FaceProcessorActivity extends AppCompatActivity {

    private FaceProcessorFragment mFaceProcessorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_face_processor);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (null == savedInstanceState) {
            mFaceProcessorFragment = FaceProcessorFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, mFaceProcessorFragment)
                    .commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // Rotating the device creates a new instance of the fragment. Update reference here.
        if (fragment instanceof FaceProcessorFragment)
            mFaceProcessorFragment = (FaceProcessorFragment) fragment;
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        String stats = mFaceProcessorFragment.getStatsText();
        resultIntent.putExtra("STATS", stats);
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }

}
