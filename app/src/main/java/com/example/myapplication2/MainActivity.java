package com.example.myapplication2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    EditText  _txtPassword, _txtUser, _txtEmail;
    Button _btnCreate;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText _txtPassword  = findViewById(R.id.txtPassword);
        final EditText _txtUser  = findViewById(R.id.txtUser);
        final EditText _txtEmail  = findViewById(R.id.txtEmail);
        Button _btnCreate = findViewById(R.id.btnCreate);
        _btnCreate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (_txtEmail.getText().toString().trim().matches(emailPattern)) {
                    Intent intent = new Intent(MainActivity.this, next.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Invalid email address", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}