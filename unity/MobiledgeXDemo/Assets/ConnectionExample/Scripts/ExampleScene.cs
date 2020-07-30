using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Threading.Tasks;
using System;
using MobiledgeX;
using DistributedMatchEngine;

[RequireComponent(typeof(MobiledgeX.LocationService))]
public class ExampleScene : MonoBehaviour
{
    #region public Variables assigned in the inspector
    public Text statusText;
    public Text detailedStatusText;
    public Button detailedInfoButton;

    public Animator statusAnimator;
    public Animator loadingBall;
    public Animator cameraAniamtor;
    public Animator planeAnimator;
    public GameObject protoPanel;

    public Button nextButton;
    public Text infoText;
    public GameObject errorPanel;
    public Text errorText;
    public EdgeConnectionSteps edgeConnectionSteps;
    public GameObject connectedParticleEffect;

    #endregion
    
    static int step;// for scene progression
    MobiledgeXIntegration mxi; 
    static MobiledgeXSettings settings;
    LProto selectedProto; //selected protocol
    AppPort selectedPort;

    #region MonoBehaviour Callbacks

    IEnumerator Start()
    {
        nextButton.onClick.AddListener(IncrementStep);
        settings = Resources.Load<MobiledgeXSettings>("MobiledgeXSettings"); 
        yield return StartCoroutine(MobiledgeX.LocationService.EnsureLocation());
        statusText.text = "";
        SceneFlow();
    }

    #endregion

    #region Scene Functions

    public void IncrementStep()
    {
        step += 1;
        if (statusAnimator.enabled)
        {
            statusAnimator.SetInteger("Step", step);
        }

        if (cameraAniamtor.enabled)
        {
            cameraAniamtor.SetInteger("Step", step);
        }

        if (planeAnimator.enabled)
        {
            planeAnimator.SetInteger("Step", step);
        }

        if (infoText.enabled)
        {
            infoText.text = InfoText(step);
        }
    }

    string InfoText(int step)
    {

        switch (step)
        {
            case 0:
                infoText.transform.parent.gameObject.SetActive(true);
                return infoText.text = "<b>RegisterClient()</b> \n Verifies the app has been deployed to MobiledgeX Cloudlets using Organization Name, App Name ,and App Version.";

            case 2:
                infoText.transform.parent.gameObject.SetActive(true);
                return infoText.text = "<b>FindCloudlet() </b> \n Gets the address of the best cloudlet that is running your server application.";

            case 4:
                infoText.transform.parent.gameObject.SetActive(true);
                return infoText.text = "<b>GetAppPort(Protocol) </b> \n Gets the mapped Port for the selected protocol ";
            default:
                infoText.transform.parent.gameObject.SetActive(false);
                return "";
        }
    }

    public void SetAppPort(string protoString)
    {
        try
        {
            switch (protoString)
            {
                case "ws":
                    selectedProto = LProto.L_PROTO_TCP;
                    mxi.GetAppPort(selectedProto);
                    break;
                case "http":
                    selectedProto = LProto.L_PROTO_HTTP;
                    mxi.GetAppPort(selectedProto);
                    break;
                case "udp":
                    selectedProto = LProto.L_PROTO_UDP;
                    mxi.GetAppPort(selectedProto);
                    break;
            }
        }
        catch (AppPortException)
        {
            errorPanel.SetActive(true);
            errorText.text = "Error in GetAppPort, check the Console for more details";
            return;
        }
    }

    async Task SceneFlow()
    {

        // Register Client
        edgeConnectionSteps.gameObject.SetActive(true);
        edgeConnectionSteps.registerClientStep.GetComponentInChildren<Image>().enabled = true;
        await Task.Delay(TimeSpan.FromSeconds(3));
        edgeConnectionSteps.gameObject.SetActive(false);
        statusText.text = "Register Client";
        infoText.text = InfoText(step);
        while (statusAnimator.GetInteger("Step") != 1)
        {
            await Task.Delay(TimeSpan.FromSeconds(.1));
        }
        nextButton.interactable = false;
        mxi = new MobiledgeXIntegration();
        try
        {
            statusText.text = "Register Client\n <b> Org Name:</b> " + settings.orgName + "\n <b>App Name:</b> " + settings.appName + "\n <b>App Version:</b> " + settings.appVers;
            await mxi.Register();
            await Task.Delay(TimeSpan.FromSeconds(2));
       
            statusAnimator.SetTrigger("Start");
            await Task.Delay(TimeSpan.FromSeconds(.1));
            statusText.color = Color.green;
            detailedInfoButton.gameObject.SetActive(true);
            detailedStatusText.text = "Register Client Status : "+ mxi.RegisterStatus;
            statusText.text = "Verified";
        }
        catch (RegisterClientException)
        {
            statusText.text = "Failed to verify";
            errorPanel.SetActive(true);
            errorText.text = "Restart, Error in RegisterClient, check Console for more details";
            return;
        }
        statusAnimator.SetTrigger("End");
        await Task.Delay(TimeSpan.FromSeconds(2));
        nextButton.interactable = true;
        while (statusAnimator.GetInteger("Step") != 2)
        {
            await Task.Delay(TimeSpan.FromSeconds(.1));
        }
        edgeConnectionSteps.registerClientStep.GetComponentInChildren<Image>().enabled = false;
        edgeConnectionSteps.findCloudletStep.GetComponentInChildren<Image>().enabled = true;
        edgeConnectionSteps.gameObject.SetActive(true);
        await Task.Delay(TimeSpan.FromSeconds(3));
        edgeConnectionSteps.gameObject.SetActive(false);

        detailedInfoButton.gameObject.SetActive(false);
        nextButton.interactable = false;
        statusAnimator.SetTrigger("Reset");
        statusText.color = Color.black;
        statusText.text = "";
        await Task.Delay(TimeSpan.FromSeconds(1.5));



        // Find Cloudlet
        statusText.text = "Find Cloudlet";
        await Task.Delay(TimeSpan.FromSeconds(1));
        statusAnimator.SetTrigger("Start");
        try
        {
            await mxi.FindCloudlet();


            string portsString = "<b>Ports:</b>";
            foreach (AppPort appPort in mxi.FindCloudletReply.ports)
            {
                portsString += "\n Port : (" + appPort.proto + ") " + appPort.public_port;
            }
            FindCloudletReply findCloudletReply = mxi.FindCloudletReply;
            detailedStatusText.text = "<b>FindCloudlet Status:</b> " + findCloudletReply.status +
                "\n<b>Cloudlet Location:</b> " + findCloudletReply.cloudlet_location.longitude + ", " + findCloudletReply.cloudlet_location.latitude
                + "\n<b>Application URL:</b> " + findCloudletReply.fqdn + "\n" + portsString;

            detailedInfoButton.gameObject.SetActive(true);
            planeAnimator.gameObject.GetComponent<MeshRenderer>().material.SetFloat("Alpha", 1);
            cameraAniamtor.SetTrigger("CameraMove");
            await Task.Delay(TimeSpan.FromSeconds(1));
            planeAnimator.enabled = true;
            nextButton.interactable = true;
            while (statusAnimator.GetInteger("Step") != 3)
            {
                await Task.Delay(TimeSpan.FromSeconds(.1));
            }
            nextButton.interactable = false;
            await Task.Delay(TimeSpan.FromSeconds(1));
            cameraAniamtor.SetTrigger("CameraBack");
            await Task.Delay(TimeSpan.FromSeconds(1));
            planeAnimator.gameObject.SetActive(false);
            statusText.text = "Cloudlet Found";
        }
        catch (FindCloudletException)
        {
            errorPanel.SetActive(true);
            errorText.text = "Restart, Error in FindCloudlet, check Console for more details";
            return;
        }
        statusAnimator.SetTrigger("End");
        await Task.Delay(TimeSpan.FromSeconds(2));
        statusAnimator.SetTrigger("Reset");
        detailedInfoButton.gameObject.SetActive(false);
        statusText.text = "";
        await Task.Delay(TimeSpan.FromSeconds(1.5));


        //  GetAppPort
        edgeConnectionSteps.findCloudletStep.GetComponentInChildren<Image>().enabled = false;
        edgeConnectionSteps.getAppPortStep.GetComponentInChildren<Image>().enabled = true;
        edgeConnectionSteps.gameObject.SetActive(true);
        await Task.Delay(TimeSpan.FromSeconds(3));
        edgeConnectionSteps.gameObject.SetActive(false);
        statusText.text = "Connect to the desired port";
        nextButton.interactable = true;
        while (statusAnimator.GetInteger("Step") != 4)
        {
            await Task.Delay(TimeSpan.FromSeconds(.1));
        }
        statusText.text = "Select The desired protocol";
        while (statusAnimator.GetInteger("Step") != 5)
        {
            await Task.Delay(TimeSpan.FromSeconds(.1));
        }
        nextButton.interactable = false;
        await Task.Delay(TimeSpan.FromSeconds(1.5));
        statusAnimator.SetTrigger("Start");
        cameraAniamtor.SetTrigger("CameraMove");
        await Task.Delay(TimeSpan.FromSeconds(1));
        protoPanel.SetActive(true);
        await Task.Delay(TimeSpan.FromSeconds(1.5));
        nextButton.interactable = true;
        while (statusAnimator.GetInteger("Step") != 6)
        {
            await Task.Delay(TimeSpan.FromSeconds(.1));
        }
        protoPanel.SetActive(false);
        nextButton.interactable = false;
        cameraAniamtor.SetTrigger("CameraBack");
        await Task.Delay(TimeSpan.FromSeconds(2));
        statusText.text = "Port Found";
        statusAnimator.SetTrigger("End");
        await Task.Delay(TimeSpan.FromSeconds(1.5));
        statusAnimator.SetTrigger("Reset");


        // GetUrl
        edgeConnectionSteps.getAppPortStep.GetComponentInChildren<Image>().enabled = false;
        edgeConnectionSteps.getUrlStep.GetComponentInChildren<Image>().enabled = true;
        edgeConnectionSteps.gameObject.SetActive(true);
        await Task.Delay(TimeSpan.FromSeconds(3));
        edgeConnectionSteps.gameObject.SetActive(false);
        await Task.Delay(TimeSpan.FromSeconds(1));
        try
        {
            switch (selectedProto)
            {
                case LProto.L_PROTO_HTTP:
                    detailedStatusText.text = "GetUrl(\"http\"):\n" + mxi.GetUrl("http");
                    break;
                case LProto.L_PROTO_TCP:
                    detailedStatusText.text = "GetUrl(\"ws\"):\n" + mxi.GetUrl("ws");
                    break;
                case LProto.L_PROTO_UDP:
                    detailedStatusText.text = "GetUrl(\"udp\"):\n" + mxi.GetUrl("udp");
                    break;
            }
            statusText.text = "Connected";
            connectedParticleEffect.SetActive(true);
            detailedInfoButton.gameObject.SetActive(true);
            loadingBall.enabled = true;
        }
        catch (GetConnectionException)
        {
            errorPanel.SetActive(true);
            errorText.text = "Restart, Error in GetUrl, check Console for more details";
            return;
        }
    }

    #endregion
}
