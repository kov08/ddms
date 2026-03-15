package com.d2db.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.d2db.engine.VMID;
import com.d2db.logging.EventLogger;
import com.d2db.logging.GeneralLogger;

public class VMSyncServer implements Runnable {
    private final int port;
    private volatile boolean isRunning;

    public VMSyncServer(int port) {
        this.port = port;
        this.isRunning = true;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("VMSyncServer listening on posrt " + port);
            EventLogger.getInstance().logEvent("VMSyncServer", "Connection Established", VMID.resolveMachineIdentity());

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();

                new Thread(() -> handleIncomingSync(clientSocket)).start();
            }
        } catch (Exception e) {
            EventLogger.getInstance().logEvent("Connection Failure", "serverSocket connection failure", VMID.resolveMachineIdentity());
            System.err.println("Network Server EWrror: " + e);
        }
    }
    
    private void handleIncomingSync(Socket clienSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clienSocket.getInputStream())); clienSocket) 
        {
            String incomingPayLoad = in.readLine();
            System.out.println("[NETWORK] Received sync command: " + incomingPayLoad);
            EventLogger.getInstance().logEvent("Connection packet reading", incomingPayLoad,VMID.resolveMachineIdentity());
            
            if (incomingPayLoad != null && incomingPayLoad.startsWith("REPLICA_SYNC|")) {
                String sqlCommand = incomingPayLoad.substring(13);
            }
            // Route to parse and Executor
        } catch (Exception e) {
           System.err.println("Failed to process incoming sync: " + e.getMessage());
        }
    }
    
    public void stopServer() {
        this.isRunning = false;
    }
}
