package com.d2db.engine.executor;

import java.util.List;

import com.d2db.engine.parser.QueryExecutor;
import com.d2db.model.Table;
import com.d2db.storage.CustomFileReader;
import com.d2db.storage.CustomFileWriter;
import com.d2db.storage.LocalMetadataManager;

public class InsertExecutor implements QueryExecutor {

    private final String dbName;
    private final String tableName;
    private final List<String> values;
    
    public InsertExecutor(String dbName, String tableName, List<String> values) {
        this.dbName = dbName;
        this.tableName = tableName;
        this.values = values;
    }
    
    @Override
    public void execute() throws Exception {
        LocalMetadataManager meta = LocalMetadataManager.getInstacne();
        if (!meta.hasLocalTable(tableName)) {
            throw new Exception("Error: Table '" + tableName + "' does't exist");
        }

        CustomFileReader reader = new CustomFileReader(dbName);
        CustomFileWriter writer = new CustomFileWriter(dbName);

        // load existing data
        Table table = reader.loadTable(tableName);

        // Modify in-memory structure
        table.insertRow(values);

        // persist back to disk
        writer.writeTable(table);

        System.out.println("1 row inserted successfully into "+ tableName);
    }    
}
