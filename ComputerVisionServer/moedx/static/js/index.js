/**
* This facial recognition demo that implements a front end to the OpenCV instance running on MobiledgeX:
*/
const webcamElement = document.getElementById('webcam');
const canvasResize = document.getElementById('canvasResize');
const canvasOutput = document.getElementById('canvasOutput');
const fullLatencySpan = document.getElementById('full-latency');
const networkLatencySpan = document.getElementById('network-latency');

const RECOGNITION_CONFIDENCE_THRESHOLD = 120;

const faceDetectionEndpoint = "/detector/detect/";
const faceRecognitionEndpoint = "/recognizer/predict/";
const objectDetectionEndpoint = "/object/detect/";
const poseDetectionEndpoint = "/openpose/detect/";

var currentEndpoint = faceDetectionEndpoint;

var serverGpuSupport = false;

var renderScale = canvasOutput.width / canvasResize.width;
var renderData = null;
var mirrored = true;
var busyProcessing = false;
var busyNetworkLatency = false;
var elapsed = 0;

var animationAlpha = 1;
var animationStart = 0; //Timestamp
var animationDuration = 3000; //ms

var frameMillis = 33;
var frameInterval = setInterval(processImage, frameMillis); // Repeat every x milliseconds
var networkLatencyInterval = setInterval(networkLatency, 1000);
var sessionTimeoutMillis = 2*60*1000; // 2 minutes
var sessionTimeoutInterval = setTimeout(sessionTimeout, sessionTimeoutMillis);

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
})
.catch(error => {
  console.log('navigator.getUserMedia error: ', error);
  endSession(false);
  $.alert("Could not connect to webcam. Please load this page on a device with webcam capabilities.");
});

getServerCapabilities();

function enableGpuActivities(enabled) {
  console.log("enableGpuActivities("+enabled+")");
  $("#button-od").prop('disabled', !enabled);
  $("#button-pd").prop('disabled', !enabled);
}

$("#websocket-onoffswitch").prop('disabled', true);
$("#websocket-onoffswitch").prop("checked", false);

$("#button-fd").addClass("cv-activity-selected");

// "Secret" method of enabling GPU activities even when connected to non-GPU server.
$(".header-img").dblclick(function() {
  enableGpuActivities(true);
});

$("#button-fd").click(function () {
  animationDuration = 3000; //ms
  currentEndpoint = faceDetectionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-activity-selected");
  restartProcessing();
});

$("#button-fr").click(function () {
  animationDuration = 3000; //ms
  currentEndpoint = faceRecognitionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-activity-selected");
  restartProcessing();
});

$("#button-od").click(function () {
  animationDuration = 5000; //ms
  currentEndpoint = objectDetectionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-activity-selected");
  restartProcessing();
});

$("#button-pd").click(function () {
  animationDuration = 5000; //ms
  currentEndpoint = poseDetectionEndpoint;
  resetActivityStates();
  $(this).addClass("cv-activity-selected");
  restartProcessing();
});

function resetActivityStates() {
  $(".cv-activity").removeClass("cv-activity-selected");
}

function sessionTimeout() {
  endSession(true);
  $.alert("Click an activity button to restart.", "Session Timeout");
}

function endSession(allowRestart) {
  renderData = null;
  console.log("endSession allowRestart="+allowRestart);
  clearInterval(frameInterval);
  clearInterval(networkLatencyInterval);
  clearTimeout(sessionTimeoutInterval);
  resetActivityStates() ;
  $("#process-onoffswitch").prop("checked", false);
  $("#network-onoffswitch").prop("checked", false);

  if (!allowRestart) {
    // Disable all controls.
    $(".cv-activity").prop('disabled', true);
    $(".onoffswitch-checkbox").prop('disabled', true);
  }

}

function restartProcessing() {
  renderData = null;
  clearTimeout(sessionTimeoutInterval);
  sessionTimeoutInterval = setTimeout(sessionTimeout, sessionTimeoutMillis);
  clearInterval(frameInterval);
  frameInterval = setInterval(processImage, frameMillis);
  $("#process-onoffswitch").prop("checked", true);
}

$("#process-onoffswitch").click(function () {
  if($(this).prop("checked") == true){
    restartProcessing();
  }
  else if($(this).prop("checked") == false){
    renderData = null;
    clearInterval(frameInterval);
    resetActivityStates();
  }
});

$("#network-onoffswitch").click(function () {
  if($(this).prop("checked") == true){
    clearInterval(networkLatencyInterval);
    networkLatencyInterval = setInterval(networkLatency, 1000);
  }
  else if($(this).prop("checked") == false){
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
      color = "120, 192, 67"; // MobiledgeX Blue
      for (let i = 0; i < rects.length; i++) {
        renderRect(rects[i], null, color, "circle");
      }
      break;
    case faceRecognitionEndpoint:
      if (!renderData.rect) {
        console.log(renderData);
        console.log("Transitioning to single rect");
        return;
      }
      // TODO: Test confidence threshold and display uncertain images differently.
      color = "239, 76, 35"; // MobiledgeX Orange
      renderRect(renderData.rect, renderData.subject, color, "rect");
      break;
      case objectDetectionEndpoint:
        let objects = renderData.objects;
        if (!objects) {
          console.log(renderData);
          console.log("Transitioning to objects");
          return;
        }
        for (let i = 0; i < objects.length; i++) {
          color = "36, 176, 219"; // MobiledgeX Green
          let caption = objects[i].class + " " + objects[i].confidence*100 + "%"
          renderRect(objects[i].rect, caption, color, "rect");
        }
        break;
      case poseDetectionEndpoint:
        break;
    default:
      console.log("Unknown endpoint: "+currentEndpoint);

  }
}

function renderRect(rect, caption, color, shape, fill) {
  let x = rect[0] * renderScale;
  let y = rect[1] * renderScale;
  let right = rect[2] * renderScale;
  let bottom = rect[3] * renderScale;
  let width = right - x;
  let height = bottom - y;

  ctx.beginPath();
  ctx.lineWidth = "4";
  ctx.strokeStyle = "rgba("+color+", "+animationAlpha+")";
  if (shape == "rect") {
    ctx.rect(x, y, width, height);
  } else if (shape == "circle") {
    ctx.arc(x+(width/2), y+(height/2), (width/2), 0, 2*Math.PI);
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

function processImage() {
  if(mirrored) {
    ctx.translate(canvasOutput.width, 0);
    ctx.scale(-1, 1);
    ctx2.translate(canvasResize.width, 0);
    ctx2.scale(-1, 1);
  }
  ctx.drawImage(webcamElement, 0, 0, canvasOutput.width, canvasOutput.height);
  ctx2.drawImage(webcamElement, 0, 0, canvasResize.width, canvasResize.height);
  if(mirrored) {
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
    busyProcessing = true;
    let start = Date.now();
    fetch(currentEndpoint, {
        method: 'post',
        headers: { 'Content-Type': 'image/jpeg', 'Mobiledgex-Debug': 'true' },
        body: blob
      })
      .then(response => response.json())
      .then(data => {
        elapsed = Date.now() - start;
        console.log(data);
        if (data.success == "true") {
          renderData = data;
          animationStart = Date.now();
        } else {
          console.log("Image processing failed.")
        }
      })
      .catch(err => {
        elapsed = 9999;
        console.log(currentEndpoint + " failed. Error="+err);
        endSession(true);
        $.alert(currentEndpoint + " failed. Please ensure that the CV server is running. Error="+err);
      })
      .finally(() => {
        busyProcessing = false;
        console.log("elapsed="+elapsed);
        fullLatencySpan.textContent = elapsed + " ms";
      })
    }, 'image/jpeg', 0.9);
}

function networkLatency() {
  if (busyNetworkLatency) {
    console.log("busyNetworkLatency");
    return;
  }
  busyNetworkLatency = true;
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
      $.alert("Network latency test failed! Please ensure that the CV server is running. Error="+err);
    })
    .finally(() => {
      busyNetworkLatency = false;
      networkLatencySpan.textContent = elapsed + " ms";
    })
}

// Does the server have GPU capabilities?
function getServerCapabilities() {
  fetch('/server/capabilities/', {
      method: 'GET',
    })
    .then(response => response.json())
    .then(data => {
      console.log(data);
      if (data.gpu_support == "true") {
        serverGpuSupport = true;
      } else {
        serverGpuSupport = false;
      }
      console.log("serverGpuSupport="+serverGpuSupport);
      enableGpuActivities(serverGpuSupport);
    })
}

// Enable use of a Jquery/CSS alert dialog instead of the browser's built-in.
$.extend({ alert: function (message, title) {
    $("<div></div>").dialog( {
      buttons: { "Ok": function () { $(this).dialog("close"); } },
      close: function (event, ui) { $(this).remove(); },
      resizable: false,
      title: title,
      modal: true
    }).text(message);
  }
});
