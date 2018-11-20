//package com.mobiledgex.matchingengine;
//
//import android.util.Log;
//
//import com.squareup.okhttp.Headers;
//import com.squareup.okhttp.HttpUrl;
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;
//import com.squareup.okhttp.Response;
//
//import java.io.IOException;
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
//import distributed_match_engine.AppClient;
//import distributed_match_engine.AppClient.VerifyLocationRequest;
//import distributed_match_engine.AppClient.VerifyLocationReply;
//import distributed_match_engine.Match_Engine_ApiGrpc;
//import io.grpc.ManagedChannel;
//import io.grpc.StatusRuntimeException;

import  Foundation

public class VerifyLocation
//implements Callable
{
    public static let TAG:String = "VerifyLocation";

    private var mMatchingEngine:MatchingEngine?
    private var mRequest: DistributedMatchEngine_VerifyLocationRequest?  //VerifyLocationRequest
   private var mHost: String? = ""
    private var mPort: Int = 0
    private var mTimeoutInMilliseconds:Int64 = -1;

    init () // JT 18.10.23
    {        
    }
    
    func VerifyLocation(_ matchingEngine: MatchingEngine) {
        mMatchingEngine = matchingEngine;
    }


     func setRequest(_ request:DistributedMatchEngine_VerifyLocationRequest?,
                           _ host: String?,
                           _ port:Int, _ timeoutInMilliseconds:Int64) throws ->Bool
    {
        if (request == nil) {
         //   throw  IllegalArgumentException("Request object must not be nil.");
            let exception = NSException(   name: .invalidArgumentException, reason: "Request object must not be nil.",
                userInfo: nil
            )
            
            exception.raise()
            
        } else if (!mMatchingEngine!.isMexLocationAllowed())    // JT 18.10.28
        {
            //Log.e(TAG, "Mex Location is disabled.");
            Swift.print("Mex Location is disabled.")
            mRequest = nil;
            return false;
        }

        if (host == nil || host == "") {
            return false;
        }
        mRequest = request;
        mHost = host;
        mPort = port;

        if (timeoutInMilliseconds <= 0) {
           // throw  IllegalArgumentException("VerifyLocation timeout must be positive.");
            let exception = NSException(   name: .invalidArgumentException,
                                           reason: "VerifyLocation timeout must be positive.",
                                           userInfo: nil
            )
            
            exception.raise()
            
        }
        mTimeoutInMilliseconds = timeoutInMilliseconds;
        return true;
    }

    private func getToken() throws ->String
    //throws IOException
        
    {
        var token:String = ""
        // ??? todo?
        //        OkHttpClient httpClient =  OkHttpClient();
        //        httpClient.setFollowSslRedirects(false);
        //        httpClient.setFollowRedirects(false);
        //
        //    let request:Request =  Request.Builder()
        //                .url(mMatchingEngine.getTokenServerURI())
        //                .build();
        //
        //        Response response = httpClient.newCall(request).execute();
        //        if (!response.isRedirect()) {
        //            throw  IllegalStateException("Expected a redirect!");
        //        } else {
        //            Headers headers = response.headers();
        //            String locationHeaderUrl = headers.get("Location");
        //            if (locationHeaderUrl == nil) {
        //                throw  IllegalStateException("Required Location Header Missing.");
        //            }
        //            HttpUrl url = HttpUrl.parse(locationHeaderUrl);
        //            token = url.queryParameter("dt-id");
        //            if (token == nil) {
        //                throw  IllegalStateException("Required Token ID Missinng");
        //            }
        //        }
        
        
   //     let request:Request =  Request.Builder()
   //         .url(mMatchingEngine.getTokenServerURI())
   //         .build();
    //    request.httpMethod = "GET"
        
        
            
     
        let url = URL(string: mMatchingEngine!.getTokenServerURI())    // JT 18.11.06
            let request = NSMutableURLRequest(url:url!);  // JT 18.11.06
            request.httpMethod = "GET"  // JT 18.11.06
            
        
          URLSession.shared.dataTask(with: request as URLRequest, completionHandler:
        { data, response, error -> Void in
            do {
                
                
//                if (!response.isRedirect()) {
//                    //throw  IllegalStateException("Expected a redirect!");
//                    let exception = NSException(   name: .invalidArgumentException, reason: "Expected a redirect!",  userInfo: nil
//                    )
//
//                    exception.raise()
//                } else
                    if true
                {
                  //  let headers:Headers = response.headers();
                 //   let headers = response.headers();
                    
                    if let headers = response?.allHeaderFields as? [String: String]
                    {
                        let locationHeaderUrl:String? = headers["Location"]
                        if (locationHeaderUrl == nil) {
                            throw  MyError.illegalStateException("Required Location Header Missing.");
                        }
                        let url:HttpUrl = HttpUrl.parse(locationHeaderUrl);
                        token = url.queryParameter("dt-id");
                        if (token == nil) {
                            //throw  IllegalStateException("Required Token ID Missinng");
                            throw  MyError.illegalStateException("Required Token ID Missinng");
                        }
                    }
 
                }
            } catch {
                print("JSON Serialization error")
            }
        }).resume()
        
        return token;
    }

    private func addTokenToRequest( _ token: String) ->DistributedMatchEngine_VerifyLocationRequest
    {
        var tokenizedRequest:DistributedMatchEngine_VerifyLocationRequest = DistributedMatchEngine_VerifyLocationRequest()
        
//        DistributedMatchEngine_VerifyLocationRequest.newBuilder()   // JT 18.10.28 build pattern
//                .setVer(mRequest.ver)
//                .setSessionCookie(mRequest.getSessionCookie())
//                .setCarrierName(mRequest.getCarrierName())
//                .setGpsLocation(mRequest.getGpsLocation())
//                .setVerifyLocToken(token)
//                .build();   // AppClient.
        
        tokenizedRequest.ver = (mRequest?.ver)!
        tokenizedRequest.sessionCookie = mRequest!.sessionCookie
        tokenizedRequest.carrierName = mRequest!.carrierName
        tokenizedRequest.gpsLocation = mRequest!.gpsLocation
        tokenizedRequest.verifyLocToken = token

        return tokenizedRequest;
    }

     func call() throws
        //    throws MissingRequestException, StatusRuntimeException,
        //           IOException, InterruptedException, ExecutionException
    ->DistributedMatchEngine_VerifyLocationReply?    // VerifyLocationReply
    {
        if (mRequest == nil) {
           // throw  MissingRequestException("Usage error: VerifyLocation does not have a request object to make location verification call!");
            
            throw MyError.MissingRequestException("Usage error: VerifyLocation does not have a request object to make location verification call!")

        }
        var grpcRequest: DistributedMatchEngine_VerifyLocationRequest?

        // Make One time use of HTTP Request to Token Server:
//        let nm:NetworkManager = mMatchingEngine.getNetworkManager();
//        nm.switchToCellularInternetNetworkBlocking();

        var token:String = try getToken(); // This token is short lived.
        grpcRequest = addTokenToRequest(token);

        
        var reply:DistributedMatchEngine_VerifyLocationReply?   // JT 18.10.28
        var channel:ManagedChannel? = nil  //GRPCChannel//ManagedChannel? = nil;    // JT 18.10.31
        do {
            channel = try mMatchingEngine!.channelPicker(mHost!, mPort);

            let stub:Match_Engine_ApiGrpc.Match_Engine_ApiBlockingStub = Match_Engine_ApiGrpc.newBlockingStub(channel);


            reply = stub.withDeadlineAfter(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                    .verifyLocation(grpcRequest);

            // Nothing a sdk user can do below but read the exception cause:
    } catch ( mkse: MyError.MexKeyStoreException) {
  //          throw  ExecutionException("Exception calling VerifyLocation: ", mkse);
        
        throw  MyError.MexTrustStoreException("Exception calling VerifyLocation: "); // JT 18.10.28

    } catch ( mtse: MyError.MexTrustStoreException) {
        //    throw  ExecutionException("Exception calling VerifyLocation: ", mtse);
        throw  MyError.MexTrustStoreException("Exception calling VerifyLocation: "); // JT 18.10.28
    } catch ( kme: MyError.KeyManagementException) {
            throw  MyError.KeyManagementException("Exception calling VerifyLocation: ");
        throw  MyError.KeyManagementException("Exception calling VerifyLocation: "); // JT 18.10.28
    } catch ( nsa: MyError.NoSuchAlgorithmException) {
            throw  MyError.NoSuchAlgorithmException("Exception calling VerifyLocation: ");
    } catch ( ioe: MyError.IOException) {
            throw  MyError.IOException("Exception calling VerifyLocation: ");
        }
    defer {
            if (channel != nil) {
                channel!.shutdown();
                channel?.awaitTermination(mTimeoutInMilliseconds, TimeUnit.MILLISECONDS);
            }
           // nm.resetNetworkToDefault();
        }
        mRequest = nil;

        // FIXME: Reply TBD.
    var ver:Int = -1;
        if (reply != nil) {
            ver = Int(reply!.ver);    // JT 18.10.28
        //    Log.d(TAG, "Version of VerifyLocationReply: " + ver);
    Swift.print("Version of VerifyLocationReply: \(ver)")
        }

        mMatchingEngine!.setTokenServerToken(token);
        mMatchingEngine!.setVerifyLocationReply(reply!);
    
        return reply;
    }
}

enum MyError: Error // JT
{
  //  case illegalStateException(String)
 //   case missingRequestException(String)
 //   case mexKeyStoreException(String)
  //  case IllegalArgumentException(String)
    
    case IllegalArgumentException(String)
    case MexKeyStoreException(String)
    case KeyManagementException(String)
    case NoSuchAlgorithmException(String)
   
    case MexTrustStoreException(String)
    case IOException(String)

    case ExecutionException(String)
    case MissingRequestException(String)

    case InvalidKeySpecException(String)
    case KeyStoreException(String)
    case UnsupportedOperationException(String)
    
    case StatusRuntimeException(String) // JT 18.11.02
    case InterruptedException(String) // JT 18.11.02
 

    case SecurityException(String)  // JT 18.11.05

}
