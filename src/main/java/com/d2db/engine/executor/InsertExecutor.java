package com.d2db.engine.executor;

import java.util.List;

import com.d2db.engine.ExecutionContext;
import com.d2db.engine.VMID;
import com.d2db.engine.parser.QueryExecutor;
import com.d2db.logging.EventLogger;
import com.d2db.logging.GeneralLogger;
import com.d2db.model.Table;
import com.d2db.network.VMSyncClient;
import com.d2db.storage.CustomFileWriter;
import com.d2db.storage.LocalMetadataManager;
import com.d2db.transaction.TransactionManager;

public class InsertExecutor implements QueryExecutor {

    private final String tableName;
    private final List<String> values;
    
    public InsertExecutor(String tableName, List<String> values) {
        this.tableName = tableName;
        this.values = values;
    }
    
    @Override
    public void execute(boolean isReplicaSync) throws Exception {
        long startTime = System.currentTimeMillis();
        String dbName = ExecutionContext.getCurrentDatabase();
        if (dbName == null) {
            throw new Exception("No database selected. Use 'USE <database_name>;' first.");
        }
        
        LocalMetadataManager meta = LocalMetadataManager.getInstance();
        if (!meta.hasLocalTable(tableName)) {
            throw new Exception("Error: Table '" + tableName + "' doesn't exist");
        }

        // load existing data
        TransactionManager tManager = TransactionManager.getInstance();
        
        // Get the table context
        Table table = tManager.getTableContext(dbName, tableName);
        
        // Modify in-memory structure
        table.insertRow(values);

        EventLogger.getInstance().logEvent("DataChange", "Inserted row into " + tableName, VMID.resolveMachineIdentity());
        GeneralLogger.getInstance().logDatabaseState(dbName, VMID.resolveMachineIdentity());
                
        // persist immediately only if it is a standalone query 
        if (!tManager.isActive()) {
            CustomFileWriter writer = new CustomFileWriter(dbName);
            writer.writeTable(table);
            
            // Network Broadcast (Prevent infinite loop)
            if (!isReplicaSync) {
                String rawQuery = "INSERT INTO " + tableName + "VALUES (" + String.join(",", values) + ");";
                VMSyncClient.broadcastCommit("VM2_IP_ADDRESS", 9090, rawQuery);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        GeneralLogger.getInstance().logExecutionTime("Insert into "+tableName+"...", duration, VMID.resolveMachineIdentity());
        System.out.println("1 row inserted successfully into "+ tableName);
    }    
}