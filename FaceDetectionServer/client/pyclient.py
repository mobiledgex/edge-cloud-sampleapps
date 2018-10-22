"""
Simple Http client that sends an image to the facial detection
server and displays the result.
"""
import json
import requests
import cv2

class RequestClient(object):
    """ """
    BASE_URL = 'http://127.0.0.1:8000'
    API_ENDPOINT = '/detect/'
    
    def __init__(self):
        """ """
        return

    def send_image(self, image):
        """ """
        data = image.tolist()
        data = { 'file': json.dumps(data) }
        return requests.post(
            self.BASE_URL + self.API_ENDPOINT, json=data)

    
def image_to_gray(image):
    """ 
    Wrapper to convert images to their cv grayscale version.
    The facial tracker uses grayscale images, so we want to
    call this function prior to sending the image to the 
    server.
    """
    return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) < 2:
        print("Must supply a filename")
        exit()

    try:
        image = cv2.imread(sys.argv[1])
    except e:
        print(e)
        exit()
        
    # image = image_to_gray(image)

    rc = RequestClient()
    print(rc.send_image(image).content)
