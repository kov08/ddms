package com.d2db;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.d2db.auth.AuthenticationManager;
import com.d2db.config.ConfigManager;
import com.d2db.engine.ExecutionContext;
import com.d2db.engine.Token;
import com.d2db.engine.Tokenizer;
import com.d2db.engine.parser.QueryExecutor;
import com.d2db.engine.parser.SQLParser;
import com.d2db.network.VMSyncServer;
import com.d2db.storage.CustomFileReader;
import com.d2db.storage.LocalMetadataManager;
import com.d2db.tools.erd.ERDGenerator;
import com.d2db.tools.export.SQLDumpGenerator;

public class AppCLI {
    private static Scanner scanner = new Scanner(System.in);
    private static String loggedInUser = null;
    private static String currentDatabase = "DefaultDB";

    public static void main(String[] args) {
        ConfigManager config = ConfigManager.getInstance();
        int SyncPort = Integer.parseInt(config.getProperty("SYNC_PORT", "9090"));

        VMSyncServer syncServer = new VMSyncServer(SyncPort);
        Thread serverThread = new Thread(syncServer);
        serverThread.setDaemon(true);
        serverThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            syncServer.stopServer();
        }));

        System.out.println("loaded Current state: "+com.d2db.storage.LocalMetadataManager.getInstance().loadStateFromDisk(currentDatabase));
        try {
            while (loggedInUser == null) {
                handleAuthentication();
            }

            runGuidedMenu();
        } catch (Exception e) {
            System.err.println("Fatal CLI Error: " + e.getMessage());
        } finally {
            syncServer.stopServer();
            scanner.close();
            System.exit(0);
        }
    }
    
    private static void handleAuthentication(){
        System.out.println("\n--- D2DB Authentication (Module 1) ---");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();
        // String choice = scanner.nextLine().trim();

        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        // String password = scanner.nextLine().trim();

        AuthenticationManager auth = AuthenticationManager.getInstance();

        try {
            if (choice.equals("1")) {
                if (auth.authenticateUser(username, password)) {
                    loggedInUser = username;
                    System.out.println("LogIn Successful: "+ username);
                    } else {
                    System.out.println("Invalid credentials.");
                    }
                } else if (choice.equals("2")) {
                    if (auth.registerUser(username, password, false)) {
                        loggedInUser = username;
                        System.out.println("Registration successful: "+username);
                    } else {
                        System.out.println("Invalid choice.");
                    }
                } 
            } catch (Exception e) {
                System.err.println("Auth Error: " + e.getMessage());
            }
        }

    private static void runGuidedMenu() throws Exception {
        boolean isRunning = true;

        while (isRunning) {
            System.out.println("\n=== D2DB Main Menu (User: " + loggedInUser + " | DB: " + currentDatabase + ") ===");
            System.out.println("1. Write SQL Query");
            System.out.println("2. Generate ERD");
            System.out.println("3. Generate SQL Dump");
            System.out.println("4. Analytics Engine");
            System.out.println("5. Change Database Context");
            System.out.println("6. EXIT");
            // System.out.println("7. DB NAME");
            System.out.println("8. TABLE NAME");
            System.out.println("9. READ TABLE");

            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                handleSQLQuery();
            } else if (choice.equals("2")) {
                handleERDGeneration();
            } else if (choice.equals("3")) {
                handleSQLDumpGeneration();
            } else if (choice.equals("4")) {
                handleAnalytics();
            } else if (choice.equals("5")) {
                System.out.print("Create/Set Database Name: ");
                currentDatabase = scanner.nextLine().trim();
            } else if (choice.equals("6")) {
                isRunning = false;
            } else if (choice.equals("8")) {
                System.out.print("Enter Database Name: ");
                currentDatabase = scanner.nextLine().trim();
                printTableName(currentDatabase);
            } else if (choice.equals("9")) {
                printTable();
            } 
            else {
                System.out.println("Invalid option. Try again.");
            }
        }    
    }
    
    private static void printTableName(String dbName) {
        File dbFolder = new File("D2_DB_Storage/" + dbName);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
            for (File file : dbFolder.listFiles()) {
                String tableName = file.getName().replace(".d2db", "");
                System.out.println(tableName);
            }
        }
    }

    private static void printTable() throws Exception {
        System.out.print("Enter Database name: ");
        String dbName = scanner.nextLine().trim();
        printTableName(dbName);
        System.out.print("Enter Table name: ");
        String tableName = scanner.nextLine().trim();
        // String currentDb = ExecutionContext.getCurrentDatabase();
        // File file = new File("D2_DB_Storage/" + dbName+ tableName+".d2db");
        try {
            CustomFileReader reader = new CustomFileReader(dbName);
            Iterator<List<String>> rowStream = reader.streamTableRow(tableName);
            
            while (rowStream.hasNext()) {
                List<String> row = rowStream.next();
                System.out.println(row.toString());
            }

        } catch (Exception e) {
            throw new Exception("Error reading table data.");
        }
    }
 
    private static void handleSQLQuery() {
        Map<String, String> localTables = LocalMetadataManager.getInstance().loadStateFromDisk(currentDatabase);
        System.out.println("Loaded Local Tables: "+localTables.entrySet().size());
        System.out.println("\n--- Execute SQL Query ---");
        System.out.println("Supported Syntax Examples:");
        System.out.println("  CREATE: CREATE TABLE Users (id INT PRIMARY KEY, name VARCHAR);");
        System.out.println("  INSERT: INSERT INTO Users VALUES (1, 'Alex');");
        System.out.println("  SELECT: SELECT * FROM Users WHERE id = 1;");
        System.out.println("  DELETE: DELETE FROM Users WHERE id = 1;");
        System.out.println("--------------------------------------------------");
        System.out.print("Type your query (end with ';'): ");

        String rawQuery = scanner.nextLine().trim();
        
        if (rawQuery.isEmpty()) {
            System.out.println("Query cannot be empty.");
            return;
        }

        try {
            System.out.println("inside TRY Block");
            ExecutionContext.setCurrentUserId(loggedInUser);
            ExecutionContext.setCurretnDatabases(currentDatabase);

            Tokenizer tokenizer = new Tokenizer(rawQuery);
            List<Token> tokens = tokenizer.tokenize();
            SQLParser parser = new SQLParser(tokens);
            QueryExecutor executor = parser.parse();

            executor.execute(false);
        } catch (Exception e) {
            System.err.println("Query Execution Error: " + e.getMessage());
        } finally {
            ExecutionContext.clear();
        }
    }

    private static void handleERDGeneration() {
        System.out.println("\n--- Generating ERD ---");

        try {
            ERDGenerator erdGenerator = new ERDGenerator(currentDatabase);
            erdGenerator.generateTextERD();    
        } catch (Exception e) {
            System.err.println("ERD Generation Error: " + e.getMessage());
        }
    }

    private static void handleSQLDumpGeneration() {
        System.out.println("\n--- Generating SQL Dump ---");

        try {
            SQLDumpGenerator dumpGenerator = new SQLDumpGenerator(currentDatabase);
            dumpGenerator.generateSQLDump();
        } catch (Exception e) {
            System.err.println("SQL Dump Error: " + e.getMessage());
        }
    }

    private static void handleAnalytics() {
        System.out.println("Coming Soon...");
    }
}

