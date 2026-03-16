package com.d2db.network;

import java.io.PrintWriter;
import java.net.Socket;

import com.d2db.engine.ExecutionContext;
import com.d2db.engine.VMID;
import com.d2db.logging.EventLogger;

public class VMSyncClient {
    public static void broadcastCommit(String targetIp, int targetPort, String sqlCommand) {
        
        try (Socket socket = new Socket(targetIp, targetPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String dbName = ExecutionContext.getCurrentDatabase();
            String userId = ExecutionContext.getCurrentUserId();
                    
            String payload = String.format("REPLICA_SYNC| %s | %s | %s ", dbName, userId, sqlCommand);
            out.println(payload);
            
            // Event logging
            EventLogger.getInstance().logEvent("[NETWORK] Broadcasted sync to " + targetIp, "REPLICA_SYNC|" + sqlCommand,VMID.resolveMachineIdentity());
            System.out.println("[NETWORK] Broadcasted sync to " + targetIp);
            
        } catch (Exception e) {
            EventLogger.getInstance().logEvent("[NETWORK] Failed to reach replica at" + targetIp, e.getMessage(), VMID.resolveMachineIdentity());
            System.err.println("[NETWORK] Failed to reach replica at " + targetIp + ": " + e.getMessage());
        }
    }
}
