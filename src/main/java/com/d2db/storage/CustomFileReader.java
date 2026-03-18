package com.d2db.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.d2db.model.Table;

public class CustomFileReader {
    private static final String DELIMITER = "\\|#\\|";
    private final String dbDirectory;
    
    public CustomFileReader(String dbName){
        this.dbDirectory = "D2_db_Storage/"+dbName+"/";
    }
    
    // Read file and reconstruct the in-memory Table Obj.
    public Table loadTable(String tableName) throws IOException {
        File file = new File(dbDirectory + tableName + ".d2db");
        if (!file.exists()) {
            return new Table(tableName);
        }

        // Snapshot in memory
        List<String> lines = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line); // snap stored
            }
        }

        Table table = new Table(tableName);
        
        for (String line : lines) {
            String[] rowData = line.split(DELIMITER);
            table.insertRow(Arrays.asList(rowData));
        }
        
        return table;
    }
}
