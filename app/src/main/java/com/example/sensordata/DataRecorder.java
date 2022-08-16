package com.example.sensordata;

import static com.example.sensordata.FileUtils.createTempFile;
import static com.example.sensordata.FileUtils.initializePrintWriter;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataRecorder extends Service {
    private final int FREQUENCY = 100;
    SensorManager sm;
    LocationManager lm;

    private final IBinder binder = new LocalBinder();

    File path;
    File spd;
    private PrintWriter spdOut;

    private HashMap<Integer, SensorContainer> sensors = new HashMap<>();

    public void saveZipFile(String prefix) throws IOException {
        prefix = prefix.replaceAll("\\W+", "");
        String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());

        ArrayList<File> files = new ArrayList<File>();

        for(SensorContainer sc : sensors.values()){
            sc.getOut().close();
            files.add(sc.getFile());
        }
        spdOut.close();
        files.add(spd);

        File z = new File(path, prefix + "_" + time + ".zip");
        z.createNewFile();
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(z));

        for(File file : files){
            zout.putNextEntry(new ZipEntry(file.getName()));
            Files.copy(file.toPath(), zout);
            file.delete();
        }
        zout.close();
    }


    public class LocalBinder extends Binder {
        DataRecorder getService() {
            return DataRecorder.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @SuppressLint("MissingPermission")
    public void restartSensors(){
        lm.removeUpdates(locationListener);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        sm.unregisterListener(sensorListener);
        for( SensorContainer sc: sensors.values()){
            sm.registerListener(sensorListener, sc.getSensor(), 1000000 / FREQUENCY);
        }
    }

    private void initializeFiles() {
        String root = Environment.getExternalStorageDirectory().toString();
        path = new File(root +"/sensordata");
        path.mkdirs();
        try{
            sensors.put(Sensor.TYPE_ACCELEROMETER, new SensorContainer(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), path, "acc.csv", "time,x,y,z"));
            sensors.put(Sensor.TYPE_ROTATION_VECTOR, new SensorContainer(sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), path, "rot.csv", "time,x,y,z,c"));

            spd = createTempFile("spd.csv", path);
            spdOut = initializePrintWriter(spd);
            spdOut.println("time,speed");
        }
        catch (IOException e) {
            Log.e("", e.toString());
        }

    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            spdOut.print(location.getElapsedRealtimeNanos()+","+ location.getSpeed());
            spdOut.println();
        }
    };

    /**
     * Listener that handles sensor events
     */
    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            PrintWriter out = sensors.get(event.sensor.getType()).getOut();

            out.write(String.valueOf(event.timestamp));
            for(float val:event.values)
                out.write(","+String.format("%.5f", val));
            out.println();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(){
        NotificationChannel serviceChannel = new NotificationChannel(
                "vmp","Recording data",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
        final NotificationCompat.Builder notification = new NotificationCompat.Builder(this,"vmp")
                .setContentTitle("Sensordata")
                .setContentText("Recording data in the background")
                .setSmallIcon(R.drawable.icon)
                .setOnlyAlertOnce(true)
                .setOngoing(true);
        startForeground(1,notification.build());

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        initializeFiles();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        restartSensors();
        Log.i("", "In onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("","Service Destroyd");
        sm.unregisterListener(sensorListener);
        lm.removeUpdates(locationListener);

        super.onDestroy();
    }
}