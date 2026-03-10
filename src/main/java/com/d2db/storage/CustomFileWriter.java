package com.d2db.storage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.d2db.model.Table;

public class CustomFileWriter {
    private static final String DELIMITER= "|*|";
    private static final String ROW_SEPARATOR = System.lineSeparator();
    private final String dbDirectory;

    public CustomFileWriter(String dbName) {
        this.dbDirectory = "D2_DB_Storage/" + dbName + "/";
        new File(this.dbDirectory).mkdirs();
    }
    
    public synchronized void writeTable(Table table) throws IOException {
        File file = new File(dbDirectory + table.getTableName() + ".d2db");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (List<String> row : table.getRows()) {
                writer.write(String.join(DELIMITER, row) + ROW_SEPARATOR);
            }
            writer.flush();
        }
    }
}
