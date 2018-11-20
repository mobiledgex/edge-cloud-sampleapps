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

// See grpc/impl/codegen/propagation_bits.h for details.
public enum PropagationBits: UInt32 {
    case GRPC_PROPAGATE_DEADLINE = 1
    case GRPC_PROPAGATE_CENSUS_STATS_CONTEXT = 2
    case GRPC_PROPAGATE_CENSUS_TRACING_CONTEXT = 4
    case GRPC_PROPAGATE_CANCELLATION = 8
    case GRPC_PROPAGATE_DEFAULTS = 0xFFFF
}

public class GRPCCall {
    internal typealias grpc_call = OpaquePointer
    internal let referent: grpc_call

    init(context: GRPCContext, method: GRPCMethod, completionQueue: GRPCCompletionQueue) {
        self.referent = grpc_channel_create_call(context.channel.referent, nil,
                                PropagationBits.GRPC_PROPAGATE_DEFAULTS.rawValue,
                                completionQueue.referent, method.name, "",
                                context.deadline, nil)
    }

    deinit {
#if DEBUG
        print("[DEBUG] GRPCCall.deinit: grpc_call_destroy(\(self.referent))")
#endif
        grpc_call_destroy(self.referent)
    }
}
