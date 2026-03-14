package com.d2db.storage;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.d2db.engine.VMID;

public class GlobalMetadataManager {
    public static GlobalMetadataManager instance;
    private static Map<String, String> globalSchema;
    
    private final String currentVmId;

    private GlobalMetadataManager() {
        globalSchema = new ConcurrentHashMap<>();
        this.currentVmId = VMID.resolveMachineIdentity();
    }
    
    public static synchronized GlobalMetadataManager getInstance() {
        if (instance == null) {
            instance = new GlobalMetadataManager();
        }
        return instance;
    }
    
    public void registerGlobalTable(String tableName) {
        globalSchema.put(tableName, this.currentVmId);
    }

    public void registerTableFromNetwork(String tableName, String VMID) {
        globalSchema.put(tableName, VMID);
    }

    // Retuns which VM has data
    public String locateTable(String tableName) {
        return globalSchema.get(tableName);
    }

    public String getCurrentVmId() {
        return currentVmId;
    }


}
