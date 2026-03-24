package com.d2db.engine.parser;

import java.util.ArrayList;
import java.util.List;

import com.d2db.engine.Token;
import com.d2db.engine.TokenType;
import com.d2db.engine.executor.CreateTableExecutor;
import com.d2db.engine.executor.InsertExecutor;
import com.d2db.engine.executor.SelectExecutor;
import com.d2db.model.ColumnMetadata;

public class SQLParser {
    private final List<Token> tokens;
    private int currentPosition;

    // Constructor
    public SQLParser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentPosition = 0;
    }
    
    //Entry point
    public QueryExecutor parse() throws Exception {
        if (tokens == null || tokens.isEmpty()) {
            throw new Exception("Empty Query");
        }

        Token currentToken = peek();

        // Routing based on keyword
        if (currentToken.getTokenType() == TokenType.KEYWORD) {
            switch (currentToken.getvalue().toUpperCase()) {
                case "CREATE":
                    return parseCreate();
                case "INSERT":
                    return parseInsert();
                case "SELECT":
                    return parseSelect();
                case "DELETE":
                    // return parseDelete();

                default:
                    throw new Exception("Unexpected SQL Command: " + currentToken.getvalue());
            }
        }
        throw new Exception("Invalid SQL Syntax: Query must start with a KEYWORD");
    }

    private QueryExecutor parseCreate() throws Exception {
        match("CREATE");
        match("TABLE");
        String tableName = consume(TokenType.IDENTIFIER).getvalue();
        match("(");

        List<ColumnMetadata> columnMetadatas = new ArrayList<>();

        while (peek().getTokenType() != TokenType.EOF && !peek().getvalue().equals(")")) {
            String columnName = consume(TokenType.IDENTIFIER).getvalue();
            String dataType = consume(TokenType.IDENTIFIER).getvalue();

            ColumnMetadata columnMetadata = new ColumnMetadata(columnName, dataType);

            if (peek().getvalue().equalsIgnoreCase("PRIMARY")) {
                match("PRIMARY");
                match("KEY");
                columnMetadata.setPrimaryKey(true);
            } else if (peek().getvalue().equalsIgnoreCase("UNIQUE")) {
                match("UNIQUE");
                columnMetadata.setUnique(true);
            }

            columnMetadatas.add(columnMetadata);

            if (peek().getvalue().equals(",")) {
                match(",");
            }
        }

        match(")");
        match(";");

        return new CreateTableExecutor(tableName, columnMetadatas);
    }
    
    private QueryExecutor parseInsert() throws Exception {
        match("INSERT");
        match("INTO");
        String tableName = consume(TokenType.IDENTIFIER).getvalue();
        match("VALUES");
        match("(");

        List<String> values = new ArrayList<>();

        while (peek().getTokenType() != TokenType.EOF && !peek().getvalue().equals(")")) {
            Token valueToken = advance();
            String rawValue = valueToken.getvalue();

            if (valueToken.getTokenType() == TokenType.STRING_LITERAL) {
                rawValue = rawValue.substring(1, rawValue.length() - 1);
            }

            values.add(rawValue);

            if (peek().getvalue().equals(",")) {
                match(",");
            }
        }

        match(")");
        match(";");

        return new InsertExecutor(tableName, values);
    }
    
    private QueryExecutor parseDelete() throws Exception {
        return null;
    }

    private QueryExecutor parseSelect() throws Exception {
        match("SELECT");
        
        List<String> columns = new ArrayList<>();
        if (peek().getvalue().equals("*")) {
            match("*");
            columns.add("*");
        } else {
            columns.add(consume(TokenType.IDENTIFIER).getvalue());
        }

        match("FROM");
        String tableName = consume(TokenType.IDENTIFIER).getvalue();

        String whereColumn = null;
        String whereValue = null;

        if (peek().getvalue().equalsIgnoreCase("WHERE")) {
            match("WHERE");
            // WHERE id = 5
            whereColumn = consume(TokenType.IDENTIFIER).getvalue();
            match("=");

            Token valueToken = advance();
            if (valueToken.getTokenType() == TokenType.STRING_LITERAL || valueToken.getTokenType() == TokenType.NUMBER) {
                whereValue = valueToken.getvalue();
            } else {
                throw new Exception("Syntax Error: Unexpected literal or number after '='");
            }
        }

        match(";");

        return new SelectExecutor(tableName, columns, whereColumn, whereValue);
    }

    // Check the current token
    private Token peek() {
        if (currentPosition < tokens.size()) {
            return tokens.get(currentPosition);
        }
        return new Token(TokenType.EOF, "");
    }
    
    // Consume the current token and move forward
    private Token advance() {
        Token token = peek();
        currentPosition++;
        return token;
    }

    // validate that the token matches an expected specific String value
    private void match(String expectedValue) throws Exception {
        Token token = advance();
        if (!token.getvalue().equalsIgnoreCase(expectedValue)) {
            throw new Exception(
                    "Syntax Error: Expected Value: '" + expectedValue + "' but found '" + token.getvalue() + "'");
        }
    }
    
    // Validate the token is of an specific type
    private Token consume(TokenType expectedType) throws Exception {
        Token token = advance();
        if (token.getTokenType() != expectedType) {
            throw new Exception("Syntax error: Expected '" + expectedType + "' but found '" + token.getTokenType()+"'");
        }
        return token;
    }
}
