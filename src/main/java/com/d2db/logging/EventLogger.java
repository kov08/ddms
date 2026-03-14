package com.d2db.logging;

import java.time.Instant;

public class EventLogger extends LogManager {
    private static EventLogger instance;
    
    private EventLogger() {
        super();
    }
    
    public static synchronized EventLogger getInstance() {
        if (instance == null) {
            instance = new EventLogger();
        }
        return instance;
    }

    public void logEvent(String eventType, String details, String vmID) {
        String timeStamp = Instant.now().toString();

        String jsonPayload = String.format(
                "{\"timestamp\":\"%s\", \'eventType\":\"%s\",\"details\":\"%s\",\"vmID\":\"%s\"}", timeStamp, eventType,
                details, vmID);
                
                appendLog("EventLog.json", jsonPayload);
    }
}
