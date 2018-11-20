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
//import distributed_match_engine.AppClient.AppInstListRequest;
//import distributed_match_engine.AppClient.AppInstListReply;
//import distributed_match_engine.Match_Engine_ApiGrpc;
//import io.grpc.ManagedChannel;
//import io.grpc.StatusRuntimeException;

public class GetAppInstList
//implements Callable
{
    public static let TAG:String = "GetLocation"

    private var mMatchingEngine:MatchingEngine?
    private var mRequest: DistributedMatchEngine_AppInstListRequest?
    private var mHost: String?
    private var mPort: Int = 0
    private var mTimeoutInMilliseconds:Int64 = -1;

    func GetAppInstList(_ matchingEngine: MatchingEngine) {
        mMatchingEngine = matchingEngine;
    }

     func setRequest(_  request: DistributedMatchEngine_AppInstListRequest?,
                           _ host: String?,  _ port:Int,  _ timeoutInMilliseconds:Int64) throws ->Bool
    {
        if (request == nil) {
            throw  MyError.IllegalArgumentException("Request object must not be nil.");
        } else if (!mMatchingEngine!.isMexLocationAllowed()) {
           // Log.e(TAG, "Mex Location is disabled.");
            Swift.print("Mex Location is disabled.")
            mRequest = nil;
            return false;
        }

        if (host == nil || host!.equals("")) {
            return false;
        }
        mHost = host;
        mPort = port;
        mRequest = request;

        if (timeoutInMilliseconds <= 0) {
            throw  MyError.IllegalArgumentException("GetCloudletList() timeout must be positive.");
        }
        mTimeoutInMilliseconds = timeoutInMilliseconds;
        return true;
    }

     func call() throws
           // throws MissingRequestException, StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_AppInstListReply
    {
        if (mRequest == nil) {
            throw  MyError.MissingRequestException("Usage error: GetCloudletList does not have a request object!");
        }

    var reply:DistributedMatchEngine_AppInstListReply?

    var channel:ManagedChannel? = nil; // JT 18.10.30 GRPCChannel
    //var nm:NetworkManager = nil;
        do {
            channel = try mMatchingEngine!.channelPicker(mHost!, mPort);
            let stub:  Match_Engine_ApiGrpc.Match_Engine_ApiBlockingStub = Match_Engine_ApiGrpc.newBlockingStub(channel);

           // nm = mMatchingEngine.getNetworkManager();
           // nm.switchToCellularInternetNetworkBlocking();

            reply = stub.withDeadlineAfter(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                    .getAppInstList(mRequest);



            // Nothing a sdk user can do below but read the exception cause:
        } catch ( mkse: MyError.MexKeyStoreException) {
            throw  MyError.MexKeyStoreException("Exception calling GetCloudletList: ");
        } catch ( mtse: MyError.MexTrustStoreException) {
            throw  MyError.MexTrustStoreException("Exception calling GetCloudletList: ");
        } catch ( kme: MyError.KeyManagementException) {
            throw  MyError.KeyManagementException("Exception calling GetCloudletList: ");
        } catch ( nsa: MyError.NoSuchAlgorithmException) {
            throw  MyError.NoSuchAlgorithmException("Exception calling GetCloudletList: ");
        } catch ( ioe: MyError.IOException) {
            throw  MyError.IOException("Exception calling GetCloudletList: ");
        }
    defer{
            if (channel != nil) {
                channel.shutdown();
                channel.awaitTermination(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS);
            }
//            if (nm != nil) {
//                nm.resetNetworkToDefault();
//            }
        }
        mRequest = nil;

        var ver:Int = 0
        if (reply != nil) {
            ver = Int(reply!.ver)
           // Log.d(TAG, "Version of AppInstListReply: " + ver);
       
            Swift.print("Version of AppInstListReply: \(ver) ")
        }

        return reply!;
    }
}
