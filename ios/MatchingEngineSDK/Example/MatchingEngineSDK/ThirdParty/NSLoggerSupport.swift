//
//  NSLoggerSupport.swift
//  NSLoggerExample
//
//  Created by Kawajiri Ryoma on 4/19/15.
//  Copyright (c) 2015 Strobo Inc. All rights reserved.
//

import NSLogger

func LogMessage(domain: String!, level: Int32, format: String!, args: CVarArgType...) {
  LogMessage_va(domain, level, format, getVaList(args))
}

func LogMessageF(filename: UnsafePointer<Int8>, lineNumber: Int32, functionName: UnsafePointer<Int8>, domain: String!, level: Int32, format: String!, args: CVarArgType...) {
  LogMessageF_va(filename, lineNumber, functionName, domain, level, format, getVaList(args))
}

func LogMessageTo(logger: UnsafeMutablePointer<Logger>, domain: String!, level: Int32, format: String!, args: CVarArgType...) {
  LogMessageTo_va(logger, domain, level, format, getVaList(args))
}

func LogMessageToF(logger: UnsafeMutablePointer<Logger>, filename: UnsafePointer<Int8>, lineNumber: Int32, functionName: UnsafePointer<Int8>, domain: String!, level: Int32, format: String!, args: CVarArgType...) {
  LogMessageToF_va(logger, filename, lineNumber, functionName, domain, level, format, getVaList(args))
}
