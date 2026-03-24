package com.d2db.logging;

import java.time.Instant;

import com.d2db.model.Table;
import com.d2db.storage.CustomFileReader;
import com.d2db.storage.LocalMetadataManager;

public class GeneralLogger extends LogManager {
    private static GeneralLogger instance;

    private GeneralLogger() {
        super();
    }

    public static synchronized GeneralLogger getInstance() {
        if (instance == null) {
            instance = new GeneralLogger();
        }
        return instance;
    }

    public void logExecutionTime(String queryString, long durationMs, String vmId) {
        String timeStamp = Instant.now().toString();
        String escapedQuery = queryString.replace("\"", "\\\"");
        String jsonPayload = String.format(
                "{\"timestamp\": \"%s\", \"logType\": \"ExecutionTime\", \"query\": \"%s\", \"durationMs\": %d, \"vmId\": \"%s\"}",
                timeStamp, escapedQuery, durationMs, vmId);

        appendLog("GeneralLog.json", jsonPayload);
    }

    // Save the current state(size) of tables in database
    public void logDatabaseState(String dbName, String vmId) {
        String timeStamp = Instant.now().toString();
        LocalMetadataManager meta = LocalMetadataManager.getInstance(); 
        CustomFileReader reader = new CustomFileReader(dbName);

        StringBuilder stateBuilder = new StringBuilder();
        stateBuilder.append("[");
        
        boolean isFirst = true;

        for (String tableName : meta.getAllTableNames()) {
            try {
                Table table = reader.loadTable(tableName);
                int recordCount = table.getRows().size();
                if (!isFirst) {
                    stateBuilder.append(",");
                }
                stateBuilder.append(String.format("{\"table\": \"%s\", \"records\": %d}", tableName, recordCount));
                isFirst = false;
            } catch (Exception e) {
                // skip the table
            }
        }
        stateBuilder.append("]");

        String jsonPayload = String.format(
                "{\"timestamp\": \"%s\", \"logType\":\"DatabaseState\", \"state\": %s, \"vmId\": \"%s\"}", timeStamp,
                stateBuilder.toString(), vmId);
                
        appendLog("GeneralLog.json", jsonPayload);

    }
}
