/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System.Diagnostics;
using UnityEngine;

// Minimal logger without log levels:
public class Logger
{
  // Stdout:
  public static void S(string TAG, string msg)
  {
    UnityEngine.Debug.LogFormat(LogType.Log, LogOption.NoStacktrace, null, "[" + TAG + "] {0}", msg);
  }
  // Stderr:
  public static void E(string TAG, string msg)
  {
    UnityEngine.Debug.LogFormat(LogType.Error, LogOption.NoStacktrace, null, "[" + TAG + "] {0}", msg);
  }

  // Stderr:
  [ConditionalAttribute("DEBUG")]
  public static void D(string TAG, string msg)
  {
    UnityEngine.Debug.LogFormat(LogType.Log, LogOption.NoStacktrace, null, "[" + TAG + "] {0}", msg);
  }
}
