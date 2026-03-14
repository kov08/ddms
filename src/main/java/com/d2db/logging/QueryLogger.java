package com.d2db.logging;

import java.time.Instant;

public class QueryLogger extends LogManager {
    private static QueryLogger instance;

    private QueryLogger() {
        super();
    }

    public static synchronized QueryLogger getInstance() {
        if (instance == null) {
            instance = new QueryLogger();
        }
        return instance;
    }

    public void logQuery(String userId, String dbName, String queryString, String vmID) {
        String timeStamp = Instant.now().toString();

        String excapedQuery = queryString.replace("\"", "\\\"");

        String jsonPayload = String.format(
                "{\"timestamp\": \"%s\", \"userId\": \"%s\", \"database\": \"%s\", \"query\": \"%s\", \"vmId\": \"%s\"}",
                timeStamp, userId, dbName, excapedQuery, vmID);

        appendLog("QueryLog.json", jsonPayload);
    } 
}
