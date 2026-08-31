package com.library.service;

import java.util.*;

/**
 * Thread-safe Trie (prefix tree) for fast autocomplete suggestions.
 * Used for book title, author, and member name search.
 *
 * Insert: O(m)  where m = word length
 * Search: O(m)
 * Prefix suggestions: O(m + k) where k = number of results
 */
public class SearchTrie {

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
        String fullWord = null;
    }

    private final TrieNode root = new TrieNode();

    /** Insert a word into the Trie. */
    public synchronized void insert(String word) {
        if (word == null || word.isBlank()) return;
        String lower = word.toLowerCase().trim();
        TrieNode node = root;
        for (char c : lower.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd = true;
        node.fullWord = word.trim();
    }

    /** Insert all words from a list. */
    public synchronized void insertAll(List<String> words) {
        if (words != null) {
            words.forEach(this::insert);
        }
    }

    /** Clear all entries. */
    public synchronized void clear() {
        root.children.clear();
    }

    /**
     * Returns up to {@code limit} suggestions that start with the given prefix.
     * Case-insensitive and thread-safe.
     */
    public synchronized List<String> suggest(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        if (prefix == null || prefix.isBlank()) return results;

        String lower = prefix.toLowerCase().trim();
        TrieNode node = root;

        for (char c : lower.toCharArray()) {
            if (!node.children.containsKey(c)) return results;
            node = node.children.get(c);
        }

        collectWords(node, results, limit);
        return results;
    }

    private void collectWords(TrieNode node, List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.isEnd && node.fullWord != null) results.add(node.fullWord);
        for (TrieNode child : node.children.values()) {
            if (results.size() >= limit) return;
            collectWords(child, results, limit);
        }
    }

    public synchronized boolean isEmpty() {
        return root.children.isEmpty();
    }
}
