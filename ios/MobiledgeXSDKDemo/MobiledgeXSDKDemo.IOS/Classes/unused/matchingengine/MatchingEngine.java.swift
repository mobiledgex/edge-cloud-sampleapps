//package com.mobiledgex.matchingengine;
//
//import android.content.Context;
//import android.content.pm.ApplicationInfo;
//import android.content.pm.PackageManager;
//import android.location.Location;
//import android.net.ConnectivityManager;
//import android.net.wifi.WifiManager;
//import android.provider.Settings;
//import android.support.annotation.NonNull;
//import android.support.annotation.RequiresApi;
//import android.telephony.CarrierConfigManager;
//import android.telephony.NeighboringCellInfo;
//import android.telephony.TelephonyManager;
//
//import com.google.protobuf.ByteString;
//import com.mobiledgex.matchingengine.util.OkHttpSSLChannelHelper;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.security.KeyManagementException;
//import java.security.KeyStoreException;
//import java.security.NoSuchAlgorithmException;
//import java.security.PrivateKey;
//import java.security.spec.InvalidKeySpecException;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//
//import distributed_match_engine.AppClient;
//import distributed_match_engine.AppClient.RegisterClientRequest;
//import distributed_match_engine.AppClient.RegisterClientReply;
//import distributed_match_engine.AppClient.VerifyLocationRequest;
//import distributed_match_engine.AppClient.VerifyLocationReply;
//import distributed_match_engine.AppClient.FindCloudletRequest;
//import distributed_match_engine.AppClient.FindCloudletReply;
//import distributed_match_engine.AppClient.GetLocationRequest;
//import distributed_match_engine.AppClient.GetLocationReply;
//import distributed_match_engine.AppClient.AppInstListRequest;
//import distributed_match_engine.AppClient.AppInstListReply;
//
//import distributed_match_engine.AppClient.DynamicLocGroupRequest;
//import distributed_match_engine.AppClient.DynamicLocGroupReply;
//
//import distributed_match_engine.LocOuterClass;
//import distributed_match_engine.LocOuterClass.Loc;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//import io.grpc.StatusRuntimeException;
//import io.grpc.okhttp.OkHttpChannelBuilder;
//
//import android.content.pm.PackageInfo;
//import android.util.Log;
//
//import javax.net.ssl.SSLSocketFactory;
//


import Foundation   // JT 18.10.27
import CoreLocation // JT 18.10.28
import CoreTelephony    // JT 18.10.29
//import SSLService
//import GRPC // JT 18.10.29
//import grpc // JT 18.10.29
//import CGRPC    // JT 18.10.30

class Context   // JT 18.10.29
{
    
}
// TODO: GRPC (which needs http/2).
public class MatchingEngine
{
    public let  TAG:String = "MatchingEngine";  // JT 18.10.27
    private let mInitialDMEContactHost:String = "tdg.dme.mobiledgex.net";
    private var host:String? // = mInitialDMEContactHost;
    private var mNetworkManager:NetworkManager   // JT 18.10.27 todo
    private var port:Int = 50051;

    // A threadpool for all the MatchEngine API callable interfaces:
//    final ExecutorService threadpool; // JT 18.10.27 todo

    // State info for engine
    private var mUUID: UUID;
    private var mSessionCookie:String;
    private var mTokenServerURI:String;
    private var mTokenServerToken:String;

    private var mRegisterClientReply: DistributedMatchEngine_RegisterClientReply?    // JT 18.10.28
    private var mFindCloudletReply: DistributedMatchEngine_FindCloudletReply?
    private var mVerifyLocationReply: DistributedMatchEngine_VerifyLocationReply?
    private var mGetLocationReply: DistributedMatchEngine_GetLocationReply
    private var mDynamicLocGroupReply: DistributedMatchEngine_DynamicLocGroupReply

    private var mMatchEngineLocation: CLLocationCoordinate2D?

    private var isSSLEnabled:Bool = true;
    private var mMutualAuthSocketFactory: SSLSocketFactory
 //   private var mySSLConfig: SSLService.Configuration // JT 18.10.29

    private var mContext: Context // JT 18.10.27 todo
   var theHost:GRPCHost   // JT 18.10.30

    init (_ context:Context) // JT 18.10.23
    {
      //  let withCredential: Context = Context.current().withValue(CRED_KEY, cred);   // JT 18.10.28
        mContext = context;

         host = mInitialDMEContactHost  // JT 18.10.29
        theHost =  GRPCHost(address: host!)!
    }
    
    func testttt()
    {
        //let test = GRPCCall.useInsecureConnectionsForHost("")    // JT 18.10.27

    }

//     func MatchingEngine(_ context:Context)
//    {
////        threadpool = Executors.newSingleThreadExecutor();
////        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
////        mNetworkManager = NetworkManager.getInstance(connectivityManager);
//        mContext = context;
//    }
//     func MatchingEngine(_ context:Context,  executorService: ExecutorService)
//    {
//        //threadpool = executorService;
////        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
////        mNetworkManager = NetworkManager.getInstance(connectivityManager, threadpool);
//        mContext = context;
//    }

    // Application state Bundle Key.
    public let  MEX_LOCATION_PERMISSION:String = "MEX_LOCATION_PERMISSION";
    static var mMexLocationAllowed:Bool = false    // JT 18.11.02

    public  func isMexLocationAllowed() ->Bool {
        return MatchingEngine.mMexLocationAllowed
    }

    public func setMexLocationAllowed(_ allowMexLocation:Bool) {
        MatchingEngine.mMexLocationAllowed = allowMexLocation;
    }

//    public func isNetworkSwitchingEnabled() ->Bool
//    {
//        return self.getNetworkManager().isNetworkSwitchingEnabled();
//    }   // JT 18.10.29 todo
//
//    public func setNetworkSwitchingEnabled( networkSwitchingEnabled:Bool) {
//        self.getNetworkManager().setNetworkSwitchingEnabled(networkSwitchingEnabled);
//    }   // JT 18.10.29 todo?

    /**
     * Check if Roaming Data is enabled on the System.
     * @return
     */
     func isRoamingDataEanbled(_ context: Context) ->Bool
     {
        var enabled = false;
//        do {
//            enabled = android.provider.Settings.Global.getInt(context.getContentResolver(), Settings.Global.DATA_ROAMING) == 1;
//        } catch (snfe: Settings.SettingNotFoundException ) {
//            //Log.i(TAG, "android.provider.Settings.Global.DATA_ROAMING key is not present!");
//            Swift.print("android.provider.Settings.Global.DATA_ROAMING key is not present!")
//            return false; // Unavailable setting.
//        }

        return enabled;
    }

//    public func submit( task: Callable) ->Future    // JT 18.10.29 todo
//    {
//        return threadpool.submit(task);
//    }

    public func getUUID() ->UUID {
        return mUUID;
    }

    public func setUUID(_ uuid: UUID) {
        mUUID = uuid;
    }

    public func createUUID()  ->UUID {
        return UUID()    // UUID.randomUUID();   // JT 18.10.28
    }

    func setSessionCookie(_ sessionCookie: String) {
        self.mSessionCookie = sessionCookie;
    }
    func getSessionCookie() ->String
    {
        return self.mSessionCookie;
    }

    func setMatchEngineStatus(_ status: DistributedMatchEngine_RegisterClientReply) // AppClient.RegisterClientReply)
    {
        mRegisterClientReply = status;
    }

    func setGetLocationReply(_ locationReply: DistributedMatchEngine_GetLocationReply)  //GetLocationReply)
    {
        mGetLocationReply = locationReply;

        let loc = CLLocationCoordinate2D(latitude: locationReply.networkLocation.lat, longitude:locationReply.networkLocation.long)
        mMatchEngineLocation = loc //locationReply.networkLocation    // JT 18.10.28 todo DistributedMatchEngine_Loc
    }

    func setVerifyLocationReply(_ locationVerify: DistributedMatchEngine_VerifyLocationReply)
    {
        mVerifyLocationReply = locationVerify;
    }

    func setFindCloudletResponse(_ reply: DistributedMatchEngine_FindCloudletReply)
    {
        mFindCloudletReply = reply;
    }

    func setDynamicLocGroupReply(_ reply: DistributedMatchEngine_DynamicLocGroupReply)
    {
        mDynamicLocGroupReply = reply;
    }
    /**
     * Utility method retrieves current network CarrierName from system service.
     * @param context
     * @return
     */
     func retrieveNetworkCarrierName(_ context: Context) ->String
{
    return getCarrierName!   // JT 18.10.29
    
//    let  telManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE); // (TelephonyManager)
//    let networkOperatorName:String? = telManager.getNetworkOperatorName();
//        if (networkOperatorName == nil) {
//            //Log.w(TAG, "Network Carrier name is not found on device.");
//            Swift.print("Network Carrier name is not found on device.")
//        }
//        return networkOperatorName;
    }

    public func generateDmeHostAddress(_ networkOperatorName: String?) ->String
    {
        var host:String;

        if (networkOperatorName == nil || networkOperatorName == "") {
            host = mInitialDMEContactHost;
            return host;
        }

        host = networkOperatorName! + ".dme.mobiledgex.net";
        return host;
    }

    func getNetworkManager() ->NetworkManager
    {
        return mNetworkManager;
    }

    func setNetworkManager(_ networkManager: NetworkManager) {
        mNetworkManager = networkManager;
    }

     func getAppName(_ context: Context) ->String {
        var applicationName: String = Bundle.main.infoDictionary![kCFBundleNameKey as String] as! String;   // JT 18.10.29
        
        return applicationName
        // App
//        let appInfo: ApplicationInfo = context.getApplicationInfo();
//        var packageLabel:String = "";
//        if (context.getPackageManager() != nil) {
//            let seq:String? = appInfo.loadLabel(context.getPackageManager()); // CharSequence
//            if (seq != nil) {
//                packageLabel = seq!  //.toString();
//            }
//        }
//        var appName:String?
//        if (applicationName == nil || applicationName == "") {
//            appName = packageLabel;
//        } else {
//            appName = applicationName;
//        }
//        return appName;
    }

     func createRegisterClientRequest(_ context: Context?, _ developerName: String,
                                            _ applicationName: String, _ appVersion: String) throws
        ->DistributedMatchEngine_RegisterClientRequest?
    {
        if (!MatchingEngine.mMexLocationAllowed)
        {
          //  Log.d(TAG, "Create Request disabled. Matching engine is not configured to allow use.");
            Swift.print("Create Request disabled. Matching engine is not configured to allow use.")
            return nil;
        }
        
        if (context == nil)
        {
            throw  MyError.IllegalArgumentException("MatchingEngine requires a working application context.");
        }
        // App
//        let appInfo:ApplicationInfo = context.getApplicationInfo();
//        var packageLabel:String = "";
//        if (context.getPackageManager() != nil) {
//            let seq:String = appInfo.loadLabel(context.getPackageManager()); //CharSequence
//            if (seq != nil) {
//                packageLabel = seq.toString();
//            }
//        }
//        var pInfo: PackageInfo?
        let versionName:String =          (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String)!

        let appName: String = Bundle.main.infoDictionary![kCFBundleNameKey as String] as! String;   // JT 18.10.29
//        if (applicationName == nil || applicationName.equals("")) {
//            appName = packageLabel;
//        } else {
//            appName = applicationName;
//        }


//        do {
//            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//            versionName = (appVersion == nil) ? pInfo.versionName : appVersion;
//        } catch (nfe: PackageManager.NameNotFoundException ) {
//           // nfe.printStackTrace();
//            // Hard stop, or continue?
//        }
//        if(developerName == nil || developerName.equals("")) {
//            developerName = packageLabel; // From signing certificate?
//        }

        
        var result = DistributedMatchEngine_RegisterClientRequest()

        result.devName = developerName
        result.appName = appName
        result.appVers = versionName
        
        return result

//        return AppClient.RegisterClientRequest.newBuilder()
//            .setDevName(developerName)
//            .setAppName(appName)
//            .setAppVers(versionName)
//            .build();
    }

     func createVerifyLocationRequest(_ context: Context?, _ carrierName: String?,
                                            _ location: CLLocationCoordinate2D?) throws ->DistributedMatchEngine_VerifyLocationRequest?
    {
        if (context == nil) {
            throw  MyError.IllegalArgumentException("MatchingEngine requires a working application context.");
        }

        if (!MatchingEngine.mMexLocationAllowed)    // JT 18.11.02
        {
           // Log.d(TAG, "Create Request disabled. Matching engine is not configured to allow use.");
            Swift.print("Create Request disabled. Matching engine is not configured to allow use.")
            return nil;
        }

        if (location == nil) {
            throw MyError.IllegalArgumentException("Location parameter is required.");
        }

        let retrievedNetworkOperatorName: String = retrieveNetworkCarrierName(context!);
        
        var myCarrierName =  ""
        if(carrierName == nil || carrierName!.equals("")) {
            myCarrierName = retrievedNetworkOperatorName;
        }
        
        var aLoc:DistributedMatchEngine_Loc = DistributedMatchEngine_Loc()  // JT 18.10.29
        aLoc.lat = location!.latitude
        aLoc.long = location!.longitude


        var result = DistributedMatchEngine_VerifyLocationRequest() // JT 18.10.29
        
        result.sessionCookie = mSessionCookie
        result.carrierName = myCarrierName
        result.gpsLocation = aLoc
        
        return result
        
//        return AppClient.VerifyLocationRequest.newBuilder()
//                .setSessionCookie(mSessionCookie)
//                .setCarrierName(carrierName)
//                .setGpsLocation(aLoc) // Latest token is unknown until retrieved.
//                .build();
    }

     func createFindCloudletRequest(_ context: Context?, _ carrierName: String?,
                                          _ location: CLLocationCoordinate2D) throws
        ->DistributedMatchEngine_FindCloudletRequest?
    {
        if (!MatchingEngine.mMexLocationAllowed)    // JT 18.11.02
        {
         //   Log.d(TAG, "Create Request disabled. Matching engine is not configured to allow use.");
            Swift.print("Create Request disabled. Matching engine is not configured to allow use.")
            return nil;
        }
        if (context == nil) {
            throw  MyError.IllegalArgumentException("MatchingEngine requires a working application context.");
        }

  //      let aLoc:CLLocationCoordinate2D = location  //androidLocToMexLoc(location);

        var aLoc:DistributedMatchEngine_Loc = DistributedMatchEngine_Loc()  // JT 18.10.29
        aLoc.lat = location.latitude
        aLoc.long = location.longitude

        
        var result = DistributedMatchEngine_FindCloudletRequest()
        
        
        result.sessionCookie = mSessionCookie
        result.carrierName =  (carrierName == nil || (carrierName?.equals(""))!)
            ? retrieveNetworkCarrierName(context!) : carrierName!
        result.gpsLocation = aLoc
        
        return result

//        return FindCloudletRequest.newBuilder()
//                .setSessionCookie(mSessionCookie)
//                .setCarrierName(
//                        (carrierName == nil || carrierName.equals(""))
//                            ? retrieveNetworkCarrierName(context) : carrierName
//                )
//                .setGpsLocation(aLoc)
//                .build();
    }

     func createGetLocationRequest(_ context: Context?, _ carrierName: String?) throws ->DistributedMatchEngine_GetLocationRequest?
    {
        if (!MatchingEngine.mMexLocationAllowed)    // JT 18.11.02
        {
          //  Log.d(TAG, "Create Request disabled. Matching engine is not configured to allow use.");
            Swift.print("Create Request disabled. Matching engine is not configured to allow use.")
            return nil;
        }
        if (context == nil) {
            throw  MyError.IllegalArgumentException("MatchingEngine requires a working application context.");
        }

        var result = DistributedMatchEngine_GetLocationRequest()
        
        
        result.sessionCookie = mSessionCookie
        result.carrierName =  (carrierName == nil || carrierName!.equals(""))
            ? retrieveNetworkCarrierName(context!) : carrierName!
        
        let result2: DistributedMatchEngine_GetLocationRequest? = result
        
        return result2
        
//        return GetLocationRequest.newBuilder()
//                .setSessionCookie(mSessionCookie)
//                .setCarrierName(
//                        (carrierName == nil || carrierName.equals(""))
//                            ? retrieveNetworkCarrierName(context) : carrierName
//
//                )
//                .build();
    }

     func createAppInstListRequest(_ context: Context?, _ carrierName: String?,
                                         _ location: CLLocationCoordinate2D?) throws ->DistributedMatchEngine_AppInstListRequest?
    {
        if (!MatchingEngine.mMexLocationAllowed) {
           // Log.d(TAG, "Create Request disabled. Matching engine is not configured to allow use.");
            Swift.print("Create Request disabled. Matching engine is not configured to allow use.")
            return nil;
        }
        if (context == nil) {
            throw  MyError.IllegalArgumentException("MatchingEngine requires a working application context.");
        }


        if (location == nil) {
            throw  MyError.IllegalArgumentException("Location parameter is required.");
        }

        let retrievedNetworkOperatorName: String = retrieveNetworkCarrierName(context!);
        
        var myCarrierName =  ""

        if(carrierName == nil || carrierName!.equals("")) {
            myCarrierName = retrievedNetworkOperatorName;
        }
       // let aLoc:CLLocationCoordinate2D = location  //androidLocToMexLoc(location);
        var aLoc:DistributedMatchEngine_Loc = DistributedMatchEngine_Loc()  // JT 18.10.29
        aLoc.lat = location!.latitude
        aLoc.long = location!.longitude

        var result = DistributedMatchEngine_AppInstListRequest() // JT 18.10.29
        
        result.sessionCookie = mSessionCookie
        result.carrierName = myCarrierName
        result.gpsLocation = aLoc
        
        return result
        
//        return AppClient.AppInstListRequest.newBuilder()
//                .setSessionCookie(mSessionCookie)
//                .setCarrierName(carrierName)
//                .setGpsLocation(aLoc)
//                .build();
    }
    
     func createDynamicLocGroupRequest(_ context: Context?,
                                             _ commType: DistributedMatchEngine_DynamicLocGroupRequest.DlgCommType?,
        _ userData: String) throws ->DistributedMatchEngine_DynamicLocGroupRequest?
    {
        if (!MatchingEngine.mMexLocationAllowed) {
           // Log.d(TAG, "Create Request disabled. Matching engine is not configured to allow use.");
            Swift.print("Create Request disabled. Matching engine is not configured to allow use.")
            return nil;
        }
        if (context == nil) {
            throw  MyError.IllegalArgumentException("MatchingEngine requires a working application context.");
        }
        var commType2: DistributedMatchEngine_DynamicLocGroupRequest.DlgCommType
        if (commType == nil || commType == DistributedMatchEngine_DynamicLocGroupRequest.DlgCommType.dlgUndefined) {
            commType2 = DistributedMatchEngine_DynamicLocGroupRequest.DlgCommType.dlgSecure;
        }
        else {
            commType2 = commType!
        }

        var result = DistributedMatchEngine_DynamicLocGroupRequest() // JT 18.10.29
        
        result.sessionCookie = mSessionCookie
        result.lgID = UInt64(1001)
        result.commType = commType2
       result.userData = userData == nil ? "" : userData
        
        return result
        
//        return DynamicLocGroupRequest.newBuilder()
//                .setSessionCookie(mSessionCookie)
//                .setLgId(Int64(1001)) // FIXME: NOT IMPLEMENTED
//                .setCommType(commType)
//                .setUserData(userData == nil ? "" : userData)
//                .build();
    }

        // JT 18.10.28 todo
//    private Loc androidLocToMexLoc(android.location.Location loc) {
//        return Loc.newBuilder()
//                .setLat((loc == nil) ? 0.0d : loc.getLatitude())
//                .setLong((loc == nil) ? 0.0d : loc.getLongitude())
//                .setHorizontalAccuracy((loc == nil) ? 0.0d :loc.getAccuracy())
//                //.setVerticalAccuracy(loc.getVerticalAccuracyMeters()) // API Level 26 required.
//                .setVerticalAccuracy(0)
//                .setAltitude((loc == nil) ? 0.0d : loc.getAltitude())
//                .setCourse((loc == nil) ? 0.0d : loc.getBearing())
//                .setSpeed((loc == nil) ? 0.0d : loc.getSpeed())
//                .build();
//    }

    /**
     * Registers Client using blocking API call.
     * @param request
     * @param timeoutInMilliseconds
     * @return
     * @throws StatusRuntimeException
     * @throws InterruptedException
     * @throws ExecutionException
     */
     func registerClient(_ context: Context,
                               _ request: DistributedMatchEngine_RegisterClientRequest?,
                               _ timeoutInMilliseconds: Int64) throws
           // throws StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_RegisterClientReply
    {
        let carrierName:String = retrieveNetworkCarrierName(context);
        return try registerClient(request!, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * Registers Client using blocking API call. Allows specifying a DME host and port.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return
     * @throws StatusRuntimeException
     */
     func registerClient(_ request: DistributedMatchEngine_RegisterClientRequest,
                               _ host: String, _ port: Int,
                                             _ timeoutInMilliseconds: Int64) throws
      //      throws StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_RegisterClientReply
    {
        let registerClient:RegisterClient =  RegisterClient(); // Instanced, so just add host, port as field.
        try registerClient.setRequest(request, host, port, timeoutInMilliseconds);
        
        return try registerClient.call();
    }

     func  registerClientFuture(_ context: Context,
    _ request: DistributedMatchEngine_RegisterClientRequest,
    _ timeoutInMilliseconds: Int64) throws -> Future<DistributedMatchEngine_RegisterClientReply>
    {
        let carrierName: String = retrieveNetworkCarrierName(context);
    
        return try registerClientFuture(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * Registers device on the MatchingEngine server. Returns a Future.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return
     */
    
     func registerClientFuture(_ request: DistributedMatchEngine_RegisterClientRequest?,
    _ host: String?, _ port: Int,
    _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_RegisterClientReply>
    {
        let registerClient:RegisterClient =  RegisterClient();
        try registerClient.setRequest(request, host, port, timeoutInMilliseconds);
    
       // return submit(registerClient);
        
        // submit
        let p = Promise<DistributedMatchEngine_RegisterClientReply>()   // JT 18.10.29

       DispatchQueue.global(qos: DispatchQoS.QoSClass.userInitiated).async
        {
            do
            {
                let reply = try registerClient.call()   // JT 18.10.29
                p.completeWithSuccess(reply)
            }
            catch
            {
                Swift.print("registerClient failed")
            }
        }
        return p.future
    }

    /**
     * findCloudlet finds the closest cloudlet instance as per request.
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     * @throws StatusRuntimeException
     * @throws InterruptedException
     * @throws ExecutionException
     */
     func findCloudlet(_ context: Context,
                             _ request: DistributedMatchEngine_FindCloudletRequest?,
                             _ timeoutInMilliseconds: Int64) throws
           // throws StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_FindCloudletReply
    {
        let carrierName: String = retrieveNetworkCarrierName(context);
        
        return try findCloudlet(request!, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * findCloudlet finds the closest cloudlet instance as per request.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return cloudlet URI.
     * @throws StatusRuntimeException
     */
     func findCloudlet(_ request: DistributedMatchEngine_FindCloudletRequest,
                             _ host: String, _ port: Int,
                             _ timeoutInMilliseconds: Int64) throws
          //  throws StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_FindCloudletReply
    {
        let findCloudlet: FindCloudlet =  FindCloudlet();
        try findCloudlet.setRequest(request, host, port, timeoutInMilliseconds);
        
        return try findCloudlet.call();
    }


    /**
     * findCloudlet finds the closest cloudlet instance as per request. Returns a Future.
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     */
     func  findCloudletFuture(_ context: Context,
    _ request: DistributedMatchEngine_FindCloudletRequest,
                                          _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_FindCloudletReply>
    {
        let carrierName: String = retrieveNetworkCarrierName(context);
        
        return try findCloudletFuture(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }

    /**
     * findCloudletFuture finds the closest cloudlet instance as per request. Returns a Future.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return cloudlet URI Future.
     */
     func  findCloudletFuture(_ request: DistributedMatchEngine_FindCloudletRequest,
                                    _ host: String, _ port: Int,
                                    _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_FindCloudletReply>
    {
        let findCloudlet:FindCloudlet =  FindCloudlet();
        try findCloudlet.setRequest(request, host, port, timeoutInMilliseconds);
        
        //return submit(findCloudlet);    // JT 18.10.29
        
        // submit
        let p = Promise<DistributedMatchEngine_FindCloudletReply>()   // JT 18.10.29
        
        DispatchQueue.global(qos: .background).async
            {
                do
                {
                    let reply = try findCloudlet.call()   // JT 18.10.29
                    p.completeWithSuccess(reply)
                }
                catch
                {
                    Swift.print("registerClient failed")
                }
        }
        return p.future
    }


    /**
     * verifyLocationFuture validates the client submitted information against known network
     * parameters on the subscriber network side.
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     * @throws StatusRuntimeException
     * @throws InterruptedException
     * @throws IOException
     * @throws ExecutionException
     */
     func verifyLocation(_ context: Context,
                               _ request: DistributedMatchEngine_VerifyLocationRequest,
                                             _ timeoutInMilliseconds: Int64) throws
            //throws StatusRuntimeException, InterruptedException, IOException, ExecutionException
    ->DistributedMatchEngine_VerifyLocationReply
    {
        let carrierName: String = retrieveNetworkCarrierName(context);
        
        return try verifyLocation(request, generateDmeHostAddress(carrierName),
                                  getPort(),
                                  timeoutInMilliseconds);
    }
    /**
     * verifyLocationFuture validates the client submitted information against known network
     * parameters on the subscriber network side.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return boolean validated or not.
     * @throws StatusRuntimeException
     * @throws InterruptedException
     * @throws IOException
     */
     func verifyLocation(_ request: DistributedMatchEngine_VerifyLocationRequest,
                                              _ host: String, _ port: Int,
                                              _ timeoutInMilliseconds: Int64) throws
         //   throws StatusRuntimeException, InterruptedException, IOException, ExecutionException
    ->DistributedMatchEngine_VerifyLocationReply
    {
        let verifyLocation: VerifyLocation =  VerifyLocation();
       try  verifyLocation.setRequest(request, host, port, timeoutInMilliseconds);
        
        return try verifyLocation.call()!;
    }

    /**
     * verifyLocationFuture validates the client submitted information against known network
     * parameters on the subscriber network side. Returns a future.
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     */
     func  verifyLocationFuture(_ context: Context,
                                      _ request: DistributedMatchEngine_VerifyLocationRequest,
                                      _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_VerifyLocationReply>
    {
        let carrierName: String = retrieveNetworkCarrierName(context);
        
        return try verifyLocationFuture(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * verifyLocationFuture validates the client submitted information against known network
     * parameters on the subscriber network side. Returns a future.
     * @param request
     * @return Future<Boolean> validated or not.
     */
     func  verifyLocationFuture(_ request: DistributedMatchEngine_VerifyLocationRequest,
                                                            _ host: String, _ port: Int,
                                                            _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_VerifyLocationReply>
    {
        let verifyLocation:VerifyLocation =  VerifyLocation();
       try verifyLocation.setRequest(request, host, port, timeoutInMilliseconds);
    
     //   return submit(verifyLocation);    // JT 18.10.29
        
        let p = Promise<DistributedMatchEngine_VerifyLocationReply>()   // JT 18.10.29
        
        DispatchQueue.global(qos: .background).async
            {
                do
                {
                    let reply = try verifyLocation.call()   // JT 18.10.29
                    p.completeWithSuccess(reply!)
                }
                catch
                {
                    Swift.print("verifyLocation failed")
                }
        }
        return p.future
    }

    /**
     * getLocation returns the network verified location of this device.
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     * @throws StatusRuntimeException
     * @throws InterruptedException
     * @throws ExecutionException
     */
     func getLocation(_ context: Context,
                            _ request: DistributedMatchEngine_GetLocationRequest,
                                        _ timeoutInMilliseconds: Int64) throws
         ///   throws StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_GetLocationReply
    {
        let carrierName:String = retrieveNetworkCarrierName(context);
        
        return try getLocation(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * getLocation returns the network verified location of this device.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return
     * @throws StatusRuntimeException
     */
     func  getLocation(_ request: DistributedMatchEngine_GetLocationRequest,
                                        _ host: String, _ port: Int,
                                        _ timeoutInMilliseconds: Int64) throws
          //  throws StatusRuntimeException, InterruptedException, ExecutionException
        ->DistributedMatchEngine_GetLocationReply
    {
        let getLocation:GetLocation =  GetLocation(self)    // JT 18.11.06
        try getLocation.setRequest(request, host, port, timeoutInMilliseconds);
        
        return try getLocation.call();
    }

    /**
     * getLocation returns the network verified location of this device. Returns a Future.
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     */
     func getLocationFuture(_ context: Context,
                                  _ request: DistributedMatchEngine_GetLocationRequest,
                                                      _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_GetLocationReply>
    {
        let carrierName: String = retrieveNetworkCarrierName(context);
        
        return try getLocationFuture(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * getLocation returns the network verified location of this device. Returns a Future.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return
     */
     func  getLocationFuture(_ request: DistributedMatchEngine_GetLocationRequest,
                                                      _ host: String, _ port: Int,
                                                      _ timeoutInMilliseconds: Int64) throws
    ->Future<DistributedMatchEngine_GetLocationReply>
    {
        let getLocation: GetLocation =  GetLocation(self)   // JT 18.11.06
        try getLocation.setRequest(request, host, port, timeoutInMilliseconds);
        
       // return submit(getLocation);   // JT 18.10.29
        
        let p = Promise<DistributedMatchEngine_GetLocationReply>()   // JT 18.10.29
        
        DispatchQueue.global(qos: .background).async
            {
                do
                {
                    let reply = try getLocation.call()   // JT 18.10.29
                    p.completeWithSuccess(reply)
                }
                catch
                {
                    Swift.print("verifyLocation failed")
                }
        }
        return p.future
    }


    /**
     * addUserToGroup is a blocking call.
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
     func  addUserToGroup(_ context: Context,
                                _ request: DistributedMatchEngine_DynamicLocGroupRequest,
                                               _ timeoutInMilliseconds: Int64) throws
            //throws InterruptedException, ExecutionException
    ->DistributedMatchEngine_DynamicLocGroupReply
    {
        let carrierName: String = retrieveNetworkCarrierName(context);
        
        return try addUserToGroup(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * addUserToGroup is a blocking call.
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return
     */
     func  addUserToGroup(_ request: DistributedMatchEngine_DynamicLocGroupRequest,
                                               _ host: String, _ port: Int,
                                               _ timeoutInMilliseconds: Int64) throws
          //  throws InterruptedException, ExecutionException
    ->DistributedMatchEngine_DynamicLocGroupReply
    {
        let addUserToGroup: AddUserToGroup =  AddUserToGroup();
        try addUserToGroup.setRequest(request, host, port, timeoutInMilliseconds);
        return try addUserToGroup.call();
    }

    /**
     * addUserToGroupFuture
     * @param context
     * @param request
     * @param timeoutInMilliseconds
     * @return
     */
     func addUserToGroupFuture(_ context: Context,
                                     _ request: DistributedMatchEngine_DynamicLocGroupRequest,
                                                             _ timeoutInMilliseconds: Int64) throws
    -> Future<DistributedMatchEngine_DynamicLocGroupReply>
    {
    let carrierName:String = retrieveNetworkCarrierName(context);
    
    return try addUserToGroupFuture(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * addUserToGroupFuture
     * @param request
     * @param host Distributed Matching Engine hostname
     * @param port Distributed Matching Engine port
     * @param timeoutInMilliseconds
     * @return
     */
     func  addUserToGroupFuture(_ request: DistributedMatchEngine_DynamicLocGroupRequest,
                                                             _ host: String, _ port: Int,
                                                             _ timeoutInMilliseconds: Int64) throws
    ->Future<DistributedMatchEngine_DynamicLocGroupReply>
    {
        let addUserToGroup: AddUserToGroup =  AddUserToGroup();
        try addUserToGroup.setRequest(request, host, port, timeoutInMilliseconds);
        
        //return submit(addUserToGroup);    // JT 18.10.29
        
        let p = Promise<DistributedMatchEngine_DynamicLocGroupReply>()   // JT 18.10.29
        
        DispatchQueue.global(qos: .background).async
            {
                do
                {
                    let reply = try addUserToGroup.call()   // JT 18.10.29
                    p.completeWithSuccess(reply)
                }
                catch
                {
                    Swift.print("verifyLocation failed")
                }
        }
        return p.future
    }

    /**
     * Retrieve nearby AppInsts for registered application. This is a blocking call.
     * @param request
     * @param timeoutInMilliseconds
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
     func  getAppInstList(_ request: DistributedMatchEngine_AppInstListRequest,
                                _ timeoutInMilliseconds: Int64) throws
        //throws InterruptedException, ExecutionException
        ->DistributedMatchEngine_AppInstListReply
    {
        let carrierName: String = request.carrierName;
        
        return try getAppInstList(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }

    /**
     * Retrieve nearby AppInsts for registered application. This is a blocking call.
     * @param request
     * @param timeoutInMilliseconds
     * @return
     */
     func  getAppInstList(_ request: DistributedMatchEngine_AppInstListRequest,
                                _ host: String, _ port: Int,
                                _ timeoutInMilliseconds: Int64) throws
        ->DistributedMatchEngine_AppInstListReply
        //           throws InterruptedException, ExecutionException
    {
        let getAppInstList: GetAppInstList =  GetAppInstList();
        try getAppInstList.setRequest(request, host, port, timeoutInMilliseconds);
        
        return try getAppInstList.call();
    }


    /**
     * Retrieve nearby AppInsts for registered application. Returns a Future.
     * @param request
     * @param timeoutInMilliseconds
     * @return
     */
     func getAppInstListFuture(_ request: DistributedMatchEngine_AppInstListRequest,
                                     _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_AppInstListReply>
    {
        
        let carrierName: String = request.carrierName;
        
        return try getAppInstListFuture(request, generateDmeHostAddress(carrierName), getPort(), timeoutInMilliseconds);
    }
    /**
     * Retrieve nearby AppInsts for registered application. Returns a Future.
     * @param request
     * @param host
     * @param port
     * @param timeoutInMilliseconds
     * @return
     */
     func  getAppInstListFuture(_ request: DistributedMatchEngine_AppInstListRequest,
                                      _ host: String, _ port: Int,
                                      _ timeoutInMilliseconds: Int64) throws
        ->Future<DistributedMatchEngine_AppInstListReply>
     {
        let getAppInstList: GetAppInstList =  GetAppInstList();
        try getAppInstList.setRequest(request, host, port, timeoutInMilliseconds);
        
        //return submit(getAppInstList);
        
        let p = Promise<DistributedMatchEngine_AppInstListReply>()   // JT 18.10.29
        
        DispatchQueue.global(qos: .background).async
        {
                do
                {
                    let reply = try getAppInstList.call()   // JT 18.10.29
                    p.completeWithSuccess(reply)
                }
                catch
                {
                    Swift.print("verifyLocation failed")
                }
        }
        return p.future
    }

    //
    // Network Wrappers:
    //

    /**
     * Returns if the bound Data Network for application is currently roaming or not.
     * @return
     */
  //  @RequiresApi(api = android.os.Build.VERSION_CODES.P)
//    public func isRoamingData() ->Bool    // JT 18.10.29 todo?
//    {
//        return mNetworkManager.isRoamingData();
//    }

    /**
     * Returns whether Wifi is enabled on the system or not, independent of Application's network state.
     * @param context
     * @return
     */
//    public func isWiFiEnabled(_ context: Context) -> Bool
//    {
//    let wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE);
//
//        return wifiManager.isWifiEnabled();
//    }

    func isWifiEnabled() -> Bool    // JT 18.10.29
    {
        print("Function: \(#function), line: \(#line)") // JT 18.10.29

        var addresses = [String]()
        
        var ifaddr : UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return false }
        guard let firstAddr = ifaddr else { return false }
        
        for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
            addresses.append(String(cString: ptr.pointee.ifa_name))
        }
        
        var counts:[String:Int] = [:]
        
        for item in addresses {
            counts[item] = (counts[item] ?? 0) + 1
        }
        
        freeifaddrs(ifaddr)
        guard let count = counts["awdl0"] else { return false }
        return count > 1
    }
    
    /**
     * Checks if the Carrier + Phone combination supports WiFiCalling. This does not return whether it is enabled.
     * If under roaming conditions, WiFi Calling may disable cellular network data interfaces needed by
     * cellular data only Distributed Matching Engine and Cloudlet network operations.
     *
     * @return
     */
        // JT 18.10.29 todo?
//    public func isWiFiCallingSupported(_ carrierConfigManager: CarrierConfigManager) ->Bool
//    {
//        return mNetworkManager.isWiFiCallingSupported(carrierConfigManager);
//    }


    public func getHost() ->String {
        return host!;
    }

    public func setHost( host: String) {
        self.host = host;
    }

    public func getPort() ->Int {
        return port;
    }

    public func setPort( port:Int) {
        self.port = port;
    }

    func setTokenServerURI(_ tokenFollowURI: String) {
        mTokenServerURI = tokenFollowURI;
    }

    func getTokenServerURI() ->String {
        return mTokenServerURI;
    }

    func setTokenServerToken(_ token: String) {
        mTokenServerToken = token;
    }

     func getTokenServerToken() ->String{
        return mTokenServerToken;
    }


//    public func isSSLEnabled() -> Bool {
//        return isSSLEnabled;
//    }

    public func setSSLEnabled(_ SSLEnabled: Bool) {
        isSSLEnabled = SSLEnabled;
    }

    public func getMutualAuthSSLSocketFactoryInstance() throws
//            throws IOException, InvalidKeySpecException, MexKeyStoreException, MexTrustStoreException,
 //               KeyManagementException, KeyStoreException, NoSuchAlgorithmException
    ->SSLSocketFactory
    {
        if (mMutualAuthSocketFactory != nil) {
            return mMutualAuthSocketFactory;
        }

        // FIXME: Temporary. This is NOT the right place to put the CA, cert and key.
        // First, copy asset files to local storage
     //   let outputDir: String = mContext.getFilesDir().getAbsolutePath();   // JT 18.10.29 todo
        
                let outputDir = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]    // JT 18.10.29
        
   //     OkHttpSSLChannelHelper.copyAssets(mContext, "mexcerts", outputDir); // JT 18.10.29 todo?

   //     let trustCaFilePath: String = outputDir + "/mex-ca.crt";
    //    let clientCertFilePath: String = outputDir + "/mex-client.crt";
    //    let privateKeyFilePath: String = outputDir + "/mex-client.key";

        
        let trustCaFilePath: String  = Bundle.main.path(forResource: "/mex-ca", ofType: "crt")! // CA Certficate 
        let clientCertFilePath: String  = Bundle.main.path(forResource: "/mex-client", ofType: "crt")! // Application certificate (certificateFilePath)
        let privateKeyFilePath: String  = Bundle.main.path(forResource: "/mex-client", ofType: "key")! // Private Key file (keyFilePath)


        // theHost =  GRPCHost(address: host!)
        
        theHost.setTLSPEMRootCerts(trustCaFilePath, withPrivateKey:privateKeyFilePath, withCertChain:nil)   // JT 18.10.30

//        var  trustCAFis: FileInputStream? = nil;
//        var  clientCertFis: FileInputStream? = nil;
//        let privateKey: PrivateKey = OkHttpSSLChannelHelper.getPrivateKey(privateKeyFilePath);
//
//        do {
//            trustCAFis =  FileInputStream(trustCaFilePath);
//            clientCertFis =  FileInputStream(clientCertFilePath);
//
//            mMutualAuthSocketFactory = OkHttpSSLChannelHelper.getMutualAuthSSLSocketFactory(
//                    trustCAFis,
//                    clientCertFis,
//                    privateKey);
//        }
//        defer {
//            if (trustCAFis != nil) {
//                trustCAFis.close();
//            }
//            if (clientCertFis != nil) {
//                clientCertFis.close();
//            }
//        }

        return mMutualAuthSocketFactory;
    }

    /**
     * Helper function to return a channel that handles SSL Mutual Authentication,
     * or returns a more basic ManagedChannelBuilder.
     * @param host
     * @param port
     * @return
     * @throws MexKeyStoreException
     * @throws MexTrustStoreException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    func channelPicker(_ host: String, _ port: Int) throws
            //throws IOException, MexKeyStoreException, MexTrustStoreException, KeyManagementException, NoSuchAlgorithmException
    ->ManagedChannel    //GRPCChannel      //ManagedChannel    // JT 18.10.29  // JT 18.10.30
    {

        do {
            if (isSSLEnabled) {
//                return OkHttpChannelBuilder
//                        .forAddress(host, port)
//                        .sslSocketFactory(getMutualAuthSSLSocketFactoryInstance())
//                        .build();
                


                let ch =  GRPCChannel.secureChannel(withHost: host,
                                                            credentials: theHost.channelCreds!,
                //(nonnull struct grpc_channel_credentials *)credentials
                    channelArgs:nil) as! ManagedChannel  // JT 18.10.31 todo ! ???
                
                return ch   // JT 18.10.30
          //      getMutualAuthSSLSocketFactoryInstance() // JT 18.10.30 sets mySSLConfig
         //       return SSLSocket(to: host, port: port)  // JT 18.10.29
                
            } else {
//                return ManagedChannelBuilder
//                        .forAddress(host, port)
//                        .usePlaintext()
//                        .build();
            }
        } catch ( ikse: MyError.InvalidKeySpecException) {
            throw  MyError.MexKeyStoreException("InvalidKeystore: ");
        } catch ( kse: MyError.KeyStoreException) {
            throw  MyError.MexKeyStoreException("MexKeyStoreException: ");
        }
    }
    
 

}

extension String    // JT 18.10.28
{
    func equals(_ s: String) -> Bool
    {
        return self == s
    }
}

var getCarrierName:String? {
    let networkInfo = CTTelephonyNetworkInfo()
    let carrier = networkInfo.subscriberCellularProvider
   
    return carrier?.carrierName
}

