echo "=== openpose.bonn-mexdemo.mobiledgex.net"

scp -i ~/.ssh/id_rsa_mobiledgex tracker/views.py tracker/models.py tracker/apps.py mobiledgex@openpose.bonn-mexdemo.mobiledgex.net:/home/mobiledgex/edge-cloud-sampleapps/FaceDetectionServer/moedx/tracker
scp -i ~/.ssh/id_rsa_mobiledgex moedx/urls.py moedx/settings.py mobiledgex@openpose.bonn-mexdemo.mobiledgex.net:/home/mobiledgex/edge-cloud-sampleapps/FaceDetectionServer/moedx/moedx
scp -i ~/.ssh/id_rsa_mobiledgex facial_detection/facedetector.py facial_detection/faceRecognizer.py mobiledgex@openpose.bonn-mexdemo.mobiledgex.net:/home/mobiledgex/edge-cloud-sampleapps/FaceDetectionServer/moedx/facial_detection
# scp -r facial_detection/training-data mobiledgex@openpose.bonn-mexdemo.mobiledgex.net:/home/mobiledgex/edge-cloud-sampleapps/FaceDetectionServer/moedx/facial_detection

echo "=== acrotopia.com"

scp tracker/views.py tracker/models.py tracker/apps.py root@acrotopia.com:/root/github/edge-cloud-sampleapps/FaceDetectionServer/moedx/tracker
scp moedx/urls.py moedx/settings.py root@acrotopia.com:/root/github/edge-cloud-sampleapps/FaceDetectionServer/moedx/moedx
scp facial_detection/facedetector.py facial_detection/faceRecognizer.py root@acrotopia.com:/root/github/edge-cloud-sampleapps/FaceDetectionServer/moedx/facial_detection
# scp -r facial_detection/training-data root@acrotopia.com:/root/github/edge-cloud-sampleapps/FaceDetectionServer/moedx/facial_detection
