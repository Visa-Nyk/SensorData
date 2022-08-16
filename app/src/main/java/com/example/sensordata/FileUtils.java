package com.example.sensordata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileUtils {

    public static PrintWriter initializePrintWriter(File file) throws IOException {
        FileWriter fw = new FileWriter(file, true);
        BufferedWriter bw = new BufferedWriter(fw);
        return new PrintWriter(bw);
    }

    public static File createTempFile(String fileName, File path) throws IOException {
        File file = new File(path, fileName);
        file.delete();
        file.createNewFile();
        return file;
    }

}
