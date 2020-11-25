#!/usr/local/bin/python
import sys
import json
import requests

latitude = 32.489442
longitude = -96.916345

# dme = "310-410.dme.mobiledgex.net"
# dme = "wifi.dme.mobiledgex.net"
# dme = "310-260.dme.mobiledgex.net"
# dme = "262-01.dme.mobiledgex.net"
# dme = "us-mexdemo.dme.mobiledgex.net"
dme = "eu-mexdemo.dme.mobiledgex.net"
# dme = "emeraldeyeconstruct.mywire.org"
# dme = "eu-tef.dme.mobiledgex.net"
# dme = "eu-qa.dme.mobiledgex.net"
# dme = "eu-stage.dme.mobiledgex.net"
# dme = "262-01.dme.mobiledgex.net"

# app_name = "MobiledgeX SDK Demo"

app_name = "ComputerVision"
org_name = "MobiledgeX-Samples"
carrier_name = "GDDT"
app_vers = "2.2"

# app_name = "ComputerVision-GPU"
# org_name = "MobiledgeX"
# carrier_name = "GDDT"
# app_vers = "2.0"

# app_name = "mobiledgexsdkdemo20"
# carrier_name = "GDDT"
# app_vers = "2020-05-19-GPU"
# org_name = "TEF_Dev_Spain"

# app_name = "mobiledgexsdkdemo"
# carrier_name = "Sonoral"
# app_vers = "v1"
# org_name = "teftest"

# app_name = "HA Demo"
# org_name = "Hooli"
# carrier_name = "gcp"
# app_vers = "1.0"

# carrier_name = "TELUS"
# carrier_name = "26201"
# carrier_name = "310260"
# carrier_name = "wifi"

# app_name = "mx-bks400"
# org_name = "Broadpeak"
# app_vers = "02.00.rc2-2923"
# carrier_name = "GDDT"

# dme = "eu-qa.dme.mobiledgex.net"
# app_name = "automation-sdk-porttest"
# org_name = "MobiledgeX"
# app_vers = "1.0"
# carrier_name = "GDDT"

print("dme: %s, app_name: %s, org_name: %s, app_vers: %s, carrier_name: %s" %(dme, app_name, org_name, app_vers, carrier_name))
url = "https://%s:38001/v1/registerclient" %dme
data = {
  "app_name": app_name,
  "org_name": org_name,
  "carrier_name": carrier_name,
  "app_vers": app_vers
}
resp = requests.post(url, data=json.dumps(data))
# print(resp.content)
decoded_json = json.loads(resp.content)
if "status" not in decoded_json:
    print("'status' not returned:\n%s" %resp.content)
    sys.exit()
status = decoded_json["status"]
session_cookie = decoded_json["session_cookie"]
# print("registerclient status: %s" %status)

url = "https://%s:38001/v1/getappinstlist" %dme

data = {
  "session_cookie": session_cookie,
  "gps_location": {
    "latitude": latitude,
    "timestamp": {
      "seconds": "0",
      "nanos": 0
    },
    "longitude": longitude,
    "course": 0,
    "altitude": 0,
    "horizontal_accuracy": 0,
    "speed": 0,
    "vertical_accuracy": 0
  },
  "limit": 6,
}
resp = requests.post(url, data=json.dumps(data))
# print(resp.content)
decoded_json = json.loads(resp.content)
status = decoded_json["status"]
cloudlets = decoded_json["cloudlets"]
print("getappinstlist status: %s. %d app instances returned." %(status, len(cloudlets)))
for cloudlet in cloudlets:
    print("    cloudlet_name: %s \tcarrier_name: %s \tfqdn: %s" %(cloudlet["cloudlet_name"], cloudlet["carrier_name"], cloudlet["appinstances"][0]["fqdn"]))


url = "https://%s:38001/v1/findcloudlet" %dme
data = {
  "session_cookie": session_cookie,
  "gps_location": {
    "latitude": latitude,
    "timestamp": {
      "seconds": "0",
      "nanos": 0
    },
    "longitude": longitude,
    "course": 0,
    "altitude": 0,
    "horizontal_accuracy": 0,
    "speed": 0,
    "vertical_accuracy": 0
  },
  "carrier_name": carrier_name,
}
resp = requests.post(url, data=json.dumps(data))
# print(resp.content)
decoded_json = json.loads(resp.content)
status = decoded_json["status"]
fqdn = decoded_json["fqdn"]
print("findcloudlet status: %s. FQDN: %s" %(status, fqdn))
