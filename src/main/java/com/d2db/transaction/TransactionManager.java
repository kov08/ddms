package com.d2db.transaction;

import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.d2db.model.Table;
import com.d2db.storage.CustomFileReader;
import com.d2db.storage.CustomFileWriter;

public class TransactionManager {
    private static TransactionManager instance;
    private boolean isTrasactionActive;
    private Map<String, Table> inMemoryWorkspace;
    private long transactionStartTime;
    private static final long TIMEOUT_MS = 60000; //60 seconds
    private final ReentrantLock rLock = new ReentrantLock(true);

    private TransactionManager() {
        isTrasactionActive = true;
        inMemoryWorkspace = new HashMap<>();
        startWatchDogDemon();
    }

    private void startWatchDogDemon() {
        Thread watchDog = new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(5000);
                    if (isTrasactionActive && (System.currentTimeMillis() - transactionStartTime > TIMEOUT_MS)) {
                        System.out.println("TIMEOUT: Forcefully rolling back abandoned transaction.");
                        rollbackTransaction();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        watchDog.setDaemon(true);
        watchDog.start();
    }

    public static synchronized TransactionManager getInstance() {
        if (instance == null) {
            instance = new TransactionManager();
        }
        return instance;
    }
    
    // Begin Transaction
    public void begintransaction() throws Exception {
        if (!rLock.tryLock(3,TimeUnit.MILLISECONDS)) {
            throw new Exception("Database is currently locked by another user.");
        }
        try {
            
            if (isTrasactionActive) {
                throw new Exception("A transaction is already in progress. Single distributed transaction limit reached.");
            }
            
            this.isTrasactionActive = true;
            this.inMemoryWorkspace.clear();
            this.transactionStartTime = System.currentTimeMillis();
            // Pre-logging Step
            System.out.println("[EVENT LOG] <StartTransaction> ID: " + transactionStartTime);
        
        } catch (Exception e) {
            rLock.unlock();
            throw e;
        }
    }
        
    public Table getTableContext(String dbName, String tableName) throws Exception {

        if (isTrasactionActive && inMemoryWorkspace.containsKey(tableName)) {
            return inMemoryWorkspace.get(tableName);
        }

        CustomFileReader reader = new CustomFileReader(dbName);
        Table table = reader.loadTable(tableName);
        if (isTrasactionActive) {
            inMemoryWorkspace.put(tableName, table);
        }
        return table;
    }
    
    // Commit datastructure to persistant storage
    public void commitTransaction(String dbName) throws Exception {
        try {
            
            
            if (!isTrasactionActive) {
                throw new Exception("No active transaction to commit.");
            }
            
            System.out.println("[EVENT LOG] <StartCommit> ID: " + transactionStartTime);
            CustomFileWriter writer = new CustomFileWriter(dbName);
            
            // Write/update all tables to storage from inmemory
            for (Table table : inMemoryWorkspace.values()) {
            writer.writeTable(table);
            }

            System.out.println("[EVENT LOG] <EndCommit> ID: " + transactionStartTime);
            this.isTrasactionActive = false;
            this.inMemoryWorkspace.clear();
            System.out.println("TRANSACTION COMMITED SUCCESSFULLY.");
        } catch (Exception e) {
            this.isTrasactionActive = false;
            rLock.unlock();
        }
    }
    
    // Rollback
    public synchronized void rollbackTransaction() throws Exception {
        if (!isTrasactionActive) {
            throw new Exception("No active transaction to rollback.");
        }

        System.out.println("[EVENT LOG] <Rollback> ID: " + transactionStartTime);
        this.inMemoryWorkspace.clear();
        this.isTrasactionActive = false;
        System.out.println("TRANSACTION ROLLED BACK.");
    }

    public boolean isActive() {
        return isTrasactionActive;
    }
}
