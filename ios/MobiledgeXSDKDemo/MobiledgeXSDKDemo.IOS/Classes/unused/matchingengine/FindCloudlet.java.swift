//package com.mobiledgex.matchingengine;
//
//import android.util.Log;
//
//import java.io.IOException;
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.Callable;
//
//import distributed_match_engine.AppClient;
//import distributed_match_engine.AppClient.FindCloudletRequest;
//import distributed_match_engine.Match_Engine_ApiGrpc;
//
//import io.grpc.ManagedChannel;
//import io.grpc.StatusRuntimeException;

public class FindCloudlet
//implements Callable
{
    public static let TAG:String = "FindCloudlet";

    private var mMatchingEngine:MatchingEngine?
    private var mRequest: DistributedMatchEngine_FindCloudletRequest? // Singleton.
    private var mHost:String
    private var mPort = 0
    private var mTimeoutInMilliseconds:Int64 = -1;

    init()  {  }
    
    public func FindCloudlet(_ matchingEngine: MatchingEngine) {
        mMatchingEngine = matchingEngine;
    }

     func setRequest(_ request:DistributedMatchEngine_FindCloudletRequest?,
    _ host: String?,  _ port:Int,
    _ timeoutInMilliseconds:Int64) throws ->Bool
{
        if (request == nil) {
            throw  MyError.IllegalArgumentException("Request object must not be nil."); // JT 18.10.29
        } else if (!mMatchingEngine!.isMexLocationAllowed()) {
          //  Log.e(TAG, "Mex Location is disabled.");
            Swift.print("Mex Location is disabled.")
            mRequest = nil;
            return false;
        }

    if (host == nil || host!.equals("")) {
            return false;
        }
        mRequest = request;
    mHost = host!;
        mPort = port;

        if (timeoutInMilliseconds <= 0) {
            throw  MyError.IllegalArgumentException("VerifyLocation timeout must be positive.");
        }
        mTimeoutInMilliseconds = timeoutInMilliseconds;
        return true;
    }

     func call() throws
         //   throws MissingRequestException, StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_FindCloudletReply
    {
        if (mRequest == nil) {
            throw  MyError.MissingRequestException("Usage error: FindCloudlet does not have a request object to use MatchEngine!");
        }

        var reply: DistributedMatchEngine_FindCloudletReply
        var channel:ManagedChannel?  = nil; // GRPCChannel
    //    var nm:NetworkManager = nil;
        do {
            channel = try mMatchingEngine!.channelPicker(mHost, mPort);
            let stub:Match_Engine_ApiGrpc.Match_Engine_ApiBlockingStub = Match_Engine_ApiGrpc.newBlockingStub(channel);


          //  nm = mMatchingEngine.getNetworkManager();
         //   nm.switchToCellularInternetNetworkBlocking();

            reply = stub.withDeadlineAfter(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                    .findCloudlet(mRequest);
            // Nothing a sdk user can do below but read the exception cause:
        } catch ( mkse: MyError.MexKeyStoreException) {
            throw  MyError.MexKeyStoreException("Exception calling FindCloudlet: ");
        } catch ( mtse: MyError.MexTrustStoreException) {
            throw  MyError.MexTrustStoreException("Exception calling FindCloudlet: ");
        } catch ( kme: MyError.KeyManagementException) {
            throw  MyError.KeyManagementException("Exception calling FindCloudlet: ");
        } catch ( nsa: MyError.NoSuchAlgorithmException) {
            throw  MyError.NoSuchAlgorithmException("Exception calling FindCloudlet: ");
        } catch ( ioe: MyError.IOException) {
            throw  MyError.IOException("Exception calling FindCloudlet: ");
        }
        
        defer {
            if (channel != nil) {
                channel!.shutdown();
                channel!.awaitTermination(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS);
            }
//            if (nm != nil) {
//               // nm.resetNetworkToDefault();
//            }
        }

        // Let MatchingEngine know of the latest cookie.
        mMatchingEngine!.setFindCloudletResponse(reply);
        
        return reply;
    }
}
