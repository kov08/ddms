package com.d2db.model;

public class ColumnMetadata{
    private final String columnName;
    private final String dataType;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private boolean isUnique;
    private String referenceTable;
    private String referenceColumn;

    // Parameterized Constructor 
    public ColumnMetadata(String columnName, String dataType) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.isPrimaryKey = false;
        this.isForeignKey = false;
        this.isUnique = false;
    }
    
    public void setPrimaryKey(boolean isPrimaryKey) {
        this.isPrimaryKey = isPrimaryKey;
        if (isPrimaryKey) {
            this.isUnique = isPrimaryKey;
        }
    }

    public void setForeignKey(String referenceTable, String referenceColumn) {
        this.isForeignKey = true;
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
    }

    public void setUnique(boolean isUnique) {
        this.isUnique = isUnique;
    }

    public String getColumnName(){
        return columnName;
    }

    public String getdataType() {
        return dataType;
    }
    
    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean isForeignKey() {
        return isForeignKey;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public String getReferenceTable() {
        return referenceTable;
    }

    public String getReferenceColumn() {
        return referenceColumn;
    }
}