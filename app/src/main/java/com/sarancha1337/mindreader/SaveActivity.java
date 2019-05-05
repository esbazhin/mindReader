package com.sarancha1337.mindreader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SaveActivity extends AppCompatActivity {

    private String text;
    private File file;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try
        {
            Bundle extras = getIntent().getExtras();
            text = extras.getString("text");

        }
        catch(NullPointerException e)
        {
            finish();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button button = findViewById(R.id.saveAndExitButton);
        View.OnClickListener end = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText et = findViewById(R.id.editTextFile);
                String name = et.getText().toString();
                saveText(text, name + ".emtn");

                CheckBox box = findViewById(R.id.checkBox);
                if(box.isChecked())
                {
                    EditText etm = findViewById(R.id.editTextMail);
                    String email = etm.getText().toString();
                    sendMail(email);
                }

                finish();
            }
        };
        button.setOnClickListener(end);
    }

    // сохранение файла
    void saveText(String text, String fileName)
    {
        // формируем объект File, который содержит путь к файлу
        file = new File(getExternalFilesDir(null), fileName);

        try {
            // открываем поток для записи
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            // пишем данные
            bw.write(text);
            // закрываем поток
            bw.close();
            Toast.makeText(this, "Файл сохранен", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMail(String email)
    {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        emailIntent.setType("plain/text");


        // Кому
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, email);

        // Зачем
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MindReader App");

        // О чём
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "File with recent session of work.");
        // С чем
        emailIntent.putExtra(
                android.content.Intent.EXTRA_STREAM,
                Uri.fromFile(file));

        emailIntent.setType("*/*");

        // Поехали!
        startActivity(Intent.createChooser(emailIntent, "Отправка письма..."));

        Toast.makeText(this, "Письмо отправлено", Toast.LENGTH_SHORT).show();
    }

}
