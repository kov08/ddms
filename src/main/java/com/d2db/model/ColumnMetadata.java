package com.d2db.model;

class ColumnMetadata{
    private final String columnName;
    private final String dataType;

    // Parameterized Constructor 
    ColumnMetadata(String columnName, String dataType){
        this.columnName  = columnName;
        this.dataType = dataType;
    }

    public String getColumnName(){
        return columnName;
    }
    public String getdataType(){
        return dataType;
    }
}