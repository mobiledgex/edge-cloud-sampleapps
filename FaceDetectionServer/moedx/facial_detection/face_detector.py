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

import cv2 as cv
import numpy as np
import os

class FaceDetector(object):
    """
    Wrapper for the OpenCV program for detecting faces.
    Calling the 'detect_faces(...)' method on an image will
    get the information on the faces in the image. This class
    allows for an implementation agnostic way of detecting
    faces.
    """
    all_faces = []
    face_cascade = None
    scale = 0
    resize_width = 320
    face_tempate = None
    face_roi = None
    face_pos = None

    # The pre-built cascade classifier xml file should be
    # located in the same directory as the facedetector.
    default_cascade_file_path = os.path.dirname(
        os.path.realpath(__file__)) + '/default.xml'

    def __init__(self,
                 cascade_file_path=default_cascade_file_path):
        """
        Loads the image classifier and raises an exception if
        the classifier fails to load the xml classifier file.

        When using a different classifier xml, make sure to
        take into account relative file paths to avoid raising
        an exception.
        """
        self.face_cascade = cv.CascadeClassifier(
            cascade_file_path)

        if self.face_cascade.empty():
            print("Classifier failed to load '{}' "
                  "filepath".format(cascade_file_path))
            raise


    def detect_faces(self, frame):
        """
        Return a list of face objects that represents detected faces.
        """
        rows, cols = frame.shape[:2]

        self.all_faces = self.face_cascade.detectMultiScale(frame, scaleFactor=1.3, minNeighbors=5)

        if len(self.all_faces) == 0:
            return []

        self.all_faces[:,2:] += self.all_faces[:,:2]
        return self.all_faces


if __name__ == "__main__":
    """
    If run from this file, open the system camera and display
    facial detection on the frames streaming from the system
    camera. This gives a good example of the proof of concept
    and shows off how the application should work.
    """
    import json

    fd = FaceDetector()
    capture = cv.VideoCapture(0)
    while True:
        _, frame = capture.read()

        data = { 'file': json.dumps(frame.tolist()) }
        frame = json.loads(data['file'])
        frame = np.array(frame)

        rects = fd.detect_faces(frame)
        print(rects)

        for x1, y1, x2, y2 in rects:
            cv.rectangle(
               frame, (x1, y1), (x2, y2), (0, 255, 0), 2
            )

        cv.imshow('frame', frame)
        if cv.waitKey(1) & 0xFF == ord('q'):
            break

    capture.release()
    cv.destroyAllWindows()
