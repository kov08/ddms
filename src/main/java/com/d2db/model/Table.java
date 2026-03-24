package com.d2db.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Table {
    
    private final String tableName;
    private final List<ColumnMetadata> schema;
    private final List<List<String>> rows;
    private final Trie index;

    public Table(String tableName) {
        this.tableName = tableName;
        this.schema = new ArrayList<>();
        this.rows = new LinkedList<>();
        this.index = new Trie();
    }

    public Table(String tableName, List<ColumnMetadata> schema) {
        this.tableName = tableName;
        this.schema = schema;
        this.rows = new LinkedList<>();
        this.index = new Trie();
    }

    public void addColumnMetadata(ColumnMetadata columnMetadata) {
        schema.add(columnMetadata);
    }
    
    public void addColumn(String columnName, String dataType) {
        schema.add(new ColumnMetadata(columnName, dataType));
    }

    public void insertRow(List<String> rowData) {
        rows.add(rowData);

        // Assuming the first column is a primary key we want t o index
        if (!rowData.isEmpty()) {
            index.insert(rowData.get(0));
        }
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnMetadata> getSchema() {
        return schema;
    }
}
