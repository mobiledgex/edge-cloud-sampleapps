# In order to run this script, you must first login using the password from edge-cloud-infra/e2e-tests/data/mc_admin.yml
# mcctl --addr https://127.0.0.1:9900 login name=mexadmin --skipverify

# Delete has to be done in reverse order of create.

# Delete any existing app instances
mcctl --addr https://127.0.0.1:9900 --skipverify appinst delete region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 cloudlet-org=gcp cloudlet=tulsa-cloudlet1 cluster=cv-cluster
mcctl --addr https://127.0.0.1:9900 --skipverify appinst delete region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 cloudlet-org=gcp cloudlet=dallas-cloudlet1 cluster=cv-cluster
mcctl --addr https://127.0.0.1:9900 --skipverify appinst delete region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 cloudlet-org=tmus cloudlet=texoma-cloudlet1 cluster=cv-cluster

# Delete any existing apps
mcctl --addr https://127.0.0.1:9900 --skipverify app delete region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2

# Delete any existing cluster inst
mcctl --addr https://127.0.0.1:9900 --skipverify clusterinst delete region=local cluster=cv-cluster cloudlet-org=gcp cloudlet=tulsa-cloudlet1 cluster-org=MobiledgeX
mcctl --addr https://127.0.0.1:9900 --skipverify clusterinst delete region=local cluster=cv-cluster cloudlet-org=gcp cloudlet=dallas-cloudlet1 cluster-org=MobiledgeX
mcctl --addr https://127.0.0.1:9900 --skipverify clusterinst delete region=local cluster=cv-cluster cloudlet-org=tmus cloudlet=texoma-cloudlet1 cluster-org=MobiledgeX

# Delete any existing cloudlets
mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet delete region=local cloudlet-org=gcp cloudlet=tulsa-cloudlet1
mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet delete region=local cloudlet-org=gcp cloudlet=dallas-cloudlet1
mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet delete region=local cloudlet-org=tmus cloudlet=texoma-cloudlet1

# exit 1

# Create cloudlets
# mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet create region=local location.latitude=32.5007 location.longitude=-94.7405 numdynamicips=254 cloudlet-org=gcp cloudlet=tyler-cloudlet1
mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet create region=local location.latitude=36.154 location.longitude=-95.9928 numdynamicips=254 cloudlet-org=gcp cloudlet=tulsa-cloudlet1
mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet create region=local location.latitude=32.7767 location.longitude=-96.797 numdynamicips=254 cloudlet-org=gcp cloudlet=dallas-cloudlet1
mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet create region=local location.latitude=33.7998 location.longitude=-96.5783 numdynamicips=254 cloudlet-org=tmus cloudlet=texoma-cloudlet1

# Create cluster inst
mcctl --addr https://127.0.0.1:9900 --skipverify clusterinst create region=local cluster=cv-cluster cloudlet-org=gcp cloudlet=tulsa-cloudlet1 cluster-org=MobiledgeX flavor=x1.small deployment=docker
mcctl --addr https://127.0.0.1:9900 --skipverify clusterinst create region=local cluster=cv-cluster cloudlet-org=gcp cloudlet=dallas-cloudlet1 cluster-org=MobiledgeX flavor=x1.small deployment=docker
mcctl --addr https://127.0.0.1:9900 --skipverify clusterinst create region=local cluster=cv-cluster cloudlet-org=tmus cloudlet=texoma-cloudlet1 cluster-org=MobiledgeX flavor=x1.small deployment=docker

# Create app
mcctl --debug --addr https://127.0.0.1:9900 --skipverify app create region=local \
app-org=MobiledgeX appname=ComputerVision appvers=2.2 deployment=docker imagetype=Docker \
imagepath=docker.mobiledgex.net/mobiledgex/images/computervision:2020-11-10 defaultflavor=x1.small \
accessports=tcp:8008,tcp:8011 qossessionprofile=QOS_THROUGHPUT_DOWN_M qossessionduration=5m

# Create app instances
mcctl --addr https://127.0.0.1:9900 --skipverify appinst create region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 cloudlet-org=gcp cloudlet=tulsa-cloudlet1 cluster=cv-cluster
mcctl --addr https://127.0.0.1:9900 --skipverify appinst create region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 cloudlet-org=gcp cloudlet=dallas-cloudlet1 cluster=cv-cluster
# mcctl --addr https://127.0.0.1:9900 --skipverify appinst create region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 cloudlet-org=tmus cloudlet=texoma-cloudlet1 cluster=cv-cluster


exit 1

mcctl --addr https://127.0.0.1:9900 --skipverify cloudlet create region=local location.latitude=33.7998 location.longitude=-96.5783 numdynamicips=254 cloudlet-org=tmus cloudlet=texoma-cloudlet1
mcctl --addr https://127.0.0.1:9900 --skipverify clusterinst create region=local cluster=cv-cluster cloudlet-org=tmus cloudlet=texoma-cloudlet1 cluster-org=MobiledgeX flavor=x1.small
mcctl --addr https://127.0.0.1:9900 --skipverify appinst create region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 cloudlet-org=tmus cloudlet=texoma-cloudlet1 cluster=cv-cluster


mcctl --debug --addr https://127.0.0.1:9900 --skipverify app update region=local app-org=MobiledgeX appname=ComputerVision appvers=2.2 qossessionprofile=QOS_THROUGHPUT_DOWN_S qossessionduration=100s
