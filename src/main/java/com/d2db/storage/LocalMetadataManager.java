package com.d2db.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// import com.d2db.model.Table;
// import com.d2db.model.TrieNode;

public class LocalMetadataManager {
    private static LocalMetadataManager instance;
    private final Map<String, String> localTables;

    // private constructor for singleton design pattern
    private LocalMetadataManager() {
        localTables = new ConcurrentHashMap<>();
    }

    public static synchronized LocalMetadataManager getInstacne() {
        if (instance == null) {
            instance = new LocalMetadataManager();
        }
        return instance;
    }

    public void registarLocalTable(String tableName, String filePath) {
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
}
