package com.example.sensordata;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button start;
    Button stop;

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
        if (view == start){
            stop.setEnabled(true);
            startService(new Intent(this, DataRecorder.class));
        }
        else {
            start.setEnabled(true);
            stopService(new Intent(this, DataRecorder.class));
        }
    }
}
