package com.d2db.network;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.d2db.engine.Tokenizer;
import com.d2db.engine.VMID;
import com.d2db.engine.parser.QueryExecutor;
import com.d2db.engine.parser.SQLParser;
import com.d2db.logging.EventLogger;

public class VMSyncServer implements Runnable {
    private final int port;
    private volatile boolean isRunning;
    private final ExecutorService threadPool;
    private static final int MAX_BUFFER_Size = 1048576; //1 MB

    public VMSyncServer(int port) {
        this.port = port;
        this.isRunning = true;
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("VMSyncServer listening on posrt " + port);
            EventLogger.getInstance().logEvent("VMSyncServer", "Connection Established", VMID.resolveMachineIdentity());

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(()-> handleIncomingSync(clientSocket));
            }
        } catch (Exception e) {
            EventLogger.getInstance().logEvent("Connection Failure", "serverSocket connection failure", VMID.resolveMachineIdentity());
            System.err.println("Network Server EWrror: " + e);
        }
    }
    
    private void handleIncomingSync(Socket clienSocket) {
        try (InputStream in = clienSocket.getInputStream(); clienSocket) {
            // logging
            
            byte[] buffer = new byte[MAX_BUFFER_Size];
            int bytesRead = in.read(buffer);
            if (bytesRead > 0) {
                String incomingPayLoad = new String(buffer, 0, bytesRead).trim();

                System.out.println("[NETWORK] Received sync command(payLoad): " + incomingPayLoad);
                EventLogger.getInstance().logEvent("Connection packet reading", incomingPayLoad,
                        VMID.resolveMachineIdentity());

                if (incomingPayLoad.startsWith("REPLICA SYNC|")) {
                    String sqlCommand = incomingPayLoad.substring(13);

                    Tokenizer tokenizer = new Tokenizer(sqlCommand);
                    QueryExecutor executor = new SQLParser("NetworkDB", tokenizer.tokenize()).parse();
                    
                    // Pass true to prevent loop effect
                    executor.execute(true);
                }
                
            }
            // Route to parse and Executor
        } 
        catch (Exception e) {
               System.err.println("Failed to process incoming sync: " + e.getMessage());
            }
    }
    
    public void stopServer() {
        this.isRunning = false;
    }
}
