package com.sarancha1337.mindreader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.res.Resources;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothSocketHandler {
    private static BluetoothSocket socket;


    private static volatile boolean stopWorker;

    private static Thread workerThread;
    private static String currentMessage = "null";

    private static boolean isReady = true;

    public static synchronized BluetoothSocket getSocket() {
        return socket;
    }

    static synchronized void setSocket(BluetoothSocket socket) {
        BluetoothSocketHandler.socket = socket;
    }

    static void sendMessage(String msg) {
        try {
            OutputStream mmOutputStream = socket.getOutputStream();
            mmOutputStream.write(msg.getBytes());

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    static void beginListenForData() {
        stopWorker = false;
        // создаем новый фоновый процесс
        workerThread = new Thread(new Runnable() {
            public void run() {
                // посылаем устройству сигнал о начале считывания данных
                sendMessage("start");
                //запускаем цикл считывания
                while (!stopWorker)
                {
                    try {
                        // если вдруг пользователь выключил блютуз
                        if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
                        {
                            currentMessage = "error";
                            return;
                        }
                        // получаем входной поток нашего блютуз сокета
                        InputStream mmInputStream = socket.getInputStream();
                        // узнаем количество доступных данных
                        int bytesAvailable = mmInputStream.available();
                        // если получили какие то данные
                        if (bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            // считываем данные
                            int r = mmInputStream.read(packetBytes);
                            // пока не считаем все доступные данные
                            while (r < bytesAvailable)
                            {
                                packetBytes[r] = (byte) mmInputStream.read();
                                r++;
                            }
                            // конвертируем в сообщение
                            String data = new String(packetBytes, "US-ASCII");
                            // если вдруг получили больше чем одно сообщение за раз, выбираем только последнее
                            String[] line = data.split("\n");
                            if (line.length > 1)
                                data = line[line.length - 1];

                            // блокируем ресурс, синхронизируем потоки
                            synchronized (currentMessage)
                            {
                                // удаляем разделительный символ перевода каретки
                                data = data.substring(0, data.length() - 1);
                                // обновляем текущее сообщение
                                currentMessage = data;
                            }
                        }
                    } catch (Exception ex)
                    {
                        // если произошел какой либо сбой останавливаем считку
                        stopWorker = true;
                    }
                }
            }
        });

        // запускаем считку
        workerThread.start();
    }

    static String getCurrentMessage() {
        synchronized (currentMessage) {
            return currentMessage;
        }
    }

    static void Stop() {
        stopWorker = true;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static boolean changeScript(Activity activity, int id) {
        isReady = false;
        // получаем файл
        String content = getStringFromRawFile(activity, id);
        // сообщаем устройству о начале перепрошивки
        sendMessage("change script");
        // ставим на небольшую паузу что бы устройство успело считать предыдущее сообщение
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // отправляем размер файла
        sendMessage(String.valueOf(content.length()));
        // ставим на небольшую паузу что бы устройство успело считать предыдущее сообщение
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // отправляем сам файл
        sendMessage(content);

        // ожидаем ответа устройства
        InputStream mmInputStream = null;
        try {
            mmInputStream = socket.getInputStream();

            // ждем
            while (true)
            {
                // получаем сообщение
                int bytesAvailable = mmInputStream.available();
                if (bytesAvailable > 0)
                {
                    byte[] packetBytes = new byte[bytesAvailable];
                    int r = mmInputStream.read(packetBytes);
                    while (r < bytesAvailable) {
                        packetBytes[r] = (byte) mmInputStream.read();
                        r++;
                    }
                    String data = new String(packetBytes, "US-ASCII");
                    // устанавливаем статус об успешности перепрошивки
                    isReady = data.equals("ready\n");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // сообщаем пользователю результат перепрошивки
        if (isReady)
            Toast.makeText(activity.getApplicationContext(), "Устройство перепрошито успешно!", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(activity.getApplicationContext(), "Устройство не перепрошито!", Toast.LENGTH_SHORT).show();

        return isReady;
    }


    private static String getStringFromRawFile(Activity activity, int id) {
        Resources r = activity.getResources();
        InputStream is = r.openRawResource(id);
        String myText = null;
        try {
            myText = convertStreamToString(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myText;
    }

    private static String convertStreamToString(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = is.read();
        while (i != -1) {
            baos.write(i);
            i = is.read();
        }
        return baos.toString();
    }
}
