import cv2
import os
import time
import numpy as np
import json
import sys
import glob

class FaceRecognizer(object):
    """
    Wrapper for the OpenCV program for recognizing faces.
    """

    def __init__(self):
        self.working_dir = os.path.dirname(os.path.realpath(__file__))

        #create our LBPH face recognizer
        self.face_recognizer = cv2.face.LBPHFaceRecognizer_create()
        #face_recognizer = cv2.face.EigenFaceRecognizer_create()
        #face_recognizer = cv2.face.FisherFaceRecognizer_create()

        self.trainingDataTimestamp = None
        self.trainingDataFileName = self.working_dir+'/trainer.yml'

    def read_trained_data(self):
        # Load the trained data
        self.face_recognizer.read(self.trainingDataFileName)

        #load subject names
        with open(self.working_dir+"/subjects.json", "r") as infile:
            self.subjects = json.load(infile)

        self.trainingDataTimestamp = time.time()
        dataFileTs = os.path.getmtime(self.trainingDataFileName)
        print("self.trainingDataTimestamp=%d dataFileTs=%d" %(self.trainingDataTimestamp,dataFileTs))

    #this function will read all persons' training images, detect face from each image
    #and will return two lists of exactly same size, one list
    # of faces and another list of labels for each face
    def prepare_training_data(self, data_folder_path):

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

    def update_training_data(self):
        faces, labels, subjects = self.prepare_training_data(self.working_dir+"/training-data")
        self.face_recognizer.train(faces, np.array(labels))
        self.face_recognizer.save(self.trainingDataFileName)

        #Save subjects array to disk
        with open(self.working_dir+"/subjects.json", "w") as outfile:
            json.dump(subjects, outfile)

    def save_subject_image(self, subject, image):
        #Save current image with timestamp
        now = time.time()
        mlsec = repr(now).split('.')[1][:3]
        timestr = time.strftime("%Y%m%d-%H%M%S")+"."+mlsec
        os.makedirs(self.working_dir+"/training-data/"+subject, exist_ok=True)
        fileName = self.working_dir+"/training-data/"+subject+"/face_"+timestr+".png"
        with open(fileName, "wb") as fh:
            fh.write(image)
        #Delete all old files except the 20 most recent
        # logger.debug(prepend_ip("Deleting all but 20 newest images", request))
        files = sorted(glob.glob(self.working_dir+"/training-data/"+subject+"/face_*.png"), key=os.path.getctime, reverse=True)
        #print(files[20:])
        for file in files[20:]:
            #print("removing %s" %file)
            os.remove(file)

    #function to detect face using OpenCV
    def detect_face(self, img):
        #convert the test image to gray image as opencv face detector expects gray images
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        #load OpenCV face detector, I am using LBP which is fast
        #there is also a more accurate but slow Haar classifier
        # face_cascade = cv2.CascadeClassifier('opencv-files/lbpcascade_frontalface.xml')
        face_cascade = cv2.CascadeClassifier(self.working_dir+'/opencv-files/haarcascade_frontalface_alt.xml')

        #let's detect multiscale (some images may be closer to camera than others) images
        #result is a list of faces
        faces = face_cascade.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=5);

        #if no faces are detected then return None
        if (len(faces) == 0):
            return None, None

        #under the assumption that there will be only one face,
        #extract the face area
        (x, y, w, h) = faces[0]

        #return only the face part of the image
        return gray[y:y+w, x:x+h], faces[0]

    #this function recognizes the person in image passed
    #and draws a rectangle around detected face with name of the
    #subject
    def predict(self, test_img):
        # Is training data current?
        dataFileTs = os.path.getmtime(self.trainingDataFileName)
        diff = dataFileTs - self.trainingDataTimestamp
        print("diff=%d. Do we need to re-read training data?" %diff)
        if diff > 10:
            print("diff=%d. Need to re-read training data" %diff)
            self.read_trained_data()

        #make a copy of the image as we don't want to chang original image
        img = test_img.copy()
        #detect face from the image
        face, rect = self.detect_face(img)

        if face is None or rect is None:
            #print("No face found for test_image ", type(test_img))
            return None, None, None, None

        #predict the image using our face recognizer
        label, confidence = self.face_recognizer.predict(face)
        #get name of respective label returned by face recognizer
        label_text = self.subjects[label]

        # print(label_text, confidence, rect)
        return img, label_text, confidence, rect

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
