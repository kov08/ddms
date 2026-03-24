package com.d2db.tools.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.d2db.model.ColumnMetadata;
import com.d2db.model.Table;
import com.d2db.storage.CustomFileReader;
import com.d2db.storage.LocalMetadataManager;

public class SQLDumpGenerator {
    private static final String DUMP_DIR = "D2_DB_Storage/Exports/";
    private final String dbName;

    public SQLDumpGenerator(String dbName) {
        this.dbName = dbName;
        InitializeDirectory();
    }

    private void InitializeDirectory() {
        File diirectory = new File(DUMP_DIR);
        if (!diirectory.exists()) {
            diirectory.mkdirs();
        }
    }

    public void generateSQLDump() throws Exception{
        LocalMetadataManager meta = LocalMetadataManager.getInstacne();
        CustomFileReader reader = new CustomFileReader(dbName);

        String timestamp = Instant.now().toString().replace(".", "-");
        File dumpFile = new File(DUMP_DIR + dbName + "backup" + timestamp + ".sql");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dumpFile, false))) {
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

            writer.write("-- ===========================================\n");
            writer.write("-- D2DB SQL DUMP\n");
            writer.write("-- Database: " + dbName + "\n");
            writer.write("-- Generated: " + timestamp + "\n");
            writer.write("-- ===========================================\n\n");
            
            writer.write("CREATE DATABASE IF NOT EXISTS " + dbName + ";\n");
            writer.write("USE " + dbName + ";\n\n");

            List<String> tableNames = meta.getAllTableNames();

            // Pass 1: Write all schemas
            writer.write("-- ===========================================\n");
            writer.write("-- TABLE STRUCTURES\n");
            writer.write("-- ===========================================\n\n");

            for (String tableName : tableNames) {
                Table table = reader.loadTable(tableName);
                writeTableStructure(writer, table, tableName);
            }

            // Pass 2: Write all Tables data 
            writer.write("-- ===========================================\n");
            writer.write("-- TABLE DATA\n");
            writer.write("-- ===========================================\n\n");

            for (String tableName : tableNames) {
                Table table = reader.loadTable(tableName);
                writeStreamedTableData(writer, table, tableName);
            }

            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
            System.out.println("SQL Dump generated successfully at: " + dumpFile.getAbsolutePath());

        } catch (Exception e) {
            throw new Exception("Failed to write SQL dump file: " + e.getMessage());
        }
    }

    private void writeTableStructure(BufferedWriter writer, Table table, String tableName) throws IOException {
        writer.write("DROP TABLE IF EXISTS " + tableName);
        writer.write("CREATE TABLE " + tableName);

        List<ColumnMetadata> schema = table.getSchema();
        List<String> constraints = new ArrayList<>();

        for (int i = 0; i < schema.size(); i++) {
            ColumnMetadata column = schema.get(i);
            String columnDef = "   " + column.getColumnName() + "   " + column.getDataType();

            if (column.isUnique() && !column.isPrimaryKey()) {
                columnDef += " UNIQUE ";
            }

            if (i < schema.size() - 1 && constraints.isEmpty()) {
                columnDef += ", ";
            }
            writer.write(columnDef + "\n");

            if (column.isPrimaryKey()) {
                constraints.add(String.format("    PRIMARY KEY ( %s )", column.getColumnName()));
            }

            if (column.isForeignKey()) {
                constraints.add(String.format("    FOREIGN KEY (%s) REFERENCES %s(%s)",
                        column.getColumnName(), column.getReferenceTable(), column.getReferenceColumn()));
            }
        }

        // Write constraints
        for (int i = 0; i < constraints.size(); i++) {
            writer.write(constraints.get(i));
            if (i < constraints.size() - 1) {
                writer.write(",");
            }
            writer.write("\n");
        }
    }

    // Load and write entire table ( we use stream to prevent out of memory problem)
    private void writeTableData(BufferedWriter writer, Table table, String tableName) throws IOException {
        writer.write("Table Name: " + tableName);
        List<List<String>> rows = table.getRows();
        List<ColumnMetadata> schema = table.getSchema();

        if (rows.isEmpty()) {
            return;
        }

        for (List<String> row : rows) {
            StringBuilder insertStatement = new StringBuilder();
            for (int i = 0; i < row.size(); i++) {
                String value = row.get(i);
                String dataType = schema.get(i).getDataType().toUpperCase();

                insertStatement.append(formatValue(value, dataType));
                if (i < row.size() - 1) {
                    insertStatement.append(", ");
                }
            }
            insertStatement.append(", \n");
            writer.write(insertStatement.toString());
        }
        writer.write("\n");

    }
    
    // Write table using Iterartor to prevent Out of Memory
    private void writeStreamedTableData(BufferedWriter writer, Table table, String tableName) throws Exception {
        CustomFileReader reader = new CustomFileReader(dbName);
        Iterator<List<String>> rowStream = reader.streamTableRow(tableName);

        List<ColumnMetadata> schema = table.getSchema();

        if (!rowStream.hasNext()) {
            return;
        }

        while (rowStream.hasNext()) {
            List<String> row = rowStream.next();

            StringBuilder insertStatement = new StringBuilder("INSERT INTO " + tableName + " VALUES (");

            for (int i = 0; i < row.size(); i++) {
                String val = row.get(i);
                String dataType = schema.get(i).getDataType().toUpperCase();

                insertStatement.append(formatValue(val, dataType));

                if (i < row.size() - 1) {
                    insertStatement.append(", ");
                }
            }

            insertStatement.append(");\n");
            writer.write(insertStatement.toString());
        }
        writer.write("\n");
    }

    private String formatValue(String value, String dataType){
        if (value == null || value.equalsIgnoreCase("NULL")) {
            return "NULL";
        }

        if (dataType.contains("INT") || dataType.contains("BOOLEAN") || dataType.contains("FLOAT") || dataType.contains("DOUBLE")) {
            return value;
        } else {
            
            // Escape single quotes, new line and carriage return in strings to prevent SQL syntax corruption
            String escapeValue = value.replace("'", "''");
            escapeValue = escapeValue.replace("\n", "\\n").replace("\r", "\\r");
            
            return "'" + escapeValue + "'";
        }
    }


}
