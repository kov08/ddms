package com.d2db.engine.executor;

import java.util.List;

import com.d2db.engine.VMID;
import com.d2db.engine.parser.QueryExecutor;
import com.d2db.logging.EventLogger;
import com.d2db.logging.GeneralLogger;
import com.d2db.model.Table;
import com.d2db.storage.CustomFileWriter;
import com.d2db.storage.LocalMetadataManager;
import com.d2db.transaction.TransactionManager;

public class InsertExecutor implements QueryExecutor {

    private final String dbName;
    private final String tableName;
    private final List<String> values;
    
    public InsertExecutor(String dbName, String tableName, List<String> values) {
        this.dbName = dbName;
        this.tableName = tableName;
        this.values = values;
    }
    
    @Override
    public void execute() throws Exception {
        long startTime = System.currentTimeMillis();
        LocalMetadataManager meta = LocalMetadataManager.getInstacne();
        if (!meta.hasLocalTable(tableName)) {
            throw new Exception("Error: Table '" + tableName + "' does't exist");
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
        }
        
        long duration = System.currentTimeMillis() - startTime;
        GeneralLogger.getInstance().logExecutionTime("Insert into "+tableName+"...", duration, VMID.resolveMachineIdentity());
        System.out.println("1 row inserted successfully into "+ tableName);
    }    
}