package com.example.sensordata;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

public class DataRecorder extends Service {
    private final int FREQUENCY = 20;
    SensorManager sm;
    Sensor accelerometer;
    Sensor rotation;
    LinkedHashMap<Long, float[]> accelerometerData = new LinkedHashMap<>();
    LinkedHashMap<Long, float[]> rotationData = new LinkedHashMap<>();

    public String makeCSV(LinkedHashMap<Long, float[]> data, String[] header){
        String csv = String.join(", ", header) + "\n";
        for (long ts:data.keySet()) {
            csv += ts;
            for(float val:data.get(ts)){
                csv += ", " + val;
            }
            csv += "\n";
        }
        return csv;
    }

    public void writeFile(LinkedHashMap data, String[] header, String fileName, File path) throws IOException {
        File file = new File(path, fileName);
        FileOutputStream stream = new FileOutputStream(file);
        try {
            String csv = makeCSV(data, header);
            stream.write(csv.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stream.close();
        }
    }

    public void stopRecording() throws IOException {
        sm.unregisterListener(sensorListener);
        Context context = new ContextWrapper(this.getApplicationContext());

        File path = context.getFilesDir();
        writeFile(accelerometerData, new String[]{"time", "acc_x", "acc_y", "acc_z"}, "acceleration.txt", path);
        writeFile(rotationData, new String[]{"time", "rot_x", "rot_y", "rot_z", "rot_constant"}, "rotation.txt", path);
    }
    public void startSensors(){
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
            //if (event.sensor.getType() == Sensor.) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                Log.i("", String.valueOf(event.values[0]));
                accelerometerData.put(event.timestamp/1000000L, new float[]{event.values[0],event.values[1],event.values[2]});
            }
            else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                rotationData.put(event.timestamp/1000000L, new float[]{event.values[0],event.values[1],event.values[2], event.values[3]});
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}