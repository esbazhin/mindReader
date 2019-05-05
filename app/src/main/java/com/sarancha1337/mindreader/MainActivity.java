package com.sarancha1337.mindreader;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final int CHOOSE_VIDEO_CODE = 0;
    private final int CHOOSE_VIDEO_CODE_FILE = 1;
    private final int REQUEST_ENABLE_BT = 2;

    private boolean videoSet = false;
    private boolean deviceConnected = false;
    private boolean fileChosen = false;

    private Uri videoUri;
    private String filePath;
    private long duration;
    private BluetoothDevice device;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setUpButtons();
    }

    private void setUpBluetooth()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void setUpButtons()
    {
        Button chooseVideoButton = findViewById(R.id.chooseVideoButton);
        View.OnClickListener choose = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseVideo = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                chooseVideo.setType("video/*");
                chooseVideo.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(chooseVideo, CHOOSE_VIDEO_CODE);
            }
        };
        chooseVideoButton.setOnClickListener(choose);

        Button connectButton = findViewById(R.id.connectButton);
        View.OnClickListener connect = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpBluetooth();
                connectToDevice();
            }
        };
        connectButton.setOnClickListener(connect);

        setUpChooseFileBtn();

        setUpStartBtn();

        setUpChooseScriptBtn();
    }

    private  void setUpChooseFileBtn()
    {
        final Button chooseFileButton = findViewById(R.id.chooseFileButton);
        View.OnClickListener chooseF = new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {

                File file = (getExternalFilesDir(null));
                final File[] listFiles = file.listFiles(new MyFileNameFilter(".emtn"));
                if(listFiles.length == 0)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Ошибка!")
                            .setMessage("Нет файлов!")
                            .setCancelable(false)
                            .setNegativeButton("ОК",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                    chooseFileButton.setEnabled(false);
                    return;
                }

                final String[] names = new String[listFiles.length];
                for(int i = 0; i < listFiles.length; ++i)
                    names[i] = listFiles[i].getName();

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setSingleChoiceItems(names, 0, null)
                        .setPositiveButton("Выбрать",
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int whichButton)
                                    {
                                        dialog.dismiss();
                                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                                         filePath = listFiles[selectedPosition].getAbsolutePath();
                                        TextView txt = findViewById(R.id.fileText);

                                        txt.setText(names[selectedPosition]);

                                        checkVideoFromFile(filePath);

                                        fileChosen = true;
                                        Switch sw = findViewById(R.id.switch1);

                                        sw.setVisibility(View.VISIBLE);
                                        sw.setChecked(true);
                                    }
                                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        };
        chooseFileButton.setOnClickListener(chooseF);
    }

    private void setUpChooseScriptBtn()
    {
        final Button chooseScriptButton = findViewById(R.id.chooseScriptButton);
        View.OnClickListener chooseS = new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {

                final Field[] listFiles = R.raw.class.getFields();

                if(listFiles.length == 0)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Ошибка!")
                            .setMessage("Нет файлов!")
                            .setCancelable(false)
                            .setNegativeButton("ОК",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                    chooseScriptButton.setEnabled(false);
                    return;
                }

                final String[] names = new String[listFiles.length];
                for(int i = 0; i < listFiles.length; ++i)
                    names[i] = listFiles[i].getName();

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setSingleChoiceItems(names, 0, null)
                        .setPositiveButton("Выбрать",
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int whichButton)
                                    {
                                        dialog.dismiss();
                                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();

                                        try {
                                            if(BluetoothSocketHandler.changeScript(MainActivity.this, listFiles[selectedPosition].getInt(listFiles[selectedPosition]))) {
                                                TextView txt = findViewById(R.id.scriptText);
                                                txt.setText(names[selectedPosition]);
                                            }
                                        } catch (IllegalAccessException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        };
        chooseScriptButton.setOnClickListener(chooseS);
    }

    private void setUpStartBtn()
    {
        Button startButton = findViewById(R.id.startButton);

        View.OnClickListener start = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //chooseFileButton.setEnabled(true);
                Intent intent = new Intent(MainActivity.this, WorkActivity.class);
                Switch sw = findViewById(R.id.switch1);
                if(fileChosen && sw.isChecked())
                {
                    if(deviceConnected)
                        BluetoothSocketHandler.Stop();

                    intent.putExtra("isFile", true);
                    intent.putExtra("path", filePath);
                    startActivity(intent);
                    recreate();
                }
                else
                if(videoSet & deviceConnected)
                {
                    intent.putExtra("isFile", false);
                    intent.putExtra("video", videoUri.toString());
                    intent.putExtra("duration", duration);
                    startActivity(intent);
                    recreate();
                    //txt.setText("new session");
                }
                else
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Ошибка!")
                            .setMessage("Выбранно не всё!")
                            .setCancelable(false)
                            .setNegativeButton("ОК",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

            }
        };
        startButton.setOnClickListener(start);

    }

    private void connectToDevice()
    {
        final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());

        if (pairedDevices.size() == 0)
            return;

        final String[] names = new String[pairedDevices.size()];
        int i = 0;
        for(BluetoothDevice x: pairedDevices)
        {
            names[i] = x.getName();
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setSingleChoiceItems(names, 0, null)
                .setPositiveButton("Выбрать",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int whichButton)
                            {
                                dialog.dismiss();
                                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                                device = pairedDevices.get(selectedPosition);
                                deviceConnected = connect();
                                if(deviceConnected) {
                                    TextView txt = findViewById(R.id.connectedText);
                                    txt.setText("Подключено к " + names[selectedPosition]);
                                    Button connectButton = findViewById(R.id.connectButton);
                                    connectButton.setEnabled(false);
                                    final Button chooseScriptButton = findViewById(R.id.chooseScriptButton);
                                    chooseScriptButton.setEnabled(true);
                                }
                            }
                        });

        AlertDialog alert = builder.create();
        alert.show();


    }

    private boolean connect()
    {
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            BluetoothSocket mmSocket = device.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
                BluetoothSocketHandler.setSocket(mmSocket);
                BluetoothSocketHandler.sendMessage("check connection");
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this.getApplicationContext(), "Произошла ошибка при подключении, попробуйте еще раз!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode)
        {
            case CHOOSE_VIDEO_CODE:
            {
                if (resultCode == RESULT_OK)
                {
                    Uri uri = data.getData();
                    final TextView txt = findViewById(R.id.videoText);

                    final Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
                    final int nameIndex;
                    if (returnCursor != null) {
                        nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        returnCursor.moveToFirst();
                    }
                    else
                    {
                        txt.setText("Error!");
                        return;
                    }


                    if(checkUri(uri))
                    {
                        txt.setText(returnCursor.getString(nameIndex));
                        videoUri = uri;
                        videoSet = true;
                    }
                    else
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Ошибка!")
                                .setMessage("Нельзя выбрать данный файл!")
                                .setCancelable(false)
                                .setNegativeButton("ОК",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
                break;
            }

            case CHOOSE_VIDEO_CODE_FILE:
            {
                if (resultCode == RESULT_OK)
                {
                    Uri uri = data.getData();

                    if(checkUri(uri))
                    {
                       fixFile(uri);
                    }
                    else
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Ошибка!")
                                .setMessage("Нельзя выбрать данный файл!")
                                .setCancelable(false)
                                .setNegativeButton("ОК",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
                break;
            }

            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_CANCELED)
                    finish();
                else
                    connectToDevice();
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    private boolean checkUri(Uri uri)
    {
        try
        {
            MediaMetadataRetriever x = new MediaMetadataRetriever();
            x.setDataSource(this.getApplicationContext(), uri);

            String time = x.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInmillisec = Long.parseLong( time );

            if(timeInmillisec > 0)
            {
                duration = timeInmillisec;
                return true;
            }
        }
        catch(Exception e)
        {
            return false;
        }
        return false;
    }

    private void checkVideoFromFile(String path)
    {
        FileInputStream fstream = null;
        try
        {
            fstream = new FileInputStream(path);
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String strLine = "";

            try
            {
                strLine = br.readLine();
            } catch (IOException e) { e.printStackTrace(); }


        if (strLine != null && !strLine.equals(""))
        {
            Uri x = Uri.parse(strLine);
            if(!checkUri(x))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Ошибка!")
                        .setMessage("Видео не найдено!")
                        .setCancelable(false)
                        .setPositiveButton("Выбрать видео",
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent chooseVideo = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                chooseVideo.setType("video/*");
                                chooseVideo.addCategory(Intent.CATEGORY_OPENABLE);
                                startActivityForResult(chooseVideo, CHOOSE_VIDEO_CODE_FILE);
                            }
                        })
                        .setNegativeButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

    private void fixFile(Uri uri)
    {
        BufferedReader reader;
        StringBuilder text = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            line = reader.readLine();

            while (line != null) {
                text.append(line).append("\n");
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter fileStream = new FileWriter(filePath);
            BufferedWriter out = new BufferedWriter(fileStream);
            out.write(uri.toString()+"\n" + text);
            out.close();
        }catch(Exception e){}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("О программе")
                    .setMessage("MindReader v.1.0 Создана как курсовой проект студента НИУ ВШЭ ФКН ПИ группы БПИ 173 Бажина Егора в 2019 году.")
                    .setCancelable(false)
                    .setNegativeButton("ОК",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        return super.onOptionsItemSelected(item);
    }

}

