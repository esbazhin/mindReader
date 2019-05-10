from scipy.signal import butter, lfilter

class Emotion(object):
    name = ""
    from_strength = 0
    to_strength = 0
    from_color = 0
    to_color = 0

    def __init__(self, n, fs, ts, fc, tc):
        self.name = n
        self.from_strength = fs
        self.to_strength = ts
        self.from_color = fc
        self.to_color = tc

def butter_bandpass(lowcut, highcut, fs, order=5):
    nyq = 0.5 * fs
    low = lowcut / nyq
    high = highcut / nyq
    b, a = butter(order, [low, high], btype='band')
    return b, a


def butter_bandpass_filter(data, lowcut, highcut, fs=256, order=5):
    b, a = butter_bandpass(lowcut, highcut, fs, order=order)
    y = lfilter(b, a, data)
    return y


def readFromEEG(ser):
    n = 26
    strArr = []
    pArr = []

    for i in range(0, n):
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
        strArr.append(ch1)
        # 7 and 8
        ch2 = conv2sig(ser.read(), ser.read())
        pArr.append(ch2)

    out1 = butter_bandpass_filter(strArr, 8, 12)
    out2 = butter_bandpass_filter(pArr, 12, 30)

    a2 = (abs(out1.min()) + abs(out1.max()))

    b2 = (abs(out2.min()) + abs(out2.max()))

    return a2, b2


def getEmotion(ch1, ch2):
    emotionsList = []
    emotionsList.append(Emotion("unplugged", 0, 2, 0, 20))
    emotionsList.append(Emotion("only first ch", 3, 20, 0, 20))
    emotionsList.append(Emotion("only secong ch", 0, 2, 100, 700))
    emotionsList.append(Emotion("plugged in", 3, 20, 100, 700))
    for emt in emotionsList:
        isRight = False
        if emt.from_strength <= ch1 <= emt.to_strength:
            if emt.from_color <= ch2 <= emt.to_color:
                isRight = True

        if isRight:
            return emt.name

    return "not detected"


def getMessage(ser):
    # read from eeg
    ch1, ch2 = readFromEEG(ser)
    # get emotion
    emtn = getEmotion(ch1, ch2)

    return str(ch1) + "|" + str(ch2) + "|" + emtn + "\n"