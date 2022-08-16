package com.example.sensordata;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button start;
    Button stop;

    DataRecorder mService;
    boolean mBound = false;

    private String m_text = "";

    public void setFilenameAndSave(Intent intent){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            m_text = input.getText().toString();

            try {
                mService.saveZipFile(m_text);
            } catch (IOException e) {
                e.printStackTrace();
            }
            stopService(intent);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            DataRecorder.LocalBinder binder = (DataRecorder.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        if (serviceIsRunning(DataRecorder.class)){
            start.setEnabled(false);
            stop.setEnabled(true);
        }
    }

    private boolean serviceIsRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        //noinspection deprecation
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void onClick(View view){
        view.setEnabled(!view.isEnabled());
        Intent intent = new Intent(this, DataRecorder.class);
        if (view == start){
            stop.setEnabled(true);
            checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 101);
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 101);
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, 102);
            checkPermission(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS, 103);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startForegroundService(intent);
        }
        else {
            start.setEnabled(true);
            setFilenameAndSave(intent);
            unbindService(connection);
            mBound = false;
        }
    }

    @Override
    public void onDestroy(){
        Intent intent = new Intent(this, DataRecorder.class);
        if(serviceIsRunning(DataRecorder.class)) {
            try {
                mService.saveZipFile("interrupted");
            } catch (IOException e) {
                e.printStackTrace();
            }
            unbindService(connection);
            stopService(intent);
        }
        super.onDestroy();
    }
    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
    }
}
