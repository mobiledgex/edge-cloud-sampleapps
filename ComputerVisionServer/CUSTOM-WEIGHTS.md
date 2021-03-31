
# Object Detection with Custom Weights File
This implementation of Object Detection uses a pre-trained model trained with the COCO dataset. You may wish to deploy the ComputerVision server with their own custom weights and class names files instead.

## Test Custom Weights File
Before doing a full Docker build and deployment, you may test your custom yolov3.weights and coco.names files locally. Here's how to do that.

### Check out repository and prepare environment
	git clone https://github.com/mobiledgex/edge-cloud-sampleapps.git
	cd edge-cloud-sampleapps/ComputerVisionServer/
	git submodule init
	git submodule update
	virtualenv env -ppython3
	source env/bin/activate
	pip install -r requirements.txt

This step may take some time as the Pytorch library is quite large.

### Download default weights file
	cd moedx/pytorch_objectdetecttrack/config/
	wget http://opencv.facetraining.mobiledgex.net/files/yolov3.weights

### Run object_detector.py with default weights and names files
	cd ../../object_detection/
	python object_detector.py --show-responses -f ../client/objects_001.jpg

The output should include results similar to this:

	[{'rect': [2, 76, 105, 200], 'class': 'bicycle', 'confidence': '1.00'}, {'rect': [57, 144, 106, 204], 'class': 'sports ball', 'confidence': '0.99'}, {'rect': [95, 62, 165, 173], 'class': 'chair', 'confidence': '0.99'}]

You can change the `-f` parameter to point to any image file you would like to perform object detection on.

### Run object_detector.py with custom weights and names files
Change to the config directory and make backups of the existing files.

	cd ../pytorch_objectdetecttrack/config/
	mv coco.names coco.names.backup
	mv yolov3.weights yolov3.weights.backup

Now copy your custom files into the config directory.

	cp <location of custom files>/coco.names .
	cp <location of custom files>/yolov3.weights .

In this same directory, you will need to edit yolov3.cfg and change some values that depend on the number of classes you have. Change the "classes=" on lines 610, 696, and 783 to the number of classes in your model. Change the "filters=" on lines 603, 689, and 776 to (classes+5)\*3. For example, for 4 classes, you would use (4+5)\*3=27, so the lines would look like this: `filters=27`.

It may be confusing to have your "names" file named coco.names if your model is not actually based on the COCO dataset. If you prefer, you can edit object_detector.py and change the `class_path` variable to point to your names file with your preferred name. Example:

	class_path=package_dir+'/config/custom.names'

Change back to the `object_detection` directory and give it a try, using as the input an image that has your custom object visible.

	cd ../../object_detection/
	python object_detector.py --show-responses -f customImageFile.jpg

If everything worked, you should see a JSON list of detected objects with bounding box values and class names.

## Test and Deploy Custom Weights File
These steps will result in a ComputerVision Docker image which includes your custom weights and names file. This assumes you are deploying to a GPU-enabled cloudlet.
Normally, the Docker build process downloads the weights file on the fly. Since you already have your weights file in place from the previous steps, you just need to comment out the download step.

Edit `Dockerfile_gpu` and find this line:

	RUN wget http://opencv.facetraining.mobiledgex.net/files/yolov3.weights

Now comment it out by adding a `#` to the beginning of the line:

	# RUN wget http://opencv.facetraining.mobiledgex.net/files/yolov3.weights

### Test Docker image locally
At this point, you can build the Docker image with the following command:

	TAG=2021-03-30 GPU=true make docker-build

And run it with this command:

	docker run --gpus all --rm -p 8008:8008 mobiledgex-samples/computervision-gpu:2021-03-30

Test the REST interface of the container with a curl command (of course you can substitute your own image file name):

	curl -X POST "http://localhost:8008/object/detect/" --data-binary "@objects_001.jpg" -H "Content-Type: image/jpeg"

Similar to the tests above, the output should be a JSON list of detected objects with bounding box values and class names

### Deploy to Cloudlet

If the server is working to your satisfaction, you can now build the image and publish it to the Docker registry with this command:

	TAG=2021-03-30 GPU=true make

Now your image is in the registry and you can deploy it to a MobiledgeX cloudlet as usual.
