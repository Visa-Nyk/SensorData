package com.example.sensordata;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

public class DataRecorder extends Service {
    private final int FREQUENCY = 20;
    SensorManager sm;
    Sensor accelerometer;
    Sensor rotation;


    private PrintWriter accOut;
    private PrintWriter rotOut;

    File path;
    File rot;
    File acc;

    public void setFileNames(String prefix) {
        prefix = prefix.replaceAll("\\W+", "");
        String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
        rot.renameTo(new File(path, prefix+"_acc_"+time+".csv"));
        acc.renameTo(new File(path, prefix+"_rot_"+time+".csv"));
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        DataRecorder getService() {
            return DataRecorder.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    };

    public void stopRecording() throws IOException {
    }
    public void startSensors(){
        String root = Environment.getExternalStorageDirectory().toString();
        path = new File(root +"/sensordata");
        path.mkdirs();
        try{
            rot = new File(path, "temp_rot.csv");
            rot.delete();
            rot.createNewFile();
            FileWriter fw = new FileWriter(rot, true);
            BufferedWriter bw = new BufferedWriter(fw);
            rotOut = new PrintWriter(bw);

            acc = new File(path, "temp_acc.csv");
            acc.delete();
            acc.createNewFile();
            FileWriter fw1 = new FileWriter(acc, true);
            BufferedWriter bw1 = new BufferedWriter(fw1);
            accOut = new PrintWriter(bw1);
        }
        catch (IOException e) {
            Log.e("", e.toString());
        }

        rotOut.println("time, rot_x, rot_y, rot_z, rot_c");
        accOut.println("time, acc_x, acc_y, acc_z");

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sm.registerListener(sensorListener, accelerometer, 1000000 / FREQUENCY);
        sm.registerListener(sensorListener, rotation, 1000000 / FREQUENCY);
    }


    /**
     * Listener that handles sensor events
     */
    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accOut.write(String.valueOf(event.timestamp/1000000L));
                for(float val:event.values)
                    accOut.write(","+val);
                accOut.println();
            }
            else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                rotOut.write(String.valueOf(event.timestamp/1000000L));
                for(float val:event.values)
                    rotOut.write(","+val);
                rotOut.println();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSensors();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            stopRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}