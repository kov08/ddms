package com.d2db.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
            
            List<String> data = parseRow(line);
            table.insertRow(data);
        }

        return table;
    }
    
    // Read file using Iterator to avoid Out of Memory
    public Iterator<List<String>> streamTableRow(String tableName) throws Exception {
        File file = new File(dbDirectory + tableName + ".d2db");
        if (!file.exists()) {
            throw new Exception("\"FILEREADER EXCEPTION\": File not found");
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));

        return new Iterator<List<String>>(){
            String nextLine = fetchNext();

            private String fetchNext() {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        reader.close();
                    }
                    return line;
                } catch (Exception e) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                    throw new RuntimeException("\"FILEREADER EXCEPTION\": Error reading file", e);
                }
            }
            
            @Override
            public boolean hasNext(){
                return nextLine != null;
            }

            @Override
            public List<String> next(){
                String currentLine = nextLine;
                nextLine = fetchNext();

                return parseRow(currentLine);
            }
        };
    }

    private List<String> parseRow(String line) {
        if (line == null) {
            return new ArrayList<>();
        }
        String[] rowData = line.split(DELIMITER,-1);
        return Arrays.asList(rowData);
    }
}
