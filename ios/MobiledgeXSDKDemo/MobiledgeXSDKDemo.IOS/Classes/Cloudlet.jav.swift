
// JT 18.10.23
import Foundation

import GoogleMaps // JT 18.10.23
import PlainPing // for latency
import Alamofire    // JT 18.11.16

private var cl: Cloudlet? // JT 18.11.01

public class Cloudlet // implements Serializable
{
    private static let TAG: String = "Cloudlet"
    public static let BYTES_TO_MBYTES: Int = 1024 * 1024

    var mCloudletName: String = ""  // JT 18.11.22 leave lagacy m prefix nameing convention
    private var mAppName: String = ""
    private var mCarrierName: String = ""
    
    private var mLatitude: Double = 0
    private var mLongitude: Double = 0
    
    private var mDistance: Double = 0
    private var bestMatch: Bool = false
    
    private var mMarker: GMSMarker? // JT 18.10.23

    var latencyMin: Double = 9999.0
    var latencyAvg: Double = 0
    var latencyMax: Double = 0
    var latencyStddev: Double = 0
    var latencyTotal: Double = 0

    var pings: [String] = [String]() // JT 18.11.13 // JT 18.11.22
    var latencies = [Double]() // JT 18.11.13

    private var mbps: Int64 = 0 // BigDecimal.valueOf(0);  // JT 18.10.23 todo?
    //var latencyTestProgress: Double = 0
    private var speedTestProgress: Double = 0 // 0-1  // JT 18.11.22 updating
    var startTime: Double = 0 // Int64 // JT 18.10.24
       var startTime1:DispatchTime? // JT 18.11.16
    var timeDifference: Double = 0
    var mNumPackets: Int = 4
    private var mNumBytes: Int = 1_048_576
    private var runningOnEmulator: Bool = false
    var pingFailed: Bool = false

    //    var mSpeedTestResultsListener: SpeedTestResultsListener? // JT 18.11.11

    private var mBaseUri: String = ""    // JT 18.11.17
     private var downloadUri: String = ""   // JT 18.11.17 rebuilt at runtime
    var hostName: String = ""
    var openPort: Int = 7777
    let socketTimeout: Int = 3000
    var latencyTestTaskRunning: Bool = false
    var speedTestTaskRunning: Bool = false
    private var uri: String = ""

    init()
    {}

    init(_ cloudletName: String,
         _ appName: String,
         _ carrierName: String,
         _ gpsLocation: CLLocationCoordinate2D,
         _ distance: Double,
         _ uri: String,
         _ marker: GMSMarker,
         _ numBytes: Int,
         _ numPackets: Int) // LatLng    // JT 18.11.06
    {
        Swift.print("Cloudlet contructor. cloudletName= \(cloudletName)")

        update(cloudletName, appName, carrierName, gpsLocation, distance, uri, marker, numBytes, numPackets)

        if CloudletListHolder.getSingleton().getLatencyTestAutoStart()
        {
            // All AsyncTask instances are run on the same thread, so this queues up the tasks.
            startLatencyTest()
        }
        else
        {
            Swift.print("LatencyTestAutoStart is disabled")
        }
    }

    public func update(_ cloudletName: String,
                       _ appName: String,
                       _ carrierName: String,
                       _ gpsLocation: CLLocationCoordinate2D,
                       _ distance: Double,
                       _ uri: String,
                       _ marker: GMSMarker,
                       _ numBytes: Int,
                       _ numPackets: Int    // # packets to ping
        )
    {
        Swift.print("Cloudlet update. cloudletName= \(cloudletName)")

        mCloudletName = cloudletName
        mAppName = appName
        mCarrierName = carrierName
        mLatitude = gpsLocation.latitude
        mLongitude = gpsLocation.longitude
        mDistance = distance
        mMarker = marker
        mNumBytes = numBytes
        mNumPackets = numPackets
        
        mBaseUri = uri  // JT 18.11.17
        setDownloadUri(uri) // JT 18.11.17


        let numPings = Int(UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "5" )  // JT 18.11.16

        runLatencyTest(numPings:numPings!) // JT 18.11.13
    }

    func runLatencyTest(numPings: Int)
    {
        if latencyTestTaskRunning
        {
            Swift.print("LatencyTest already running")
            SKToast.show(withMessage: "LatencyTest already running")    // JT 18.11.18
            return
        }
        latencyTestTaskRunning = true  // JT 18.11.18
        
        if uri != "" && uri.range(of: "azure") == nil // JT 18.11.13
        {
            Swift.print("uri: \(uri)")
            // Ping several times
            latencies.removeAll()
            pings.removeAll()

            
            for _ in 0 ..< numPings
            {
                pings.append(uri) //  N pings
            }

            pingNext()
        }
        
        latencyTestTaskRunning = false  // JT 18.11.18
        
        // post upateLatencies
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateLatencies"), object: nil)
    }

    deinit
    {
        NotificationCenter.default.removeObserver(self)
    }

    func pingNext()
    {
        guard pings.count > 0 else
        {
            return
        }

        let ping = pings.removeFirst()
        PlainPing.ping(ping, withTimeout: 1.0, completionBlock: { (timeElapsed: Double?, error: Error?) in
            if let latency = timeElapsed
            {
               // print("\(ping) latency (ms): \(latency)") // JT 18.11.27
                self.latencies.append(latency)
                self.latencyMin = self.latencies.min()! // JT 18.11.13
                self.latencyMax = self.latencies.max()! // JT 18.11.13

                let sumArray = self.latencies.reduce(0, +)

                self.latencyAvg = sumArray / Double(self.latencies.count)

                self.latencyStddev = standardDeviation(arr: self.latencies) // JT 18.11.13

                
                let latencyMsg = String( format: "%4.3f", self.latencyAvg ) // JT 18.12.11  // JT 18.12.20
                
               NotificationCenter.default.post(name: NSNotification.Name(rawValue: "latencyAvg"), object: latencyMsg)       // JT 18.12.12
                
            }
            if let error = error
            {
                print("error: \(error.localizedDescription)")
            }
            self.pingNext()
        })
    }



    /**
     * From the given string, create the hostname that will be pinged,
     * and the URI that will be downloaded from.
     * @param uri
     */
    public func setDownloadUri(_ uri: String)   // JT 18.11.17
    {
        if mCarrierName.caseInsensitiveCompare("TDG") == .orderedSame
        {
            openPort = 443  // JT 18.11.16 unused
            hostName = uri
     //       downloadUri = "https://\(hostName)/mobiledgexsdkdemohttp/getdata?numbytes=\(mNumBytes)"
            openPort = 7777

            let downLoadStringSize  = UserDefaults.standard.string(forKey: "Download Size") ?? "1 MB"
            let n = downLoadStringSize.components(separatedBy: " ")
            
            mNumBytes = Int(n[0])! * 1_048_576  // JT 18.11.18
            
            downloadUri = "http://\(hostName):\(openPort)/getdata?numbytes=\(mNumBytes)"
           Swift.print("downloadUri1: \(downloadUri)")  // JT 18.11.16
        }
        else
        {
            openPort = 7777
            hostName = "mobiledgexsdkdemo." + uri
            downloadUri = "http://\(hostName):\(openPort)/getdata?numbytes=\(mNumBytes)"
            Swift.print("downloadUri: \(downloadUri)")  // JT 18.11.16
        }
        self.uri = uri
    }

    public func getUri() -> String
    {
        return uri
    }

    public func toString() -> String
    {
        return "mCarrierName=\(mCarrierName) mCloudletName=\(mCloudletName) mLatitude=\(mLatitude) mLongitude=\(mLongitude) mDistance=\(mDistance) uri=\(uri)"
    }


    public func startLatencyTest()
    {
        Swift.print("startLatencyTest()")
        if latencyTestTaskRunning
        {
            Swift.print("LatencyTest already running")
            return
        }

        latencyTestTaskRunning = true   // JT 18.11.18
        
        latencyMin = 9999
        latencyAvg = 0
        latencyMax = 0
        latencyStddev = 0
        latencyTotal = 0

        // ping can't run on an emulator, so detect that case.
        //  Swift.print("PRODUCT= \(Build.PRODUCT)")

        if isSimulator
        {
            runningOnEmulator = true
            // Log.i(TAG, "YES, I am an emulator.");
            Swift.print("YES, I am an emulator/simulator.")
        }
        else
        {
            runningOnEmulator = false
            Swift.print("NO, I am NOT an emulator/simulator.")
        }

        var latencyTestMethod: CloudletListHolder.LatencyTestMethod // CloudletListHolder.  // JT 18.11.01
            = CloudletListHolder.getSingleton().getLatencyTestMethod()

        if mCarrierName.caseInsensitiveCompare("azure") == .orderedSame
        {
            Swift.print("Socket test forced for Azure")

            latencyTestMethod = CloudletListHolder.LatencyTestMethod.socket
        }
        if runningOnEmulator
        {
            Swift.print("Socket test forced for emulator")
            latencyTestMethod = CloudletListHolder.LatencyTestMethod.socket
        }

        if latencyTestMethod == CloudletListHolder.LatencyTestMethod.socket
        {
            Swift.print("LatencyTestTaskSocket todo") // JT 18.10.23
            //  LatencyTestTaskSocket().execute();
        }
        else if latencyTestMethod == CloudletListHolder.LatencyTestMethod.ping
        {
            Swift.print("LatencyTestTaskPing todo") // JT 18.10.23
            // LatencyTestTaskPing().execute();
        }
        else
        {
            Swift.print("Unknown latencyTestMethod: \(latencyTestMethod) ")
        }
    }

    
  
    
    

    public func getCloudletName() -> String
    {
        return mCloudletName
    }

    public func setCloudletName(_ mCloudletName: String)
    {
        self.mCloudletName = mCloudletName
    }

    public func getCarrierName() -> String
    {
        return mCarrierName
    }

    public func setCarrierName(_ mCarrierName: String)
    {
        self.mCarrierName = mCarrierName
    }

    public func getLatitude() -> Double
    {
        return mLatitude
    }

    public func setLatitude(Latitude: Double)
    {
        mLatitude = Latitude
    }

    public func getLongitude() -> Double
    {
        return mLongitude
    }

    public func setLongitude(mLongitude: Double)
    {
        self.mLongitude = mLongitude
    }

    public func getDistance() -> Double
    {
        return mDistance
    }

    public func setDistance(_ mDistance: Double)
    {
        self.mDistance = mDistance
    }

    public func getMarker() -> GMSMarker
    { return mMarker! }

    public func setMarker(_ mMarker: GMSMarker) { self.mMarker = mMarker }

    public func isBestMatch() -> Bool { return bestMatch }

    public func setBestMatch(_ bestMatch: Bool) { self.bestMatch = bestMatch }

    public func getLatencyMin() -> Double
    {
        return latencyMin
    }

    public func getLatencyAvg() -> Double
    {
        return latencyAvg
    }

    public func getLatencyMax() -> Double
    {
        return latencyMax
    }

    public func getLatencyStddev() -> Double
    {
        return latencyStddev
    }

    public func getMbps() -> Int64 // JT 18.10.23 BigDecimal
    {
        return mbps
    }

//    public func getLatencyTestProgress() -> Double
//    {
//        return latencyTestProgress
//    }

    public func getSpeedTestProgress() -> Double    // JT 18.11.22
    {
        return speedTestProgress
    }

    public func isPingFailed() -> Bool
    {
        return pingFailed
    }

    public func setPingFailed(_ pingFailed: Bool)
    {
        self.pingFailed = pingFailed
    }

    public func isLatencyTestTaskRunning() -> Bool
    {
        return latencyTestTaskRunning
    }

    public func setLatencyTestTaskRunning(_ latencyTestTaskRunning: Bool)
    {
        self.latencyTestTaskRunning = latencyTestTaskRunning
    }

    public func getAppName() -> String
    {
        return mAppName
    }

    public func setAppName(_ mAppName: String)
    {
        self.mAppName = mAppName
    }

    public func getNumPackets() -> Int
    { return mNumPackets }

    public func setNumPackets(_ mNumPings: Int) { mNumPackets = mNumPings }

    public func getNumBytes() -> Int
    { return mNumBytes }

    public func setNumBytes(_ mNumBytes: Int) { self.mNumBytes = mNumBytes }

    var isSimulator: Bool
    {
        #if arch(i386) || arch(x86_64)
            return true
        #else
            return false
        #endif
    }
    
    func doSpeedTest()
    {
        
        if speedTestTaskRunning
        {
            Swift.print("SpeedTest already running")
            SKToast.show(withMessage: "SpeedTest already running")    // JT 18.11.16
            
            return
        }
        speedTestTaskRunning = true // JT 18.11.18
        
        setDownloadUri( mBaseUri)    // JT 18.11.17 so we have current B bytes to download appended
        Swift.print("doSpeedTest\n  \(downloadUri)")
      startTime1 = DispatchTime.now() // <<<<<<<<<< Start time
      //  let todoEndpoint: String = "https://jsonplaceholder.typicode.com/todos/1"
        Alamofire.request(downloadUri)
            .downloadProgress(queue: DispatchQueue.global(qos: .utility)) { progress in
           //     print("Progress: \(progress.fractionCompleted)")
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "speedTestProgress"), object: progress.fractionCompleted)   // JT 18.11.16

                self.speedTestProgress = progress.fractionCompleted    // JT 18.11.22
            }
            .responseString
            { response in
                self.speedTestTaskRunning = false    // JT 18.11.18
                // check for errors
                guard response.result.error == nil else {
                    // got an error in getting the data, need to handle it
                    print("error doSpeedTest")
                    print(response.result.error!)
                    return
                }
                let end = DispatchTime.now()   // <<<<<<<<<<   end time
                let nanoTime = end.uptimeNanoseconds - self.startTime1!.uptimeNanoseconds // <<<<< Difference in nano seconds (UInt64)
                let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
                print("Time: \(timeInterval) seconds")
                let tranferRateD = Double(self.mNumBytes)/timeInterval
                let tranferRate = Int(tranferRateD) // JT 18.11.16

                Swift.print("[COMPLETED] rate in bit/s   : \(tranferRate * 8)" )   // JT 18.11.16

                SKToast.show(withMessage: "[COMPLETED] rate in MBs   : \(Double(tranferRate) / (1024*1024.0))")    // JT 18.11.16
               NotificationCenter.default.post(name: NSNotification.Name(rawValue: "tranferRate"), object: tranferRate) // JT 18.11.09


                
        }
    }
}



extension UIDevice
{
    var isSimulator: Bool
    {
        #if arch(i386) || arch(x86_64)
            return true
        #else
            return false
        #endif
    }
}

func standardDeviation(arr: [Double]) -> Double // JT 18.11.13
{
    let length = Double(arr.count)
    let avg = arr.reduce(0, { $0 + $1 }) / length
    let sumOfSquaredAvgDiff = arr.map { pow($0 - avg, 2.0) }.reduce(0, { $0 + $1 })
    
    return sqrt(sumOfSquaredAvgDiff / length)
}
