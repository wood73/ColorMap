import sys
import io


class Huge_Hash_Parser:
    def __init__(self, path):
        self.rgba_rgba_file = io.open(path, 'rb')
        self.possible_colors = 256
        self.rgba_rgba_hashTableSize = 21810380 * 14
        self.rgba_rgba_hashTableIndexSize = 21810380
        self.rgba_rgba_hashTableByteLocation = 0

    def get_3_overlapping_colors(self, red, green, blue):

        closest_red = -1
        closest_green = -1
        closest_blue = -1
        brightness_multiplier = -1

        #  convert to base 256
        b256 = red * 65536 + green * 256 + blue
        hashValue = self.rgba_rgba_hash(b256)
        byteLocationOfHash = int(self.rgba_rgba_hashTableByteLocation + hashValue * 14)

        self.rgba_rgba_file.seek(byteLocationOfHash + 1)
        r_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        g_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        b_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        r2_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        g2_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        b2_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        a2_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        r = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        ra = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        g = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        ga = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        b = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
        ba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)

        print("Inputted RGB: " + str(red) + ", " + str(green) + ", " + str(blue))
        print("Hash Table > RGB found: " + str(r_rgba) + ", " + str(g_rgba) + ", " + str(b_rgba))
        print("Hash Table > Closest RGB match from 3 overlapping colors: " + str(r2_rgba) + ", " + str(g2_rgba) +
              ", " + str(b2_rgba))
        print("Hash Table > 3 overlapping colors: [" + str(r) + ", 0, 0, " + str(ra) + "], [0, " + str(g) + ", 0, " +
              str(ga) + "], [0, 0, " + str(b) + ", " + str(ba) + "]")

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

            byteLocationOfHash = int(self.rgba_rgba_hashTableByteLocation + hashed * 14)
            self.rgba_rgba_file.seek(byteLocationOfHash)
            firstByte = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
            isUnusedIndex = True if firstByte == 0 else False
            isUnexpectedValue = True if not isUnusedIndex and firstByte != 1 else False
            if isUnexpectedValue:
                print("non-valid first byte of hash table index found")
                sys.exit()
            if isUnusedIndex:
                print("unexpected unused index found in hash table")
            else:
                r_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
                g_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)
                b_rgba = int.from_bytes(self.rgba_rgba_file.read(1), byteorder="big", signed=False)

                rgba = r_rgba * 65536 + g_rgba * 256 + b_rgba
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
