package com.example.myapplication2;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static android.os.Environment.DIRECTORY_DOWNLOADS;


public class next extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_AUDIO_REQUEST = 3;
    Button upLoadFile, selectFile, downloadFile, convertFile;
    TextView fileName;
    TextView convert;
    TextView down;
    StorageReference storageRef;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    StorageReference ref;
    private Uri filePath;
    String path = null;
    boolean click = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);

        storageReference = FirebaseStorage.getInstance().getReference();

        selectFile = findViewById(R.id.selectFile);
        upLoadFile = findViewById(R.id.uploadFile);
        downloadFile = findViewById(R.id.downloadFile);
        convertFile = findViewById(R.id.convertFile);

        fileName = findViewById(R.id.textView4);
        fileName.setText("No file choosen!");

        selectFile.setOnClickListener(this);
        upLoadFile.setOnClickListener(this);
        downloadFile.setOnClickListener(this);
        convertFile.setOnClickListener(this);

        down = findViewById(R.id.textView6);
        down.setText("Файл недоступен для скачивания");

        convert = findViewById(R.id.textConvert);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            fileName.setText(filePath.getEncodedPath());
        }
    }

    private void selectAudioFile() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select audio file"), PICK_AUDIO_REQUEST);
    }

    private void uploadAudioFile() {

        if (filePath != null) {
            storageRef = storageReference.child("music.mp3");
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            storageRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(),"Файл загружен!", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(),"Ошибка загрузки", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage(((int)progress) + "% uploaded");
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(),"Файл не выбран!", Toast.LENGTH_LONG).show();
        }

    }

    public void download()
    {
        if (filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            storageReference = firebaseStorage.getInstance().getReference();
            ref = storageReference.child(path);
            ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {

                    String url = uri.toString();

                    downloadFile(next.this, path, DIRECTORY_DOWNLOADS, url);
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Файл скачен!", Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), "Ошибка!", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Файл не выбран!", Toast.LENGTH_LONG).show();
        }
    }

    public void downloadFile(Context context, String fileName, String destinationDirectory, String url){

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, destinationDirectory, fileName);
        downloadManager.enqueue(request);
    }
    @Override
    public void onClick(View v) {
        if (v == selectFile) {
            selectAudioFile();
        } else if (v == upLoadFile) {
            uploadAudioFile();
        } else if(v == convertFile && click == true){
            click = false;
            convert.setText("Ожидайте появления названия файла. Далее он будет готов к скачиванию");
            RequestQueue queue = Volley.newRequestQueue(this);
            String urlServer = "https://d94bc123ddee.ngrok.io/api/convert?path=music.mp3";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, urlServer, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                path = response.getString("path");
                                down.setText(path);
                            }
                            catch (Exception ex) {
                                Log.i("MSG", ex.getMessage(), ex);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i("MSG", error.getMessage(), error);
                        }
                    });

            jsonObjectRequest.setRetryPolicy(new RetryPolicy() {
                @Override
                public int getCurrentTimeout() { return 50000; }
                @Override
                public int getCurrentRetryCount() { return 50000;  }
                @Override
                public void retry(VolleyError error) throws VolleyError { }
            });
            queue.add(jsonObjectRequest);

        } else if(v == downloadFile && path != null){
            download();
            click = true;
        }
    }
}
