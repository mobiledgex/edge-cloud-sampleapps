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

# app_name = "ComputerVision"
# org_name = "MobiledgeX-Samples"
# # carrier_name = ""
# carrier_name = "TDG"
# app_vers = "2.2"

# app_name = "automation-sdk-porttest"
# org_name = "MobiledgeX"
# carrier_name = "TDG"
# app_vers = "1.0"

# app_name = "mwgame"
# org_name = "SlavicMonstersLLC"
# carrier_name = ""
# app_vers = "1.1"

# app_name = "ComputerVision-GPU"
# org_name = "BruceEdgeboxDev"
# carrier_name = "BruceEdgebox"
# app_vers = "2.2"

# app_name = "mobiledgexsdkdemo20"
# carrier_name = "TDG"
# app_vers = "2020-05-19-GPU"
# org_name = "TEF_Dev_Spain"

# app_name = "mobiledgexsdkdemo"
# carrier_name = "Telefonica"
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
# carrier_name = "TDG"

# dme = "eu-qa.dme.mobiledgex.net"
# app_name = "automation-sdk-porttest"
# org_name = "MobiledgeX"
# app_vers = "1.0"
# carrier_name = "TDG"

# app_name = "ComputerVision"
# org_name = "WWT-Dev"
# carrier_name = "WWT"
# app_vers = "2.2"

# app_name = "TritonInferenceServer"
# org_name = "MobiledgeX-Samples"
# carrier_name = "TDG"
# app_vers = "1.0"



dme = "127.0.0.1"
app_name = "ComputerVision"
app_vers = "2.2"
org_name = "MobiledgeX"
carrier_name = "TDG"
protocol = "http" # http or https


# dme = "eu-stage.dme.mobiledgex.net"
# app_name = "ComputerVision"
# app_vers = "2.2"
# org_name = "MobiledgeX-Samples"
# carrier_name = "TDG"
# protocol = "https" # http or https

# app_name = "someapplication1"
# org_name = "AcmeAppCo"
# app_vers = "1.0"
# carrier_name = ""

tls_verify=True
session_cookie=None
priority_session_id = ""
qos_profile_name = ""

print("dme: %s, app_name: %s, org_name: %s, app_vers: %s, carrier_name: %s" %(dme, app_name, org_name, app_vers, carrier_name))
def register_client():
    global session_cookie
    url = "%s://%s:38001/v1/registerclient" %(protocol, dme)
    data = {
      "app_name": app_name,
      "org_name": org_name,
      "carrier_name": carrier_name,
      "app_vers": app_vers
    }
    resp = requests.post(url, data=json.dumps(data), verify=tls_verify)
    # print("RESP:", resp.content)
    try:
        decoded_json = json.loads(resp.content)
    except Exception as e:
        print("Could not decode result. RESP: "+str(resp.content))
        sys.exit(1)
    if "status" not in decoded_json:
        print("'status' not returned:\n%s" %resp.content)
        sys.exit(1)
    status = decoded_json["status"]
    session_cookie = decoded_json["session_cookie"]
    print("registerclient status: %s" %status)

def get_app_inst_list():
    url = "%s://%s:38001/v1/getappinstlist" %(protocol, dme)

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
    resp = requests.post(url, data=json.dumps(data), verify=tls_verify)
    # print(resp.content)
    decoded_json = json.loads(resp.content)
    status = decoded_json["status"]
    cloudlets = decoded_json["cloudlets"]
    print("getappinstlist status: %s. %d app instances returned." %(status, len(cloudlets)))
    if len(cloudlets) == 0:
        sys.exit(1)
    for cloudlet in cloudlets:
        print(f"    cloudlet_name: {cloudlet['cloudlet_name']:20} carrier_name: {cloudlet['carrier_name']:10} fqdn: {cloudlet['appinstances'][0]['fqdn']}")

def find_cloudlet():
    global priority_session_id
    global qos_profile_name
    url = "%s://%s:38001/v1/findcloudlet" %(protocol, dme)
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
      "tags": {
        "ip_user_equipment": "172.24.8.2"
      },
      "cell_id": 532
    }
    # print(data)
    resp = requests.post(url, data=json.dumps(data), verify=tls_verify)
    # print(resp.content)
    decoded_json = json.loads(resp.content)
    status = decoded_json["status"]
    if status != "Found":
        print(f"status={status}")
        sys.exit(1)

    fqdn = decoded_json["fqdn"]
    fqdn_prefix = decoded_json["ports"][0]["fqdn_prefix"]

    priority_session_id = decoded_json["tags"].get("priority_session_id")
    qos_profile_name = decoded_json["tags"].get("qos_profile_name")

    print(f"findcloudlet status: {status}. FQDN: {fqdn} FQDN_Prefix: {fqdn_prefix}")
    print(f"Full URI: {fqdn_prefix}{fqdn}")
    print(f"Tags: {priority_session_id}, {qos_profile_name}")

def qos_priority_session_create():
    url = "%s://%s:38001/v1/qosprioritysessioncreate" %(protocol, dme)
    data = {
      "session_cookie": session_cookie,
      "profile": "QOS_LOW_LATENCY",
      "session_duration": 300,
      "ip_user_equipment": "172.24.8.2",
      "ip_application_server": "127.0.0.1",
      "port_application_server": "8008"
    }
    print(f"qos_priority_session_create: {data}")
    resp = requests.post(url, data=json.dumps(data), verify=tls_verify)
    print(f"{resp.status_code} {resp.content}")
    decoded_json = json.loads(resp.content)
    print(f"Session ID: {decoded_json['session_id']} Profile: {decoded_json['profile']}")

def qos_priority_session_delete():
    global priority_session_id
    global qos_profile_name
    url = "%s://%s:38001/v1/qosprioritysessiondelete" %(protocol, dme)
    data = {
      "session_cookie": session_cookie,
      "session_id": priority_session_id,
      "profile": qos_profile_name
    }
    print(f"qos_priority_session_create: {data}")
    resp = requests.post(url, data=json.dumps(data), verify=tls_verify)
    print(f"{resp.status_code} {resp.content}")

register_client()
get_app_inst_list()
find_cloudlet()
input("Press Enter to continue...")
qos_priority_session_delete()
input("Press Enter to continue...")
qos_priority_session_create()
sys.exit(0)

# Test rate limiting
for x in range(100):
    register_client()
