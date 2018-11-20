// Copyright (C) 2016. Huawei Technologies Co., Ltd. All rights reserved.
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

//import CGRPC  // JT 18.10.27

// This must be used in a precondition to initialize the gRPC runtime.
internal var grpcInitialized = grpcInitializeGRPC()

internal func grpcInitializeGRPC() -> Bool {
    grpc_init()
    assert(atexit(grpc_shutdown) == 0, "atexit() failed")
    return true
}

public class GRPCChannel {
    internal typealias grpc_channel = OpaquePointer
    internal let referent: grpc_channel

    public init(to target: String) {
        precondition(grpcInitialized == true, "cannot initialize GRPC")
        // TODO(bryanpkc): Handle errors (e.g. invalid target specification).
        // TODO(bryanpkc): Support channel arguments.
        self.referent = grpc_insecure_channel_create(target, nil, nil)
    }

    deinit {
#if DEBUG
        print("[DEBUG] GRPCChannel.deinit: grpc_channel_destroy(\(self.referent))")
#endif
        grpc_channel_destroy(self.referent)
    }
}
