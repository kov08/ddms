package com.d2db.auth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.d2db.network.VMSyncClient;

public class AuthenticationManager {
    private static final  String USER_DIR = "D2_DB_Storage/";
    private static final  String USER_File = USER_DIR + "User_Profile.txt";
    private static final File profileFileObj = new File(USER_File);
    private static AuthenticationManager instance;

    private static final Pattern VAILD_USERNAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final Map<String, String> userCache;


    private AuthenticationManager() {
        initializeDirectory();
        this.userCache = new ConcurrentHashMap<>();
        loadUsersIntoCache();
    }

    public static synchronized AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }

    private void initializeDirectory() {
        File directory = new File(USER_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public synchronized boolean registerUser(String userName, String password, boolean isReplicaSync) throws Exception {
        if (userName == null || password == null || userName.trim().isEmpty() || password.trim().isEmpty()) {
            throw new Exception("User name or password cannot be empty");
        }
        
        if (!VAILD_USERNAME.matcher(userName).matches()) {
            throw new Exception("Invalid username. Only alphanumeric characters and underscores are allowed.");
        }

        if (userCache.containsKey(userName)) {
            throw new Exception("Error: Username '" + userName + "' already exists.");
        }

        String hashedPassword = hashData(password);
        String profileLine = userName + " | " + hashedPassword;
        appendUserToFile(profileLine);
        userCache.put(userName, hashedPassword);
        
        // if (!isReplicaSync) {
        //     String payLoad = "REGISTER USER " + profileLine;
        //     // Check 
        //     VMSyncClient.broadcastCommit("targetIP", 0, payLoad);
        // }
        
        System.out.println("User '" + userName + "' registered successfully.");
        return true;
    }
    
    public boolean authenticateUser(String userName, String password) throws Exception {
        if (userName == null || password == null) {
            return false;
        }

        if (!userCache.containsKey(userName)) {
            return false;
        }
        
        String storedHasedPassword = userCache.get(userName);
        String hashedPassword = hashData(password);
        if (storedHasedPassword.equals(hashedPassword)) {
            return true;
        }
        
        return false;
    }
    
    private void appendUserToFile(String profileLne) throws Exception {
        File file = new File(USER_File);
        try (BufferedWriter writer = new BufferedWriter(new  FileWriter(file, true))) {
            writer.write(profileLne);
            writer.newLine();
        } catch (IOException e) {
            throw new Exception("Failed to write to User_Profile file: " + e.getMessage());
        }
    }

    private String hashData(String password) throws Exception {
        try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            
            for (int i = 0; i < encodedhash.length; i++){
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if ( hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } 
        catch (Exception e) {
            throw new Exception("Error hashing data: " + e.getMessage());
        }
    }

    private void loadUsersIntoCache(){
        if (!profileFileObj.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(profileFileObj))) {
            String line;

            while ((line = reader.readLine())!= null) {
                String[] user = line.split("\\|");
                if (user.length >= 2) {
                    userCache.put(user[0], user[1]);
                }
            }

        } catch (Exception e) {
            System.err.println("\"loadUsersIntoCache: \" Critical Warning: Failed to load user profile to cache.");
        }
    }
    
}
