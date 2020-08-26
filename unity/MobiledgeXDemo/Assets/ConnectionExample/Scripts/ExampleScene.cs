using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Threading.Tasks;
using System;
using MobiledgeX;
using DistributedMatchEngine;
using System.Linq;
using System.Collections.Generic;

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
    public GameObject L7protoPanel;
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
    string selectedL7Proto;
    string url;

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
                return infoText.text = "<b>RegisterClient()</b> \nRegisters the client with the closest Distributed Matching Engine (the nearest edge location in the Operator network) and validates the legitimacy of the mobile subscriber. All session information is encrypted.";

            case 2:
                infoText.transform.parent.gameObject.SetActive(true);
                return infoText.text = "<b>FindCloudlet() </b> \n Locates the most optimal edge computing footprint and allows the registered application to find the application backend by leveraging location, application subscription, and service provider agreement. If there are no suitable cloudlet instances available, the client may connect to the application server located in the public cloud.";

            case 4:
                infoText.transform.parent.gameObject.SetActive(true);
                return infoText.text = "<b>GetAppPort( L4 Protocols ) </b> \nReturns the mapped port for the selected L4 protocol.";
            case 7:
                infoText.transform.parent.gameObject.SetActive(true);
                return infoText.text = "<b>GetUrl( L7 Protocols ) </b> \nReturns the url for the selected L7 protocol.";
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
                case "tcp":
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

    public void SetGetUrl(string l7protoString)
    {
        try
        {
            switch (l7protoString)
            {
                case "tcp":
                    url = mxi.GetUrl("tcp");
                    break;
                case "ws":
                    url = mxi.GetUrl("ws");
                    break;
                case "http":
                    url = mxi.GetUrl("http");
                    break;
                case "udp":
                    url = mxi.GetUrl("udp");
                    break;
            }
            selectedL7Proto = l7protoString;
        }
        catch (GetConnectionException)
        {
            errorPanel.SetActive(true);
            errorText.text = "Restart, Error in GetUrl, check Console for more details";
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
        nextButton.interactable = false;
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
        statusText.text = "";
        IncrementStep();
        cameraAniamtor.SetTrigger("CameraMove");
        await Task.Delay(TimeSpan.FromSeconds(1));
    
        L7protoPanel.SetActive(true);
        List<RectTransform> L7protos = (L7protoPanel.transform as RectTransform).Cast<RectTransform>().ToList();
        switch (selectedProto)
        {
            case LProto.L_PROTO_HTTP:
                L7protos.Find(proto => proto.gameObject.name == "TCP").gameObject.SetActive(false);
                L7protos.Find(proto => proto.gameObject.name == "WebSocket").gameObject.SetActive(false);
                L7protos.Find(proto => proto.gameObject.name == "UDP").gameObject.SetActive(false);
                break;
            case LProto.L_PROTO_TCP:
                L7protos.Find(proto => proto.gameObject.name == "UDP").gameObject.SetActive(false);
                L7protos.Find(proto => proto.gameObject.name == "HTTP").gameObject.SetActive(false);
                break;
            case LProto.L_PROTO_UDP:
                L7protos.Find(proto => proto.gameObject.name == "TCP").gameObject.SetActive(false);
                L7protos.Find(proto => proto.gameObject.name == "WebSocket").gameObject.SetActive(false);
                L7protos.Find(proto => proto.gameObject.name == "HTTP").gameObject.SetActive(false);
                break;
        }

        while (statusAnimator.GetInteger("Step") != 8)
        {
            await Task.Delay(TimeSpan.FromSeconds(.1));
        }
        L7protoPanel.SetActive(false);

        cameraAniamtor.SetTrigger("CameraBack");
        await Task.Delay(TimeSpan.FromSeconds(1));
        statusText.text = "Connected";
        connectedParticleEffect.SetActive(true);
        detailedInfoButton.gameObject.SetActive(true);
        detailedStatusText.text = "GetUrl(\"" + selectedL7Proto + "\"):\n" + url;
        loadingBall.enabled = true;
    }

    #endregion
}
