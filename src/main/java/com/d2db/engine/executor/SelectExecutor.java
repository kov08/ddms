package com.d2db.engine.executor;

import java.util.ArrayList;
import java.util.List;

import com.d2db.engine.parser.QueryExecutor;
import com.d2db.model.Table;
import com.d2db.storage.CustomFileReader;
import com.d2db.storage.LocalMetadataManager;

public class SelectExecutor implements QueryExecutor {

    private final String dbName;
    private final String tableName;
    private final List<String> selectColumns;
    private final String whereColumn;
    private final String whereValue;
    
    public SelectExecutor(String dbName, String tableName,List<String> selectColumns, String whereColumn, String whereValue) {
        this.dbName = dbName;
        this.tableName = tableName;
        this.selectColumns = selectColumns;
        this.whereColumn = whereColumn;
        this.whereValue = whereValue;
    }
    
    @Override
    public void execute() throws Exception {
        LocalMetadataManager meta = LocalMetadataManager.getInstacne();
        if (!meta.hasLocalTable(tableName)) {
            throw new Exception("Error table: '" + tableName + "'does not exist.");
        }

        CustomFileReader reader = new CustomFileReader(dbName);
        Table table = reader.loadTable(tableName);

        int filterColumnIndex = 0; // placeholder

        List<List<String>> results = new ArrayList<>();

        for (List<String> row : table.getRows()) {
            boolean match = true;

            if (whereColumn != null && whereValue != null) {
                String cellValue = row.get(filterColumnIndex);
                if (!cellValue.equals(whereValue)) {
                    match = false;
                }
            }

            if (match) {
                results.add(row);
            }

        }

        System.out.println("---Results for " + tableName + "---");
        for (List<String> resultRow : results) {
            System.out.println(String.join(" | ", resultRow));
        }
    }
    
    
}
