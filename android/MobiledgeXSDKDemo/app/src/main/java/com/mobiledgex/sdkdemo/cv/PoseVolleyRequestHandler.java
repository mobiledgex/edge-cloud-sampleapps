package com.mobiledgex.sdkdemo.cv;

import android.app.Activity;

public class PoseVolleyRequestHandler extends VolleyRequestHandler {

    public PoseVolleyRequestHandler(ImageServerInterface imageServerInterface, Activity activity) {
        super(imageServerInterface, activity);
    }
}
