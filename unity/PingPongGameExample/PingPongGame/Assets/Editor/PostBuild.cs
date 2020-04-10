/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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

using System.IO;
using UnityEngine;
using UnityEditor;
using UnityEditor.Callbacks;
using UnityEditor.iOS.Xcode;
public class PostBuild
{


    [PostProcessBuildAttribute(1)]
    public static void OnPostProcessBuild(BuildTarget target, string path)
    {
        if (target == BuildTarget.iOS)
        {
            // Read.
            string projectPath = PBXProject.GetPBXProjectPath(path);
            PBXProject project = new PBXProject();
            project.ReadFromString(File.ReadAllText(projectPath));
            string targetGUID = project.GetUnityMainTargetGuid();
            string unityFrameworkGUID = project.GetUnityFrameworkTargetGuid();

            AddFrameworks(project, targetGUID, unityFrameworkGUID);

            // Write.
            File.WriteAllText(projectPath, project.WriteToString());
        }
    }

    static void AddFrameworks(PBXProject project,string targetGUID,string unityFrameworkGUID)
    {
        // to add CoreTelephonyFramwork to the project
        project.AddFrameworkToProject(targetGUID, "CoreTelephony.framework", false);
        // to add CoreTelephonyFramework to UnityFramework
        project.AddFrameworkToProject(unityFrameworkGUID, "CoreTelephony.framework", false);

        // Add `-ObjC` to "Other Linker Flags".
        project.AddBuildProperty(targetGUID, "OTHER_LDFLAGS", "-ObjC");
    }
}

