package com.d2db.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final Map<String, String> properties;

    private ConfigManager() {
        this.properties = new HashMap<>();
        loadEnvironmentVariables();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadEnvironmentVariables() {
        File file = new File(".env");
        if (!file.exists()) {
            System.err.println("Warning: .env file not found. Falling back to defaults.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] prop = line.split("=", 2);
                if (prop.length == 2) {
                    properties.put(prop[0].trim(), prop[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to read .env file: " + e.getMessage());
        }
    }

    public String getProperty(String property, String defaultValue) {
        return properties.getOrDefault(property, defaultValue);
    }
}
