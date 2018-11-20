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
//import distributed_match_engine.AppClient.DynamicLocGroupRequest;
//import distributed_match_engine.AppClient.DynamicLocGroupReply;
//import distributed_match_engine.Match_Engine_ApiGrpc;
//import io.grpc.ManagedChannel;
//import io.grpc.StatusRuntimeException;

    // JT 18.10.27
public class AddUserToGroup
//implements Callable
{
    public static let  TAG:String = "AddUserToGroup";

    private var mMatchingEngine:MatchingEngine?

    private var mRequest: DistributedMatchEngine_DynamicLocGroupRequest?
    private var mHost: String?
    private var mPort: Int = 0
    private var mTimeoutInMilliseconds:Int64 = -1;

    init() {}   // JT 18.10.29
    
    public func AddUserToGroup(_ matchingEngine: MatchingEngine) {
        mMatchingEngine = matchingEngine;
    }

     func setRequest(_ request:DistributedMatchEngine_DynamicLocGroupRequest?,
                           _ host: String?,  _ port:Int,  _ timeoutInMilliseconds:Int64) throws ->Bool
    {
        
        if (request == nil) {
            throw  MyError.IllegalArgumentException("Request object must not be nil.");
        } else if (!mMatchingEngine!.isMexLocationAllowed()) {
            //Log.e(TAG, "Mex MatchEngine is disabled.");
            Swift.print("Mex MatchEngine is disabled.")
            mRequest = nil;
            
            return false;
        }
        if (host == nil || host!.equals("")) {
            return false;
        }
        mRequest = request!;
        mHost = host;
        mPort = port;

        if (timeoutInMilliseconds <= 0) {
            throw  MyError.IllegalArgumentException("timeout must be positive.");
        }
        mTimeoutInMilliseconds = timeoutInMilliseconds;
        return true;
    }

      func call() throws
           // throws MissingRequestException, StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_DynamicLocGroupReply
    {
        if (mRequest == nil) {
            throw  MyError.MissingRequestException("Usage error: AddUserToGroup does not have a request object to use MatchEngine!");
        }

        var reply:DistributedMatchEngine_DynamicLocGroupReply?
        var channel:ManagedChannel? = nil  //GRPCChannel//ManagedChannel? = nil;    // JT 18.10.31
        ///var nm:NetworkManager = nil;
        do {
            channel = try mMatchingEngine!.channelPicker(mHost!, mPort) as! ManagedChannel;
            let stub:Match_Engine_ApiGrpc.Match_Engine_ApiBlockingStub = Match_Engine_ApiGrpc.newBlockingStub(channel);

            ///nm = mMatchingEngine.getNetworkManager();
           /// nm.switchToCellularInternetNetworkBlocking();
///
            reply = stub.withDeadlineAfter(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                    .addUserToGroup(mRequest);

            // Nothing a sdk user can do below but read the exception cause:
        } catch ( mkse: MyError.MexKeyStoreException) {
            throw  MyError.MexKeyStoreException("Exception calling AddUserToGroup: ");
        } catch ( mtse: MyError.MexTrustStoreException) {
            throw  MyError.MexKeyStoreException("Exception calling AddUserToGroup: ");
        } catch ( kme: MyError.KeyManagementException) {
            throw  MyError.KeyManagementException("Exception calling AddUserToGroup: ");
        } catch ( nsa: MyError.NoSuchAlgorithmException) {
            throw  MyError.NoSuchAlgorithmException("Exception calling AddUserToGroup: ");
        } catch ( ioe: MyError.IOException) {
            throw  MyError.IOException("Exception calling AddUserToGroup: ");
        }
        
        defer{
            if (channel != nil) {
                channel!.shutdown();
              //  try
                channel!.awaitTermination(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS);
            }
//            if (nm != nil) {
//                nm.resetNetworkToDefault();
//            }
        }

        mMatchingEngine!.setDynamicLocGroupReply(reply!);
        return reply!;
    }
}
