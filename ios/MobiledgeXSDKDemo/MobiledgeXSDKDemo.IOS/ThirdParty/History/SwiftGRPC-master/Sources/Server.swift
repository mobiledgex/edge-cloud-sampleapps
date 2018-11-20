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

import CGRPC

typealias GRPCServiceMethod = String

public class GRPCServer {
    internal typealias grpc_server = OpaquePointer
    internal let referent: grpc_server

    internal let completionQueue: GRPCCompletionQueue
    internal var port: Int32

    internal var started = false

    internal typealias GRPCMethodTag = Int
    internal var methods: [GRPCMethodTag:GRPCServiceMethod] = [:]

    init(listenAt address: String) {
        precondition(grpcInitialized == true, "cannot initialize GRPC")
        // TODO(bryanpkc): Support channel arguments.
        referent = grpc_server_create(nil, nil)
        completionQueue = GRPCCompletionQueue()
        grpc_server_register_completion_queue(referent, completionQueue.referent, nil)
        // TODO(bryanpkc): Handle errors (e.g. invalid address specification).
        port = grpc_server_add_insecure_http2_port(referent, address)
    }

    func registerCall(name: String, host: String?) {
        assert(!started, "cannot register call after server has been started")
        // TODO(bryanpkc): Use GRPC_SRM_PAYLOAD_NONE for a method that has no parameter.
        // This information needs to come from the generated code for the method.
        let ptr = grpc_server_register_method(referent, name, host, GRPC_SRM_PAYLOAD_READ_INITIAL_BYTE_BUFFER, 0)
        if let methodPointer = ptr {
            let tag = unsafeBitCast(methodPointer, to: GRPCMethodTag.self)
            methods[tag] = name
        } else {
            fatalError("attempted to register \(name) multiple times")
        }
    }

    func start() {
        grpc_server_start(referent)
        started = true
    }

    // Wait for the server to shut down. Note that some other thread must be
    // responsible for shutting down the server for this call to ever return.
    func wait() {
        fatalError("unimplemented")
    }
}
