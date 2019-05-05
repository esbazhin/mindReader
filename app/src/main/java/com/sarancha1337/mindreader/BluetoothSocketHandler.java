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

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    static void beginListenForData() {
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                sendMessage("start");
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
                        {
                            currentMessage = "error";
                            return;
                        }
                        InputStream mmInputStream = socket.getInputStream();
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            int r = mmInputStream.read(packetBytes);
                            while (r < bytesAvailable) {
                                packetBytes[r] = (byte) mmInputStream.read();
                                r++;
                            }
                            String data = new String(packetBytes, "US-ASCII");
                            String[] line = data.split("\n");
                            if (line.length > 1)
                                data = line[line.length - 1];
//                            for(int i=0;i<bytesAvailable;i++)
//                            {
//                                byte b = packetBytes[i];
//                                if(b == delimiter)
//                                {
//                                    byte[] encodedBytes = new byte[readBufferPosition];
//                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
//                                    final String data = new String(encodedBytes, "US-ASCII");
//                                    readBufferPosition = 0;
//
//                                    System.out.println(data);
//
                            synchronized (currentMessage) {
                                //currentMessage = line[line.length - 2];
                                data = data.substring(0, data.length() - 1);
                                currentMessage = data;
                            }
//                                }
//                                else
//                                {
//                                     readBuffer[readBufferPosition++] = b;
//                                }
//                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

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
        //ProgressDialog pd = new ProgressDialog(activity);
        //pd.setTitle("Пожалуйста, подождите");
        //pd.setMessage("Ожидаем готовность устройства");
        //pd.show();

        isReady = false;
        String content = getStringFromRawFile(activity, id);
        sendMessage("change script");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendMessage(String.valueOf(content.length()));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendMessage(content);

        InputStream mmInputStream = null;
        try {
            mmInputStream = socket.getInputStream();

            while (true) {
                int bytesAvailable = mmInputStream.available();
                if (bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    int r = mmInputStream.read(packetBytes);
                    while (r < bytesAvailable) {
                        packetBytes[r] = (byte) mmInputStream.read();
                        r++;
                    }
                    String data = new String(packetBytes, "US-ASCII");
                    isReady = data.equals("ready\n");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
