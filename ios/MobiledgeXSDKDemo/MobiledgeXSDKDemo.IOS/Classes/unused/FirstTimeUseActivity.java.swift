//package com.mobiledgex.sdkdemo;
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.preference.PreferenceManager;
//import android.support.annotation.NonNull;
//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import com.mobiledgex.matchingengine.util.RequestPermissions;

import UIKit

public class FirstTimeUseActivity
        // JT 18.10.23 todo
//extends AppCompatActivity
{
    private let  TAG:String = "FirstTimeUseActivity";
    //private var mRpUtil :RequestPermissions   // JT 18.11.02
   // private var myself: AppCompatActivity // JT 18.11.02
   // private var prefs: SharedPreferences  // JT 18.11.02
    private var prefKeyAllowMEX: String = ""

     @IBOutlet public weak var  devLocationWhy:UILabel?  // JT 18.11.02 todo IB
    @IBOutlet public weak var  carrierLocationWhy:UILabel? // JT 18.11.02 todo IB
    @IBOutlet public weak var  ok:UIButton? // JT 18.11.02 todo IB
    
    init()  // JT 18.11.02
    {
        
    }
    func onCreate( savedInstanceState: Bundle)
    {
       // super.onCreate(savedInstanceState);
      //  setContentView(R.layout.activity_first_time_use);

//        mRpUtil =  RequestPermissions();
//        myself = self;
//        prefs = PreferenceManager.getDefaultSharedPreferences(this);
     //   prefKeyAllowMEX = getResources().getString(R.string.preference_mex_location_verification);
       prefKeyAllowMEX   = UserDefaults.standard.string(forKey:"preference_mex_location_verification") ?? "false"  // JT 18.11.02


//        let devLocationWhy:UILabelView = findViewById(R.id.permission_location_device_why);
        
        // JT 18.10.23 todo
//        devLocationWhy.setOnClickListener( View.OnClickListener() {
//
//            public func onClick(View view) {
//                 AlertDialog.Builder(view.getContext())
//                        .setTitle(R.string.location_device_permission_title)
//                        .setMessage(R.string.location_device_permission_explanation)
//                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                            @Override
//                            public func onClick(DialogInterface dialogInterface, int i) {
//                                dialogInterface.dismiss();
//                            }
//                        })
//                        .create().show();
//            }
//        });


//        let carrierLocationWhy:UILabel = findViewById(R.id.permission_location_carrier_why);
 //       carrierLocationWhy.setOnClickListener( View.OnClickListener() {

//

   ///         let ok:UIButton = findViewById(R.id.okbutton);
//        ok.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public func onClick(View view) {
//                Activity activity = (Activity) view.getContext();
//                if (shouldFinish(activity)) {
//
//                    // Disable first time use.
//                    String firstTimeUseKey = getResources().getString(R.string.preference_first_time_use);
//                    prefs.edit()
//                            .putBoolean(firstTimeUseKey, false)
//                            .apply();
//
//                    activity.finish();
//                } else {
//                    requestPermissions(self);
//                }
//            }
//        });

    }

    func shouldFinish(/*_ activity: Activity*/) ->Bool
    {
        let prefKeyAllowMEX:String   = UserDefaults.standard.string(forKey:"preference_mex_location_verification") ?? "false"  // JT 18.11.02
        //getResources().getString(R.string.preference_mex_location_verification);
     //   let mexLocationAllowed:Bool = prefs.getBoolean(prefKeyAllowMEX, false);

        let mexLocationAllowed:Bool   = UserDefaults.standard.bool(forKey:prefKeyAllowMEX) ?? false  // JT 18.11.02

        
    //    Log.d(TAG, "mexLocationallowed: " + mexLocationAllowed);
        Swift.print("mexLocationallowed \(mexLocationAllowed)")   // JT 18.10.23
       // Log.d(TAG, "Needs More Permissions: " + mRpUtil.getNeededPermissions(self).size());
        Swift.print("Needs More Permissions: ") //\(mRpUtil.getNeededPermissions(self).size()) ")   // JT 18.10.23
//     if (mexLocationAllowed &&
//            (mRpUtil.getNeededPermissions(activity).size() == 0)) {
//
//            // Nothing to ask for. Close FirstTimeUseActivity.
//            return true;
//        }
        Swift.print("todo shouldFinish")    // JT 18.11.02
        return false;
    }

    // plist hands this // JT 18.11.02
    //
//        func requestPermissions(_ appCompatActivity: AppCompatActivity) {
//        // As of Android 23, permissions can be asked for while the app is still running.
//        if (mRpUtil.getNeededPermissions(appCompatActivity).size() > 0) {
//            mRpUtil.requestMultiplePermissions(appCompatActivity);
//        }
//        if (!prefs.getBoolean(prefKeyAllowMEX, false)) {
//             EnhancedLocationDialog().show(appCompatActivity.getSupportFragmentManager(), "dialog");
//        }
//    }

//     func onResume() {
//        super.onResume();
//
//        if (shouldFinish(this)) {
//            // Nothing to ask for. Close FirstTimeUseActivity.
//            finish();
//        }
//    }

//    // From AppCompatActivity interface.
//    func onRequestPermissionsResult(_ requestCode: Int, _ permissions:[String],
//                                   _ grantResults:[Int]) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        // Or replace with an app specific dialog set.
//        mRpUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
//    }
}


