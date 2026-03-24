package com.d2db.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.DocFlavor.STRING;

public class Tokenizer {
    private static final String KEYWORDS_REGEX = "(?i)\\b(CREATE|DATABASE|USE|TABLE|INSERT|INTO|VALUES|SELECT|FROM|WHERE|UPDATE|SET|DELETE)\\b";
    private static final String IDENTIFIER_REGEX = "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b";
    private static final String STRING_LITERAL_REGEX = "'[^']*'";
    private static final String NUMBER_REGEX = "\\b\\d+\\b";
    private static final String SYMBOL_REGEX = "[=(),;*]";

    private static final Pattern TOKEN_PATTERN = Pattern
            .compile(String.format("(?<KEYWORD>%s)|(?<STRING>%s)|(?<NUMBER>%s)|(?<IDENTIFIER>%s)|(?<SYMBOL>%s)",
                    KEYWORDS_REGEX, STRING_LITERAL_REGEX, NUMBER_REGEX, IDENTIFIER_REGEX, SYMBOL_REGEX));

    private final String input;
    private int position;

    public Tokenizer(String input){
        this.input = input;
        this.position = 0;
    }

    public List<Token> tokenize(){
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);

        while (matcher.find()) {
            if (matcher.group("KEYWORD") != null) {
                tokens.add(new Token(TokenType.KEYWORD, matcher.group("KEYWORD").toUpperCase()));
            } else if (matcher.group("STRING") != null) {
                String literal = matcher.group("STRING");
                tokens.add(new Token(TokenType.STRING_LITERAL, literal.substring(0, literal.length())));
            } else if (matcher.group("NUMBER") != null) {
                tokens.add(new Token(TokenType.NUMBER, matcher.group("NUMBER")));
            } else if (matcher.group("IDENTIFIER") != null) {
                tokens.add(new Token(TokenType.IDENTIFIER, matcher.group("IDENTIFIER")));
            } else if (matcher.group("SYMBOL") != null) {
                tokens.add(new Token(TokenType.SYMBOL, matcher.group("SYMBOL")));
            }
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }
}
