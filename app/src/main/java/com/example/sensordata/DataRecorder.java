package com.example.sensordata;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataRecorder extends Service {
    private final int FREQUENCY = 100;
    SensorManager sm;
    LocationManager lm;


    private PrintWriter accOut;
    private PrintWriter rotOut;
    private PrintWriter spdOut;

    PowerManager.WakeLock wl;
    //Intent restartIntent;
    private final IBinder binder = new LocalBinder();

    File path;
    File rot;
    File acc;
    File spd;

    public void saveZipFile(String prefix) throws IOException {

        prefix = prefix.replaceAll("\\W+", "");
        String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
        accOut.close();
        rotOut.close();
        spdOut.close();
        File z = new File(path, prefix + "_" + time + ".zip");
        z.createNewFile();
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(z));
        for( File file : new File[] {acc, rot, spd}){
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
        Sensor accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sm.registerListener(sensorListener, accelerometer, 1000000 / FREQUENCY);
        sm.registerListener(sensorListener, rotation, 1000000 / FREQUENCY);
    }

    private void initializeFiles() {
        String root = Environment.getExternalStorageDirectory().toString();
        path = new File(root +"/sensordata");
        path.mkdirs();
        try{
            rot = createTempFile("rot.csv");
            rotOut = initializePrintWriter(rot);
            rotOut.println("time,rot_x,rot_y,rot_z,rot_c");

            acc = createTempFile("acc.csv");
            accOut = initializePrintWriter(acc);
            accOut.println("time,acc_x,acc_y,acc_z");

            spd = createTempFile("spd.csv");
            spdOut = initializePrintWriter(spd);
            spdOut.println("time,speed");
        }
        catch (IOException e) {
            Log.e("", e.toString());
        }

    }

    private PrintWriter initializePrintWriter(File file) throws IOException {
        FileWriter fw = new FileWriter(file, true);
        BufferedWriter bw = new BufferedWriter(fw);
        return new PrintWriter(bw);
    }

    private File createTempFile(String fileName) throws IOException {
        File file = new File(path, fileName);
        file.delete();
        file.createNewFile();
        return file;
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
            PrintWriter out = null;
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                out = accOut;
            }
            else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                out = rotOut;
            }

            if (out != null){
                out.write(String.valueOf(event.timestamp/1000000L));
                for(float val:event.values)
                    out.write(","+String.format("%.5f", val));
                out.println();
            }
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
        initializeFiles();
        startForeground(1,notification.build());

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        //restartIntent = new Intent(this, DataRecorder.class);
    }

    public BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i("", "screen_off");

                //startForegroundService(restartIntent);
                restartSensors();
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(wl == null){
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "datarecorder::wakelock");
            wl.acquire();
            Log.i("","initialized waKELOCK");
            }
        else if (!wl.isHeld()) {
            wl.acquire();
            Log.i("", "reacquired wakelock");
        }
        restartSensors();
        Log.i("", "In onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        wl.release();
        Log.i("","Service Destroyd");
        sm.unregisterListener(sensorListener);
        unregisterReceiver(mReceiver);
        lm.removeUpdates(locationListener);
        super.onDestroy();
    }
}