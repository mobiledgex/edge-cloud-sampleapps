#!/usr/local/bin/python
import json
import requests

# dme = "wifi.dme.mobiledgex.net"
dme = "us-mexdemo.dme.mobiledgex.net"

app_name = "MobiledgeX SDK Demo"
dev_name = "MobiledgeX"
carrier_name = "TELUS"
app_vers = "2.0"

url = "https://%s:38001/v1/registerclient" %dme
data = {
  "app_name": app_name,
  "dev_name": dev_name,
  "carrier_name": carrier_name,
  "app_vers": app_vers
}
resp = requests.post(url, data=json.dumps(data))
# print(resp.content)
decoded_json = json.loads(resp.content)
status = decoded_json["status"]
session_cookie = decoded_json["session_cookie"]
print("registerclient status: %s" %status)

url = "https://%s:38001/v1/getappinstlist" %dme
data = {
  "session_cookie": session_cookie,
  "gps_location": {
    "latitude": 1,
    "timestamp": {
      "seconds": "0",
      "nanos": 0
    },
    "longitude": 1,
    "course": 0,
    "altitude": 0,
    "horizontal_accuracy": 0,
    "speed": 0,
    "vertical_accuracy": 0
  },
  "app_name": app_name,
  "dev_name": dev_name,
  "carrier_name": carrier_name,
  "app_vers": app_vers
}
resp = requests.post(url, data=json.dumps(data))
# print(resp.content)
decoded_json = json.loads(resp.content)
status = decoded_json["status"]
cloudlets = decoded_json["cloudlets"]
print("getappinstlist status: %s. %d app instances returned." %(status, len(cloudlets)))
for cloudlet in cloudlets:
    print("    cloudlet_name: %s" %cloudlet["cloudlet_name"])


url = "https://%s:38001/v1/findcloudlet" %dme
data = {
  "session_cookie": session_cookie,
  "gps_location": {
    "latitude": 32.4894,
    "timestamp": {
      "seconds": "0",
      "nanos": 0
    },
    "longitude": -96.9163,
    "course": 0,
    "altitude": 0,
    "horizontal_accuracy": 0,
    "speed": 0,
    "vertical_accuracy": 0
  },
  "app_name": app_name,
  "dev_name": dev_name,
  "carrier_name": carrier_name,
  "app_vers": app_vers
}
resp = requests.post(url, data=json.dumps(data))
# print(resp.content)
decoded_json = json.loads(resp.content)
status = decoded_json["status"]
fqdn = decoded_json["fqdn"]
print("findcloudlet status: %s. FQDN: %s" %(status, fqdn))
