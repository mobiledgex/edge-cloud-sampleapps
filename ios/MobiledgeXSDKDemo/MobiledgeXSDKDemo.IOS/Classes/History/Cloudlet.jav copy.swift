
    // JT 18.10.23
import Foundation

import GoogleMaps   // JT 18.10.23
import PlainPing    // for latency

private     var cl:Cloudlet?    // JT 18.11.01


public class Cloudlet //implements Serializable
{
    private static let  TAG:String = "Cloudlet"
    public static let  BYTES_TO_MBYTES:Int = 1024*1024

     var mCloudletName: String = ""
    private var mAppName: String = ""
    private var mCarrierName: String = ""
    private var mLatitude: Double   = 0
    private var mLongitude: Double = 0
    private var mDistance: Double = 0
    private var bestMatch: Bool = false
    private var  mMarker: GMSMarker?;    // JT 18.10.23

     var latencyMin: Double = 9999.0
     var latencyAvg: Double = 0;
     var latencyMax: Double = 0;
     var latencyStddev: Double = 0;
     var latencyTotal: Double = 0;
    var mNumPings: Int = 3; // JT 18.11.13

    var pings:[String] = []         // JT 18.11.13
    var latencies  = [Double]()    // JT 18.11.13

    private var mbps:Int64 = 0  //BigDecimal.valueOf(0);  // JT 18.10.23 todo
     var latencyTestProgress: Int = 0;
    private var speedTestProgress: Int = 0;
     var startTime: Double = 0  //Int64 // JT 18.10.24
     var timeDifference: Double = 0
     var mNumPackets: Int = 4;
    private var mNumBytes: Int = 1048576;
    private var runningOnEmulator:Bool = false;
     var pingFailed:Bool = false;

 //    var mSpeedTestResultsListener: SpeedTestResultsListener? // JT 18.11.11

    private var downloadUri: String = ""
     var hostName: String = ""
     var openPort:Int = 7777;
      let socketTimeout:Int = 3000;
     var latencyTestTaskRunning: Bool = false;
     var speedTestTaskRunning: Bool = false;
    private var uri: String = "";


    
  ///  var socket :Socket?     // JT 18.10.23
    
//    init () // JT 18.10.23
//    {
//    }
    
  //  public func Cloudlet(_ cloudletName: String, _ appName: String, _ carrierName: String,_  gpsLocation: CLLocationCoordinate2D , _ distance: Double, _ uri: String, _ marker: GMSMarker, _ numBytes: Int, _ numPackets: Int) // LatLng
    
    init()
    {
    }
    
     init(_ cloudletName: String,
          _ appName: String,
          _ carrierName: String,
          _ gpsLocation: CLLocationCoordinate2D ,
          _ distance: Double,
          _ uri: String,
          _ marker: GMSMarker,
          _ numBytes: Int,
          _ numPackets: Int) // LatLng    // JT 18.11.06
    {
     //   Log.d(TAG, "Cloudlet contructor. cloudletName="+cloudletName);
        Swift.print("Cloudlet contructor. cloudletName= \(cloudletName)")
        update(cloudletName, appName, carrierName, gpsLocation, distance, uri, marker, numBytes, numPackets);

        if(CloudletListHolder.getSingleton().getLatencyTestAutoStart()) {
            //All AsyncTask instances are run on the same thread, so this queues up the tasks.
            startLatencyTest();
        } else {
         //   Log.i(TAG, "LatencyTestAutoStart is disabled");
            Swift.print("LatencyTestAutoStart is disabled")
     }
    }

    public func update(_ cloudletName: String,
                       _ appName: String,
                       _ carrierName: String,
                       _ gpsLocation: CLLocationCoordinate2D,
                       _ distance: Double,
                       _ uri:String,
                       _ marker: GMSMarker,
                       _ numBytes: Int,
                       _ numPackets:Int)
    {
        //Log.d(TAG, "Cloudlet update. cloudletName="+cloudletName);
    Swift.print("Cloudlet update. cloudletName= \(cloudletName)")

        mCloudletName = cloudletName;
        mAppName = appName;
        mCarrierName = carrierName;
        mLatitude = gpsLocation.latitude;
        mLongitude = gpsLocation.longitude;
        mDistance = distance;
        mMarker = marker;
        mNumBytes = numBytes;
        mNumPackets = numPackets;
        setUri(uri);
        
//        PlainPing.ping( uri, withTimeout: 1.0, completionBlock: { (timeElapsed:Double?, error:Error?) in    // JT 18.11.13
//            if let latency = timeElapsed {
//                //  self.pingResultLabel.text = "latency (ms): \(latency)"
//                self.latencyMin = latency
//                self.latencyAvg = latency
//                self.latencyMin = latency
//                self.latencyMax = latency
//
//
//            }
//
//            if let error = error {
//                print("ping error: \(error.localizedDescription)")
//            }
//        })
        

        runLatencyTest()    // JT 18.11.13
        
     }
    
    func runLatencyTest()
    {
        if   uri != "" && uri.range(of:"azure") == nil  // JT 18.11.13
        {
            Swift.print("uri: \(uri)")
            // Ping several times
            latencies.removeAll()
            pings.removeAll()
            
            for _ in 0..<mNumPings
            {
                pings.append(uri)   //  N pings
            }
            
            pingNext()
        }
        // post upateLatencies
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateLatencies"),  object: nil )
    }

    deinit
    {
        NotificationCenter.default.removeObserver(self)
    }
    
    func pingNext() {
        guard pings.count > 0 else {
            return
        }
        
        let ping = pings.removeFirst()
        PlainPing.ping(ping, withTimeout: 1.0, completionBlock: { (timeElapsed:Double?, error:Error?) in
            if let latency = timeElapsed {
                print("\(ping) latency (ms): \(latency)")
                self.latencies.append(latency)
                self.latencyMin = self.latencies.min()!  // JT 18.11.13
                self.latencyMax = self.latencies.max()!  // JT 18.11.13
 
                let sumArray = self.latencies.reduce(0, +)
                
                self.latencyAvg = sumArray / Double(self.latencies.count)
                
                self.latencyStddev = self.standardDeviation(arr: self.latencies)  // JT 18.11.13
            }
            if let error = error {
                print("error: \(error.localizedDescription)")
            }
            self.pingNext()
        })
    }
    
    func standardDeviation(arr : [Double]) -> Double    // JT 18.11.13
    {
        let length = Double(arr.count)
        let avg = arr.reduce(0, {$0 + $1}) / length
        let sumOfSquaredAvgDiff = arr.map { pow($0 - avg, 2.0)}.reduce(0, {$0 + $1})
        return sqrt(sumOfSquaredAvgDiff / length)
    }
    
    /**
     * From the given string, create the hostname that will be pinged,
     * and the URI that will be downloaded from.
     * @param uri
     */
    public func setUri(_ uri: String) {
        if mCarrierName.caseInsensitiveCompare("TDG") == . orderedSame
        {
            self.openPort = 443;
            self.hostName = uri;
            self.downloadUri = "https://\(hostName)/mobiledgexsdkdemohttp/getdata?numbytes= \(mNumBytes)"
        } else {
            self.openPort = 7777;
            self.hostName = "mobiledgexsdkdemo." + uri;
            self.downloadUri = "http://\(hostName):\(openPort)/getdata?numbytes= \(mNumBytes)"
        }
        self.uri = uri;
    }

    public func  getUri() ->String {
        return uri;
    }

    public func toString() ->String
    {
        return "mCarrierName=\(mCarrierName) mCloudletName=\(mCloudletName) mLatitude=\(mLatitude) mLongitude=\(mLongitude) mDistance=\(mDistance) uri=\(uri)"
    }

//     func setSpeedTestResultsListener(_ speedTestResultsListener: SpeedTestResultsListener)   // JT 18.11.01
//    {
//        self.mSpeedTestResultsListener = speedTestResultsListener;
//    } // JT 18.11.11 todo

    public func startLatencyTest() {
        //Log.d(TAG, "startLatencyTest()");
        Swift.print("startLatencyTest()")
        if(latencyTestTaskRunning) {
        //    Log.d(TAG, "LatencyTest already running");
            Swift.print("LatencyTest already running")
            return;
        }

        latencyMin=9999;
        latencyAvg=0;
        latencyMax=0;
        latencyStddev=0;
        latencyTotal=0;

        //ping can't run on an emulator, so detect that case.
       // Log.i(TAG, "PRODUCT="+ Build.PRODUCT);
      //  Swift.print("PRODUCT= \(Build.PRODUCT)")

        
        if (isSimulator)
        {
            runningOnEmulator = true;
           // Log.i(TAG, "YES, I am an emulator.");
            Swift.print("YES, I am an emulator.")

        } else {
            runningOnEmulator = false;
        //    Log.i(TAG, "NO, I am NOT an emulator.");
            Swift.print("NO, I am NOT an emulator.")
       }

        var latencyTestMethod:CloudletListHolder.LatencyTestMethod  // CloudletListHolder.  // JT 18.11.01
        = CloudletListHolder.getSingleton().getLatencyTestMethod();

        if mCarrierName.caseInsensitiveCompare("azure") == . orderedSame
        {
            //Log.i(TAG, "Socket test forced for Azure");
            Swift.print("Socket test forced for Azure")

            latencyTestMethod = CloudletListHolder.LatencyTestMethod.socket;
        }
        if(runningOnEmulator) {
        //    Log.i(TAG, "Socket test forced for emulator");
            Swift.print("Socket test forced for emulator")
            latencyTestMethod = CloudletListHolder.LatencyTestMethod.socket;
        }

        if (latencyTestMethod == CloudletListHolder.LatencyTestMethod.socket) {
            Swift.print("LatencyTestTaskSocket todo")   // JT 18.10.23
           //  LatencyTestTaskSocket().execute();
        } else if (latencyTestMethod == CloudletListHolder.LatencyTestMethod.ping) {
            Swift.print("LatencyTestTaskPing todo")   // JT 18.10.23
           // LatencyTestTaskPing().execute();
        } else {
           // Log.e(TAG, "Unknown latencyTestMethod: " + latencyTestMethod);
            Swift.print("Unknown latencyTestMethod: \(latencyTestMethod) ")
      }
    }

    public func startBandwidthTest() {
        if(speedTestTaskRunning) {
          //  Log.d(TAG, "SpeedTest already running");
            Swift.print( "SpeedTest already running")

            return;
        }
     //   Log.d(TAG, "downloadUri=" + downloadUri + " speedTestTaskRunning="+speedTestTaskRunning);
        
        Swift.print("downloadUri= \(downloadUri) speedTestTaskRunning= \(speedTestTaskRunning)")
        if(!speedTestTaskRunning) {
            Swift.print("SpeedTestTask todo")
         //    SpeedTestTask().execute();
        }
    }

      func isReachable(_ addr: String, _ openPort: Int, _ timeOutMillis: Int) ->Bool
    {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
//        do{
//            let soc =  try Socket()
//
//            soc.connect( InetSocketAddress(addr, openPort), timeOutMillis);
//
//            return true;
//        } catch ( ex: UnknownHostException) {
//          //  ex.printStackTrace();
//            return false;
//        } catch ( ex: IOException) {
//           // ex.printStackTrace();
//            return false;
//        }
        
        do {
             let socket = try Socket.create(family: .inet)
            //    socket.delegate = self
            try socket.connect(to: addr, port: Int32(openPort))
            Swift.print("💜 \(#function) Socket.create connect")
        } catch let error {
            
            // See if it's a socket error or something else...
            guard let socketError = error as? Socket.Error else {
                
                print("❌  \(#function) Unexpected error...")  // JT 17.04.28
                return false
            }
            
            print("❌  \(#function)  Failed Error reported: \(socketError.description)\n host:\(addr):\(openPort)")
        }
        return true

    }




    public func getCloudletName() ->String {
        return mCloudletName;
    }

    public func setCloudletName(_ mCloudletName: String) {
        self.mCloudletName = mCloudletName;
    }

    public func getCarrierName() ->String {
        return mCarrierName;
    }

    public func setCarrierName(_ mCarrierName: String) {
        self.mCarrierName = mCarrierName;
    }

    public func getLatitude() ->Double
{
        return mLatitude;
    }

    public func setLatitude( Latitude: Double) {
        self.mLatitude = Latitude;
    }

    public func getLongitude() ->Double
    {
        return mLongitude;
    }

    public func setLongitude( mLongitude: Double) {
        self.mLongitude = mLongitude;
    }

    public func getDistance()->Double
{
        return mDistance;
    }

    public func setDistance(_ mDistance:Double) {
        self.mDistance = mDistance;
    }

    public func getMarker() ->GMSMarker
    { return mMarker!; }

    public func setMarker(_ mMarker:GMSMarker) { self.mMarker = mMarker; }

    public func isBestMatch() ->Bool { return bestMatch; }

    public func setBestMatch( _ bestMatch:Bool) { self.bestMatch = bestMatch; }

    public func getLatencyMin() ->Double
{
        return latencyMin;
    }

    public func getLatencyAvg() ->Double
    {
        return latencyAvg;
    }

    public func getLatencyMax() ->Double
{
        return latencyMax;
    }

    public func getLatencyStddev() ->Double
{
        return latencyStddev;
    }

    public func getMbps() ->Int64   // JT 18.10.23 BigDecimal
{
        return mbps;
    }

    public func getLatencyTestProgress() ->Int {
        return latencyTestProgress;
    }

    public func getSpeedTestProgress() ->Int
{
        return speedTestProgress;
    }

    public func isPingFailed() ->Bool
    {
        return pingFailed;
    }

    public func setPingFailed(_ pingFailed: Bool) {
        self.pingFailed = pingFailed;
    }

    public func isLatencyTestTaskRunning() ->Bool
{
        return latencyTestTaskRunning;
    }

    public func setLatencyTestTaskRunning(_ latencyTestTaskRunning: Bool) {
        self.latencyTestTaskRunning = latencyTestTaskRunning;
    }

    public func getAppName() ->String {
        return mAppName;
    }

    public func setAppName(_ mAppName : String) {
        self.mAppName = mAppName;
    }

    public func getNumPackets()-> Int
{ return mNumPackets; }

    public func setNumPackets(_ mNumPings: Int) { self.mNumPackets = mNumPings; }

    public func getNumBytes() -> Int
    { return mNumBytes; }

    public func setNumBytes(_ mNumBytes: Int) { self.mNumBytes = mNumBytes; }

    var isSimulator: Bool {
        #if arch(i386) || arch(x86_64)
        return true
        #else
        return false
        #endif
    }
}

extension UIDevice {
    var isSimulator: Bool {
        #if arch(i386) || arch(x86_64)
        return true
        #else
        return false
        #endif
    }
}

public class LatencyTestTaskSocket
    // JT 18.10.23 todo
    //extends AsyncTask<Void, Integer, String>
{
    var cloudlet:Cloudlet?
    
    init()  // JT 18.11.01
    {
        
    }
    
    
    func execute(_ cl:    Cloudlet?) { // background
        
        cloudlet = cl  // JT 18.10.24
        
        DispatchQueue.main.async {
            
            
        }
        
        DispatchQueue.global().async {
            
            
        }
        
        DispatchQueue.global(qos: .background).async {
            
        }
    }
    
    func doInBackground( voids: [Int]) -> String
    {
        
        cloudlet?.latencyTestTaskRunning = true;
        cloudlet?.pingFailed = false;
        var sumSquare:Double = 0;
        var countFail:Int = 0;
        var countSuccess: Int = 0;
        var reachable: Bool? = false
        //First time may be slower because of DNS lookup. Run once before it counts.
         cloudlet?.isReachable(cloudlet?.hostName ?? "", cloudlet?.openPort ?? 0, cloudlet?.socketTimeout ?? 3000); // JT 18.10.23
        let n = cloudlet?.mNumPackets ?? 0
        for  i in 0..<n
        {
            //  startTime = System.nanoTime();
            cl!.startTime =  CACurrentMediaTime();   // JT 18.10.24
            reachable = cl?.isReachable(cl!.hostName  , cl!.openPort  , cl!.socketTimeout  );
            if(reachable ?? false) {
                //     let endTime:Int64 = System.nanoTime();
                let endTime:Double = CACurrentMediaTime();
                cl!.timeDifference = (endTime - cl!.startTime) ///1000000.0;
                //    Log.d(TAG, hostName+" reachable="+reachable+" Latency=" + timeDifference + " ms.");
                Swift.print("\(cl!.hostName) reachable= \(reachable) Latency= \(cl!.timeDifference) ms.")
                cl!.latencyTotal += cl!.timeDifference;
                cl!.latencyAvg = cl!.latencyTotal/Double((i+1));
                if(cl!.timeDifference < cl!.latencyMin) { cl!.latencyMin = cl!.timeDifference; }
                if(cl!.timeDifference > cl!.latencyMax) { cl!.latencyMax = cl!.timeDifference; }
                sumSquare +=  pow((cl!.timeDifference - cl!.latencyAvg),2);   // JT 18.10.24
                cl!.latencyStddev = sqrt( Double(Int(sumSquare)/(i+1)) );
                countSuccess += 1;
            } else {
                countFail += 1;
            }
            Swift.print("publishProgress todo") // JT 18.10.24
            // publishProgress( ((i+1.0)/mNumPackets*100));
            //   Thread.sleep(500);
            usleep(500000) //will sleep for .5 second
            
            
            
        }
        if(countFail == cl!.mNumPackets) {
            //Log.w(TAG, "ping failed");
            Swift.print("ping failed")
            cl!.pingFailed = true;
        }
        
        // Summary logs to match ping output
        // 10 packets transmitted, 10 packets received, 0.0% packet loss
        // round-trip min/avg/max/stddev = 202.167/219.318/335.734/38.879 ms
        let percent:String = String(format:"%.1f", (countFail/cl!.mNumPackets*100))
        
        let avg: String = String(format:"%.3f", (cl!.latencyAvg));
        let stddev:String = String(format:"%.3f", (cl!.latencyStddev));
        
        // Log.i(TAG, hostName+" "+mNumPackets+" packets transmitted, "+countSuccess+" packets received, "+percent+"% packet loss");
        Swift.print("\(cl!.hostName) \(cl!.mNumPackets) packets transmitted, \(countSuccess) packets received, \(percent)% packet loss")
        //Log.i(TAG, hostName+" round-trip min/avg/max/stddev = "+latencyMin+"/"+avg+"/"+latencyMax+"/"+stddev+" ms");
        Swift.print("\(cl!.hostName) round-trip min/avg/max/stddev = \(cl!.latencyMin)/\(avg)/\(cl!.latencyMax)/\(stddev) ms")
        
        return "";  // JT 18.10.24
    }
    
    func onPostExecute(_ s: String)
    {
        // super.onPostExecute(s);
        cl!.latencyTestProgress = cl!.pingFailed ? 0 : 100;
        cl!.latencyTestTaskRunning = false;
//        if(cl!.mSpeedTestResultsListener != nil) {
//            cl!.mSpeedTestResultsListener!.onLatencyProgress();
//        } // JT 18.11.11 todo?
    }
    
    func onProgressUpdate( _ progress :[Int])
    {
        cl!.latencyTestProgress = progress[0];
//        if(cl!.mSpeedTestResultsListener != nil) {
//            cl!.mSpeedTestResultsListener!.onLatencyProgress();
//        } // JT 18.11.11 todo?
    }
    
}



public class LatencyTestTaskPing  // JT 18.11.01
    // JT 18.10.23 todo
    //extends AsyncTask<Void, Integer, String>
{
    var cl: Cloudlet =  Cloudlet() // JT 18.10.24

    init()
    {
        
    }
    
    func execute(_ cloudlet: Cloudlet)  // JT 18.10.24
    {
         cl = cloudlet  // JT 18.10.24

        DispatchQueue.global(qos: .default).async
            { [unowned self] in
                self.doInBackground()
        }
    }
    
    func  doInBackground() ->String // JT 18.11.01
    {
        cl.latencyTestTaskRunning = true;

        PlainPing.ping("www.google.com", withTimeout: 1.0, completionBlock: { (timeElapsed:Double?, error:Error?) in
            if let latency = timeElapsed {
           //     self.pingResultLabel.text = "latency (ms): \(latency)"
                Swift.print("ping latency (ms): \(latency)")
                
                self.cl.latencyTestProgress = self.cl.pingFailed ? 0 : 100;
                self.cl.latencyTestTaskRunning = false;
//                if(self.cl.mSpeedTestResultsListener != nil) {
//                    self.cl.mSpeedTestResultsListener!.onLatencyProgress();
//                }
Swift.print("todo mSpeedTestResultsListener")
            }
            
            if let error = error {
                print("error: \(error.localizedDescription)")
            }
        })
        
        return ""
        
//        cl.pingFailed = false;
//        let pingCommand:String = "/system/bin/ping -c \(cl.mNumPackets) \(cl.hostName)"
//        var inputLine = "";
//
//        let regex: String = "time=(\\d+.\\d+) ms";
//        let pattern:Pattern = Pattern.compile(regex);
//        var matcher:Matcher;
//
//
//        //  Log.d(TAG, mCloudletName+ " executing "+pingCommand);
//        Swift.print(  "\(cl.mCloudletName) executing \(pingCommand)")
//
//        do {
//            // execute the command on the environment interface
//            let process:Process = Runtime.getRuntime().exec(pingCommand);
//            // gets the input stream to get the output of the executed command
//            let bufferedReader:BufferedReader = BufferedReader( InputStreamReader(process.getInputStream()));
//
//            let linesTotal:Double = mNumPackets;
//            let linesRead: Double = 0;
//            inputLine = bufferedReader.readLine();
//
//            while ((inputLine != null)) {
//                // Log.i(TAG, "inputLine="+inputLine);
//                Swift.print(  "inputLine= \(inputLine)")
//
//                if (inputLine.length() > 0 && inputLine.contains("time=")) {
//                    linesRead++;
//                    matcher = pattern.matcher(inputLine);
//                    if(matcher.find()) {
//                        let val: Double = Double.parseDouble(matcher.group(1));
//                        latencyTotal += val;
//                        latencyAvg = latencyTotal/linesRead;
//                        if(val < latencyMin) { latencyMin = val; }
//                        if(val > latencyMax) { latencyMax = val; }
//                    }
//                    // Log.d(TAG, "linesRead="+linesRead+" linesTotal="+linesTotal+" "+(linesRead/linesTotal*100)+" "+(int)(linesRead/linesTotal*100));
//
//                    Swift.print( "linesRead= \(linesRead) linesTotal=\(linesTotal) \(linesRead/linesTotal*100) \(linesRead/linesTotal*100)")
//
//                    publishProgress((int)(linesRead/linesTotal*100));
//                }
//                if (inputLine.length() > 0 && inputLine.contains("avg")) {  // when we get to the last line of executed ping command
//                    break;
//                }
//                if (inputLine.length() > 0 && inputLine.contains("100% packet loss")) {  // when we get to the last line of executed ping command (all packets lost)
//                    break;
//                }
//                inputLine = bufferedReader.readLine();
//            }
//        }
//        catch ( e:IOException){
//            //  Log.e(TAG, "getLatency: EXCEPTION");
//            Swift.print("getLatency: EXCEPTION")
//            // e.printStackTrace();
//        }
//
//        if(inputLine == null || inputLine.contains("100% packet loss")) {
//            //  Log.w(TAG, "ping failed");
//            Swift.print("ping failed")
//
//            pingFailed = true;
//        } else {
//            // Extract the average round trip time from the inputLine string
//            regex = "(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+) ms";
//            pattern = Pattern.compile(regex);
//            matcher = pattern.matcher(inputLine);
//            if (matcher.find()) {
//                Log.i(TAG, "output=" + matcher.group(0));
//                latencyMin = Double.parseDouble(matcher.group(1));
//                latencyAvg = Double.parseDouble(matcher.group(2));
//                latencyMax = Double.parseDouble(matcher.group(3));
//                latencyStddev = Double.parseDouble(matcher.group(4));
//            }
//        }
//
//        return nil; // JT 18.10.24
//    }
//
//    func onPostExecute(_ s:String)
//    {
//        super.onPostExecute(s);
//        latencyTestProgress = pingFailed ? 0 : 100;
//        latencyTestTaskRunning = false;
//        if(mSpeedTestResultsListener != null) {
//            mSpeedTestResultsListener.onLatencyProgress();
//        }
//    }
//
//    func onProgressUpdate(_ progress: [Int]) {
//        latencyTestProgress = progress[0];
//        if(mSpeedTestResultsListener != null) {
//            mSpeedTestResultsListener.onLatencyProgress();
//        }
//    }
    
}



public class SpeedTestTask
    // JT 18.10.23 todo
    //extends AsyncTask<Void, Void, String>
{
    var cl:    Cloudlet?
    
    func execute(_ inCloudlet:    Cloudlet?)  // JT 18.10.24
    {
         cl = inCloudlet  // JT 18.10.24

        DispatchQueue.global(qos: .default).async
            { [unowned self] in
                self.doInBackground()
        }
    }
    
    
    func  doInBackground() ->String
    {
        
      // defer speedtest    // JT 18.11.06 translate java?
//        cl!.speedTestTaskRunning = true;
//        let speedTestSocket:SpeedTestSocket =  SpeedTestSocket();
//
//        // add a listener to wait for speedtest completion and progress
//        speedTestSocket.addSpeedTestListener( ISpeedTestListener()
//            {
//
//                func onCompletion(_  report: SpeedTestReport) {
//                    // called when download/upload is finished
//                    Log.v(TAG, "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
//                    let divisor =  BigDecimal(BYTES_TO_MBYTES); // BigDecimal todo
//                    mbps = report.getTransferRateBit().divide(divisor);
//                    speedTestProgress = 100;
//                    speedTestTaskRunning = false;
//                    mSpeedTestResultsListener.onBandwidthProgress();
//                }
//
//                func onError(_ speedTestError: SpeedTestError, _ errorMessage:String) {
//                    // called when a download/upload error occur
//                }
//
//                func onProgress( _ percent: Double, _  report: SpeedTestReport) {
//                    // called to notify download/upload progress
//                    //  Log.v(TAG, "[PROGRESS] "+percent + "% - rate in bit/s   : " + report.getTransferRateBit());
//                    Swift.print("[PROGRESS] \(percent)% - rate in bit/s   :\( report.getTransferRateBit())")
//                    let  divisor:BigDecimal =  BigDecimal(BYTES_TO_MBYTES);  // JT 18.10.23
//                    mbps = report.getTransferRateBit().divide(divisor);
//                    speedTestProgress =   percent;
//                    if(mSpeedTestResultsListener != null) {
//                        mSpeedTestResultsListener.onBandwidthProgress();
//                    }
//                }
//        });
//
//        speedTestSocket.startDownload(downloadUri);
        
        return "nil"; // JT 18.10.24
    }
}


    
}

//extension Array where Element: Numeric
//{
//
//    var standardDeviation  : Element
//    { get {
//        let sss = self.reduce((0.0, 0.0)){ return ($0.0 + $1.asDouble, $0.1 + ($1.asDouble * $1.asDouble))}
//        let n = Double(self.count)
//        return Element(sqrt(sss.1/n - (sss.0/n * sss.0/n)))
//        }}
//}
//
