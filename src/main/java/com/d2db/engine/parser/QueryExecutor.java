package com.d2db.engine.parser;

public interface QueryExecutor {
    void execute(boolean isReplicaSync) throws Exception;
}
