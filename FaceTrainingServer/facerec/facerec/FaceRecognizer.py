import cv2
import os
import time
import datetime
import numpy as np
import json
import sys
import glob
import logging
import imghdr
from threading import Thread
import zlib
import shutil

logger = logging.getLogger(__name__)

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

        self.training_data_filename = 'trained.yml'
        self.training_data_filepath = self.working_dir+"/"+self.training_data_filename
        self.training_data_timestamp = 0

        if not os.path.exists(self.training_data_filepath):
            self.update_training_data()

        self.read_trained_data()


    def init_database(self):
        """
        This function scans the training-data directory and if no DB entry
        exists for a subject, it is created.

        This is for legacy subjects that were created by an individual
        FaceDetectionServer before the FaceTrainingServer existed.
        """
        from trainer.models import Subject, Owner

        default_owner_id = "000000000000000000000"
        default_owner_name = "Legacy Owner"
        owner_record, created = Owner.objects.get_or_create(id = default_owner_id, name = default_owner_name)
        if created:
            logger.info("Added Owner: %s" %owner_record)
            owner_record.save()
        else:
            logger.info("Existing Owner: %s" %owner_record)

        data_folder_path = self.working_dir+"/training-data"
        #get the directories (one directory for each subject) in data folder
        dirs = os.listdir(data_folder_path)
        for dir_name in dirs:

            #ignore system files like .DS_Store
            if dir_name.startswith("."):
                continue;

            subject = dir_name
            subject_record, created = Subject.objects.get_or_create(name = subject, owner = owner_record, in_training = True)
            if created:
                logger.info("Added Subject: %s" %subject_record)
                subject_record.save()
            else:
                logger.info("Existing Subject: %s" %subject_record)

    def prepare_training_data(self, data_folder_path):
        """
        this function will read all persons' training images, detect face from each image
        and will return two lists of exactly same size, one list
        of faces and another list of labels for each face
        """
        logger.info("prepare_training_data(%s)" %data_folder_path)

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
        of with the single face and another list with the single labels for the face
        """
        logger.info("prepare_training_data_single(%s, %s)" %(data_folder_path, subject))

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

    def update_training_data(self):
        """
        From all subject directories, add new images to the training data.
        """
        logger.info("update_training_data()")
        faces, labels, subjects = self.prepare_training_data(self.working_dir+"/training-data")
        logger.info("subjects=%s len(subjects)=%d len(labels)=%d" %(subjects, len(subjects), len(labels)))

        # Wipe out any existing LabelInfo values.
        index = 0
        while self.face_recognizer.getLabelInfo(index) != "":
            logger.info("setLabelInfo(%d, %s)" %(index, ""))
            self.face_recognizer.setLabelInfo(index, "")
            index += 1

        # Save the subjects (directory names) to their corresponding labels.
        index = 0
        for subject in subjects:
            logger.info("setLabelInfo(%d, %s)" %(index, subject))
            self.face_recognizer.setLabelInfo(index, subject)
            index += 1

        self.face_recognizer.train(faces, np.array(labels))
        self.face_recognizer.save(self.training_data_filepath)

    def update_training_data_single(self, subject):
        """
        From a single subject directory, add new images to the training data.
        """
        logger.info("update_training_data_single(%s)" %subject)
        faces, labels, subjects = self.prepare_training_data_single(self.working_dir+"/training-data", subject)
        logger.info("subjects=%s len(subjects)=%d len(labels)=%d" %(subjects, len(subjects), len(labels)))
        if len(subjects) != 1:
            logger.error("Must be a single subject")
            return False

        # Count existing LabelInfo values.
        index = 0
        label_info = self.face_recognizer.getLabelInfo(index)
        while label_info != "":
            logger.info("getLabelInfo(%d)=%s" %(index, label_info))
            if label_info == subject:
                logger.info("%s already exists in training data. Rebuilding full set." %subject)
                self.update_training_data()
                return
            index += 1
            label_info = self.face_recognizer.getLabelInfo(index)

        # Save the subject (directory name) to its corresponding label.
        logger.info("setLabelInfo(%d, %s)" %(index, subjects[0]))
        self.face_recognizer.setLabelInfo(index, subjects[0])
        self.face_recognizer.update(faces, np.array(labels))
        self.face_recognizer.save(self.training_data_filepath)

    def read_trained_data(self):
        logger.info("read_trained_data")
        self.face_recognizer.read(self.training_data_filepath)

        x = 0
        while self.face_recognizer.getLabelInfo(x) != "":
            logger.info("%d. Loaded trained data for label: %s" %(x, self.face_recognizer.getLabelInfo(x)))
            x += 1

    def get_training_data_timestamp(self):
        self.training_data_timestamp = os.path.getmtime(self.training_data_filepath)
        logger.info("get_training_data_timestamp=%d ctime=%s" %(self.training_data_timestamp, time.ctime(self.training_data_timestamp)))
        return self.training_data_timestamp

    def save_subject_image(self, subject, image):
        dir = self.working_dir+"/training-data/"+subject
        os.makedirs(dir, exist_ok=True)
        self.save_image(dir, image)

    def save_debug_image(self, image):
        logger.debug("Saving image for debugging")
        self.save_image("/tmp", image)

    def save_image(self, dir, image):
        """
        Save current image with timestamp
        """
        extension = imghdr.what("", image)
        now = time.time()
        mlsec = repr(now).split('.')[1][:3]
        timestr = time.strftime("%Y%m%d-%H%M%S")+"."+mlsec
        fileName = dir+"/face_"+timestr+"."+extension
        with open(fileName, "wb") as fh:
            fh.write(image)
        #Delete all old files except the 20 most recent
        logger.debug("Deleting all but 20 newest images")
        try:
            files = sorted(glob.glob(dir+"/face_*.*"), key=os.path.getctime, reverse=True)
            for file in files[20:]:
                os.remove(file)
        except FileNotFoundError:
            logger.warn("Cleanup of "+dir+"/face* failed. Next request will retry.")

    def remove_subject(self, subject):
        """
        Delete the subject directory including all images,
        then rebuild the training data file.
        """
        dir = self.working_dir+"/training-data/"+subject
        logger.info("Deleting subject directory %s" %dir)
        if not os.path.exists(dir):
            raise FileNotFoundError('Subject directory "%s" does not exist' %subject)
        shutil.rmtree(dir)

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

    def detect_faces(self, frame):
        """
        Return a list of face objects that represent detected faces.
        """
        rows, cols = frame.shape[:2]

        self.all_faces = self.face_cascade.detectMultiScale(
            frame, scaleFactor=1.1, minNeighbors=3, flags=0,
            minSize=(int(rows / 5), int(rows / 5)),
            maxSize=(int(rows * 2 / 3), int(rows * 2 / 3)))

        if len(self.all_faces) == 0:
            return []

        self.all_faces[:,2:] += self.all_faces[:,:2]
        return self.all_faces

    def predict(self, test_img):
        """
        this function recognizes the person in image passed
        and draws a rectangle around detected face with name of the
        subject
        """
        #detect face from the image
        face, rect = self.detect_face(test_img)

        if face is None or rect is None:
            #print("No face found for test_image ", type(test_img))
            return None, None, None

        #predict the image using our face recognizer
        label, confidence = self.face_recognizer.predict(face)
        #get name of respective label returned by face recognizer
        label_text = self.face_recognizer.getLabelInfo(label)
        logger.info("label=%s label_text=%s" %(label, label_text))
        return label_text, confidence, rect

if __name__ == "__main__":

    fr = FaceRecognizer()
    fr.update_training_data()
    fr.read_trained_data()
