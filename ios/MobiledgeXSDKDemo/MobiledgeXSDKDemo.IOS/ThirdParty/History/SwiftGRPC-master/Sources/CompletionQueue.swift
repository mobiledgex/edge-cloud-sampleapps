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

public class GRPCCompletionQueue {
    enum OperationStatus {
        case GRPC_COMPLETION_QUEUE_SHUTDOWN   /// The completion queue has been shut down.
        case GRPC_COMPLETION_QUEUE_GOT_EVENT  /// Got a new event, \a tag will be filled in with its associated value; \a ok indicating its success.
        case GRPC_COMPLETION_QUEUE_TIMEOUT    /// Deadline was reached.
    }

    internal typealias grpc_completion_queue = OpaquePointer
    let referent: grpc_completion_queue

    public init() {
        self.referent = grpc_completion_queue_create(nil)
    }

    deinit {
#if DEBUG
        print("[DEBUG] GRPCCompletionQueue.deinit: grpc_completion_queue_destroy(\(self.referent))")
#endif
        grpc_completion_queue_destroy(self.referent)
    }

    func shutdown() {
        grpc_completion_queue_shutdown(self.referent)
    }

    func shutdownAndWait() {
        self.shutdown()
        var status: OperationStatus
        repeat {
            (_, _, status) = commitOpsAndWait()
        } while status != .GRPC_COMPLETION_QUEUE_SHUTDOWN
    }

    func commitOpsAndWait() -> (Bool, GRPCTag, OperationStatus) {
        return commitOpsAndWaitDeadline(deadline: gpr_inf_future(GPR_CLOCK_REALTIME))
    }

    func commitOpsAndWaitDeadline(deadline: gpr_timespec) -> (Bool, GRPCTag, OperationStatus) {
        while (true) {
            let event = grpc_completion_queue_next(referent, deadline, nil)
            switch event.type {
            case GRPC_QUEUE_TIMEOUT:
                return (true, nil, .GRPC_COMPLETION_QUEUE_TIMEOUT)
            case GRPC_QUEUE_SHUTDOWN:
                return (true, nil, .GRPC_COMPLETION_QUEUE_SHUTDOWN)
            case GRPC_OP_COMPLETE:
                let callOpSet = Unmanaged<GRPCCallOpSet>.fromOpaque(event.tag).takeRetainedValue()
                let status = callOpSet.finishOp(context: callOpSet.context)
                if callOpSet.hideFromUser {
                    continue
                }
                let ok = (status && event.success != 0)
                return (ok, callOpSet.userTag, .GRPC_COMPLETION_QUEUE_GOT_EVENT)
            default:
                fatalError("unexpected grpc_completion_type value")
            }
        }
    }
}
