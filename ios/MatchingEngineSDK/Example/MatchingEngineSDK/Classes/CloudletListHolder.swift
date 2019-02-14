
//  CloudletListHolder.swift
//  MatchingEngineSDK Example
//
// Copyright 2019 MobiledgeX
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

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

    public func setLatencyTestMethod(_ latencyTestMethod: LatencyTestMethod) //
    {
        self.latencyTestMethod = latencyTestMethod //

        Swift.print("String latencyTestMethod= \(latencyTestMethod) enum latencyTestMethod= \(self.latencyTestMethod)")
    }
}
