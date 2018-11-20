//#include <curl/curl.h>
//
//#include <sstream>
//#include <iostream>
//
//#include <nlohmann/json.hpp>
//
//#include <algorithm>
//
//#include "test_credentials.hpp"

//using namespace std;
//using namespace std::chrono;
//using namespace nlohmann;
//import Wings    // JT 18.11.07


class MexRestClient 
{
    let carrierNameDefault :String  = "tdg2"
    let baseDmeHost :String  = "dme.mobiledgex.net"

    // API Paths:
    let registerAPI :String = "/v1/registerclient"
    let verifylocationAPI :String = "/v1/verifylocation"
    let findcloudletAPI :String = "/v1/findcloudlet"
    let getlocatiyonAPI :String = "/v1/getlocation"
    let appinstlistAPI :String = "/v1/getappinstlist"
    let dynamiclocgroupAPI :String = "/v1/dynamiclocgroup"

    let timeoutSec : UInt64 = 5000
    let dmePort: UInt  = 38001
    let appName = "EmptyMatchEngineApp"; // Your application name
    let devName = "EmptyMatchEngineApp"; // Your developer name
    let appVersionStr = "1.0";

    // SSL files:

    let CaCertFile = "mex-ca.crt";
    let ClientCertFile = "mex-client.crt";
    let ClientPrivKey = "mex-client.key";

    init() {}   // JT 18.11.07

    // Retrieve the carrier name of the cellular network interface.
    func getCarrierName() ->String
    {
        return carrierNameDefault;
    }

    func generateDmeHostPath(_ carrierName: String) ->String
    {
        if (carrierName == "")
        {
            return carrierNameDefault + "." + baseDmeHost
        }
        return carrierName + "." + baseDmeHost
    }

    func generateBaseUri(_ carrierName: String, _ port: UInt) ->String
    {
//        stringstream ss;
//        ss << "https://" << generateDmeHostPath(carrierName) << ":" << dmePort;
//
//        return ss.str();

        return "https://\(generateDmeHostPath(carrierName)):\(dmePort)"  // JT 18.11.07
    }

   // json currentGoogleTimestamp()
    func currentGoogleTimestamp() ->[String:Double]  // JT 18.11.07
  {
        // Google's protobuf timestamp format.
 //       let microseconds = std::chrono::system_clock::now().time_since_epoch();
 //       let ts_sec = duration_cast<std::chrono::milliseconds>(microseconds);
  //      let ts_ns = duration_cast<std::chrono::nanoseconds>(microseconds);

        let secs = Date().timeIntervalSince1970 // JT 18.11.07
     //   json googleTimestamp;
        var googleTimestamp = [String:Double]()    // JT 18.11.07
//        googleTimestamp["seconds"] = ts_sec.count();
//        googleTimestamp["nanos"] = ts_sec.count();

    googleTimestamp["seconds"] = secs   // JT 18.11.07
    googleTimestamp["nanos"] = secs

        return googleTimestamp;
    }

    // A C++ GPS location provider/binding is needed here.
    func retrieveLocation() ->[String:Any?]
    {
      //  json location;
    
    var location = [String:Any?]()    // JT 18.11.07

        location["lat"] = -122.149349;
        location["long"] = 37.459609;
        location["horizontal_accuracy"] = 5;
        location["vertical_accuracy"] = 20;
        location["altitude"] = 100;
        location["course"] = 0;
        location["speed"] = 2;
        location["timestamp"] = "2008-09-08T22:47:31-07:00"; // currentGoogleTimestamp();

        return location;
    }

    func createRegisterClientRequest() ->[String:String]
    {
     //   json regClientRequest;
    var regClientRequest = [String:String]()    // JT 18.11.07

        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = appName
        regClientRequest["DevName"] = devName
        regClientRequest["AppVers"] = appVersionStr

        return regClientRequest;
    }

    // Carrier name can change depending on cell tower.
  //  json createVerifyLocationRequest(const string &carrierName, const json gpslocation, const string verifyloctoken)
    func createVerifyLocationRequest( _ carrierName: String,
    _ gpslocation : [String:String], _ verifyloctoken: String)
    -> [String:Any?]
    {
       // json verifyLocationRequest;
        var verifyLocationRequest = [String:Any?]()    // JT 18.11.07

        verifyLocationRequest["ver"] = 1;
        verifyLocationRequest["SessionCookie"] = sessioncookie;
        verifyLocationRequest["CarrierName"] = carrierName;
        verifyLocationRequest["GpsLocation"] = gpslocation;
        verifyLocationRequest["VerifyLocToken"] = verifyloctoken;

        return verifyLocationRequest;
    }

    // Carrier name can change depending on cell tower.
    func createFindCloudletRequest(_ carrierName: String, _ gpslocation : [String:Any?]) ->  [String:Any?]
    {
     //   json findCloudletRequest;
        var findCloudletRequest = [String:Any?]()    // JT 18.11.07

        findCloudletRequest["vers"] = 1;
        findCloudletRequest["SessionCookie"] = sessioncookie;
        findCloudletRequest["CarrierName"] = carrierName;
        findCloudletRequest["GpsLocation"] = gpslocation;
    
        return findCloudletRequest;
    }

    func postRequest(_ uri: String,
                     _ request: String,
                     _ responseData: String

                 //    size_t (*responseCallback)(void *ptr, size_t size, size_t nmemb, void *s)
        ) // ->json
    {
       // CURL *curl;
        //CURLcode res;
      //  let curl = Wings()  // JT 18.11.07

       // cout << "URI to post to: " << uri << endl;
Swift.print( "URI to post to: \(uri)")  // JT 18.11.07
      //  curl = curl_easy_init();
//        if (curl != nil) {
//            curl_easy_setopt(curl, CURLOPT_URL, uri.c_str());
//            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, request.c_str());

//            struct curl_slist *headers = NULL;
//            headers = curl_slist_append(headers, "Accept: application/json");
//            headers = curl_slist_append(headers, "Content-Type: application/json");
//            headers = curl_slist_append(headers, "Charsets: utf-8");
//
//            curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
//            curl_easy_setopt(curl, CURLOPT_TIMEOUT, timeoutSec);
//            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &responseData);
//            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, responseCallback);
//
//            // SSL Setup:
//            curl_easy_setopt(curl, CURLOPT_SSLCERT, ClientCertFile.c_str());
//            curl_easy_setopt(curl, CURLOPT_SSLKEY, ClientPrivKey.c_str());
//            // CA:
//            curl_easy_setopt(curl, CURLOPT_CAINFO, CaCertFile.c_str());
//            // verify peer or disconnect
//            curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1);
//
//            res = curl_easy_perform(curl);
//

//            /// Path to the client SSL certificate.
//            sslCert(String),
//            /// Specifies the type for the client SSL certificate. Defaults to `.pem`.
//            sslCertType(SSLFileType),
//            /// Path to client private key file.
//            sslKey(String),
//            /// Password to be used if the SSL key file is password protected.
//            sslKeyPwd(String),
//            /// Specifies the type for the SSL private key file.
//            sslKeyType(SSLFileType),


            
        let json:  [String:Any] = try CURLRequest( uri, .failOnError,
                                    //   .postField(.init(name: "CURLOPT_HTTPHEADER",
                                                     //   value: headers)),   // JT 18.11.07 todo key
                                       .postField(.sslCert(ClientCertFile)),
                                       .postField(.sslKey(ClientPrivKey))  // JT 18.11.07
                                        , .postField(.timeout(timeoutSec))  // JT 18.11.07
            , .postField(headers)  // JT 18.11.07
          //  .postField(.init(name: "file1",
                                //                        filePath: testFile.path,
                                 //                       mimeType: "text/plain"))
                )   // JT 18.11.07
                .perform(
                    { confirmation in
                        do {
                            let response = try confirmation()
                            let json: [String:Any] = response.bodyJSON
                            
                            Swift.print("\(json)")
                        } catch let error as CURLResponse.Error {
                            print("Failed: response code \(error.response.responseCode)")
                        } catch {
                            print("Fatal error \(error)")
                        }
                        Swift.print("callback  ")
                    }
                ).bodyJSON
            
//            if (res != CURLE_OK) {
//                cout << "curl_easy_perform() failed: " << curl_easy_strerror(res) << endl;
//                curl_easy_cleanup(curl);
//                Swift.print("curl_easy_perform() failed: ")
//            }
//        }
        //json replyData = json::parse(responseData);
        //cout << "Reply: [" << replyData.dump() << "]" << endl;
            Swift.print("json \(json)")
        return json //replyData;
    }

//    static size_t getReplyCallback(void *contentptr, size_t size, size_t nmemb, void *replyBuf) {
//        size_t dataSize = size * nmemb;
//        string *buf = ((string*)replyBuf);
//
//        if (contentptr != NULL && buf) {
//            string *buf = ((string*)replyBuf);
//            buf->append((char*)contentptr, dataSize);
//
//            cout << "Data Size: " << dataSize << endl;
//            //cout << "Current replyBuf: [" << *buf << "]" << endl;
//        }
//
//
//        return dataSize;
//    }

    func RegisterClient(_ baseuri: String, _ request: [String:Any?], reply: inout String) ->[String:Any?]
    {
        let jreply = postRequest(baseuri + registerAPI, request.dump(), reply, getReplyCallback);
        tokenserveruri = jreply["TokenServerURI"];
        sessioncookie = jreply["SessionCookie"];

        return jreply;
    }

    // string formatted json args and reply.
    func VerifyLocation(_ baseuri: String, _ request: [String:Any?], reply: inout String)  ->[String:Any?] // json
    {
        if (tokenserveruri.size() == 0) {
           // cerr << "TokenURI is empty!" << endl;
            Swift.print("TokenURI is empty!")
            let empty = [String:Any?]()
            return empty;
        }

        let token:String = getToken(tokenserveruri);
        //cout << "VerifyLocation: Retrieved token: [" << token << "]" << endl;
Swift.print("VerifyLocation: Retrieved token: [" + token + "]")
        
        // Update request with the new token:
       // json tokenizedRequest;
        var tokenizedRequest = [String:Any?]()
        
        tokenizedRequest["ver"] = request["ver"];
        tokenizedRequest["SessionCookie"] = request["SessionCookie"];
        tokenizedRequest["CarrierName"] = request["CarrierName"];
        tokenizedRequest["GpsLocation"] = request["GpsLocation"];
        tokenizedRequest["VerifyLocToken"] = token;

        //cout << "VeriyLocation actual call..." << endl;
        Swift.print("VeriyLocation actual call...")
        let jreply = postRequest(baseuri + verifylocationAPI, tokenizedRequest.dump(), reply, getReplyCallback);
        return jreply;

    }

    func FindCloudlet(_ baseuri: String, _ request: [String:Any?], reply: inout String) ->[String:Any?]
    {
        let jreply = postRequest(baseuri + findcloudletAPI, request.dump(), reply, getReplyCallback);

        return jreply;
    }

    func getToken(_ uri: String) ->String
    {
       // cout << "In Get Token" << endl;
        Swift.print("In Get Token")
        if (uri.length() == 0) {
            //cerr << "No URI to get token!" << endl;
            Swift.print("No URI to get token!")
            return nil;
        }

//        // Since GPRC's Channel is somewhat hidden
//        // we can use CURL here instead.
//        CURL *curl = curl_easy_init();
//        if (curl == nil) {
//            cerr << "Curl could not be initialized." << endl;
//            return nil;
//        }
//        CURLcode res;
//        cout << "uri: " << uri << endl;
//        curl_easy_setopt(curl, CURLOPT_URL, uri.c_str());
//        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, false);  // Do not follow redirect.
//        curl_easy_setopt(curl, CURLOPT_HEADER, 1);              // Keep headers.
//
//        // Set return pointer (the token), for the header callback.
//        curl_easy_setopt(curl, CURLOPT_HEADERDATA, &(this->token));
//        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_callback);
//
//        // SSL Setup:
//        curl_easy_setopt(curl, CURLOPT_SSLCERT, ClientCertFile.c_str());
//        curl_easy_setopt(curl, CURLOPT_SSLKEY, ClientPrivKey.c_str());
//        // CA:
//        curl_easy_setopt(curl, CURLOPT_CAINFO, CaCertFile.c_str());
//        // verify peer or disconnect
//        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1);
//
//        res = curl_easy_perform(curl);
//        if (res != CURLE_OK) {
//           cerr << "Error getting token: " << res << endl;
//        }
//
//        curl_easy_cleanup(curl);

        let json = try CURLRequest( uri, .failOnError,
                                    .postField(.init(name: "CURLOPT_HTTPHEADER",
                                                     value: headers)),   // JT 18.11.07 todo key
          //  .postField(.init(name: "CURLOPT_TIMEOUT", value: "timeoutSec")),
            
            .postField(.followLocation(false))  // JT 18.11.07

            .postField(.sslCert(ClientCertFile)),
            .postField(.sslKey(ClientPrivKey))  // JT 18.11.07
                .postField(.sslKey(CaCertFile))  // JT 18.11.07
                .postField(.sslVerifyPeer(1))  // JT 18.11.07
           
 
       //  .postField(.init(name: "file1",
            //                        filePath: testFile.path,
            //                       mimeType: "text/plain"))
            )   // JT 18.11.07
            .perform().bodyJSON
        
        
        return token;
    }

    var token: String = ""  // short lived carrier dt-id token.
    var tokenserveruri = ""
    var sessioncookie = ""

    func parseParameter( _ queryParameter: String, _ keyFind: String) ->String
    {
        let value: String = ""
        let foundToken: String = ""
//        size_t vpos = queryParameter.find("=");
//
//        string key = queryParameter.substr(0, vpos);
//        cout << "Key: " << key << endl;
//        vpos += 1; // skip over '='
//        string valPart = queryParameter.substr(vpos, queryParameter.length() - vpos);
//        cout << "ValPart: " << valPart << endl;
        
        
        let a = fullName.components(separatedBy: "=")   // key, value
        let key = a[0]
let valPart = a[1]
        Swift.print("\(key) = \(valPart)")
        if ((key == keyFind) && a.count > 1 ) {

                foundToken = valPart // queryParameter.substr(vpos, queryParameter.length() - vpos);
            //    cout << "Found Token: " << foundToken << endl;
                Swift.print("Found Token: \(foundToken)")
        }
        return foundToken;
    }

    func parseToken( _ locationUri: String) ->String
     {
        // Looking for carrier dt-id: <token> in the URL, and ignoring everything else.
        var pos = locationUri.find("?")
        pos += 1;
        let uriParameters = locationUri.substr(pos, locationUri.length() - pos);
        pos = 0;
        var start = 0;
        var foundToken = ""

        // Carrier token.
        var keyFind:String = "dt-id";

        var queryParameter: String = ""
        repeat {
            pos = uriParameters.find("&", start);
            if (pos+1 >= uriParameters.length()) {
                break;
            }

            if (pos == nil) {  // Last one, or no terminating &
                queryParameter = uriParameters.substr(start, uriParameters.length() - start);
                foundToken = parseParameter(queryParameter, keyFind);
                break;
            } else {
                queryParameter = uriParameters.substr(start, pos - start);
                foundToken = parseParameter(queryParameter, keyFind);
            }

            // Next.
            start = pos+1;
            if (foundToken != "") {
                break;
            }
        } while (pos != nil )   //std::string::npos);   // JT 18.11.07

        return foundToken;
    }

    func trimStringEol( _ stringBuf: String) ->String
    {
        let size = stringBuf.length();

        // HTTP/1.1 RFC 2616: Should only be "\r\n" (and not '\n')
        if (size >= 2 && (stringBuf[size-2] == "\r" && stringBuf[size-1] == "\n")) {
            let seol = size-2;
            return stringBuf.substr(0,seol);
        } else {
            // Not expected EOL format, returning as-is.
            return stringBuf;
        }

    }

 
    #if false
    // Callback function to retrieve headers, called once per header line.
    func  header_callback(/* char * */ _ buffer: String, _ size: UInt64, _ n_items: UInt64, void *userdata) ->UInt64
    {
        let bufferLen = n_items * size;
        
        // Need to get "Location: ....dt-id=ABCDEF01234"
        var s = String(buffer);
        s = trimStringEol(s);
        
        var key = "";
        var value = "";
        string *token = (string *)userdata;
        
        // split buffer:
        let colonPos = s.firstIndex(of: ":"); //String firstIndex
        var blankPos = 0
        
        if (colonPos != nil) {
            key = stringBuf.substr(0, colonPos);
            if (key == "Location") {
                // Skip blank
                blankPos = stringBuf.firstIndex(of: " ") + 1;
                if (//(blankPos != std::string::npos) &&
                    (blankPos < stringBuf.length()))
                {
                    value = stringBuf.substr(blankPos, stringBuf.length() - blankPos);
                    //cout << "Location Header Value: [" << value << "]" << endl;
                    Swift.print("Location Header Value: [" + value + "]")
                    *token = parseToken(value);
                }
            }
        }
        
        // Return number of bytes read thus far from reply stream.
        return bufferLen;
    }
    #endif


}

func main()
{
    //cout << "Hello C++ MEX REST Lib" << endl;
    Swift.print("Hello C++ MEX REST Lib")
 //   curl_global_init(CURL_GLOBAL_DEFAULT);

    // Credentials, Mutual Authentication:
   // unique_ptr<MexRestClient> mexClient = unique_ptr<MexRestClient>(new MexRestClient());
let mexClient = MexRestClient()
    do {
        let loc = mexClient.retrieveLocation()

 //       cout << "Register MEX client." << endl;
   //     cout << "====================" << endl
    //         << endl;
        Swift.print("Register MEX client.")
        Swift.print("====================\n")

        var baseuri = mexClient.generateBaseUri(mexClient.getCarrierName(), mexClient.dmePort);
        var strRegisterClientReply:String = ""
        let registerClientRequest = mexClient.createRegisterClientRequest()
        
        let registerClientReply = mexClient.RegisterClient(baseuri, registerClientRequest, strRegisterClientReply)

        if (registerClientReply.size() == 0) {
         //   cerr << "REST RegisterClient Error: NO RESPONSE." << endl;
            Swift.print("REST RegisterClient Error: NO RESPONSE.")
            return 1;
        } else {
//            cout << "REST RegisterClient Status: "
//                 << "Version: " << registerClientReply["ver"]
//                 << ", Client Status: " << registerClientReply["status"]
//                 << ", SessionCookie: " << registerClientReply["SessionCookie"]
//                 << endl
//                 << endl;
            
            var line1 = "REST RegisterClient Status: "
            var line2 = "Version:" + registerClientReply["ver"]
            var line3 = ", Client Status:" + registerClientReply["status"]
            var line4 = ", SessionCookie:" + registerClientReply[ "SessionCookie"]

           Swift.print( line1 + line2 + line3 + line4 + "\n\n" )
        }

        // Get the token (and wait for it)
        // GPRC uses "Channel". But, we can use libcurl here.
       // cout << "Token Server URI: " << registerClientReply["TokenServerURI"] << endl;
        
        Swift.print("Token Server URI: " + egisterClientReply["TokenServerURI"] + "\n")

        // Produces a new request. Now with sessioncooke and token initialized.
//        cout << "Verify Location of this Mex client." << endl;
//        cout << "===================================" << endl
//             << endl;
        
        Swift.print("Verify Location of this Mex client.")
Swift.print("===================================\n\n")
        
        baseuri = mexClient.generateBaseUri(mexClient.getCarrierName(), mexClient.dmePort);
        loc = mexClient.retrieveLocation();
        var strVerifyLocationReply = ""
        var verifyLocationRequest = mexClient.createVerifyLocationRequest(mexClient.getCarrierName(), loc, "");
        
        var verifyLocationReply = mexClient.VerifyLocation(baseuri, verifyLocationRequest, strVerifyLocationReply);

        // Print some reply values out:
        if (verifyLocationReply.size() == 0) {
            //cout << "REST VerifyLocation Status: NO RESPONSE" << endl;
            Swift.print("REST VerifyLocation Status: NO RESPONSE")
        }
        else {
           // cout << "[" << verifyLocationReply.dump() << "]" << endl;
            Swift.print("[" + verifyLocationReply.dump() + "]")
        }

 //      cout << "Finding nearest Cloudlet appInsts matching this Mex client." << endl;
   //     cout << "===========================================================" << endl
  //           << endl;

        Swift.print("Finding nearest Cloudlet appInsts matching this Mex client.")
        Swift.print("===========================================================")
        
        baseuri = mexClient.generateBaseUri(mexClient.getCarrierName(), mexClient.dmePort);
        loc = mexClient.retrieveLocation();
        var strFindCloudletReply: String = ""
        var findCloudletRequest = mexClient.createFindCloudletRequest(mexClient.getCarrierName(), loc);
        var findCloudletReply = mexClient.FindCloudlet( baseuri,
                                                        findCloudletRequest,
                                                        strFindCloudletReply);

        if (findCloudletReply.size() == 0) {
           // cout << "REST VerifyLocation Status: NO RESPONSE" << endl;
            Swift.print("REST VerifyLocation Status: NO RESPONSE")
        }
        else {
//            cout << "REST FindCloudlet Status: "
//                 << "Version: " << findCloudletReply["ver"]
//                 << ", Location Found Status: " << findCloudletReply["status"]
//                 << ", Location of cloudlet. Latitude: " << findCloudletReply["cloudlet_location"]["lat"]
//                 << ", Longitude: " << findCloudletReply["cloudlet_location"]["long"]
//                 << ", Cloudlet FQDN: " << findCloudletReply["fqdn"] << endl;
            
          let line1 = "REST FindCloudlet Status: "
            let line2 = "Version: " + findCloudletReply["ver"]
          let line3 = ", Location Found Status: " + findCloudletReply["status"]
           let line4 = ", Location of cloudlet. Latitude: " + findCloudletReply["cloudlet_location"]["lat"]
             let line5 = ", Longitude: " + findCloudletReply["cloudlet_location"]["long"]
            let line6 = ", Cloudlet FQDN: " + findCloudletReply["fqdn"] << endl;
           
            Swift.print(line1 + line2 + line3 + line4 + line5 + line6)  // JT 18.11.07
            let ports = findCloudletReply["ports"]; // json
            let size = ports.size();    // size_t
            for appPort in ports
            {
//                cout << ", AppPort: Protocol: " << appPort["proto"]
//                     << ", AppPort: Internal Port: " << appPort["internal_port"]
//                     << ", AppPort: Public Port: " << appPort["public_port"]
//                     << ", AppPort: Public Path: " << appPort["public_path"]
//                     << endl;
//
                let proto = appPort["proto"]
                let internal_port = appPort["internal_port"]

                let public_port = appPort["public_port"]
                let public_path = appPort["public_path"]
                
                Swift.print(", AppPort: Protocol: \(proto)" +
                ", AppPort: Internal Port: \(internal_port)" +
                    ", AppPort: Internal Port: \(public_port)" +
                    ", AppPort: ublic Path:  \(public_path)"

                )

            }
        }

      //  cout << endl;
        Swift.print("")

    }
//    catch (std::runtime_error &re) {
//        cerr << "Runtime error occurred: " << re.what() << endl;
//    } catch (std::exception &ex) {
//        cerr << "Exception ocurred: " << ex.what() << endl;
//    } catch (char *what) {
//        cerr << "Exception: " << what << endl;
//    }
    catch   {
      //  cerr << "Unknown failure happened." << endl;
        Swift.print(public_port)
    }

   // curl_global_cleanup();
    return 0;
}
