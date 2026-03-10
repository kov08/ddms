package com.d2db.engine;

public class Token {
    private final TokenType type;
    private final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getTokenType() {
        return type;
    }

    public String getvalue() {
        return value;
    }
    
    @Override
    public String toString() {
       return String.format("Token[%s,'%s']",type.name(), value);
    }
}
