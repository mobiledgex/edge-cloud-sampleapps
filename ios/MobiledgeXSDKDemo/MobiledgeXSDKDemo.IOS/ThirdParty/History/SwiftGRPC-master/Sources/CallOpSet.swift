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

internal typealias GRPCTag = UnsafeMutableRawPointer?

internal class GRPCCallOpSet {
    internal static let maxOpCount = 8

    // TODO(bryanpkc): Eliminate GRPCTag if it is not needed.
    var userTag: GRPCTag = nil
    var userDone: UnsafeMutablePointer<Bool>? = nil

    let opManagers: [GRPCOpManager]
    let context: GRPCContext

    // Used by individual operations
    var response: GRPCMessage? = nil
    var recvBuffer: UnsafeMutablePointer<grpc_byte_buffer>? = nil
    var messageReceived = false

    // If true, the event tagged by this GRPCCallOpSet will not be emitted from the completion queue wrapper
    var hideFromUser = false

    init(opManagers: [GRPCOpManager], context: GRPCContext) {
        self.opManagers = opManagers
        self.context = context
        self.userTag = Unmanaged.passUnretained(self).toOpaque()
    }

    func fillOp(rpcMethod: GRPCMethod, context: GRPCContext, message: GRPCMessage, ops: GRPCOp) -> Int {
        var numFilled = 0
        for manager in self.opManagers {
            // guard manager.fill != nil else { continue }
            let result = manager.fill(ops.advanced(by: numFilled), rpcMethod, context, self, message)
            if (result) {
                numFilled += 1
            }
        }
        return numFilled
    }

    func finishOp(context: GRPCContext) -> Bool {
        var count = 0
        var allStatus = true
        for manager in self.opManagers {
            // guard manager.finish != nil else { continue }
            let size = 100 // TODO(bryanpkc): hook up this value
            let status = manager.finish(context, self, size)
            allStatus = (allStatus && status)
            count += 1
        }
        return allStatus
    }

    func startBatch(call: GRPCCall, context: GRPCContext, request: GRPCMessage) {
        guard let rpcMethod = context.rpcMethod else {
            fatalError("context.rpcMethod cannot be nil")
        }
        let ops = GRPCOp.allocate(capacity: GRPCCallOpSet.maxOpCount)
        let nops = fillOp(rpcMethod: rpcMethod, context: context, message: request, ops: ops)
        // Use address of this GRPCCallOpSet as tag for the completion queue; be sure to retain the object
        let tag = UnsafeMutableRawPointer(Unmanaged.passRetained(self).toOpaque())
        let result = grpc_call_start_batch(call.referent, ops, nops, tag, nil)
        assert(result == GRPC_CALL_OK, "grpc_call_start_batch failed")
    }
}
