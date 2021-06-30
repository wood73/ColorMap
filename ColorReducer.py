from sklearn.cluster import KMeans
import matplotlib as plt
from matplotlib import pyplot as display
import numpy as np
import cv2
from collections import Counter
from skimage.color import rgb2lab, deltaE_cie76
from PIL import Image
import math
import numpy as np


def RGB2HEX(color):
    return "#{:02x}{:02x}{:02x}".format(int(color[0]), int(color[1]), int(color[2]))


def reduce(input_img_path, output_png_path, number_of_colors):
    show_chart = False
    srcImage = cv2.imread(input_img_path)
    srcImage = cv2.cvtColor(srcImage, cv2.COLOR_BGR2RGB)

    '''
    First, we resize the image to the size 600 x 400. It is not required to resize it to a smaller size but we do so to
    lessen the pixels which’ll reduce the time needed to extract the colors from the image.
    '''
    h, w, color = srcImage.shape
    modified_image = srcImage.copy()
    if w > 600 or h > 400:
        modified_image = cv2.resize(srcImage, (600, 400), interpolation=cv2.INTER_AREA)
    modified_image = modified_image.reshape(modified_image.shape[0] * modified_image.shape[1], 3)
    clf = KMeans(n_clusters=number_of_colors)
    labels = clf.fit_predict(modified_image)

    counts = Counter(labels)

    center_colors = clf.cluster_centers_
    # We get ordered colors by iterating through the keys
    ordered_colors = [center_colors[i] for i in counts.keys()]
    hex_colors = [RGB2HEX(ordered_colors[i]) for i in counts.keys()]
    rgb_colors = [ordered_colors[i] for i in counts.keys()]

    if show_chart:
        display.figure(figsize=(8, 6))
        display.pie(counts.values(), labels=hex_colors, colors=hex_colors)
        display.show()

    input_image = Image.open(input_img_path)

    output_image = Image.new(input_image.mode, input_image.size)
    pixels_new = output_image.load()

    width, height = input_image.size
    maxIterations = width * height
    iterator = 1
    for i in range(width):
        for j in range(height):

            # getting the RGB pixel value.
            r, g, b, p = input_image.getpixel((i, j))

            closest_color = []
            closest_delta = 10000
            # find nearest color:
            for k in rgb_colors:
                delta = color_distance(k, [r, g, b])
                if delta < closest_delta:
                    closest_delta = delta
                    closest_color = k

            # setting the pixel value.
            pixels_new[i, j] = (int(closest_color[0]), int(closest_color[1]), int(closest_color[2]))

            print("Iteration " + str(iterator) + " / " + str(maxIterations))
            iterator += 1

    # Saving the final output
    output_image.save(output_png_path, format="png")

    # can use output_image.show() to display image

    return rgb_colors


def color_distance(color1, color2):
    rmean = (color1[0] + color2[0]) / 2
    r = color1[0] - color2[0]
    g = color1[1] - color2[1]
    b = color1[2] - color2[2]

    return math.sqrt(np.right_shift(int((512 + rmean) * r * r), 8) + 4 * g * g +
                     np.right_shift(int((767 - rmean) * b * b), 8))
