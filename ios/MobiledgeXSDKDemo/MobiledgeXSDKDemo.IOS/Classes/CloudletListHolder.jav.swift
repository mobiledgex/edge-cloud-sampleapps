// CloudletListHolder

/**
 * Singleton class to allow access to cloudlet list throughout the app.
 */
public class CloudletListHolder
{
    private static let ourInstance: CloudletListHolder = CloudletListHolder()

    private var mCloudletList = [String: Cloudlet]()
    private var latencyTestMethod: LatencyTestMethod = LatencyTestMethod.ping
    private var latencyTestAutoStart: Bool = false

    public static func getSingleton() -> CloudletListHolder
    {
        return ourInstance
    }

    public enum LatencyTestMethod
    {
        case ping
        case socket
    }

    public func getCloudletList() -> [String: Cloudlet]
    {
        return mCloudletList
    }

    public func setCloudlets(mCloudlets: [String: Cloudlet])
    {
        mCloudletList = mCloudlets
    }

    public func getLatencyTestAutoStart() -> Bool
    {
        return latencyTestAutoStart
    }

    public func setLatencyTestAutoStart(_ latencyTestAutoStart: Bool)
    {
        self.latencyTestAutoStart = latencyTestAutoStart
    }

    public func getLatencyTestMethod() -> LatencyTestMethod
    {
        return latencyTestMethod
    }

    public func setLatencyTestMethod(_ latencyTestMethod: LatencyTestMethod) // JT 18.10.24
    {
        self.latencyTestMethod = latencyTestMethod // JT 18.10.24

        Swift.print("String latencyTestMethod= \(latencyTestMethod) enum latencyTestMethod= \(self.latencyTestMethod)")
    }
}
