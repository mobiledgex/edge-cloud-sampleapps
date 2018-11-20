//package com.mobiledgex.sdkdemo;
//
//import com.mobiledgex.matchingengine.FindCloudletResponse;
//
//import distributed_match_engine.AppClient;

//public class
    protocol MatchingEngineResultsListener  // JT 18.10.22  // JT 18.11.01
{
    func onRegister(_ sessionCookie:String)
        
        func onVerifyLocation( _ status: DistributedMatchEngine_VerifyLocationReply.GPS_Location_Status
                               /* AppClient.Match_Engine_Loc_Verify.GPS_Location_Status*/ ,
       _ gpsLocationAccuracyKM: Double)
 
        func onFindCloudlet(_ closestCloudlet:DistributedMatchEngine_FindCloudletReply /*?FindCloudletResponse*/);   // JT 18.11.02 assume response is a reply???
 
        func onGetCloudletList(_ cloudletList: DistributedMatchEngine_AppInstListRequest /*AppClient.Match_Engine_AppInst_List*/)
}
