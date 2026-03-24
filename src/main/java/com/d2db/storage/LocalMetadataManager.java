package com.d2db.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalMetadataManager {
    private static LocalMetadataManager instance;
    private final Map<String, String> localTables;

    // private constructor for singleton design pattern
    private LocalMetadataManager() {
        localTables = new ConcurrentHashMap<>();
    }

    public static synchronized LocalMetadataManager getInstance() {
        if (instance == null) {
            instance = new LocalMetadataManager();
        }
        return instance;
    }

    public void registerLocalTable(String tableName, String filePath) {
        if (!localTables.containsKey(tableName)) {
            localTables.put(tableName, filePath);
        }
    }

    public boolean hasLocalTable(String tableName) {
        return localTables.containsKey(tableName);
    }

    public List<String> getAllTableNames() {
        List<String> allTableNames = new ArrayList<>();
        for (String tableName : localTables.keySet()) {
            allTableNames.add(tableName);
        }
        return allTableNames;
    }

    public Map<String, String> loadStateFromDisk(String dbName) {           
            File dbFolder = new File("D2_DB_Storage/" + dbName);
            if (dbFolder.exists() && dbFolder.isDirectory()) {
                for (File file : dbFolder.listFiles()) {
                    String tableName = file.getName().replace(".d2db", "");
                    if (!localTables.containsKey(tableName)) {
                        String filePath = dbFolder + "/" + dbName + ".d2db";
                        localTables.put(tableName, filePath);
                    }
                }
            }
            return localTables;
    }
}
