package com.d2db.engine;

public class ExecutionContext {
    private static final ThreadLocal<String> currentDbName = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();
    
    public static void setCurretnDatabases(String dbName) {
        currentDbName.set(dbName);
    }

    public static String getCurrentDatabase() {
        return currentDbName.get();
    }

    public static void setCurrentUserId(String userId) {
        currentUserId.set(userId);
    }

    public static String getCurrentUserId() {
        return currentUserId.get();
    }

    public static void clear() {
        currentDbName.remove();
        currentUserId.remove();
    }
}
