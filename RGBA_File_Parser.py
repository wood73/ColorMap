import sys
import io

class RGBA_File_Parser:
    def __init__(self, path):
        self.rgba_rgba_file = io.open(path, 'rb')
        self.possible_colors = self.byteToInt(self.rgba_rgba_file.read(1))
        self.rgba_rgba_hashTableSize = int(self.bytesToString(self.rgba_rgba_file.read(10)))
        self.rgba_rgba_hashTableIndexSize = self.rgba_rgba_hashTableSize / 15
        self.rgba_rgba_hashTableByteLocation = 11

    def get_3_overlapping_colors(self, red, green, blue, useScaling):

        closest_red = -1
        closest_green = -1
        closest_blue = -1
        brightness_multiplier = -1

        color_increments = 255 / (self.possible_colors - 1)
        red_remainder = red % color_increments
        if red >= 255 - color_increments / 2:
            closest_red = 255
        else:
            if red_remainder == 0:
                closest_red = red
            elif red_remainder <= color_increments / 2:
                closest_red = round(red - red_remainder)
            else:
                closest_red = round(red + (color_increments - red_remainder))

        green_remainder = green % color_increments
        if green >= 255 - color_increments / 2:
            closest_green = 255
        else:
            if green_remainder == 0:
                closest_green = green
            elif green_remainder <= color_increments / 2:
                closest_green = round(green - green_remainder)
            else:
                closest_green = round(green + (color_increments - green_remainder))

        blue_remainder = blue % color_increments
        if blue >= 255 - color_increments / 2:
            closest_blue = 255
        else:
            if blue_remainder == 0:
                closest_blue = blue
            elif blue_remainder <= color_increments / 2:
                closest_blue = round(blue - blue_remainder)
            else:
                closest_blue = round(blue + (color_increments - blue_remainder))

        if useScaling:
            max_color = max(closest_red, max(closest_green, closest_blue))
            brightness_multiplier = 255/max_color
            closest_red = int(min(round(closest_red*brightness_multiplier), 255))
            closest_green = int(min(round(closest_green*brightness_multiplier), 255))
            closest_blue = int(min(round(closest_blue * brightness_multiplier), 255))

        #  convert to base 256
        b256 = closest_red * 16777216 + closest_green * 65536 + closest_blue * 256 + 255
        hashValue = self.rgba_rgba_hash(b256)
        byteLocationOfHash = int(self.rgba_rgba_hashTableByteLocation + hashValue * 15)

        self.rgba_rgba_file.seek(byteLocationOfHash + 1)
        r_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        g_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        b_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        a_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        r2_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        g2_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        b2_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        a2_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
        r = self.byteToInt(self.rgba_rgba_file.read(1))
        ra = self.byteToInt(self.rgba_rgba_file.read(1))
        g = self.byteToInt(self.rgba_rgba_file.read(1))
        ga = self.byteToInt(self.rgba_rgba_file.read(1))
        b = self.byteToInt(self.rgba_rgba_file.read(1))
        ba = self.byteToInt(self.rgba_rgba_file.read(1))

        brightness_adjusted_rgba_output = [round(r2_rgba / brightness_multiplier),
                                           round(g2_rgba / brightness_multiplier),
                                           round(b2_rgba / brightness_multiplier)]

        print("Possible RGBA: " + str(r_rgba) + ", " + str(g_rgba) + ", " + str(b_rgba) + ", " + str(a_rgba) +
              "  |  Closest Mapped RGBA: " + str(brightness_adjusted_rgba_output[0]) + ", " +
              str(brightness_adjusted_rgba_output[1]) + ", " + str(brightness_adjusted_rgba_output[2]) +
              ", " + str(a2_rgba))

        return [r, ra, g, ga, b, ba]

    def rgba_rgba_hash(self, toHash):
        foundHash = False
        initialHash = -1
        hashed = -1
        exponentTooLarge = False
        maxExponent = -1
        linearIncrement = 1

        iterator = 1
        while not foundHash:
            # print(hashed)
            if not exponentTooLarge:
                if initialHash == -1:
                    initialHash = toHash % self.rgba_rgba_hashTableIndexSize
                    if initialHash == 0 or initialHash == 1:
                        initialHash = (toHash + 2) % self.rgba_rgba_hashTableIndexSize

                exponentiated = initialHash ** iterator
                if exponentiated > 100000000000000:
                    exponentTooLarge = True
                else:
                    maxExponent = iterator
                    hashed = exponentiated % self.rgba_rgba_hashTableIndexSize

            if exponentTooLarge:
                hashed = ((initialHash ** maxExponent) + linearIncrement) % self.rgba_rgba_hashTableIndexSize
                linearIncrement += 1

            byteLocationOfHash = int(self.rgba_rgba_hashTableByteLocation + hashed * 15)
            self.rgba_rgba_file.seek(byteLocationOfHash)
            firstByte = int(self.bytesToString(self.rgba_rgba_file.read(1)))
            isUnusedIndex = True if firstByte == 0 else False
            isUnexpectedValue = True if not isUnusedIndex and firstByte != 1 else False
            if isUnexpectedValue:
                print("non-valid first byte of hash table index found")
                sys.exit()
            if isUnusedIndex:
                print("unexpected unused index found in hash table")
            else:
                r_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
                g_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
                b_rgba = self.byteToInt(self.rgba_rgba_file.read(1))
                a_rgba = self.byteToInt(self.rgba_rgba_file.read(1))

                rgba = r_rgba * 16777216 + g_rgba * 65536 + b_rgba * 256 + a_rgba
                isMatchingRGBA = rgba == toHash

                if iterator > 100000:
                    print("100,000 Hash iterations detected")
                    sys.exit()
                if isMatchingRGBA:
                    return hashed

            iterator += 1

    def close(self):
        self.rgba_rgba_file.close()

    #  splits a String into an array of characters
    def strToCharArray(self, text):
        return [char for char in text]

    def bytesToString(self, b):
        return str(b)[2:len(str(b)) - 1]

    def byteToInt(self, b):
        return int.from_bytes(b, byteorder="big", signed=False)


'''
ord('a') prints 97 | chr(97) prints a

in following error below, was only reading bytes 0-11, how did it know about a byte in position 656?
would it be inefficient for huge files?

E:\conda\envs\colors\python.exe E:/seampy/colors/main.py
Traceback (most recent call last):
  File "E:/seampy/colors/main.py", line 15, in <module>
    rf = RGBA_File_Parser("E:\\seam\\hash_table_output\\crispy_pear24579.txt")
  File "E:\seampy\colors\RGBA_File_Parser.py", line 17, in __init__
    self.hashTableSize = int(self.f.read(11))
  File "E:\conda\envs\colors\lib\encodings\cp1252.py", line 23, in decode
    return codecs.charmap_decode(input,self.errors,decoding_table)[0]
UnicodeDecodeError: 'charmap' codec can't decode byte 0x9d in position 656: character maps to <undefined>

Process finished with exit code 1

'''
