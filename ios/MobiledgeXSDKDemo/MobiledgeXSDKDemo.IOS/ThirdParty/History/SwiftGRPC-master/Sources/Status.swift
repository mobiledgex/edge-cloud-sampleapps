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

// Re-export all status codes.
@_exported import CGRPC

// See grpc/include/grpc/impl/codegen/status.h for list of grpc_status_code values.
public struct GRPCStatus {
    // TODO(bryanpkc): These fields are directly written to by the C code, but
    // details and detailsLength ought to be hidden from users; instead an
    // read-only String property should be provided for the status details.
    var code: grpc_status_code = GRPC_STATUS_OK
    var details: UnsafeMutablePointer<CChar>? = nil
    var detailsLength: Int = 0
}
