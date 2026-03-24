package com.d2db.engine.executor;

import java.util.List;

import com.d2db.engine.ExecutionContext;
import com.d2db.engine.parser.QueryExecutor;
import com.d2db.model.ColumnMetadata;
import com.d2db.model.Table;
import com.d2db.network.VMSyncClient;
import com.d2db.storage.CustomFileWriter;
import com.d2db.storage.LocalMetadataManager;

public class CreateTableExecutor implements QueryExecutor{
    private final String tableName;
    private final List<ColumnMetadata> columnMetadatas;

    public CreateTableExecutor(String tableName, List<ColumnMetadata> columnMetadatas) {
        this.tableName = tableName;
        this.columnMetadatas = columnMetadatas;
    }

    @Override
    public void execute(boolean isReplicaSync) throws Exception {
        String dbName = ExecutionContext.getCurrentDatabase();
        if ( dbName == null ){
            throw new Exception("No database selected.");
        }

        LocalMetadataManager meta = LocalMetadataManager.getInstance();
        if ( meta.hasLocalTable(tableName)) {
            throw new Exception("Error: Table '" + tableName + "' already exists.");
        }

        Table table = new Table(tableName);
        for (ColumnMetadata columnMetadata : columnMetadatas) {
            table.addColumnMetadata(columnMetadata);
        }

        CustomFileWriter writer = new CustomFileWriter(dbName);
        writer.writeTable(table);
        String filePath = "D2_db_Storage/"+dbName+"/"+tableName+".d2db";
        meta.registerLocalTable(tableName, filePath);

        if ( !isReplicaSync ) {
            StringBuilder queryBuilder = new StringBuilder("CREATE TABLE ");
            queryBuilder.append(tableName).append(" (");
            
            for (int i = 0; i < columnMetadatas.size(); i++) {
                ColumnMetadata cmd = columnMetadatas.get(i);
                queryBuilder.append(cmd.getColumnName()).append(" ").append(cmd.getDataType());
                
                if ( cmd.isPrimaryKey()) {
                    queryBuilder.append(" PRIMARY KEY");
                } else if (cmd.isUnique()) {
                    queryBuilder.append(" UNIQUE");
                }

                if ( i < columnMetadatas.size() - 1) {
                    queryBuilder.append(", ");
                }
            }

            queryBuilder.append(");");
            String targetIp = "TARGET_VM_IP";
            VMSyncClient.broadcastCommit(targetIp,9090, queryBuilder.toString());
        }

        System.out.println("Table '" + tableName + "' created successfully.");
    }
}
