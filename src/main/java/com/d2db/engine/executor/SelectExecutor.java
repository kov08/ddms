package com.d2db.engine.executor;

import java.util.ArrayList;
import java.util.List;

import com.d2db.engine.ExecutionContext;
import com.d2db.engine.VMID;
import com.d2db.engine.parser.QueryExecutor;
import com.d2db.logging.EventLogger;
import com.d2db.logging.GeneralLogger;
import com.d2db.model.Table;
import com.d2db.storage.LocalMetadataManager;
import com.d2db.transaction.TransactionManager;

public class SelectExecutor implements QueryExecutor {

    private final String tableName;
    private final List<String> selectColumns;
    private final String whereColumn;
    private final String whereValue;
    
    public SelectExecutor(String tableName,List<String> selectColumns, String whereColumn, String whereValue) {
        this.tableName = tableName;
        this.selectColumns = selectColumns;
        this.whereColumn = whereColumn;
        this.whereValue = whereValue;
    }
    
    @Override
    public void execute(boolean isReplicaSync) throws Exception {
        long startTime = System.currentTimeMillis();
        LocalMetadataManager meta = LocalMetadataManager.getInstacne();
        if (!meta.hasLocalTable(tableName)) {
            throw new Exception("Error table: '" + tableName + "'does not exist.");
        }
        EventLogger.getInstance().logEvent("Data fetching: ", "Started", VMID.resolveMachineIdentity());
        
        // TransactionManager to get table context
        String dbName = ExecutionContext.getCurrentDatabase();
        if (dbName == null) {
            throw new Exception("No database selected. Use 'USE <database_name>;' first.");
        }

        TransactionManager tmanager = TransactionManager.getInstance();
        Table table = tmanager.getTableContext(dbName, tableName);

        int filterColumnIndex = 0; // placeholder

        List<List<String>> results = new ArrayList<>();

        for (List<String> row : table.getRows()) {
            boolean match = true;

            if (whereColumn != null && whereValue != null) {
                String cellValue = row.get(filterColumnIndex);
                if (!cellValue.equals(whereValue)) {
                    match = false;
                }
            }

            if (match) {
                results.add(row);
            }

        }
        
        long duration = System.currentTimeMillis() - startTime;
        EventLogger.getInstance().logEvent("Data fetching", "Ended", VMID.resolveMachineIdentity());
        System.out.println("---Results for " + tableName + "---");
        for (List<String> resultRow : results) {
            System.out.println(String.join(" | ", resultRow));
        }
        GeneralLogger.getInstance().logExecutionTime("Data Fetching", duration, VMID.resolveMachineIdentity());
    }
    
    
}
