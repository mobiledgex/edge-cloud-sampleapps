//package com.mobiledgex.sdkdemo;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.location.Location;
//import android.os.AsyncTask;
//import android.preference.PreferenceManager;
//import android.support.design.widget.Snackbar;
//import android.util.Log;
//import android.view.View;
//import android.widget.Toast;
//
//import com.mobiledgex.matchingengine.FindCloudletResponse;
//import com.mobiledgex.matchingengine.MatchingEngine;
//import com.mobiledgex.matchingengine.MatchingEngineRequest;
//
//import java.io.IOException;
//import java.util.concurrent.ExecutionException;
//
//import distributed_match_engine.AppClient;
//import io.grpc.StatusRuntimeException;

import UIKit
import GoogleMaps   // JT 18.10.23

public class MatchingEngineHelper2
{
    private static let  TAG: String = "MatchingEngineHelper"
    private var  mContext: Context  // JT 18.10.22
    private var  mView :UIView

    private var mMatchingEngineResultsListener: MatchingEngineResultsListener
    private  var mSpoofLocation:CLLocationCoordinate2D? // JT 18.11.02 Location

    private var mClosestCloudlet: DistributedMatchEngine_FindCloudletReply  //FindCloudletResponse  // JT 18.11.02 where is this define
    private var mMatchingEngine: MatchingEngine
     var someText: String? = nil
     var mHostname: String = ""

    init () // JT 18.10.23
    {
    }
    
     init(_ context: Context, _ hostname: String, _ view: UIView)   // JT 18.11.02
     {
        mContext = context;
        mView = view;
        mHostname = hostname;
        mMatchingEngine =  MatchingEngine(mContext);
    }
    /**
     * Possible actions to perform with the matching engine.
     */
    enum RequestType : Int {
        case REQ_REGISTER_CLIENT
        case REQ_VERIFY_LOCATION
        case REQ_FIND_CLOUDLET
        case REQ_GET_CLOUDLETS
    }

    func MatchingEngineHelper(_ context: Context, _ hostname: String, _ view: UIView) {
        mContext = context;
        mView = view;
        mHostname = hostname;
        mMatchingEngine =  MatchingEngine(mContext);
    }

    /**
     * This method performs several actions with the matching engine in the background,
     * one after the other:
     * <ol>
     *     <li>registerClient</li>
     *     <li>verifyLocation</li>
     *     <li>findCloudlet</li>
     * </ol>
     *
     * @param location  The location to pass to the matching engine.
     */
    func  doEnhancedLocationUpdateInBackground (_  location: CLLocationCoordinate2D?) {
Swift.print("doEnhancedLocationUpdateInBackground") // JT 18.10.22 todo
        //EnhancedLocationUpdate(self).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, location);
        
        let enhancedLocationUpdate = EnhancedLocationUpdate(self)   // JT 18.11.02
    }

    public class EnhancedLocationUpdate
            // JT 18.10.22 to do
    //extends AsyncTask<Location, Void, Void>
{
        var  matchingEngineHelper: MatchingEngineHelper
        init(_ matchingEngineHelper: MatchingEngineHelper)
        {
            self.matchingEngineHelper = matchingEngineHelper    // JT 18.11.02
        }
        func  doInBackground( locations: [CLLocationCoordinate2D?]) -> Int?
        {
           let location = locations[0];
          // Log.i(TAG, "EnhancedLocationUpdate location="+location);
  ///          let  ctx:Activity? = nil //(Activity) mContext;  // JT 18.10.22
           if (location == nil) {
              // Log.w(TAG, "location is null. Aborting.");
               return 0;    // JT 18.10.22
           }

           do {
//            let prefs:SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            
       //     let mexAllowed:Bool = prefs.getBoolean(mContext.getResources().getString(R.string.preference_mex_location_verification), false);
            
            let mexAllowed:Bool =  UserDefaults.standard.bool(forKey:"preference_mex_location_verification")      // JT 18.11.02


               if(!mexAllowed)
               {
 //           let snackbar:Snackbar = Snackbar.make(mView, "Enhanced Location not enabled", Snackbar.LENGTH_LONG);
                // JT 18.10.22 todo
            Swift.print("Enhanced Location not enabled")
//                   snackbar.setAction("Settings", new View.OnClickListener() {
//                       @Override
//                       public void onClick(View v) {
//                           Intent intent = new Intent(mContext, SettingsActivity.class);
//                           mContext.startActivity(intent);
//                       }
//                   });
 ///                  snackbar.show();
                
                let alert = UIAlertController(title: "Enhanced Location not enabled", message: "???", preferredStyle: .alert)
                
                alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                
                self.present(alert, animated: true)
                return 0;
               }

               // Create a request:
            let host:String = matchingEngineHelper.mHostname // Override host.
            let port:Int = matchingEngineHelper.mMatchingEngine.getPort(); // Keep same port.   // JT 18.11.02
            let carrierName:String = "TDG";
            let devName:String = "MobiledgeX SDK Demo"; //TODO: In the current demo config, this matches the appName.

            let req /*MatchingEngineRequest*/ = matchingEngineHelper.mMatchingEngine.createRequest( Context(), host, port, carrierName, devName, location);
            
            let registerStatus: /*AppClient.Match_Engine_Status */
                = matchingEngineHelper.mMatchingEngine.registerClient(req, 10000);
            
               if (registerStatus.getStatus() != AppClient.Match_Engine_Status.ME_Status.ME_SUCCESS)
               {
//                   someText = "Registration Failed. Error: " + registerStatus.getStatus();
//                   Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                
                let alert = UIAlertController(title: "Registration Failed. Error: \(registerStatus.getStatus())", message: "???", preferredStyle: .alert)
                
                alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                
                self.present(alert, animated: true)

                
                   return 0;
               }

               req = mMatchingEngine.createRequest(Context(), host, port, carrierName, devName, location);
               if (req != nil) {
                   // Location Verification (Blocking, or use verifyLocationFuture):
                let verifiedLocation //:AppClient.Match_Engine_Loc_Verify
                = mMatchingEngine.verifyLocation(req, 10000);
                   someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                           ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                           ", Location Accuracy: " + verifiedLocation.getGPSLocationAccuracyKM() + " ]\n";

                   if(matchingEngineHelper.mMatchingEngineResultsListener != nil)
                   {
                       mMatchingEngineResultsListener.onVerifyLocation(verifiedLocation.getGpsLocationStatus(),
                               verifiedLocation.getGPSLocationAccuracyKM());
                   }

                   // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
                   mClosestCloudlet = mMatchingEngine.findCloudlet(req, 10000);
                 //  Log.i(TAG, "mClosestCloudlet.uri="+mClosestCloudlet.uri);
            Swift.print(" mClosestCloudlet.uri==  \(mClosestCloudlet.uri)")

//            ctx.runOnUiThread(new Runnable() {
//
//                 void run() {
//                           if(mMatchingEngineResultsListener != nil) {
//                               mMatchingEngineResultsListener.onFindCloudlet(mClosestCloudlet);
//                           }
//                       }
//                   });

                DispatchQueue.global().async {
                    
                    if(self.matchingEngineHelper.mMatchingEngineResultsListener != nil)   // JT 18.10.28
                   {
                       self.matchingEngineHelper.mMatchingEngineResultsListener.onFindCloudlet(mClosestCloudlet);
                   }
                }
                
                matchingEngineHelper.someText += "[Cloudlet Server: URI: [" + mClosestCloudlet.uri + "], Port: " + mClosestCloudlet.port + "]";
               } else {
                   matchingEngineHelper.someText = "Cannot create request object.";
                   if (!mexAllowed) {
                       matchingEngineHelper.someText += " Reason: Enhanced location is disabled.";
                   }
                 ///  Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                
                let alert = UIAlertController(title: someText, message: "-", preferredStyle: .alert)
                
                alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                
                self.present(alert, animated: true)

                
               }
              // Log.i(TAG, "0. someText=" + someText);
            Swift.print(" someText==  \(someText)")

           } catch ( ioe: IOException) {
            //   ioe.printStackTrace();
               toastOnUiThread(ioe.getMessage(), Toast.LENGTH_LONG);
           } catch ( sre: StatusRuntimeException) {
            //   sre.printStackTrace();
               toastOnUiThread(sre.getMessage(), Toast.LENGTH_LONG);
           } catch ( iae: IllegalArgumentException) {
            //   iae.printStackTrace();
               toastOnUiThread(iae.getMessage(), Toast.LENGTH_LONG);
           } catch ( ie: InterruptedException) {
            //   ie.printStackTrace();
               toastOnUiThread(ie.getMessage(), Toast.LENGTH_LONG);
           } catch ( ee: ExecutionException) {
            //   ee.printStackTrace();
               toastOnUiThread(ee.getMessage(), Toast.LENGTH_LONG);
           }
           return nil;
        }
    }

    /**
     * This method does a single matching engine action in the background, determined by the
     * reqType parameter. {@link RequestType}
     *
     * @param reqType  The request type.
     * @param location  The location to pass to the matching engine.
     */
    func  doRequestInBackground ( _ reqType: RequestType, _  location: CLLocationCoordinate2D) {
         BackgroudRequest(self).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, reqType, location);
    }

    public class BackgroudRequest
            // JT 18.10.22 todo
    //extends AsyncTask<Object, Void, Void>
    {
        var matchingEngineHelper: MatchingEngineHelper  // JT 18.11.02

        init(_ matchingEngineHelper: MatchingEngineHelper)  // JT 18.11.02
        {
            self.matchingEngineHelper = matchingEngineHelper    // JT 18.11.02
        }
        func  doInBackground( params: [Any? /*Object*/]) -> Int? // JT 18.11.02
        {
            let reqType:RequestType =  params[0] as! MatchingEngineHelper.RequestType;
            let location:CLLocationCoordinate2D? =  params[1] as! CLLocationCoordinate2D
           // Log.i(TAG, "BackgroundRequest reqType="+reqType+" location="+location);
            if(location == nil) {
              //  Log.w(TAG, "location is nil. Aborting.");
                Swift.print("location is nil. Aborting.")
                
                return nil;
            }
           //  let ctx:Activity =  mContext;
           // let prefs:SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
           // let mexAllowed: Bool = prefs.getBoolean(mContext.getResources().getString(R.string.preference_mex_location_verification), false);
            
            let mexAllowed:Bool =  UserDefaults.standard.bool(forKey:"preference_mex_location_verification")      // JT 18.11.02


            if(!mexAllowed)
            {
//                let snackbar:Snackbar = Snackbar.make(mView, "Enhanced Location not enabled", Snackbar.LENGTH_LONG);
                Swift.print("todo mexAllowed")
//                snackbar.setAction("Settings", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Intent intent = new Intent(mContext, SettingsActivity.class);
//                        mContext.startActivity(intent);
//                    }
//                });
  ///              snackbar.show();
                
                let alert = UIAlertController(title: "Enhanced Location not enabled", message: "-", preferredStyle: .alert)
                
                alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                
                self.present(alert, animated: true)
                
                return nil;
            }

            // Create a request:
            do {
            let host: String =  matchingEngineHelper.mHostname; // Override host.
            let port: Int =  matchingEngineHelper.mMatchingEngine.getPort(); // Keep same port.
            let carrierName:String = "TDG";
            let devName:String = "MobiledgeX SDK Demo";

            let req:MatchingEngineRequest =  matchingEngineHelper.mMatchingEngine.createRequest(ctx, host, port, carrierName, devName, location);
                let registerStatus:AppClient.Match_Engine_Status =  matchingEngineHelper.mMatchingEngine.registerClient(req, 10000);
                if (registerStatus.getStatus() != AppClient.Match_Engine_Status.ME_Status.ME_SUCCESS) {
                    someText = "Registration Failed. Error: " + registerStatus.getStatus();
                //    Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                    
                    let alert = UIAlertController(title: someText, message: "-", preferredStyle: .alert)
                    
                    alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                    
                    self.present(alert, animated: true)
                    
                    return nil;
                }
           //     Log.i(TAG, "SessionCookie:" + registerStatus.getSessionCookie());
                if(reqType == RequestType.REQ_REGISTER_CLIENT) {
                   // Log.i(TAG, "REQ_REGISTER_CLIENT only.");
            Swift.print("REQ_REGISTER_CLIENT only." )
 mMatchingEngineResultsListener.onRegister(registerStatus.getSessionCookie());
                    return nil;
                }

                req = matchingEngineHelper.mMatchingEngine.createRequest( Context(), host, port, carrierName, devName, location);
                if (req == nil) {
                    matchingEngineHelper.someText = "Cannot create request object.";
                   // Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show()
                    
                    let alert = UIAlertController(title: matchingEngineHelper.someText, message: "-", preferredStyle: .alert)
                    
                    alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                    
                    self.present(alert, animated: true)
                    ;
                    return nil;
                }

                switch (reqType) {
                    case .REQ_VERIFY_LOCATION:
                        // Location Verification (Blocking, or use verifyLocationFuture):
                        let verifiedLocation: //AppClient.Match_Engine_Loc_Verify
                            DistributedMatchEngine_VerifyLocationReply
                            = matchingEngineHelper.mMatchingEngine.verifyLocation(req, 10000);
                        
                        matchingEngineHelper.someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                                ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                                ", Location Accuracy: " + verifiedLocation.getGPSLocationAccuracyKM() + " ]\n";

                        if(matchingEngineHelper.mMatchingEngineResultsListener != nil) {
                            mMatchingEngineResultsListener.onVerifyLocation(verifiedLocation.getGpsLocationStatus(),
                                    verifiedLocation.getGPSLocationAccuracyKM());
                        }
                        break;
                    

                    case .REQ_FIND_CLOUDLET:
                        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
                        mClosestCloudlet = matchingEngineHelper.mMatchingEngine.findCloudlet(req, 10000);
                     //   Log.i(TAG, "mClosestCloudlet.uri="+mClosestCloudlet.uri);
            Swift.print("mClosestCloudlet.uri=  \(matchingEngineHelper.mClosestCloudlet.uri)")

                        if(matchingEngineHelper.mMatchingEngineResultsListener != nil) {
                            matchingEngineHelper.mMatchingEngineResultsListener.onFindCloudlet(mClosestCloudlet);
                        }
                        break;

                    case .REQ_GET_CLOUDLETS:
                        // Location Verification (Blocking, or use verifyLocationFuture):
            let cloudletList:DistributedMatchEngine_AppInstListReply    // JT 18.11.02
            //AppClient.Match_Engine_AppInst_List
                = mMatchingEngine.getAppInstList(req, 10000);
                        if(matchingEngineHelper.mMatchingEngineResultsListener != nil) {
                            matchingEngineHelper.mMatchingEngineResultsListener.onGetCloudletList(cloudletList);
                        }
                        break;

                    default:
                      //  Log.e(TAG, "Unknown reqType: "+reqType);
            Swift.print("Unknown reqType:  \(reqType)")

                }

           //     Log.i(TAG, "someText=" + someText);
            Swift.print("someText= \(matchingEngineHelper.someText)")
            } catch ( MyError.IOException(let errorMessage)) {
              //  ioe.printStackTrace();
              //  toastOnUiThread(ioe.getMessage(), Toast.LENGTH_LONG);
                SKToast.show(withMessage: errorMessage)  // JT 18.11.02
                
            } catch ( MyError.StatusRuntimeException(let errorMessage)) {
              //  sre.printStackTrace();
              //  toastOnUiThread(sre.getMessage(), Toast.LENGTH_LONG);
                SKToast.show(withMessage: errorMessage)  // JT 18.11.02

            } catch ( MyError.IllegalArgumentException(let errorMessage))
             {
             //   iae.printStackTrace();
               // toastOnUiThread(iae.getMessage(), Toast.LENGTH_LONG);
                SKToast.show(withMessage: errorMessage)  // JT 18.11.02

            } catch ( MyError.InterruptedException(let errorMessage))
             {
               // ie.printStackTrace();
               // toastOnUiThread(ie.getMessage(), Toast.LENGTH_LONG);
                SKToast.show(withMessage: errorMessage)  // JT 18.11.02

            } catch ( MyError.ExecutionException(let errorMessage))
            {
               // ee.printStackTrace();
               // toastOnUiThread(ee.getMessage(), Toast.LENGTH_LONG);
                SKToast.show(withMessage: errorMessage)  // JT 18.11.02

            }
            return 0;
        }
    }

    /**
     * Utility method to create a toast on the UI thread.
     *
     * @param message
     * @param length
     */
    func  toastOnUiThread( message: String,   length: Int)
    {
        // todo // JT 18.10.22
        //        ((Activity)mContext).runOnUiThread(new Runnable()
        //            {
        //            @Override
        //            public void run() {
        //                Toast.makeText(mContext, message, length).show();
        //            }
        //        });
        
    }

    func  getMatchingEngine() ->MatchingEngine
    {
        return mMatchingEngine;
    }

    func  setMatchingEngine( matchingEngine: MatchingEngine) {
        mMatchingEngine = matchingEngine;
    }

    func  getmMatchingEngineResultsListener() ->MatchingEngineResultsListener
    {
        return mMatchingEngineResultsListener;
    }

    func  setMatchingEngineResultsListener(_ matchingEngineResultsListener:MatchingEngineResultsListener)
    {
        mMatchingEngineResultsListener = matchingEngineResultsListener;
    }

    func  getSpoofedLocation() ->CLLocationCoordinate2D?    // JT 18.11.02
    {
        return mSpoofLocation;
    }

    func  setSpoofedLocation( spoofLocation:CLLocationCoordinate2D?)
    {
      //  Log.i(TAG, "setSpoofedLocation("+mSpoofLocation+")");
        mSpoofLocation = spoofLocation
    }

    func  getHostname() ->String
    {
        return mHostname;
    }

    func  setHostname( hostname:String) {
        mHostname = hostname
    }
}
