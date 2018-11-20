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
import Glibc

public class GRPCMessage {
    public let length: Int

    internal var data: UnsafeMutablePointer<CChar>
    internal let isSharedBuffer: Bool

    // Once a GRPCMessage has been created, its payload is immutable to users.
    public var payload: UnsafePointer<CChar> {
        get {
            return UnsafePointer(self.data)
        }
    }

    // Create a GRPCMessage by copying from a user-supplied buffer.
    public init(copyFrom buffer: UnsafePointer<CChar>, length: Int) {
        data = UnsafeMutablePointer<CChar>.allocate(capacity: length)
        memcpy(data, buffer, length)
        self.length = length
        isSharedBuffer = false
    }

    // Internally we can create a GRPCMessage from a shared buffer.
    // This library must ensure that the buffer does not get clobbered
    // or deallocated while the GRPCMessage is in use.
    internal init(shareWith buffer: UnsafeMutablePointer<CChar>, length: Int) {
        data = buffer
        self.length = length
        isSharedBuffer = true
    }

    deinit {
        if !isSharedBuffer {
#if DEBUG
            print("[DEBUG] GRPCMessage.deinit: \(data).deallocate(capacity: \(length))")
#endif
            data.deallocate(capacity: length)
        }
    }
}
