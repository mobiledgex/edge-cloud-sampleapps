/**
* This facial recognition demo that implements a front end to the OpenCV instance running on MobiledgeX:
*/
const webcamElement = document.getElementById('webcam');
const canvasResize = document.getElementById('canvasResize');
const canvasOutput = document.getElementById('canvasOutput');
const fullLatencySpan = document.getElementById('full-latency');
const networkLatencySpan = document.getElementById('network-latency');

const RECOGNITION_CONFIDENCE_THRESHOLD = 110;

const colors = [
            "#238bc0", "#ff9209", "#32ab39", "#e03d34", "#a57ec8", "#9e6a5d", "#ea90cc", "#919191",
            "#c8c62b", "#00c8d8", "#bbd1ec", "#ffc689", "#a6e19b", "#ffaaa6", "#cfbfdd", "#cfaca5",
            "#fac4da", "#d1d1d1", "#e1e09e", "#ace0e9"];

const faceDetectionEndpoint = "/detector/detect/";
const faceRecognitionEndpoint = "/recognizer/predict/";
const objectDetectionEndpoint = "/object/detect/";
const poseDetectionEndpoint = "/openpose/detect/";
var currentEndpoint = faceDetectionEndpoint;

const protocolWebSocket = "WebSocket";
const protocolRest = "REST";
var currentProtocol = protocolWebSocket;
var webSocket   = null;

var serverGpuSupport = false;
var webcamAvailable = false;

var renderScale = canvasOutput.width / canvasResize.width;
var renderData = null;
var mirrored = true;
var busyProcessing = false;
var fullProcessStart;
var busyNetworkLatency = false;
var elapsed = 0;

var animationAlpha = 1;
var animationStart = 0; //Timestamp
var animationDuration = 3000; //ms

var frameMillis = 33; // Repeat every x milliseconds
var networkLatencyInterval = setInterval(networkLatency, 1000);
var sessionTimeoutMillis = 2*60*1000; // 2 minutes
var sessionTimeoutInterval;
var frameInterval;

const ctx = canvasOutput.getContext('2d');
const ctx2 = canvasResize.getContext('2d');

const constraints = {
  audio: false,
  video: true
};

navigator.mediaDevices.getUserMedia(constraints)
.then(stream => {
  window.stream = stream; // make stream available to browser console
  webcamElement.srcObject = stream;
  webcamAvailable = true;
})
.catch(error => {
  console.log('navigator.getUserMedia error: ', error);
  endSession(false);
  $.alert("Could not connect to webcam. Please load this page on a device with webcam capabilities.");
});

function restartProcessing() {
  console.log("restartProcessing() "+currentProtocol+" "+currentEndpoint);
  if (webSocket != null && webSocket.readyState == WebSocket.OPEN) {
    webSocket.close(code=1000); // Normal Closure
  }

  if (currentProtocol == protocolWebSocket) {
    openWSConnection("wss", window.location.hostname, window.location.port, "/ws" + currentEndpoint);
  }

  renderData = null;
  busyProcessing = false;
  clearTimeout(sessionTimeoutInterval);
  sessionTimeoutInterval = setTimeout(sessionTimeout, sessionTimeoutMillis);
  clearInterval(frameInterval);
  frameInterval = setInterval(processCameraImage, frameMillis);
  $("#process-onoffswitch").prop("checked", true);

  resetGui();
}

function resetGui() {
  if (currentEndpoint == faceDetectionEndpoint) {
    $("#button-fd").addClass("cv-control-selected");
  } else if (currentEndpoint == faceRecognitionEndpoint) {
    $("#button-fr").addClass("cv-control-selected");
  } else if (currentEndpoint == objectDetectionEndpoint) {
    $("#button-od").addClass("cv-control-selected");
  } else if (currentEndpoint == poseDetectionEndpoint) {
    $("#button-pd").addClass("cv-control-selected");
  }

  if (currentProtocol == protocolWebSocket) {
    $("#button-websocket").addClass("cv-control-selected");
  } else if (currentProtocol == protocolRest) {
    $("#button-rest").addClass("cv-control-selected");
  }
}

function sessionTimeout() {
  if ($("#process-onoffswitch").prop("checked") ||
      $("#network-onoffswitch").prop("checked")) {
        console.log("sessionTimeout() Showing alert dialog.");
        $.alert("Click an activity button to restart.", "Session Timeout");
  } else {
    console.log("sessionTimeout() No activity. Skipping alert dialog.");
  }
  $("#network-onoffswitch").prop("checked", false);
  endSession(true);
}

function endSession(allowRestart) {
  console.log("endSession allowRestart="+allowRestart);
  renderData = null;
  clearInterval(frameInterval);
  // clearInterval(networkLatencyInterval);
  clearTimeout(sessionTimeoutInterval);
  resetActivityStates() ;
  $("#process-onoffswitch").prop("checked", false);

  if (!allowRestart) {
    // Disable all controls except network latency.
    $(".cv-activity").prop('disabled', true);
    $("#process-onoffswitch").prop('disabled', true);

    currentEndpoint = null;
    resetGui();
  }
}

restartProcessing();
getServerCapabilities();

function enableGpuActivities(enabled) {
  console.log("enableGpuActivities("+enabled+")");
  $("#button-od").prop('disabled', !enabled);
  $("#button-pd").prop('disabled', !enabled);
}

// "Secret" method of enabling GPU activities even when connected to non-GPU server.
$(".header-img").dblclick(function() {
  enableGpuActivities(true);
});

$("#button-fd").click(function () {
  animationDuration = 3000; //ms
  currentEndpoint = faceDetectionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-control-selected");
  restartProcessing();
});

$("#button-fr").click(function () {
  animationDuration = 3000; //ms
  currentEndpoint = faceRecognitionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-control-selected");
  restartProcessing();
});

$("#button-od").click(function () {
  animationDuration = 5000; //ms
  currentEndpoint = objectDetectionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-control-selected");
  restartProcessing();
});

$("#button-pd").click(function () {
  animationDuration = 5000; //ms
  currentEndpoint = poseDetectionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-control-selected");
  restartProcessing();
});

function resetActivityStates() {
  $(".cv-activity").removeClass("cv-control-selected");
}

$("#button-websocket").click(function () {
  if (currentProtocol == protocolWebSocket) {
    // Nothing to do.
    return;
  }
  currentProtocol = protocolWebSocket;
  resetProtocolStates();
  $(this).addClass("cv-control-selected");
  if (webcamAvailable) {
    restartProcessing();
  }
});

$("#button-rest").click(function () {
  if (currentProtocol == protocolRest) {
    // Nothing to do.
    return;
  }
  currentProtocol = protocolRest;
  resetProtocolStates();
  $(this).addClass("cv-control-selected");
  if (webcamAvailable) {
    restartProcessing();
  }
});

function resetProtocolStates() {
  $(".cv-protocol").removeClass("cv-control-selected");
}

$("#process-onoffswitch").click(function () {
  if ($(this).prop("checked") == true){
    restartProcessing();
  }
  else if ($(this).prop("checked") == false){
    renderData = null;
    clearInterval(frameInterval);
    resetActivityStates();
  }
});

$("#network-onoffswitch").click(function () {
  if ($(this).prop("checked") == true){
    clearInterval(networkLatencyInterval);
    networkLatencyInterval = setInterval(networkLatency, 1000);
  }
  else if ($(this).prop("checked") == false){
    clearInterval(networkLatencyInterval);
  }
});

function renderResults() {
  if (renderData == null) {
    return;
  }

  let elapsed = Date.now() - animationStart;
  animationAlpha = (animationDuration - elapsed) / animationDuration;

  let color;
  switch (currentEndpoint) {
    case faceDetectionEndpoint:
      let rects = renderData.rects;
      if (!rects) {
        console.log(renderData);
        console.log("Transitioning to rect list");
        return;
      }
      color = "36, 176, 219"; // MobiledgeX Blue
      strokeStyle = "rgba("+color+", "+animationAlpha+")";
      fillStyle = null; //"rgba("+color+", "+animationAlpha*0.3+")";
      for (let i = 0; i < rects.length; i++) {
        renderRect(rects[i], null, strokeStyle, fillStyle, "circle");
      }
      break;

    case faceRecognitionEndpoint:
      if (!renderData.rect) {
        console.log(renderData);
        console.log("Transitioning to single rect");
        return;
      }
      color = "239, 76, 35"; // MobiledgeX Orange
      strokeStyle = "rgba("+color+", "+animationAlpha+")";
      let subject = renderData.subject;
      if (renderData.confidence > RECOGNITION_CONFIDENCE_THRESHOLD) {
        subject = "Unknown";
      }
      renderRect(renderData.rect, subject, strokeStyle, null, "rect");
      break;

    case objectDetectionEndpoint:
      let objects = renderData.objects;
      if (!objects) {
        console.log(renderData);
        console.log("Transitioning to objects");
        return;
      }
      for (let i = 0; i < objects.length; i++) {
        // color = "120, 192, 67"; // MobiledgeX Green
        strokeStyle = convertHexToRGBA(colors[i], animationAlpha);
        fillStyle = convertHexToRGBA(colors[i], animationAlpha*0.3);
        let caption = objects[i].class + " " + objects[i].confidence*100 + "%"
        renderRect(objects[i].rect, caption, strokeStyle, fillStyle, "rect");
      }
      break;

    case poseDetectionEndpoint:
      break;

    default:
      console.log("Unknown endpoint: "+currentEndpoint);

  }
}

function renderRect(rect, caption, strokeStyle, fillStyle, shape) {
  let x = rect[0] * renderScale;
  let y = rect[1] * renderScale;
  let right = rect[2] * renderScale;
  let bottom = rect[3] * renderScale;
  let width = right - x;
  let height = bottom - y;

  ctx.beginPath();
  ctx.lineWidth = "4";
  ctx.strokeStyle = strokeStyle;
  if (shape == "rect") {
    ctx.rect(x, y, width, height);
    // ctx.fillRect(x, y, width, height);
  } else if (shape == "circle") {
    ctx.arc(x+(width/2), y+(height/2), (width/2), 0, 2*Math.PI);
  }
  if (fillStyle != null) {
    ctx.fillStyle = fillStyle;
    ctx.fill()
  }
  ctx.stroke();
  if (caption) {
    ctx.font = "20px sans-serif";
    ctx.fillStyle = "rgba(255, 255, 255, "+animationAlpha+")";
    ctx.shadowOffsetX = 3;
    ctx.shadowOffsetY = 3;
    ctx.shadowColor = "rgba(0,0,0,0.3)";
    ctx.shadowBlur = 4;
    ctx.fillText(caption, x+4, y+20);
  }
}

function processCameraImage() {
  if (mirrored) {
    ctx.translate(canvasOutput.width, 0);
    ctx.scale(-1, 1);
    ctx2.translate(canvasResize.width, 0);
    ctx2.scale(-1, 1);
  }
  ctx.drawImage(webcamElement, 0, 0, canvasOutput.width, canvasOutput.height);
  ctx2.drawImage(webcamElement, 0, 0, canvasResize.width, canvasResize.height);
  if (mirrored) {
    ctx.translate(canvasOutput.width, 0);
    ctx.scale(-1, 1);
    ctx2.translate(canvasResize.width, 0);
    ctx2.scale(-1, 1);
  }

  renderResults(ctx);

  if (busyProcessing) {
    console.log("busyProcessing");
    return;
  }

  // Get blob from the resize canvas, and send it to the server.
  canvasResize.toBlob(function(blob) {
    sendImageToServer(blob);
  }, 'image/jpeg', 0.9);
}

function sendImageToServer(image) {
  busyProcessing = true;
  fullProcessStart = Date.now();
  if (currentProtocol == protocolWebSocket) {
    if (webSocket.readyState != WebSocket.OPEN) {
      console.error("webSocket is not open: " + webSocket.readyState);
      busyProcessing = false;
      return;
    }
    webSocket.send(image);

  } else if (currentProtocol == "REST") {
    fetch(currentEndpoint, {
        method: 'post',
        headers: { 'Content-Type': 'image/jpeg', 'Mobiledgex-Debug': 'true' },
        body: image
      })
      .then(response => response.json())
      .then(data => {
        handleResponse(data);
      })
      .catch(err => {
        elapsed = 9999;
        fullLatencySpan.textContent = elapsed + " ms";
        console.log(currentEndpoint + " failed. Error="+err);
        endSession(true);
        $.alert("Please ensure that the CV server is running. " + currentEndpoint + " failed. Error="+err);
      })
  } else {
    console.log("Unknown currentProtocol: " + currentProtocol);
  }
}

function handleResponse(data) {
  console.log(data);
  if (data.latency_start != null) {
    // In this case, we sent a text-only message and it was echoed back by the server.
    elapsed = Date.now() - data.latency_start;
    networkLatencySpan.textContent = elapsed + " ms";
    busyNetworkLatency = false;
    return;
  }
  elapsed = Date.now() - fullProcessStart;
  fullLatencySpan.textContent = elapsed + " ms";
  if (data.success == "true") {
    renderData = data;
    animationStart = Date.now();
  } else {
    console.log("Image processing failed.");
  }
  busyProcessing = false;
}

function networkLatency() {
  if (busyNetworkLatency) {
    console.log("busyNetworkLatency");
    return;
  }
  busyNetworkLatency = true;
  if (currentProtocol == protocolWebSocket) {
    if (webSocket.readyState != WebSocket.OPEN) {
      console.error("webSocket is not open: " + webSocket.readyState);
      busyNetworkLatency = false;
      return;
    }
    webSocket.send('{"latency_start": '+Date.now()+'}');

  } else if (currentProtocol == "REST") {
    let start = Date.now();
    fetch('/test/', {
        method: 'HEAD',
      })
      .then(response => console.log(response))
      .then(data => {
        elapsed = Date.now() - start;
      })
      .catch(err => {
        elapsed = 9999;
        console.log("Network latency test failed! Error="+err);
        endSession(true);
        $.alert("Please ensure that the CV server is running. Network latency test failed! Error="+err);
      })
      .finally(() => {
        busyNetworkLatency = false;
        networkLatencySpan.textContent = elapsed + " ms";
      })
  }
}

// Does the server have GPU capabilities?
function getServerCapabilities() {
  fetch('/server/capabilities/', {
      method: 'GET',
    })
    .then(response => response.json())
    .then(data => {
      console.log(data);
      serverGpuSupport = data.gpu_support;
      console.log("serverGpuSupport="+serverGpuSupport);
      if (webcamAvailable) {
        enableGpuActivities(serverGpuSupport);
      } else {
        console.log("Skipping enableGpuActivities due to lack of webcam");
      }
    })
}

// Enable use of a Jquery/CSS alert dialog instead of the browser's built-in.
$.extend({ alert: function (message, title) {
    $("<div></div>").dialog( {
      buttons: { "Ok": function () { $(this).dialog("close"); } },
      close: function (event, ui) { $(this).remove(); },
      resizable: true,
      maxWidth: 768,
      title: title,
      modal: true
    }).text(message);
  }
});

const convertHexToRGBA = (hex, alpha) => {
  const tempHex = hex.replace('#', '');
  const r = parseInt(tempHex.substring(0, 2), 16);
  const g = parseInt(tempHex.substring(2, 4), 16);
  const b = parseInt(tempHex.substring(4, 6), 16);

  return `rgba(${r},${g},${b},${alpha})`;
};

/**
 * Open a new WebSocket connection using the given parameters
 */
function openWSConnection(protocol, hostname, port, endpoint) {
    var webSocketURL = null;
    webSocketURL = protocol + "://" + hostname + ":" + port + endpoint;
    console.log("openWSConnection::Connecting to: " + webSocketURL);
    try {
        webSocket = new WebSocket(webSocketURL);
        webSocket.onopen = function(openEvent) {
            console.log("WebSocket OPEN: " + JSON.stringify(openEvent, null, 4));
        };
        webSocket.onclose = function (closeEvent) {
            console.log("WebSocket CLOSE: " + JSON.stringify(closeEvent, null, 4));
            console.log(closeEvent);
            if (closeEvent.code != 1000) {
              endSession(true);
              $.alert("CV server has closed the connection to "+webSocketURL);
            }
        };
        webSocket.onerror = function (errorEvent) {
            console.log("WebSocket ERROR: " + JSON.stringify(errorEvent, null, 4));
            console.log(errorEvent);
            endSession(true);
            $.alert("Please ensure that the CV server is running. Connection to "+webSocketURL+" failed");
        };
        webSocket.onmessage = function (messageEvent) {
            var wsMsg = messageEvent.data;
            console.log("WebSocket MESSAGE: " + wsMsg);
            handleResponse(JSON.parse(messageEvent.data));
        };
    } catch (exception) {
        console.error(exception);
    }
}
