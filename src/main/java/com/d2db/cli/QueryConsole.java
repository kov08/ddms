// package com.d2db.cli;

// import java.util.Scanner;

// import com.d2db.auth.AuthenticationManager;
// import com.d2db.engine.ExecutionContext;
// import com.d2db.engine.Tokenizer;
// import com.d2db.engine.VMID;
// import com.d2db.engine.parser.QueryExecutor;
// import com.d2db.engine.parser.SQLParser;
// import com.d2db.logging.QueryLogger;

// public class QueryConsole {
//     private final Scanner scanner;
//     private String loggedInUser;

//     public QueryConsole() 
//     {
//         this.scanner = new Scanner(System.in);
//         this.loggedInUser = null;
//     }

//     public void start() 
//     {
//         System.out.println("=============================================");
//         System.out.println(" Welcome to D2DB - Distributed Database");
//         System.out.println("=============================================");

//         while (loggedInUser == null) 
//         {
//             authenticateUserLoop();
//         }

//         System.out.println("\nAuthentication Successful. Type 'EXIT;' to quit.");
//         runQueryLoop();
//     }

//     private void authenticateUserLoop() 
//     {
//         System.out.println("\n1. Login\n2. Register");
//         System.out.print("Select an option: ");
//         String choice = scanner.nextLine().trim();

//         System.out.print("Username: ");
//         String username = scanner.nextLine().trim();
//         System.out.print("Password: ");
//         String password = scanner.nextLine().trim();

//         AuthenticationManager auth = AuthenticationManager.getInstance();

//         try {
//             if (choice.equals("1")) {
//                 if (auth.authenticateUser(username, password)) {
//                     this.loggedInUser = username;
//                 } 
//                 else {
//                     System.out.println("Invalid credentials. Please try again.");
//                 }
//             } else if (choice.equals("2")) {
//                 if (auth.registerUser(username, password, false)) {
//                     System.out.println("Registration successful. You are now logged in.");
//                     this.loggedInUser = username;
//                 }
//             } else {
//                 System.out.println("Invalid option.");
//             }
//         } 
//         catch (Exception e) {
//             System.err.println("Authentication Error: " + e.getMessage());
//         }
//     }

//     private void runQueryLoop() {
//         while (true) {
//             System.out.print("\nD2DB (" + loggedInUser + ") > ");
//             String rawQuery = readFullQuery();

//             if (rawQuery.equalsIgnoreCase("EXIT;")) {
//                 System.out.println("Shutting down D2DB Console. Goodbye.");
//                 break;
//             }

//             if (rawQuery.isEmpty()) {
//                 continue;
//             }

//             try {
//                 ExecutionContext.setCurrentUserId(loggedInUser);
//                 QueryLogger.getInstance().logQuery(loggedInUser, ExecutionContext.getCurrentDatabase(), rawQuery, VMID.resolveMachineIdentity());

//                 Tokenizer tokenizer = new Tokenizer(rawQuery);
//                 SQLParser parser = new SQLParser(tokenizer.tokenize());
//                 QueryExecutor executor = parser.parse();
//                 executor.execute(false); 
//             } catch (Exception e) {
//                 System.err.println("Execution Error: " + e.getMessage());
//             } finally {
//                ExecutionContext.clear();
//             }
//         }
//     }

//     private String readFullQuery() {
//         StringBuilder queryBuilder = new StringBuilder();
        
//         while (true) {
//             String line = scanner.nextLine().trim();
//             queryBuilder.append(line).append(" ");
            
//             if (line.endsWith(";")) {
//                 break;
//             }
            
//             System.out.print("    -> ");
//         }

//         return queryBuilder.toString().trim();
//     }
    
// }