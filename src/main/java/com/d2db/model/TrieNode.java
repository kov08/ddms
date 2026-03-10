package com.d2db.model;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    Boolean isEndOfTheWord = false;
}
