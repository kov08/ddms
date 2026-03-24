package com.d2db.tools.erd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.d2db.storage.CustomFileReader;
import com.d2db.storage.LocalMetadataManager;
import com.d2db.engine.VMID;
import com.d2db.logging.EventLogger;
import com.d2db.model.*;;

public class ERDGenerator {
    
    private static final String ERD_DIR = "D2_DB_Storage/ERD_Exports/";
    private final String dbName;

    public ERDGenerator(String dbName){
        this.dbName = dbName;
        initializeDirectory();
    }

    private void initializeDirectory(){
        File direrctory = new File(ERD_DIR);
        if (!direrctory.exists()) {
            direrctory.mkdirs();
        } 
    }

    public void generateTextERD() throws Exception {
        LocalMetadataManager meta = LocalMetadataManager.getInstance();
        CustomFileReader reader = new CustomFileReader(dbName);
        File erdFile = new File(ERD_DIR + dbName + "_ERD.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(erdFile, false))) {
            
            List<String> tableNames = meta.getAllTableNames();
            writer.write(0);
            Map<String, Table> loadedTables = new HashMap<>();

            // Load tables
            for (String tableName : tableNames) {
                loadedTables.put(tableName, reader.loadTable(tableName));
            }
            writer.write("=== ENTITY RELATIONSHIP DIAGRAM: " + dbName + " ===\n\n");
            

            writeEntities(writer, loadedTables);

            writer.write("\n=== RELATIONSHIPS ===\n\n");

            writeRelationships(writer,loadedTables);

            writer.write("\n=== RELATIONSHIPS ===\n\n");

            writeRelationships(writer, loadedTables);

            EventLogger.getInstance().logEvent("ERD Generation: ", "Successful", VMID.resolveMachineIdentity());
            System.out.println("ERD generated successfully at: " + erdFile.getAbsolutePath());

        } catch (Exception e) {
            EventLogger.getInstance().logEvent("ERD Generation: ", "Fail", VMID.resolveMachineIdentity());
            throw new Exception("Failed to write ERD file: " + e.getMessage());

        }
    }

    private void writeEntities(BufferedWriter writer, Map<String, Table> loadedTables) throws Exception {
        for (Map.Entry<String, Table> entry : loadedTables.entrySet()) {
            writer.write("Table Name: " + entry.getKey() + "\n");
            
            Table table = entry.getValue();
            for (ColumnMetadata column : table.getSchema()) {
                String keyMarker = determineKeyMarker(column);
                writer.write(keyMarker + " Column Name: " + column.getColumnName() + " Column datatype: "
                        + column.getDataType() + "\n");
            }
            writer.write("--------------------------------\n");
        }
        
    }
    
    private void writeRelationships(BufferedWriter writer, Map<String, Table> loadedTables) throws Exception {
        for (Map.Entry<String, Table> entry : loadedTables.entrySet()) {
            Table table = entry.getValue();
            writer.write("Table: [ " + entry.getKey() + " ]\n");

            writer.write("---------------------------------\n");

            for (ColumnMetadata column : table.getSchema()) {
                if (column.isForeignKey()) {
                    
                    // Referential Integrity check
                    boolean referenceTableExists = loadedTables.containsKey(column.getReferenceTable());

                    String symbol = determineCardinality(column);
                    String relationshipText = String.format("[%s] %s [%s] : (%s)\n",
                            entry.getKey(),
                            symbol,
                            column.getReferenceTable(),
                            column.getColumnName());
                    
                    if (!referenceTableExists) {
                        relationshipText += " (ORPHANED)"; 
                    }

                    writer.write(relationshipText + "\n");
                }
            }

        }

    }

    private String determineCardinality(ColumnMetadata column) {
        if (column.isPrimaryKey() || column.isUnique()) {
            return "||--||";
        }
        return "}o--||";
    }
    
    private String determineKeyMarker(ColumnMetadata column) {
        if (column.isPrimaryKey()) {
            return "(PK)";
        }
        if (column.isForeignKey()) {
            return "(FK)";
        }
        return " -=-=-=-";
    }
}
