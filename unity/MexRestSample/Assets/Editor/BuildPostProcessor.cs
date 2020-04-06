
using System.IO;
using UnityEngine;
using UnityEditor;
using UnityEditor.Callbacks;
using UnityEditor.iOS.Xcode;
public class BuildPostProcessor
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

    static void AddFrameworks(PBXProject project, string targetGUID,string unityFrameworkGUID)
    {
        // to add CoreTelephonyFramwork to the project
        project.AddFrameworkToProject(targetGUID, "CoreTelephony.framework", false);
        // to add CoreTelephonyFramework to UnityFramework
        project.AddFrameworkToProject(unityFrameworkGUID, "CoreTelephony.framework", false);

        // Add `-ObjC` to "Other Linker Flags".
        project.AddBuildProperty(targetGUID, "OTHER_LDFLAGS", "-ObjC");
    }
}

