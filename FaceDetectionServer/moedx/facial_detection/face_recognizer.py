# Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
# MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import cv2
import os
import time
import numpy as np
import json
import sys
import glob
import logging
import requests
from requests.auth import HTTPBasicAuth
import imghdr
from threading import Thread
import zlib

logger = logging.getLogger(__name__)

TRAINING_DATA_HOSTNAME = 'opencv.facetraining.mobiledgex.net'
TRAINING_DATA_PORT_DEFAULT = 8009 # Can be overridden with envvar FD_TRAINING_DATA_PORT
TRAINING_SERVER_TIMEOUT = 20 # Seconds

class FaceRecognizer(object):
    """
    Wrapper for the OpenCV program for recognizing faces.
    """

    def __init__(self):
        self.working_dir = os.path.dirname(os.path.realpath(__file__))

        #Create face detector
        self.face_cascade = cv2.CascadeClassifier(self.working_dir+'/opencv-files/haarcascade_frontalface_alt.xml')

        #create our LBPH face recognizer
        self.face_recognizer = cv2.face.LBPHFaceRecognizer_create()
        #face_recognizer = cv2.face.EigenFaceRecognizer_create()
        #face_recognizer = cv2.face.FisherFaceRecognizer_create()

        self.training_data_local_timestamp = 0
        self.training_data_filename = self.working_dir+'/trainer.yml'

        # Check for environment variables to override settings.
        try:
            self.training_data_port = int(os.environ['FD_TRAINING_DATA_PORT'])
        except (ValueError, KeyError) as e:
            self.training_data_port = TRAINING_DATA_PORT_DEFAULT

        logger.info("Using training_data_port %d" %self.training_data_port)

        # TODO: Update this when we have a different auth method.
        self.training_data_hostname = TRAINING_DATA_HOSTNAME
        self.training_data_url = 'http://%s:%d' %(self.training_data_hostname, self.training_data_port)
        self.training_data_auth = HTTPBasicAuth('mexf4ceuser555', 'p4ssw0dmexf4c3999')
        self.training_data_timestamp = 0

        # self.training_data_startup()
        # This is commented out because at startup, uvicorn calls this __init__
        # function in async mode, and db access is not allowed. Training data
        # will be checked for updates, and downloaded if necessary on first call
        # of /recognizer/predict.

    def training_data_startup(self):
        logger.info("training_data_startup()")
        try:
            while self.is_update_in_progress():
                logger.info("Sleeping while another worker updates training data")
                time.sleep(2)
            self.download_training_data_if_needed()
        except Exception as e:
            logger.error(e)

        try:
            self.read_training_data_if_needed()
        except Exception as e:
            logger.error(e)

    def read_trained_data(self):
        logger.info("read_trained_data()")
        # self.set_update_in_progress(True)

        # Create a new instance and load the trained data
        # Note: New instance is required when starting the server with --preload,
        # otherwise, the updated data doesn't "take".
        self.face_recognizer = cv2.face.LBPHFaceRecognizer_create()
        self.face_recognizer.read(self.training_data_filename)

        x = 0
        while self.face_recognizer.getLabelInfo(x) != "":
            logger.info("%d. Loaded trained data for label: %s" %(x, self.face_recognizer.getLabelInfo(x)))
            x += 1

        self.training_data_local_timestamp = time.time()
        dataFileTs = os.path.getmtime(self.training_data_filename)
        logger.info("training_data_local_timestamp=%d dataFileTs=%d" %(self.training_data_local_timestamp,dataFileTs))
        # self.set_update_in_progress(False)

    def prepare_training_data(self, data_folder_path):
        """
        this function will read all persons' training images, detect face from each image
        and will return two lists of exactly same size, one list
        of faces and another list of labels for each face
        """

        #get the directories (one directory for each subject) in data folder
        dirs = os.listdir(data_folder_path)

        #list to hold all subject faces
        faces = []
        #list to hold labels for all subjects
        labels = []
        #List to hold subject names
        subjects = []

        label = -1;
        #let's go through each directory and read images within it
        for dir_name in dirs:

            #ignore system files like .DS_Store
            if dir_name.startswith("."):
                continue;

            label += 1
            subjects.append(dir_name)
            logger.info("label=%d subject=%s" %(label, dir_name))

            #build path of directory containing images for current subject subject
            #sample subject_dir_path = "training-data/Bruce"
            subject_dir_path = data_folder_path + "/" + dir_name

            #get the images names that are inside the given subject directory
            subject_images_names = os.listdir(subject_dir_path)

            #go through each image name, read image,
            #detect face and add face to list of faces
            for image_name in subject_images_names:

                #ignore system files like .DS_Store
                if image_name.startswith("."):
                    continue;

                #sample image path = training-data/Bruce/face1.png
                image_path = subject_dir_path + "/" + image_name

                image = cv2.imread(image_path)

                face, rect = self.detect_face(image)

                #we will ignore faces that are not detected
                if face is not None:
                    #add face to list of faces
                    faces.append(face)
                    #add label for this face
                    labels.append(label)

        return faces, labels, subjects

    def prepare_training_data_single(self, data_folder_path, subject):
        """
        this function will read a single person's training images, detect face from each image
        and will return two lists of exactly same size, one list
        of faces and another list of labels for each face
        """

        #get the directories (one directory for each subject) in data folder
        dirs = os.listdir(data_folder_path)

        #list to hold all subject faces
        faces = []
        #list to hold labels for all subjects
        labels = []
        #List to hold single subject name
        subjects = [subject]

        label = 0

        #build path of directory containing images for current subject subject
        #sample subject_dir_path = "training-data/Bruce"
        subject_dir_path = data_folder_path + "/" + subject

        #get the images names that are inside the given subject directory
        subject_images_names = os.listdir(subject_dir_path)

        #go through each image name, read image,
        #detect face and add face to list of faces
        for image_name in subject_images_names:

            #ignore system files like .DS_Store
            if image_name.startswith("."):
                continue;

            image_path = subject_dir_path + "/" + image_name
            image = cv2.imread(image_path)
            face, rect = self.detect_face(image)

            #we will ignore faces that are not detected
            if face is not None:
                #add face to list of faces
                faces.append(face)
                #add label for this face
                labels.append(label)

        return faces, labels, subjects

    def update_training_data(self, subject=None):
        faces, labels, subjects = self.prepare_training_data(self.working_dir+"/training-data")
        logger.info("subjects=%s len(subjects)=%d len(labels)=%d" %(subjects, len(subjects), len(labels)))

        # Save the subjects (directory names) to their corresponding labels.
        index = 0
        for subject in subjects:
            logger.info("setLabelInfo(%d, %s)" %(index, subject))
            self.face_recognizer.setLabelInfo(index, subject)
            index += 1

        self.face_recognizer.train(faces, np.array(labels))
        self.face_recognizer.save(self.training_data_filename)

    def save_subject_image(self, subject, image):
        #Save current image with timestamp
        extension = imghdr.what("", image)
        now = time.time()
        mlsec = repr(now).split('.')[1][:3]
        timestr = time.strftime("%Y%m%d-%H%M%S")+"."+mlsec
        os.makedirs(self.working_dir+"/training-data/"+subject, exist_ok=True)
        fileName = self.working_dir+"/training-data/"+subject+"/face_"+timestr+"."+extension
        with open(fileName, "wb") as fh:
            fh.write(image)
        #Delete all old files except the 20 most recent
        # logger.debug(prepend_ip("Deleting all but 20 newest images", request))
        files = sorted(glob.glob(self.working_dir+"/training-data/"+subject+"/face_*.png"), key=os.path.getctime, reverse=True)
        #print(files[20:])
        for file in files[20:]:
            #print("removing %s" %file)
            os.remove(file)

    def detect_face(self, img):
        """
        function to detect a single face using OpenCV
        """
        #convert the test image to gray image as opencv face detector expects gray images
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        #let's detect multiscale (some images may be closer to camera than others) images
        #result is a list of faces
        faces = self.face_cascade.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=5);

        #if no faces are detected then return None
        if (len(faces) == 0):
            return None, None

        #under the assumption that there will be only one face,
        #extract the face area
        (x, y, w, h) = faces[0]

        #return only the face part of the image
        return gray[y:y+w, x:x+h], faces[0]

    def set_db_timestamps(self, last_download_timestamp=None):
        from tracker.models import CentralizedTraining
        ct, created = CentralizedTraining.objects.get_or_create(
            server_name = self.training_data_hostname)
        ct.last_check_timestamp = time.time()
        if last_download_timestamp != None:
            ct.last_download_timestamp = last_download_timestamp
        logger.debug("set_db_timestamps %s" %ct.dump_timestamps())
        ct.save()

    def get_db_timestamps(self):
        from tracker.models import CentralizedTraining
        ct, created = CentralizedTraining.objects.get_or_create(
            server_name = self.training_data_hostname)
        logger.debug("get_db_timestamps %s" %ct.dump_timestamps())
        return ct.last_download_timestamp, ct.last_check_timestamp

    def set_update_in_progress(self, flag):
        from tracker.models import CentralizedTraining
        ct = CentralizedTraining.objects.get(
            server_name = self.training_data_hostname)
        ct.update_in_progress = flag
        ct.update_started_timestamp = int(time.time())
        ct.save()
        logger.debug("set_update_in_progress %s" %ct.dump_update_flags())

    def is_update_in_progress(self):
        from tracker.models import CentralizedTraining
        ct, created = CentralizedTraining.objects.get_or_create(
            server_name = self.training_data_hostname)
        logger.debug("is_update_in_progress %s" %ct.dump_update_flags())
        now = time.time()
        if ct.update_in_progress:
            # Check to make sure we're not hung trying to download.
            if now - ct.update_started_timestamp > 30:
                logger.error("Update in progress for more than 30 seconds. Resetting flag.")
                ct.update_in_progress = False
                ct.save()
        return ct.update_in_progress

    def predict(self, test_img):
        """
        this function recognizes the person in image passed
        and draws a rectangle around detected face with name of the
        subject
        """
        self.read_training_data_if_needed()

        #make a copy of the image as we don't want to change original image
        img = test_img.copy()
        #detect face from the image
        face, rect = self.detect_face(img)

        if face is None or rect is None:
            #print("No face found for test_image ", type(test_img))
            return None, None, None, None

        #predict the image using our face recognizer
        label, confidence = self.face_recognizer.predict(face)
        #get name of respective label returned by face recognizer
        label_text = self.face_recognizer.getLabelInfo(label)
        logger.info("label=%s label_text=%s" %(label, label_text))

        # print(label_text, confidence, rect)
        return img, label_text, confidence, rect

    def download_training_data_if_needed(self):
        # Is training data current with server?
        logger.info("Checking with FaceTrainingServer")
        if not self.is_training_data_file_current():
            self.download_training_data()
            self.read_trained_data()
            ret = "Downloaded and re-read training data"
            logger.info(ret)
            return ret
        return "No update needed"

    def read_training_data_if_needed(self):
        if self.is_training_data_read_required():
            logger.info("read_training_data_if_needed() Yes, read training data")
            self.read_trained_data()
            logger.info("Loaded training data")
            return True
        else:
            return False

    def is_training_data_read_required(self):
        # Is loaded version of local training data current?
        dataFileTs = os.path.getmtime(self.training_data_filename)
        diff = dataFileTs - self.training_data_local_timestamp
        logger.debug("is_training_data_read_required() diff=%d. Do we need to read training data?" %(diff))
        return diff > 0

    def is_training_data_file_current(self):
        """
        Makes call to centralized training data server to see if our copy is up to date.
        Returns True if our copy is up to date, False if newer data is available.
        """
        self.set_update_in_progress(True)
        self.training_data_timestamp, last_check_timestamp = self.get_db_timestamps()
        now = time.time()
        url = self.training_data_url + '/trainer/lastupdate/'
        logger.info("is_training_data_file_current() url=%s" %url)
        timestamp = int(requests.get(url, auth=self.training_data_auth, timeout=TRAINING_SERVER_TIMEOUT).content)
        elapsed = "%.3f" %((time.time() - now)*1000)
        logger.info("%s ms to get centralized training data timestamp: %d. local=%d" %(elapsed, timestamp, self.training_data_timestamp))
        self.set_db_timestamps() #Updates last_check_timestamp only
        self.set_update_in_progress(False)
        if timestamp > self.training_data_timestamp:
            logger.info("Newer training data available.")
            return False
        else:
            logger.info("Local copy of centralized training data is current")
            return True

    def download_training_data(self):
        """
        Downloads centralized training data.
        """
        self.set_update_in_progress(True)
        now = time.time()
        url = self.training_data_url + '/trainer/download'
        logger.info("Downloading from %s..." %url)
        r = requests.get(url, auth=self.training_data_auth, timeout=TRAINING_SERVER_TIMEOUT)
        if r.status_code != 200:
            logger.error("Error downloading centralized training data. status_code=%d" %r.status_code)
            return False
        self.training_data_timestamp = int(r.headers.get('Last-Modified'))
        open(self.training_data_filename, 'wb').write(r.content)
        elapsed = "%.3f" %((time.time() - now)*1000)
        logger.info("%s ms to download centralized training data. training_data_timestamp=%d" %(elapsed, self.training_data_timestamp))
        self.set_db_timestamps(self.training_data_timestamp)
        self.set_update_in_progress(False)
        return True

if __name__ == "__main__":

    #python faceRecognizer.py test-data/Bruce.png test-data/Rolf.png test-data/Lev.png test-data/Wonho.png
    print("Predicting images...")

    fr = FaceRecognizer()
    fr.read_trained_data()

    """
    path = sys.argv[1]
    images = os.listdir(path)
    for image in images:
        if image.endswith('.png'):
            print("image=%s" %image)
            test_img = cv2.imread(path+"/"+image)
            predicted_img, subject, confidence, rect = fr.predict(test_img)
            if predicted_img is None:
                cv2.imshow(image, cv2.resize(test_img, (300, 400)))
    """

    num_args = len(sys.argv)
    for arg in sys.argv[1:]:
        test_img = cv2.imread(arg)
        predicted_img, subject, confidence, rect = fr.predict(test_img)
        cv2.imshow(subject, cv2.resize(predicted_img, (300, 400)))

    cv2.waitKey(0)
    cv2.destroyAllWindows()
    cv2.waitKey(1)
    cv2.destroyAllWindows()
