/**
 * Copyright 2020 MobiledgeX, Inc. All rights and licenses reserved.
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

using System.Runtime.Serialization.Json;
using System.Text;
using System.Runtime.Serialization;
using System.IO;

namespace MobiledgeXComputerVision
{
    [DataContract]
    public class FaceDetectionResponse
    {
        [DataMember]
        public bool success;
        [DataMember]
        public float server_processing_time;
        [DataMember]
        public int[][] rects;
    }

    [DataContract]
    public class FaceRecognitionResponse
    {
        [DataMember]
        public bool success;
        [DataMember]
        public string subject;
        [DataMember]
        public float confidence;
        [DataMember]
        public float server_processing_time;
        [DataMember]
        public int[] rect;
    }

    [DataContract]
    public class PoseDetectionResponse
    {
        [DataMember]
        public bool success;
        [DataMember]
        public float server_processing_time;
        [DataMember]
        public float[][][] poses;
    }

    [DataContract]
    public class ObjectDetectionResponse
    {
        [DataMember]
        public bool success;
        [DataMember]
        public float server_processing_time;
        [DataMember]
        public bool gpu_support;
        [DataMember]
        public @Object[] objects;
    }

    [DataContract]
    public class @Object
    {
        [DataMember]
        public int[] rect;
        [DataMember]
        public string @class;
        [DataMember]
        public float confidence;
    }

    [DataContract]
    public class MessageWrapper
    {
        [DataMember]
        public string type = "utf8";
        [DataMember]
        public string utf8Data;
        public static MessageWrapper WrapTextMessage(string jsonStr)
        {
            var wrapper = new MessageWrapper();
            wrapper.utf8Data = jsonStr;
            return wrapper;
        }
        public static MessageWrapper UnWrapMessage(string wrappedJsonStr)
        {
            var wrapper = Messaging<MessageWrapper>.Deserialize(wrappedJsonStr);
            return wrapper;
        }
    }

    public static class Messaging<T>
    {
        public static string StreamToString(Stream s)
        {
            s.Position = 0;
            StreamReader reader = new StreamReader(s);
            string jsonStr = reader.ReadToEnd();
            return jsonStr;
        }
        public static string Serialize(T t)
        {
            DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(T));
            MemoryStream ms = new MemoryStream();
            serializer.WriteObject(ms, t);
            string jsonStr = StreamToString(ms);
            return jsonStr;
        }
        public static T Deserialize(string jsonString)
        {
            MemoryStream ms = new MemoryStream(Encoding.UTF8.GetBytes(jsonString ?? ""));
            return Deserialize(ms);
        }
        public static T Deserialize(Stream stream)
        {
            DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(T));
            T t = (T)deserializer.ReadObject(stream);
            return t;
        }
    }
}
