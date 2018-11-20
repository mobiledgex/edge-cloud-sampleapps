//package com.mobiledgex.matchingengine;
//
//import android.util.Log;
//
//import java.io.IOException;
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
//import distributed_match_engine.AppClient;
//import distributed_match_engine.AppClient.GetLocationRequest;
//import distributed_match_engine.AppClient.GetLocationReply;
//import distributed_match_engine.Match_Engine_ApiGrpc;
//
//import io.grpc.ManagedChannel;
//import io.grpc.StatusRuntimeException;

// JT 18.10.27

public class GetLocation
    //implements Callable
{
    public static let  TAG:String = "GetLocation";
    
    private var mMatchingEngine:MatchingEngine?
    private var mRequest: DistributedMatchEngine_GetLocationRequest?
    
    private var mHost: String?
    private var mPort: Int = 0
    private var mTimeoutInMilliseconds:Int64 = -1;
    
  //  init()  {}
    
     init(_ matchingEngine: MatchingEngine) // JT 18.11.02
     {
        mMatchingEngine = matchingEngine;
    }
    
     func setRequest(_ request:DistributedMatchEngine_GetLocationRequest?,
                           _ host: String?,  _ port:Int,  _ timeoutInMilliseconds:Int64) throws ->Bool{
        if (request == nil) {
            throw  MyError.IllegalArgumentException("Request object must not be nil.");
        } else if (!mMatchingEngine!.isMexLocationAllowed()) {
         //   Log.e(TAG, "Mex Location is disabled.");
            Swift.print("Mex Location is disabled.")
            mRequest = nil;
            return false;
        }
        
        if (host == nil || host!.equals("")) {
            return false;
        }
        
        mRequest = request;
        mHost = host;
        mPort = port;
        
        if (timeoutInMilliseconds <= 0) {
            throw  MyError.IllegalArgumentException("GetLocation() timeout must be positive.");
        }
        mTimeoutInMilliseconds = timeoutInMilliseconds;
        return true;
    }
    
     func  call() throws
  //  throws MissingRequestException, StatusRuntimeException, InterruptedException, ExecutionException
        ->DistributedMatchEngine_GetLocationReply
    {
        if (mRequest == nil) {
            throw  MyError.MissingRequestException("Usage error: GetLocation does not have a request object to make location verification call!");
        }
        
        var reply:DistributedMatchEngine_GetLocationReply?
        var channel:ManagedChannel? = nil;  // JT 18.10.31 GRPCChannel
       // var nm:NetworkManager = nil;
        
        do {
            channel = try mMatchingEngine!.channelPicker(mHost!, mPort);
            let stub:Match_Engine_ApiGrpc.Match_Engine_ApiBlockingStub = Match_Engine_ApiGrpc.newBlockingStub(channel);
            
         //   nm = mMatchingEngine.getNetworkManager();
         //   nm.switchToCellularInternetNetworkBlocking();
            
            reply = stub.withDeadlineAfter(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                .getLocation(mRequest);
            
            // Nothing a sdk user can do below but read the exception cause:
        } catch ( mkse: MyError.MexKeyStoreException) {
            throw  MyError.ExecutionException("Exception calling GetLocation: ");
        } catch ( mtse: MyError.MexTrustStoreException) {
            throw  MyError.MexTrustStoreException("Exception calling GetLocation: ");
        } catch ( kme: MyError.KeyManagementException) {
            throw  MyError.KeyManagementException("Exception calling GetLocation: ");
        } catch ( nsa: MyError.NoSuchAlgorithmException) {
            throw  MyError.NoSuchAlgorithmException("Exception calling GetLocation: ");
        } catch ( ioe: MyError.IOException) {
            throw  MyError.IOException("Exception calling GetLocation: ");
        }
        defer {
            if (channel != nil) {
                channel!.shutdown();
                channel!.awaitTermination(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS);
            }
//            if (nm != nil) {
//                //nm.resetNetworkToDefault();
//            }
        }
        mRequest = nil;
        
        var ver:Int = 0
        if (reply != nil) {
            ver = Int(reply!.ver)
            //      Log.d(TAG, "Version of GetLocationReply: " + ver);
            Swift.print("Version of GetLocationReply: \(ver)")
        }
        
        mMatchingEngine!.setGetLocationReply(reply!);
        return reply!;
    }
}

