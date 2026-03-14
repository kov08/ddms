package com.d2db.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public abstract class LogManager {
    private static final String LOG_DIR = "D2_DB_Storage/Logs/";
    
    public LogManager(){
        File directory = new File(LOG_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    protected synchronized void appendLog(String fileName, String jsonPayLoad) {
        File logFile = new File(LOG_DIR + fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            
            writer.write(jsonPayLoad);
            writer.newLine();
            writer.flush();
            
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to write to log " + fileName);
        }
    }
}
