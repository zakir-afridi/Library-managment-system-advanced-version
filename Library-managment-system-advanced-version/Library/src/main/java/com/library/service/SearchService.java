package com.library.service;

import com.library.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Autocomplete search service backed by a Trie.
 * Loads all book titles and author names from DB on first call,
 * then serves suggestions from memory.
 */
public class SearchService {

    private static SearchService instance;
    private static final Logger LOG = Logger.getLogger(SearchService.class.getName());

    private final SearchTrie titleTrie  = new SearchTrie();
    private final SearchTrie authorTrie = new SearchTrie();
    private final SearchTrie memberTrie = new SearchTrie();

    private boolean loaded = false;

    private SearchService() {}

    public static synchronized SearchService getInstance() {
        if (instance == null) instance = new SearchService();
        return instance;
    }

    /** Force reload from DB (call after bulk import). */
    public void reload() {
        titleTrie.clear();
        authorTrie.clear();
        memberTrie.clear();
        loaded = false;
        load();
    }

    private void load() {
        if (loaded) return;
        try (Connection c = DatabaseConnection.getConnection()) {
            // Load book titles and authors
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT book_name, author FROM books")) {
                while (rs.next()) {
                    titleTrie.insert(rs.getString("book_name"));
                    authorTrie.insert(rs.getString("author"));
                }
            }
            // Load member names
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT name FROM members")) {
                while (rs.next()) memberTrie.insert(rs.getString("name"));
            }
            loaded = true;
        } catch (SQLException e) {
            LOG.warning("SearchService load error: " + e.getMessage());
        }
    }

    public List<String> suggestTitles(String prefix, int limit) {
        load();
        return titleTrie.suggest(prefix, limit);
    }

    public List<String> suggestAuthors(String prefix, int limit) {
        load();
        return authorTrie.suggest(prefix, limit);
    }

    public List<String> suggestMembers(String prefix, int limit) {
        load();
        return memberTrie.suggest(prefix, limit);
    }

    /** Combined suggestions from titles + authors. */
    public List<String> suggestBooks(String prefix, int limit) {
        load();
        List<String> results = new ArrayList<>(titleTrie.suggest(prefix, limit / 2 + 1));
        results.addAll(authorTrie.suggest(prefix, limit / 2 + 1));
        return results.subList(0, Math.min(results.size(), limit));
    }

    public void addTitle(String title)   { titleTrie.insert(title); }
    public void addAuthor(String author) { authorTrie.insert(author); }
    public void addMember(String name)   { memberTrie.insert(name); }
}
