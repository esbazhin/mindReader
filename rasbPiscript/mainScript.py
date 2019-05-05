import os
import sys
import serial
import brain as b
from bluetooth import *
import importlib


def change_script(sock):
    data = sock.recv(1024)
    size = int(str(data, 'utf-8'))
    data = sock.recv(size)
    text = str(data, 'utf-8')
    file = open("brain.py", 'w')
    file.write(text)
    file.close()
    importlib.reload(b)


while True:
    ser = serial.Serial()
    ser.baudrate = 57600
    ser.port = "/dev/ttyUSB0"

    if not ser.isOpen():
        try:
            ser.open()
        except serial.SerialException:
            print("error wrong com")

    server_sock = BluetoothSocket(RFCOMM)
    server_sock.bind(("", PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

    advertise_service(server_sock, "SampleServer",
                      service_id=uuid,
                      service_classes=[uuid, SERIAL_PORT_CLASS],
                      profiles=[SERIAL_PORT_PROFILE],
                      #                   protocols = [ OBEX_UUID ]
                      )

    print("Waiting for connection on RFCOMM channel %d" % port)

    client_sock, client_info = server_sock.accept()
    print("Accepted connection from ", client_info)

    try:
        while True:
            data = client_sock.recv(1024)
            if len(data) == 0: break
            x = str(data, 'utf-8')
            print("received [%s]" % x)
            if x == "start": break
            if x == "change script":
                change_script(client_sock)
        i = 0
        while True:
            #
            # ch1 = random.randint(0, 100)
            # ch2 = random.randint(0, 100)
            ########################

            msg = b.getMessage(ser)
            # msg = str(i) +  "\n"
            # i += 1

            client_sock.send(msg)
            print(msg)
    except IOError:
        pass

    print("disconnected")

    client_sock.close()
    server_sock.close()
    ser.close()
    print("all done")
