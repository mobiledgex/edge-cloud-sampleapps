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

public class GRPCContext {
    typealias GRPCSerializer = (GRPCMessage) -> GRPCMessage
    typealias GRPCDeserializer = (GRPCMessage) -> GRPCMessage

    var sendMetadataArray: UnsafeMutablePointer<grpc_metadata>? = nil
    var recvMetadataArray = grpc_metadata_array(count: 0, capacity: 0, metadata: nil)
    var trailingMetadataArray = grpc_metadata_array(count: 0, capacity: 0, metadata: nil)
    var deadline: gpr_timespec

    let serialize: GRPCSerializer
    let deserialize: GRPCDeserializer

    var status = GRPCStatus()

    var initialMetadataReceived = false
    var rpcMethod: GRPCMethod? = nil
    var channel: GRPCChannel
    var call: GRPCCall? = nil

    public init(channel: GRPCChannel) {
        self.deadline = gpr_inf_future(GPR_CLOCK_REALTIME)
        self.channel = channel

        // TODO(bryanpkc): Allow pluggable serializer/deserializer.
        // Default serializer/deserializer only duplicate the message.
        self.serialize = {
            return GRPCMessage(copyFrom: $0.data, length: $0.length)
        }

        self.deserialize = {
            return GRPCMessage(copyFrom: $0.data, length: $0.length)
        }
    }

    deinit {
        if self.recvMetadataArray.metadata != nil {
#if DEBUG
            print("[DEBUG] GRPCContext.deinit: grpc_metadata_array_destroy(\(self.recvMetadataArray))")
#endif
            grpc_metadata_array_destroy(&self.recvMetadataArray)
        }
        if self.trailingMetadataArray.metadata != nil {
#if DEBUG
            print("[DEBUG] GRPCContext.deinit: grpc_metadata_array_destroy(\(self.trailingMetadataArray))")
#endif
            grpc_metadata_array_destroy(&self.trailingMetadataArray)
        }
    }
}
