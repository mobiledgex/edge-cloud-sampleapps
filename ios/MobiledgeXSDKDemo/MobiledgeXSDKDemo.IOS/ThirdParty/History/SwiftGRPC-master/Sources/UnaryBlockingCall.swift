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

// TODO(bryanpkc): We need an abstraction for all types of calls.
// For now, leave this as a standalone function.
public func grpcUnaryBlockingCall(channel: GRPCChannel, method: GRPCMethod, context: GRPCContext, request: GRPCMessage) -> (GRPCStatus, GRPCMessage?) {
    let completionQueue = GRPCCompletionQueue()
    let call = GRPCCall(context: context, method: method, completionQueue: completionQueue)

    context.call = call
    context.rpcMethod = method

    let callOpSet = GRPCCallOpSet(
        opManagers: [ grpcOpSendMetadata, grpcOpSendObject,
                      grpcOpRecvMetadata, grpcOpRecvObject,
                      grpcOpSendClose, grpcOpRecvStatus ],
        context: context
    )

    callOpSet.startBatch(call: call, context: context, request: request)

    var ok: Bool
    var tag: GRPCTag = nil
    var queueStatus: GRPCCompletionQueue.OperationStatus
    repeat {
        (ok, tag, queueStatus) = completionQueue.commitOpsAndWaitDeadline(deadline: context.deadline)
        assert(queueStatus == .GRPC_COMPLETION_QUEUE_GOT_EVENT, "grpc_commit_ops_and_wait_deadline failed")
        assert(ok)
    } while tag != callOpSet.userTag

    assert(context.status.code == GRPC_STATUS_OK)

    completionQueue.shutdownAndWait() // completionQueue will deinit itself at end of scope

    context.call = nil // release and deinit call
    context.rpcMethod =  nil // release method

    return (context.status, callOpSet.response)
}
