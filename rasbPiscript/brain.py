def readFromEEG(ser):
    gotBegin = False

    # seek start
    while not gotBegin:

        # 1
        x = ser.read()
        while int.from_bytes(x, byteorder='big') != 165:
            x = ser.read()
        gotBegin = True

        # 2
        x = ser.read()
        gotBegin = gotBegin and int.from_bytes(x, byteorder='big') == 90

        # 3
        x = ser.read()
        gotBegin = gotBegin and int.from_bytes(x, byteorder='big') == 2

    # 4
    ser.read()

    def conv2sig(xb, yb):
        xi = int.from_bytes(xb, byteorder='big')
        yi = int.from_bytes(yb, byteorder='big')
        a = format(xi, 'b') + format(yi, 'b')
        return int(a, 2)

    # 5 and 6
    ch1 = conv2sig(ser.read(), ser.read())

    # 7 and 8
    ch2 = conv2sig(ser.read(), ser.read())

    return ch1, ch2

def getEmotion(ch1, ch2):
    return "%" + str(ch1) + "+" + str(ch2)

def getMessage(ser):
    # read from eeg
    ch1, ch2 = readFromEEG(ser)
    # get emotion
    emtn = getEmotion(ch1, ch2)

    return str(ch1) + "|" + str(ch2) + "|" + emtn + "\n"
