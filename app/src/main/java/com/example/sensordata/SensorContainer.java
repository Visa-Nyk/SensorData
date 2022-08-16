package com.example.sensordata;

import android.hardware.Sensor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class SensorContainer {
    public PrintWriter getOut() {
        return out;
    }

    PrintWriter out;

    public File getFile() {
        return file;
    }

    File file;

    public Sensor getSensor() {
        return sensor;
    }

    Sensor sensor;

    public SensorContainer(Sensor sensor, File root, String filename,  String header)
            throws IOException {
        this.sensor = sensor;
        this.file = FileUtils.createTempFile(filename, root);
        this.out = FileUtils.initializePrintWriter(file);
        if(header != "")
            this.out.println(header);
    }

    public SensorContainer(Sensor sensor, File root, String filename)
            throws IOException {
        this(sensor, root, filename, "");
    }
}