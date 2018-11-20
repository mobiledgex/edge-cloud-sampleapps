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
//import distributed_match_engine.Match_Engine_ApiGrpc;
//import io.grpc.ManagedChannel;
//import io.grpc.StatusRuntimeException;

public class RegisterClient
//implements Callable   // JT 18.10.27 todo ?
{
    public static let TAG:String = "RegisterClient";
    public static let SESSION_COOKIE_KEY:String = "session_cookie";
    public static let TOKEN_SERVER_URI_KEY = "token_server_u_r_i";

    private var mMatchingEngine:MatchingEngine?
    private var mRequest: DistributedMatchEngine_RegisterClientRequest? //AppClient.RegisterClientRequest;
    private var mHost:String? = nil;
   private var  mPort:Int = 0
    private var mTimeoutInMilliseconds:Int64 = -1;

        init()
        {
            
    }
    
    func RegisterClient(_ matchingEngine: MatchingEngine) {
        mMatchingEngine = matchingEngine;
    }

     func  setRequest(_ request: DistributedMatchEngine_RegisterClientRequest? ,
    _ host: String?,   _ port: Int,
    _ timeoutInMilliseconds: Int64) throws ->Bool
{
        if (request == nil) {
          //  throw  IllegalArgumentException("Request object must not be nil.");
            throw  MyError.IllegalArgumentException("Request object must not be nil."); // JT 18.10.28

        } else if (!mMatchingEngine!.isMexLocationAllowed()) {
            //Log.e(TAG, "Mex Location is disabled.");
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
          //  throw  IllegalArgumentException("RegisterClient() timeout must be positive.");
            throw  MyError.IllegalArgumentException("RegisterClient() timeout must be positive."); // JT 18.10.28
      }
        mTimeoutInMilliseconds = timeoutInMilliseconds;
        return true;
    }

    private func isBoundToCellNetwork() {

    }

     func  call() throws
    //throws MissingRequestException, StatusRuntimeException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_RegisterClientReply
    {
        if (mRequest == nil) {
           // throw  MissingRequestException("Usage error: RegisterClient() does not have a request object to make call!");
            
            throw  MyError.MissingRequestException("Usage error: RegisterClient() does not have a request object to make call!"); // JT 18.10.28
        }

        var channel:ManagedChannel? = nil  //GRPCChannel//ManagedChannel? = nil;    // JT 18.10.31
     var reply: DistributedMatchEngine_RegisterClientReply? = nil
        ///        var nm:NetworkManager? = nil;
        do {
            channel = try mMatchingEngine!.channelPicker(mHost!, mPort);
//            let stub:Match_Engine_ApiGrpc.Match_Engine_ApiBlockingStub = Match_Engine_ApiGrpc.newBlockingStub(channel);

///            nm = mMatchingEngine.getNetworkManager();
///            nm.switchToCellularInternetNetworkBlocking();   // JT 18.10.29 ??? todo

           reply = stub.withDeadlineAfter(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                   .registerClient(mRequest);

     ///       reply = try mMatchingEngine!.registerClient(Context(), mRequest, mTimeoutInMilliseconds);   // JT 18.10.30
            // Nothing a sdk user can do below but read the exception cause:
        } catch ( mkse: MyError.MexKeyStoreException) {
            //throw  ExecutionException("Exception calling RegisterClient: ", mkse);
            throw MyError.MexKeyStoreException("Exception calling RegisterClient: ")
        } catch ( mtse: MyError.MexKeyStoreException) {
           // throw  ExecutionException("Exception calling RegisterClient: ", mtse);
            throw MyError.MexKeyStoreException("Exception calling RegisterClient: ")
     } catch ( kme: MyError.KeyManagementException) {
           // throw  ExecutionException("Exception calling RegisterClient: ", kme);
        throw MyError.MexKeyStoreException("Exception calling RegisterClient: ")
      } catch ( nsa: MyError.NoSuchAlgorithmException) {
         //   throw  ExecutionException("Exception calling RegisterClient: ", nsa);
        throw MyError.NoSuchAlgorithmException("Exception calling RegisterClient: ")
       } catch ( ioe: MyError.IOException) {
           // throw  ExecutionException("Exception calling RegisterClient: ", ioe);
            throw MyError.IOException("Exception calling RegisterClient: ")
    }   //  finally block is always executed whether exception is handled or not.
        //finally
            defer {
            if (channel != nil) {
                channel!.shutdown();
                channel!.awaitTermination(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS);
            }
//            if (nm != nil) {
//                nm.resetNetworkToDefault();   // JT 18.10.29 todo?
//            }
        }
        mRequest = nil;
        mHost = nil;
        mPort = 0;

        var ver:Int = 0
        if (reply != nil) {
            ver = Int(reply!.ver)
          //  Log.d(TAG, "Version of Match_Engine_Status: " + ver);
            Swift.print("Version of Match_Engine_Status: \(ver)")
        }
        var sessionCookie: String = String()
        
        /// URI for Token Server
        var tokenServerUri: String = String()
        mMatchingEngine!.setSessionCookie( reply!.sessionCookie)
        mMatchingEngine!.setTokenServerURI(   reply!.tokenServerUri)

        mMatchingEngine!.setMatchEngineStatus(reply!);

        return reply!;
    }
    
    func genericGRPCCall(_ request: DistributedMatchEngine_RegisterClientRequest, _ host:String)   // JT 18.11.07
    {
        let method = GRPCProtoMethod(package: "grpc.testing", service: "TestService", method: "UnaryCall")!
        
        let requestsWriter = GRXWriter(writerWithContainer: [request])  // JT 18.11.07
        
        let call = GRPCCall(host: host, path: method.httpPath, requestsWriter: requestsWriter)!
        
       // call.requestHeaders["My-Header"] = "My value"
        
        call.start(with: GRXWriteable { response, error in
            if let response = response as? Data {
                NSLog("3. Received response:\n\(try! RMTSimpleResponse(data: response))")
            } else {
                NSLog("3. Finished with error: \(error!)")
            }
            NSLog("3. Response headers: \(call.responseHeaders)")
            NSLog("3. Response trailers: \(call.responseTrailers)")
        })
    }
}
