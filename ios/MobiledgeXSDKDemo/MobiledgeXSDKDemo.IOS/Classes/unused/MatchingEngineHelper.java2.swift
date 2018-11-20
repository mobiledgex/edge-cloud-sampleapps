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
//import com.mobiledgex.matchingengine.MatchingEngine;
//
//import java.io.IOException;
//import java.util.concurrent.ExecutionException;
//
//import distributed_match_engine.AppClient;
//import io.grpc.StatusRuntimeException;

import UIKit
import GoogleMaps   // JT 18.10.23

public class MatchingEngineHelper
{
    private static let  TAG: String = "MatchingEngineHelper"
    private var  mContext: Context  // JT 18.10.22
    private var  mView :UIView
    
    private var mMatchingEngineResultsListener: MatchingEngineResultsListener?
    private  var mSpoofLocation:CLLocationCoordinate2D? // JT 18.11.02 Location
    
    private var mClosestCloudlet: DistributedMatchEngine_FindCloudletReply  //FindCloudletResponse  // JT 18.11.02 where is this define
    private var mMatchingEngine: MatchingEngine
    private var someText: String? = nil
     var mHostname: String = ""
    
    
    
    init () // JT 18.10.23
    {
    }
    
    init(_ context: Context, _ hostname: String, _ view: UIView)   // JT 18.11.02
    {
        Swift.print("MatchingEngineHelper mHostname= \(hostname)")  // JT
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
        case REQ_DO_ALL // JT 18.11.02
     
        
    }

//    public MatchingEngineHelper(Context context, String hostname, View view) {
//        Log.i(TAG, "MatchingEngineHelper mHostname="+hostname);
//        mContext = context;
//        mView = view;
//        mHostname = hostname;
//        mMatchingEngine = new MatchingEngine(mContext);
//    }

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


    /**
     * This method does a single matching engine action in the background, determined by the
     * reqType parameter. {@link RequestType}
     *
     * @param reqType  The request type.
     * @param location  The location to pass to the matching engine.
     */
     func doRequestInBackground (_  reqType: RequestType,
                                       _ location: CLLocationCoordinate2D)
    {
         //BackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, reqType, location);
        
        DispatchQueue.global().async {
            
            do
            {
                let  mbgr = BackgroundRequest(self)
                try mbgr.doInBackground(reqType, location)  // JT 18.11.06
                
            }
            catch{
                
            }
            
        }
    }

    public class BackgroundRequest
    //extends AsyncTask<Object, Void, Void>
{
        var matchingEngineHelper: MatchingEngineHelper  // JT 18.11.02

        init(_ matchingEngineHelper: MatchingEngineHelper)  // JT 18.11.02
        {
            self.matchingEngineHelper = matchingEngineHelper    // JT 18.11.02
        }
        

        //@Override
      //  func doInBackground(_ params: [Any?]) throws -> Int?   // JT 18.11.02
        func doInBackground(_ reqType:RequestType, _ location:CLLocationCoordinate2D?) throws -> Int?   // JT 18.11.02
       {
         //   let reqType:RequestType =  params[0] as! MatchingEngineHelper.RequestType;
         //   let location:CLLocationCoordinate2D? =  params[1] as! CLLocationCoordinate2D
            
           // Log.i(TAG, "BackgroundRequest reqType="+reqType+" location="+location);
            Swift.print("BackgroundRequest reqType= \(reqType) location= \(location)")
            if(location == nil) {
              //  Log.w(TAG, "location is nil. Aborting.");
                Swift.print("location is nil. Aborting.")

                return nil;
            }
            let mexAllowed:Bool =  UserDefaults.standard.bool(forKey:"preference_mex_location_verification")      // JT 18.11.02


            if(!mexAllowed) {
//                Snackbar snackbar = Snackbar.make(mView, "Enhanced Location not enabled", Snackbar.LENGTH_LONG);
//                snackbar.setAction("Settings", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Intent intent = new Intent(mContext, SettingsActivity.class);
//                        mContext.startActivity(intent);
//                    }
//                });
//                snackbar.show();
                
                let alert = UIAlertController(title: "Enhanced Location not enabled", message: "-", preferredStyle: .alert)
                
                alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                
                let window:UIWindow = UIApplication.shared.keyWindow!
                let vc: UIViewController  = window.rootViewController!  //  Get its rootViewController:  // JT 18.11.06
                
                vc.present(alert, animated: true)
                
                return nil;
            }

            // Create a request:
            do {
                let host: String =  matchingEngineHelper.mHostname; // Override host.
                let port: Int =  matchingEngineHelper.mMatchingEngine.getPort(); // Keep same port.
                let carrierName:String = "TDG";
                let devName:String = "MobiledgeX SDK Demo";

                let appVersion:String = "1.0"; //TODO: SDK should populate this automatically if we pass in "". Currently broken.
                var reportCookie:Bool = false;

                if(reqType == RequestType.REQ_REGISTER_CLIENT) {
                    reportCookie = true;
                }
             //   Log.i(TAG, "host:" + host);
                Swift.print("host: \(host)")    // JT 18.11.02

                let ctx = Context()
                if (try !matchingEngineHelper.registerClient(ctx, host, port, devName, appVersion, reportCookie))
                {
                    return nil;
                }

                switch (reqType) {
                    case .REQ_VERIFY_LOCATION:
                        try matchingEngineHelper.verifyLocation( location!, ctx, host, port, carrierName);
                        break;

                    case .REQ_FIND_CLOUDLET:
                        try matchingEngineHelper.findCloudlet(location!, ctx, host, port, carrierName);
                        break;

                    case .REQ_GET_CLOUDLETS:
                        try matchingEngineHelper.getAppInstList(location!, ctx, host, port, carrierName);
                        break;

                    case .REQ_DO_ALL:
                        //In this case, we do all actions in order as long as each one is successful.
                        if( try !matchingEngineHelper.getAppInstList(location!, ctx, host, port, carrierName)) {
                           // Log.e(TAG, "getAppInstList failed. aborting REQ_DO_ALL");
                            Swift.print("getAppInstList failed. aborting REQ_DO_ALL")
                            return nil;
                        }
                        if(try !matchingEngineHelper.verifyLocation(location!, ctx, host, port, carrierName)) {
                          //  Log.e(TAG, "verifyLocation failed. aborting REQ_DO_ALL");
                            Swift.print( "verifyLocation failed. aborting REQ_DO_ALL")
                            return nil;
                        }
                        if(try !matchingEngineHelper.findCloudlet(location!, ctx, host, port, carrierName)) {
                        //    Log.e(TAG, "findCloudlet failed. aborting REQ_DO_ALL");
                            Swift.print("findCloudlet failed. aborting REQ_DO_ALL")
                            return nil;
                        }
                        break;

                    default:
                        //Log.e(TAG, "Unknown reqType: "+reqType);
                    Swift.print("Unknown reqType: \(reqType)")
                }

              //  Log.i(TAG, "someText=" + someText);
                Swift.print("someText \(String(describing: matchingEngineHelper.someText))")

            } catch ( MyError.IOException(let errorMessage))
                
                //(IOException | StatusRuntimeException | IllegalArgumentException  | ExecutionException | InterruptedException ioe)
            {
            //    ioe.printStackTrace();
               // toastOnUiThread(ioe.getMessage(), Toast.LENGTH_LONG);
                        
                        SKToast.show(withMessage: errorMessage)  // JT 18.11.02
            }
            catch ( MyError.IllegalArgumentException(let errorMessage))
             {
                 SKToast.show(withMessage: errorMessage)  // JT 18.11.02
            }
            catch ( MyError.ExecutionException(let errorMessage))
            {
                SKToast.show(withMessage: errorMessage)  // JT 18.11.02
            }
            catch ( MyError.StatusRuntimeException(let errorMessage))
            {
                SKToast.show(withMessage: errorMessage)  // JT 18.11.02
            }

            return nil;
        }
    }

    private func registerClient(_ ctx: Context, _ host: String, _ port: Int, _ devName: String, _ appVersion: String, _ reportCookie: Bool) throws ->Bool
    //InterruptedException, ExecutionException
{
    let registerClientRequest: DistributedMatchEngine_RegisterClientRequest =
        try mMatchingEngine.createRegisterClientRequest(ctx,
                                                            devName, "", appVersion)!;
   //     Log.i(TAG, "registerClientRequest.getAppVers()=["+registerClientRequest.getAppVers()+"] registerClientRequest.getAppName()="+registerClientRequest.getAppName());
    Swift.print("registerClientRequest.appVers()=[\(registerClientRequest.appVers)] registerClientRequest.appName()=\(registerClientRequest.appName))")
    
    let registerStatus: DistributedMatchEngine_RegisterClientReply =
        try mMatchingEngine.registerClient(registerClientRequest,
                        host, port, 10000);
        if (registerStatus.status != DistributedMatchEngine_ReplyStatus.rsSuccess)
        {
            someText = "Registration Failed. Error: \( registerStatus.status)"
          //  Log.e(TAG, someText);
            Swift.print("\(String(describing: someText))")
    //        Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            
            let alert = UIAlertController(title: someText, message: "-", preferredStyle: .alert)
            
            alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
            
            let window:UIWindow = UIApplication.shared.keyWindow!
            let vc: UIViewController  = window.rootViewController!  //  Get its rootViewController:
            
            vc.present(alert, animated: true)   // JT 18.11.06
            
            return false;
        }
   //     Log.i(TAG, "SessionCookie:" + registerStatus.getSessionCookie());
    Swift.print( "SessionCookie: \(registerStatus.sessionCookie)")
    
        if (reportCookie) {
           // Log.i(TAG, "REQ_REGISTER_CLIENT only.");
           Swift.print("REQ_REGISTER_CLIENT only.")
            mMatchingEngineResultsListener!.onRegister(registerStatus.sessionCookie);
            
        }
        return true;
    }

    private  func getAppInstList(_ location: CLLocationCoordinate2D,
                                 _ ctx: Context, _ host: String,
                                 _ port: Int, _ carrierName: String) throws ->Bool
  //  InterruptedException, ExecutionException
{
        // Location Verification (Blocking, or use verifyLocationFuture):
     //   Log.i(TAG, "mMatchingEngine getHost()="+mMatchingEngine.getHost());
    Swift.print("mMatchingEngine getHost()= \(mMatchingEngine.getHost())")
  
    let appInstListRequest:DistributedMatchEngine_AppInstListRequest? =
        try mMatchingEngine.createAppInstListRequest(ctx, carrierName, location);
        if(appInstListRequest != nil) {
            let cloudletList: DistributedMatchEngine_AppInstListReply = try mMatchingEngine.getAppInstList(appInstListRequest!,
                    host, port, 10000);

            
          //  Log.i(TAG, "REQ_GET_CLOUDLETS cloudletList.getCloudletsCount()=" + cloudletList.getCloudletsCount());
            Swift.print("REQ_GET_CLOUDLETS cloudletList.getCloudletsCount()=\(cloudletList.cloudlets.count)")
            
            if (mMatchingEngineResultsListener != nil) {
                mMatchingEngineResultsListener!.onGetCloudletList(appInstListRequest!); // JT 18.11.02 was cloudletList
            }
        } else {
            someText = "Cannot create AppInstListRequest object.\n";
           // Log.e(TAG, someText);
            Swift.print("Cannot create AppInstListRequest object.\n")
          //  Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            
            
            let alert = UIAlertController(title: someText  , message: "-", preferredStyle: .alert)
            
            alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
            
            let window:UIWindow = UIApplication.shared.keyWindow!
            let vc: UIViewController  = window.rootViewController!  //  Get its rootViewController:
            
            vc.present(alert, animated: true)
            
            
            return false;
        }
        return true;
    }

    private func findCloudlet(_ location: CLLocationCoordinate2D,
                              _ ctx: Context,
                              _ host: String, _ port: Int,
        _ carrierName: String) throws ->Bool
    //InterruptedException, ExecutionException
    {
        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
       let findCloudletRequest: DistributedMatchEngine_FindCloudletRequest? =
        try mMatchingEngine.createFindCloudletRequest(ctx, carrierName, location);
        if (findCloudletRequest != nil)
        {
            mClosestCloudlet = try mMatchingEngine.findCloudlet( findCloudletRequest!,
                    host, port, 10000);
            if (mClosestCloudlet.status != DistributedMatchEngine_FindCloudletReply.FindStatus.findFound)
            {
                someText = "findCloudlet Failed. Error: \(mClosestCloudlet.status )" //getStatus();
             //   Log.e(TAG, someText);
                Swift.print("\(someText)")
             //   Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                
                let alert = UIAlertController(title: someText  , message: "-", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
                
                let window:UIWindow = UIApplication.shared.keyWindow!
                let vc: UIViewController  = window.rootViewController!  //  Get its rootViewController:  // JT 18.11.06
                
                vc.present(alert, animated: true)
                
                
                return false;
            }
         //   Log.i(TAG, "REQ_FIND_CLOUDLET mClosestCloudlet.uri=" + mClosestCloudlet.getFQDN());
            Swift.print("REQ_FIND_CLOUDLET mClosestCloudlet.uri= \(mClosestCloudlet.fqdn)")    // JT 18.11.02
            if (mMatchingEngineResultsListener != nil) {
                mMatchingEngineResultsListener!.onFindCloudlet(mClosestCloudlet);
            }
        } else {
            someText = "Cannot create FindCloudletRequest object.\n";
          //  Log.e(TAG, someText);
            Swift.print("\(someText)")
         //   Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            
            let alert = UIAlertController(title: someText  , message: "-", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"
            
            let window:UIWindow = UIApplication.shared.keyWindow!
            let vc: UIViewController  = window.rootViewController!  //  Get its rootViewController:  // JT 18.11.06
            
            vc.present(alert, animated: true)

            return false;
        }
        return true;
    }

    private func verifyLocation( _ location: CLLocationCoordinate2D,
                                 _ ctx:Context,
                                 _ host: String, _ port: Int,
        _ carrierName: String) throws ->Bool
    //InterruptedException, IOException, ExecutionException
    {
        // Location Verification (Blocking, or use verifyLocationFuture):
      let verifyRequest:DistributedMatchEngine_VerifyLocationRequest? =
        try mMatchingEngine.createVerifyLocationRequest(ctx, carrierName, location)!;
        if (verifyRequest != nil)
        {
            let verifiedLocation:DistributedMatchEngine_VerifyLocationReply? =
                try mMatchingEngine.verifyLocation(verifyRequest!, host, port, 10000);
            
            someText = "[Location Verified: Tower: \(verifiedLocation!.towerStatus), GPS LocationStatus: \( verifiedLocation!.gpsLocationStatus) , Location Accuracy: \(verifiedLocation!.gpsLocationAccuracyKm)  ]\n";

            if (mMatchingEngineResultsListener != nil) {
                mMatchingEngineResultsListener!.onVerifyLocation(verifiedLocation!.gpsLocationStatus,
                                                                 verifiedLocation!.gpsLocationAccuracyKm);
            }
        } else {
            someText = "Cannot create VerifyLocationRequest object.\n";
         //   Log.e(TAG, someText);
            Swift.print("\(someText)")
          //  Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            
            let alert = UIAlertController(title: someText  , message: "-", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))   // "Settings"

            let window:UIWindow = UIApplication.shared.keyWindow!
            let vc: UIViewController  = window.rootViewController!  //  Get its rootViewController:  // JT 18.11.06
            
            vc.present(alert, animated: true)   // JT 18.11.06
            
            return false;
        }
        return true;
    }

    /**
     * Utility method to create a toast on the UI thread.
     *
     * @param message
     * @param length
     */
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
        
        SKToast.show(withMessage: message)  // JT 18.11.02

    }
    
    func  getMatchingEngine() ->MatchingEngine
    {
        return mMatchingEngine;
    }
    
    func  setMatchingEngine( matchingEngine: MatchingEngine) {
        mMatchingEngine = matchingEngine;
    }
    
    func  getmMatchingEngineResultsListener() ->MatchingEngineResultsListener?
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
