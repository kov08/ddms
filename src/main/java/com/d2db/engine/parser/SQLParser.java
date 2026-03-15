package com.d2db.engine.parser;

import java.util.ArrayList;
import java.util.List;

import com.d2db.engine.Token;
import com.d2db.engine.TokenType;
import com.d2db.engine.executor.SelectExecutor;

public class SQLParser {
    private final List<Token> tokens;
    private int currentPosition;
    private final String currentDbName;

    // Constructor
    public SQLParser(String currentDbName, List<Token> tokens) {
        this.currentDbName = currentDbName;
        this.tokens = tokens;
        this.currentPosition = 0;
    }
    
    //Entry point
    public QueryExecutor parse() throws Exception {
        if (tokens == null || tokens.isEmpty()) {
            throw new Exception("Empty Query");
        }

        Token curretnToken = peek();

        // Routing based on keyword
        if (curretnToken.getTokenType() == TokenType.KEYWORD) {
            switch (curretnToken.getvalue().toUpperCase()) {
                case "CREATE":
                    // return parseCreate();
                case "INSERT":
                    // return parseInsert();
                case "SELECT":
                    return parseSelect();

                default:
                    throw new Exception("Unexpected SQL Command: " + curretnToken.getvalue());
            }
        }
        throw new Exception("Invalid SQL Syntax: Query must start with a KEYWORD");
    }

    // parseSelect
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
                throw new Exception("Syntex Error: Unexpected literal or number after '='");
            }
        }

        match(";");

        return new SelectExecutor(currentDbName, tableName, columns, whereColumn, whereValue);
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
                    "Syntex Error: Expected Value: '" + expectedValue + "' but found '" + token.getvalue() + "'");
        }
    }
    
    // Validate the token is of an specific type
    private Token consume(TokenType expectedType) throws Exception {
        Token token = advance();
        if (token.getTokenType() != expectedType) {
            throw new Exception("Syntex error: Expected " + expectedType + "but found " + token.getTokenType());
        }
        return token;
    }
}
