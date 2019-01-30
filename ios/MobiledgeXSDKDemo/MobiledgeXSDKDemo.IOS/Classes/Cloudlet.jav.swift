

import Foundation

import Alamofire
import GoogleMaps
import SwiftSocket // JT 19.01.16 to connection latency

private var cl: Cloudlet?

public class Cloudlet // implements Serializable? todo?
{
    private static let TAG: String = "Cloudlet"
    public static let BYTES_TO_MBYTES: Int = 1024 * 1024

    var mCloudletName: String = "" // note legacy m prefix nameing convention
    private var mAppName: String = ""
    private var mCarrierName: String = ""

    private var mLatitude: Double = 0
    private var mLongitude: Double = 0

    private var mDistance: Double = 0
    private var bestMatch: Bool = false

    private var mMarker: GMSMarker? // map marker, POI

    var latencyMin: Double = 9999.0
    var latencyAvg: Double = 0
    var latencyMax: Double = 0
    var latencyStddev: Double = 0
    var latencyTotal: Double = 0

    var pings: [String] = [String]()
    var latencies = [Double]()
    var future:Future<[String: AnyObject], Error>? // async result (captured by async?) // JT 19.01.16
    
    private var mbps: Int64 = 0 // BigDecimal.valueOf(0);  // JT 18.10.23 todo?
    // var latencyTestProgress: Double = 0
    private var speedTestProgress: Double = 0 // 0-1  //  updating
    var startTime: Double = 0 // Int64
    var startTime1: DispatchTime?
    var timeDifference: Double = 0
    var mNumPackets: Int = 4 // number of pings
    private var mNumBytes: Int = 1_048_576
    private var runningOnEmulator: Bool = false
    var pingFailed: Bool = false

    private var mBaseUri: String = ""
    private var downloadUri: String = "" // rebuilt at runtime
    private var socketdUri: String = "" // rebuilt at runtime
   var hostName: String = ""
    var openPort: Int = 7777
    let socketTimeout: Int = 3000
    var latencyTestTaskRunning: Bool = false
    var speedTestTaskRunning: Bool = false
    private var uri: String = ""
    private var theFQDN_prefix: String = "" // JT 19.01.30

    init()
    {}

    init(_ cloudletName: String,
         _ appName: String,
         _ carrierName: String,
         _ gpsLocation: CLLocationCoordinate2D,
         _ distance: Double,
         _ uri: String,
         _ urlPrefix: String,   // JT 19.01.30
         _ marker: GMSMarker,
         _ numBytes: Int,
         _ numPackets: Int) // LatLng
    {
        Swift.print("Cloudlet contructor. cloudletName= \(cloudletName)")

        update(cloudletName, appName, carrierName, gpsLocation, distance, uri, urlPrefix, marker, numBytes, numPackets) // JT 19.01.30

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
                       _ urlPrefix: String,   // JT 19.01.30
                       _ marker: GMSMarker,
                       _ numBytes: Int,
                       _ numPackets: Int) // # packets to ping

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

        mBaseUri = uri
        theFQDN_prefix = urlPrefix  // JT 19.01.30

        setDownloadUri(uri)

        let numPings = Int(UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "5")

        runLatencyTest(numPings: numPings!)
    }

    func runLatencyTest(numPings: Int)
    {
        if latencyTestTaskRunning
        {
            Swift.print("LatencyTest already running")
            SKToast.show(withMessage: "LatencyTest already running")
            return
        }
        latencyTestTaskRunning = true

        let azure = uri.range(of: "azure") != nil   // JT 19.01.30
        if uri != "" //&& // JT 19.01.30
        {
            Swift.print("uri: \(uri)")
            // Ping several times
            latencies.removeAll()
            pings.removeAll()

            for _ in 0 ..< numPings
            {
                if azure
                {
                    pings.append(socketdUri)    // JT 19.01.30
                }
                else
                {
                    pings.append(uri) //  N pings
                }
            
                
            }

            pingNext()
        }

        latencyTestTaskRunning = false //

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

        let host = pings.removeFirst()

        let latencyTestMethod = UserDefaults.standard.string(forKey: "LatencyTestMethod")

        var useSocket = false
        Swift.print("uri \(uri)")   // JT 19.01.30
       if ( uri.contains("azure"))
        {
            useSocket = true    // JT 19.01.30
        }

        if useSocket    // &&  latencyTestMethod == "Socket"
        {
            future = GetSocketLatency(host, 7777)   // JT 19.01.30
            
            future!.on(success:
                {
                    let d = $0 as [String: Any]
                    
                    print("GetSocketLatency: \(d["latency"])")
                    let duration = Double(d["latency"] as! String  ) // JT 19.01.16

                    // print("\(ping) latency (ms): \(latency)")
                    self.latencies.append(duration! * 1000)  // ms JT 19.01.16
                    self.latencyMin = self.latencies.min()!
                    self.latencyMax = self.latencies.max()!
                    
                    let sumArray = self.latencies.reduce(0, +)
                    
                    self.latencyAvg = sumArray / Double(self.latencies.count)
                    
                    self.latencyStddev = standardDeviation(arr: self.latencies)
                    
                    let latencyMsg = String(format: "%4.3f", self.latencyAvg)
                    
                    NotificationCenter.default.post(name: NSNotification.Name(rawValue: "latencyAvg"), object: latencyMsg)
                    
            },
                failure: { print("Socket failed with error: \($0)")
                    Swift.print("XXX")
            },
                completion: { let _ = $0    //print("completed with result: \($0)" )
                                                    
            })
        }
        else
        {
            // Ping once
            let pingOnce = SwiftyPing(host: host, configuration: PingConfiguration(interval: 0.5, with: 5), queue: DispatchQueue.global())
            pingOnce?.observer = { _, response in
                let duration = response.duration
                print("cloudlet latency: \(duration)")  // JT 19.01.28
                pingOnce?.stop()
                
                // print("\(ping) latency (ms): \(latency)")
                self.latencies.append(response.duration * 1000) // JT 19.01.14
                self.latencyMin = self.latencies.min()!
                self.latencyMax = self.latencies.max()!
                
                let sumArray = self.latencies.reduce(0, +)
                
                self.latencyAvg = sumArray / Double(self.latencies.count)
                
                self.latencyStddev = standardDeviation(arr: self.latencies)
                
                let latencyMsg = String(format: "%4.3f", self.latencyAvg)
                
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "latencyAvg"), object: latencyMsg)
            }
            pingOnce?.start() // JT 19.01.14
        }
     
    }

    /**
     * From the given string, create the hostname that will be pinged,
     * and the URI that will be downloaded from.
     * @param uri
     */
    public func setDownloadUri(_ uri: String)
    {
        if mCarrierName.caseInsensitiveCompare("TDG") == .orderedSame
        {
            openPort = 443 // JT 18.11.16 unused
            hostName = uri
            //       downloadUri = "https://\(hostName)/mobiledgexsdkdemohttp/getdata?numbytes=\(mNumBytes)"
            openPort = 7777

            let downLoadStringSize = UserDefaults.standard.string(forKey: "Download Size") ?? "1 MB"
            let n = downLoadStringSize.components(separatedBy: " ")

            mNumBytes = Int(n[0])! * 1_048_576

            downloadUri = "http://\(hostName):\(openPort)/getdata?numbytes=\(mNumBytes)"
            Swift.print("downloadUri1: \(downloadUri)") // DEBUG
            
            socketdUri = hostName   // JT 19.01.30
        }
        else
        {
            openPort = 7777
            hostName = theFQDN_prefix + uri   // JT 19.01.30
            downloadUri = "http://\(hostName):\(openPort)/getdata?numbytes=\(mNumBytes)"
            Swift.print("downloadUri: \(downloadUri)") // DEBUG
            
            socketdUri = hostName   // JT 19.01.30
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

        latencyTestTaskRunning = true //

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

        var latencyTestMethod: CloudletListHolder.LatencyTestMethod
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
            Swift.print("LatencyTestTaskSocket todo?") // JT 18.10.23
            //  LatencyTestTaskSocket().execute();
        }
        else if latencyTestMethod == CloudletListHolder.LatencyTestMethod.ping
        {
            Swift.print("LatencyTestTaskPing todo?") // JT 18.10.23
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

    public func getSpeedTestProgress() -> Double
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
            SKToast.show(withMessage: "SpeedTest already running") // UI

            return
        }
        speedTestTaskRunning = true //  // JT 19.01.14 todo how to reset this on cancel.

        setDownloadUri(mBaseUri) // so we have current B bytes to download appended
        Swift.print("doSpeedTest\n  \(downloadUri)") // DEBUG
        startTime1 = DispatchTime.now() // <<<<<<<<<< Start time
        //  let todoEndpoint: String = "https://jsonplaceholder.typicode.com/todos/1"
        Alamofire.request(downloadUri)
            .downloadProgress(queue: DispatchQueue.global(qos: .utility))
        { progress in
            //     print("Progress: \(progress.fractionCompleted)")
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "speedTestProgress"), object: progress.fractionCompleted) //

            self.speedTestProgress = progress.fractionCompleted //
        }
        .responseString
        { response in
            self.speedTestTaskRunning = false //
            // check for errors
            guard response.result.error == nil else
            {
                // got an error in getting the data, need to handle it
                print("error doSpeedTest")
                print(response.result.error!)
                
                DispatchQueue.main.async
                    {
                        CircularSpinner.hide() //   // JT 19.01.30
                }
                return
            }
            let end = DispatchTime.now() // <<<<<<<<<<   end time
            let nanoTime = end.uptimeNanoseconds - self.startTime1!.uptimeNanoseconds // <<<<< Difference in nano seconds (UInt64)
            let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
            print("Time: \(timeInterval) seconds")
            let tranferRateD = Double(self.mNumBytes) / timeInterval
            let tranferRate = Int(tranferRateD)

            Swift.print("[COMPLETED] rate in bit/s   : \(tranferRate * 8)") // Log

            SKToast.show(withMessage: "[COMPLETED] rate in MBs   : \(Double(tranferRate) / (1024 * 1024.0))") // UI
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "tranferRate"), object: tranferRate) // post
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

func standardDeviation(arr: [Double]) -> Double //
{
    let length = Double(arr.count)
    let avg = arr.reduce(0, { $0 + $1 }) / length
    let sumOfSquaredAvgDiff = arr.map { pow($0 - avg, 2.0) }.reduce(0, { $0 + $1 })

    return sqrt(sumOfSquaredAvgDiff / length)
}

func GetSocketLatency(_ host: String, _ port: Int32, _ postMsg: String? = nil)  -> Future<[String: AnyObject], Error>
    
{
    let promise = Promise<[String: AnyObject], Error>() // completion callback
    
    DispatchQueue.global(qos: .background).async
    {
        let time = measure1
        {
            let client = TCPClient(address: host, port: port) // JT 19.01.16 SwiftSocket
           let result = client.connect( timeout: 10 )    // JT

            client.close() // JT 19.01.16
    //        Swift.print("client connect \(result)") // JT 19.01.30
            
            //                 promise.failure(value: ["latency": latencyMsg as AnyObject])  // JT 19.01.16

        }

 //       print("host: \(host)\n Latency \(time / 1000.0) ms") // JT 19.01.16
        if time > 0
        {
            let latencyMsg = String(format: "%4.2f", time ) //
            
            if postMsg != nil && postMsg != ""
            {
                let latencyMsg2 = String(format: "%4.2f", time * 1000 ) // ms
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postMsg!), object: latencyMsg2)
            }
            promise.succeed(value: ["latency": latencyMsg as AnyObject])  // JT 19.01.16
        }
        else
        {
            Swift.print("")
        }
        
        
    }
    
    return promise.future
}

func measure<T>(task: () -> T) -> Double
{
    let startTime = CFAbsoluteTimeGetCurrent()
    _ = task() // JT 19.01.16
    let endTime = CFAbsoluteTimeGetCurrent()

    let result = endTime - startTime

    return result
}

func measure1<T>(task: () -> T) -> Double
{
    let startTime = DispatchTime.now() // JT 19.01.16
    _ = task() // JT 19.01.16
    let endTime = DispatchTime.now()

    let nanoTime = endTime.uptimeNanoseconds - startTime.uptimeNanoseconds

    let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests

    return timeInterval
}
