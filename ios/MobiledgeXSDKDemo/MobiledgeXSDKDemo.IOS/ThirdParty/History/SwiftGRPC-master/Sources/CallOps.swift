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

extension gpr_slice {
    mutating func withCCharBaseAddress<R>(
        _ body: (UnsafeMutablePointer<CChar>) throws -> R
    ) rethrows -> R {
        if self.refcount == nil {
            // self.data.inlined.bytes is a UInt8 tuple which cannot be directly cast into a pointer.
            return try withUnsafeMutablePointer(to: &self.data.inlined.bytes) { baseAddress in
                let p = unsafeBitCast(baseAddress, to: UnsafeMutablePointer<CChar>.self)
                return try body(p)
            }
        } else {
            return try body(unsafeBitCast(self.data.refcounted.bytes,
                                          to: UnsafeMutablePointer<CChar>.self))
        }
    }

    // The name of this property must not conflict with any field in the C type.
    var length: Int {
        get {
            if self.refcount == nil {
                return Int(self.data.inlined.length)
            } else {
                return self.data.refcounted.length
            }
        }
    }
}

internal typealias GRPCOp = UnsafeMutablePointer<grpc_op>

internal struct GRPCOpManager {
    typealias GRPCOpFiller = (GRPCOp, GRPCMethod, GRPCContext, GRPCCallOpSet, GRPCMessage) -> Bool
    typealias GRPCOpFinisher = (GRPCContext, GRPCCallOpSet, Int) -> Bool
    let fill:  GRPCOpFiller
    let finish: GRPCOpFinisher
}

internal let grpcOpSendMetadata = GRPCOpManager(fill: opSendMetadataFill, finish: opSendMetadataFinish)
internal let grpcOpRecvMetadata = GRPCOpManager(fill: opRecvMetadataFill, finish: opRecvMetadataFinish)
internal let grpcOpSendObject = GRPCOpManager(fill: opSendObjectFill, finish: opSendObjectFinish)
internal let grpcOpRecvObject = GRPCOpManager(fill: opRecvObjectFill, finish: opRecvObjectFinish)
internal let grpcOpSendClose = GRPCOpManager(fill: opSendCloseFill, finish: opSendCloseFinish)
internal let grpcOpRecvStatus = GRPCOpManager(fill: opRecvStatusFill, finish: opRecvStatusFinish)

internal func opSendMetadataFill(op: GRPCOp, method: GRPCMethod, context: GRPCContext, callOpSet: GRPCCallOpSet, message: GRPCMessage) -> Bool {
    op.pointee.op = GRPC_OP_SEND_INITIAL_METADATA
    op.pointee.data.send_initial_metadata.count = 0
    op.pointee.flags = 0
    op.pointee.reserved = nil
    return true
}

internal func opSendMetadataFinish(context: GRPCContext, callOpSet: GRPCCallOpSet, maxMessageSize: Int) -> Bool {
    return true
}

internal func opRecvMetadataFill(op: GRPCOp, method: GRPCMethod, context: GRPCContext, callOpSet: GRPCCallOpSet, message: GRPCMessage) -> Bool {
    guard !context.initialMetadataReceived else { return false }
    op.pointee.op = GRPC_OP_RECV_INITIAL_METADATA
    grpc_metadata_array_init(&context.recvMetadataArray)
    withUnsafeMutablePointer(to: &context.recvMetadataArray) {
        op.pointee.data.recv_initial_metadata = $0
    }
    op.pointee.flags = 0
    op.pointee.reserved = nil
    return true
}

internal func opRecvMetadataFinish(context: GRPCContext, callOpSet: GRPCCallOpSet, maxMessageSize: Int) -> Bool {
    context.initialMetadataReceived = true
    return true
}

internal func opSendObjectFill(op: GRPCOp, method: GRPCMethod, context: GRPCContext, callOpSet: GRPCCallOpSet, message: GRPCMessage) -> Bool {
    op.pointee.op = GRPC_OP_SEND_MESSAGE
    let serialized = context.serialize(message)
    var slice = gpr_slice_from_copied_buffer(serialized.data, serialized.length)
    op.pointee.data.send_message = grpc_raw_byte_buffer_create(&slice, 1)
    assert(op.pointee.data.send_message != nil)
    op.pointee.flags = 0
    op.pointee.reserved = nil
    return true
}

internal func opSendObjectFinish(context: GRPCContext, callOpSet: GRPCCallOpSet, maxMessageSize: Int) -> Bool {
    return true
}

internal func opRecvObjectFill(op: GRPCOp, method: GRPCMethod, context: GRPCContext, callOpSet: GRPCCallOpSet, message: GRPCMessage) -> Bool {
    callOpSet.messageReceived = false
    op.pointee.op = GRPC_OP_RECV_MESSAGE
    callOpSet.recvBuffer = nil
    withUnsafeMutablePointer(to: &callOpSet.recvBuffer) {
        op.pointee.data.recv_message = $0
    }
    op.pointee.flags = 0
    op.pointee.reserved = nil
    return true
}

internal func opRecvObjectFinish(context: GRPCContext, callOpSet: GRPCCallOpSet, maxMessageSize: Int) -> Bool {
    guard callOpSet.recvBuffer != nil else {
        return true
    }

    assert(!callOpSet.messageReceived, "duplicate receive")
    callOpSet.messageReceived = true

    var reader = grpc_byte_buffer_reader(buffer_in: nil, buffer_out: nil, current: grpc_byte_buffer_reader.__Unnamed_union_current(index: 0))
    grpc_byte_buffer_reader_init(&reader, callOpSet.recvBuffer)

    var sliceReceived = grpc_byte_buffer_reader_readall(&reader)
    let len = sliceReceived.length

    // Deserialize the payload without copying. The payload
    // is used directly within this callback only.
    sliceReceived.withCCharBaseAddress { bytes in
        let msg = GRPCMessage(shareWith: bytes, length: len)
        callOpSet.response = context.deserialize(msg)
    }

    gpr_slice_unref(sliceReceived)
    grpc_byte_buffer_reader_destroy(&reader)
    grpc_byte_buffer_destroy(callOpSet.recvBuffer)
    callOpSet.recvBuffer = nil

    return true
}

internal func opSendCloseFill(op: GRPCOp, method: GRPCMethod, context: GRPCContext, callOpSet: GRPCCallOpSet, message: GRPCMessage) -> Bool {
    op.pointee.op = GRPC_OP_SEND_CLOSE_FROM_CLIENT
    op.pointee.flags = 0
    op.pointee.reserved = nil
    return true
}

internal func opSendCloseFinish(context: GRPCContext, callOpSet: GRPCCallOpSet, maxMessageSize: Int) -> Bool {
    return true
}

internal func opRecvStatusFill(op: GRPCOp, method: GRPCMethod, context: GRPCContext, callOpSet: GRPCCallOpSet, message: GRPCMessage) -> Bool {
    op.pointee.op = GRPC_OP_RECV_STATUS_ON_CLIENT

    context.status = GRPCStatus()
    grpc_metadata_array_init(&context.trailingMetadataArray)
    withUnsafeMutablePointer(to: &context.status.code) {
        op.pointee.data.recv_status_on_client.status = $0
    }
    withUnsafeMutablePointer(to: &context.status.details) {
        op.pointee.data.recv_status_on_client.status_details = $0
    }
    withUnsafeMutablePointer(to: &context.status.detailsLength) {
        op.pointee.data.recv_status_on_client.status_details_capacity = $0
    }
    withUnsafeMutablePointer(to: &context.trailingMetadataArray) {
        op.pointee.data.recv_status_on_client.trailing_metadata = $0
    }

    op.pointee.flags = 0
    op.pointee.reserved = nil
    return true
}

internal func opRecvStatusFinish(context: GRPCContext, callOpSet: GRPCCallOpSet, maxMessageSize: Int) -> Bool {
    return true
}
