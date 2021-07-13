import sys
import io


class RGBA_File_Parser:
    def __init__(self, path):
        self.rgba_rgba_file = io.open(path, 'rb')
        self.possible_colors = self.byteToInt(self.rgba_rgba_file.read(1))
        self.rgba_rgba_hashTableSize = int(self.bytesToString(self.rgba_rgba_file.read(10)))
        self.bytes_per_index = 15
        self.rgba_rgba_hashTableIndexSize = self.rgba_rgba_hashTableSize / self.bytes_per_index
        self.rgba_rgba_hashTableByteLocation = 11

    def get_3_overlapping_colors(self, red, green, blue, *args):

        useAlpha = True if len(args) == 1 else False

        closest_red = -1
        closest_green = -1
        closest_blue = -1
        closest_alpha = -1

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

        if useAlpha:
            alpha = args[0]
            alpha_remainder = alpha % color_increments
            if alpha >= 255 - color_increments / 2:
                closest_alpha = 255
            else:
                if alpha_remainder == 0:
                    closest_alpha = alpha
                elif alpha_remainder <= color_increments / 2:
                    closest_alpha = round(alpha - alpha_remainder)
                else:
                    closest_alpha = round(alpha + (color_increments - alpha_remainder))
        else:
            closest_alpha = 255

        #  convert to base 256
        b256 = closest_red * 16777216 + closest_green * 65536 + closest_blue * 256 + closest_alpha
        hashValue = self.rgba_rgba_hash(b256)
        byteLocationOfHash = int(self.rgba_rgba_hashTableByteLocation + hashValue * self.bytes_per_index)

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

        print("Closest possibleValues RGBA: " + str(r_rgba) + ", " + str(g_rgba) + ", " + str(b_rgba) +
              ", " + str(a_rgba))
        print("Closest mapped RGBA: " + str(r2_rgba) + ", " + str(g2_rgba) + ", " + str(b2_rgba) + ", " + str(a2_rgba))

        return [r, ra, g, ga, b, ba]

    def rgba_rgba_hash(self, toHash):
        foundHash = False
        initialHash = -1
        hashed = -1
        exponentTooLarge = False
        maxExponent = -1
        linearIncrement = 1
        newBase = -1

        iterator = 1
        while not foundHash:
            # print(hashed)
            if not exponentTooLarge:
                if initialHash == -1:
                    initialHash = toHash % self.rgba_rgba_hashTableIndexSize
                    newBase = initialHash % 10000
                    if newBase < 2:
                        newBase += 2

                exponentiated = initialHash + (newBase ** iterator)
                if exponentiated > 1000000000000:
                    exponentTooLarge = True
                else:
                    maxExponent = iterator
                    hashed = exponentiated % self.rgba_rgba_hashTableIndexSize

            if exponentTooLarge:
                hashed = (initialHash + (newBase ** maxExponent) + linearIncrement) % self.rgba_rgba_hashTableIndexSize
                linearIncrement += 1

            byteLocationOfHash = int(self.rgba_rgba_hashTableByteLocation + hashed * self.bytes_per_index)
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

    def create_ragaba_file(self, path):
        #  file will separate each sub-value by a comma, set of 2 values by [], and ra,ga,ba separated by |

        f = open(path, 'w')

        ra = []
        ga = []
        ba = []
        iterator = 0
        while iterator < self.rgba_rgba_hashTableIndexSize:
            self.rgba_rgba_file.seek(self.rgba_rgba_hashTableByteLocation + iterator * self.bytes_per_index)
            index_state = int(self.bytesToString(self.rgba_rgba_file.read(1)))
            if index_state == 1:
                self.rgba_rgba_file.seek(self.rgba_rgba_hashTableByteLocation + iterator * self.bytes_per_index +
                                         (self.bytes_per_index - 6))
                red = self.byteToInt(self.rgba_rgba_file.read(1))
                r_alpha = self.byteToInt(self.rgba_rgba_file.read(1))
                green = self.byteToInt(self.rgba_rgba_file.read(1))
                g_alpha = self.byteToInt(self.rgba_rgba_file.read(1))
                blue = self.byteToInt(self.rgba_rgba_file.read(1))
                b_alpha = self.byteToInt(self.rgba_rgba_file.read(1))

                ra_holder = [red, r_alpha]
                ga_holder = [green, g_alpha]
                ba_holder = [blue, b_alpha]
                if ra_holder not in ra:
                    ra.append(ra_holder)

                if ga_holder not in ga:
                    ga.append(ga_holder)

                if ba_holder not in ba:
                    ba.append(ba_holder)

            iterator += 1

        for x in ra:
            f.write("[" + str(x[0]) + "," + str(x[1]) + "]")
        f.write(":")
        for x in ga:
            f.write("[" + str(x[0]) + "," + str(x[1]) + "]")
        f.write(":")
        for x in ba:
            f.write("[" + str(x[0]) + "," + str(x[1]) + "]")

        f.close()

    def get_ra(self, path):
        f = open(path, 'r')
        data = f.readline().split(":")[0]
        ra_data = data.split("]")
        ra_data.pop()
        ra = []
        iterator = 0

        for x in ra_data:
            x = x.replace("[", "")
            x = x.split(",")
            ra.append([int(x[0]), int(x[1])])

        f.close()
        return ra

    def get_ga(self, path):
        f = open(path, 'r')
        data = f.readline().split(":")[1]
        ga_data = data.split("]")
        ga_data.pop()
        ga = []

        for x in ga_data:
            x = x.replace("[", "")
            x = x.split(",")
            ga.append([int(x[0]), int(x[1])])

        f.close()
        return ga

    def get_ba(self, path):
        f = open(path, 'r')
        data = f.readline().split(":")[2]
        ba_data = data.split("]")
        ba_data.pop()
        ba = []

        for x in ba_data:
            x = x.replace("[", "")
            x = x.split(",")
            ba.append([int(x[0]), int(x[1])])

        f.close()
        return ba