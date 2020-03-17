import os, sys
submodule = "../pytorch_objectdetecttrack"
sys.path.append(os.path.join(os.path.dirname(__file__), submodule))

from models import *
from utils import *
# from object_tracker import detect_image

import time, datetime, random
import torch
from torch.utils.data import DataLoader
from torchvision import datasets, transforms
from torch.autograd import Variable

import matplotlib.pyplot as plt
import matplotlib.patches as patches
from PIL import Image

package_dir = os.path.dirname(os.path.abspath(__file__))+"/"+submodule
print("XXXXXXXXXXXX", package_dir)
config_path=package_dir+'/config/yolov3.cfg'
print("XXXXXXXXXXXX", config_path)
weights_path=package_dir+'/config/yolov3.weights'
class_path=package_dir+'/config/coco.names'
img_size=416
conf_thres=0.8
nms_thres=0.4

# Load model and weights
model = Darknet(config_path, img_size=img_size)
model.load_weights(weights_path)

if torch.cuda.is_available():
    model.cuda()
    Tensor = torch.cuda.FloatTensor
else:
    Tensor = torch.FloatTensor

model.eval()
classes = utils.load_classes(class_path)

# Globals
total_server_processing_time = 0
count_server_processing_time = 0

class ObjectDetector(object):
    def __init__(self):
        pass

    def detect_image(self, img):
        # scale and pad image
        ratio = min(img_size/img.size[0], img_size/img.size[1])
        imw = round(img.size[0] * ratio)
        imh = round(img.size[1] * ratio)
        img_transforms = transforms.Compose([ transforms.Resize((imh, imw)),
             transforms.Pad((max(int((imh-imw)/2),0), max(int((imw-imh)/2),0), max(int((imh-imw)/2),0), max(int((imw-imh)/2),0)),
                            (128,128,128)),
             transforms.ToTensor(),
             ])
        # convert image to Tensor
        image_tensor = img_transforms(img).float()
        image_tensor = image_tensor.unsqueeze_(0)
        input_img = Variable(image_tensor.type(Tensor))
        # run inference on the model and get detections
        with torch.no_grad():
            detections = model(input_img)
            detections = utils.non_max_suppression(detections, 80, conf_thres, nms_thres)
        return detections[0]

    def process_image(self, img, outdir = None):
        """
        This version saves processed image to outdir
        """
        global total_server_processing_time
        global count_server_processing_time
        # load image and get detections
        prev_time = time.time()
        detections = self.detect_image(img)
        millis = (time.time() - prev_time)*1000
        elapsed = "%.3f" %millis
        print("%s ms to detect objects" %(elapsed))
        count_server_processing_time += 1
        total_server_processing_time += millis

        if outdir is not None:
            filename = img.filename
            img = np.array(img)
            # Get bounding-box colors
            cmap = plt.get_cmap('tab20b')
            colors = [cmap(i) for i in np.linspace(0, 1, 20)]

            plt.figure()
            fig, ax = plt.subplots(1, figsize=(12,9))
            ax.imshow(img)
        else:
            img = np.array(img)

        pad_x = max(img.shape[0] - img.shape[1], 0) * (img_size / max(img.shape))
        pad_y = max(img.shape[1] - img.shape[0], 0) * (img_size / max(img.shape))
        unpad_h = img_size - pad_y
        unpad_w = img_size - pad_x

        # Build list of results
        objects = []
        if detections is not None:
            if outdir is not None:
                unique_labels = detections[:, -1].cpu().unique()
                n_cls_preds = len(unique_labels)
                bbox_colors = random.sample(colors, n_cls_preds)
            # browse detections and draw bounding boxes
            for x1, y1, x2, y2, conf, cls_conf, cls_pred in detections:
                box_h = ((y2 - y1) / unpad_h) * img.shape[0]
                box_w = ((x2 - x1) / unpad_w) * img.shape[1]
                y1 = ((y1 - pad_y // 2) / unpad_h) * img.shape[0]
                x1 = ((x1 - pad_x // 2) / unpad_w) * img.shape[1]
                if outdir is not None:
                    color = bbox_colors[int(np.where(unique_labels == int(cls_pred))[0])]
                    bbox = patches.Rectangle((x1, y1), box_w, box_h, linewidth=2, edgecolor=color, facecolor='none')
                    ax.add_patch(bbox)
                    plt.text(x1, y1, s=classes[int(cls_pred)], color='white', verticalalignment='top',
                            bbox={'color': color, 'pad': 0})
                confidence = "%.2f" %float(cls_conf)
                object = {"rect": [int(x1), int(y1), int(x1+box_w), int(y1+box_h)], "class": classes[int(cls_pred)], "confidence": confidence}
                objects.append(object)

        if outdir is not None:
            plt.axis('off')
            outname = outdir+"/det-"+os.path.basename(filename)
            print("Saving to "+outname)
            plt.savefig(outname, bbox_inches='tight', pad_inches=0.0)
            #plt.show()
            plt.close('all')

        print("returning objects:", objects)
        return objects

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--filename", required=False, help="Name of image file to send.")
    parser.add_argument("-d", "--directory", required=False, help="Directory containing image files to process.")
    parser.add_argument("-o", "--outdir", required=True, help="Directory to output processed images to.")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    args = parser.parse_args()

    if args.filename != None and args.directory != None:
        print("Can't include both filename and directory arguments")
        sys.exit()
    if args.filename is None and args.directory is None:
        print("Must include either filename or directory arguments")
        sys.exit()

    detector = ObjectDetector()

    if args.filename != None:
        files = [args.filename]
        dir_prefix = ""

    elif args.directory != None:
        files = os.listdir(args.directory)
        dir_prefix = args.directory+"/"

    valid_extensions = ('jpg','jpeg', 'png')

    for x in range(args.repeat):
        for image_name in files:
            if not image_name.endswith(valid_extensions):
                continue
            img = Image.open(dir_prefix+image_name)
            detector.process_image(img, args.outdir)

    if count_server_processing_time > 0:
        average_server_processing_time = total_server_processing_time / count_server_processing_time
        print("Average Server Processing Time=%.3f ms" %average_server_processing_time)
