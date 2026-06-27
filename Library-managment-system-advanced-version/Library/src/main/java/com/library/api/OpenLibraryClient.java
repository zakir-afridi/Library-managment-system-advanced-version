package com.library.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.model.BookMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Client for the Open Library Books API (100% free, no API key required).
 *
 * API docs: https://openlibrary.org/dev/docs/api
 * Endpoint: https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data
 *
 * Features:
 *  - Fetches title, author, publisher, year, pages, cover URL, description
 *  - 5-second connect + read timeout
 *  - Returns Optional.empty() on any failure (network, parse, 4xx/5xx)
 *  - Uses Java 21 built-in HttpClient (zero external dependency)
 */
public final class OpenLibraryClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenLibraryClient.class);

    private static final String BASE_URL   =
        "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data";
    private static final String COVERS_URL =
        "https://covers.openlibrary.org/b/isbn/%s-L.jpg";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper JSON = new ObjectMapper();

    private OpenLibraryClient() {}

    /**
     * Fetch book metadata by ISBN-10 or ISBN-13.
     *
     * @param isbn raw ISBN (hyphens/spaces are stripped automatically)
     * @return populated BookMetadata, or empty on failure
     */
    public static Optional<BookMetadata> fetchByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) return Optional.empty();
        String clean = isbn.replaceAll("[\\s\\-]", "");
        if (clean.length() != 10 && clean.length() != 13) return Optional.empty();

        String url = String.format(BASE_URL, clean);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "LibraCore-Pro/3.0 (library-management)")
                    .GET()
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                LOG.warn("OpenLibrary returned HTTP {} for ISBN {}", resp.statusCode(), clean);
                return Optional.empty();
            }

            String body = resp.body();
            if (body == null || body.isBlank() || body.equals("{}")) {
                LOG.debug("OpenLibrary: no data for ISBN {}", clean);
                return Optional.empty();
            }

            JsonNode root  = JSON.readTree(body);
            String   key   = "ISBN:" + clean;
            JsonNode entry = root.get(key);
            if (entry == null || entry.isNull()) return Optional.empty();

            BookMetadata meta = new BookMetadata();
            meta.setIsbn(clean);

            // Title
            if (entry.has("title")) meta.setTitle(entry.get("title").asText());

            // Authors (array)
            if (entry.has("authors")) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode a : entry.get("authors")) {
                    if (a.has("name")) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(a.get("name").asText());
                    }
                }
                meta.setAuthor(sb.toString());
            }

            // Publishers (array)
            if (entry.has("publishers")) {
                JsonNode pubs = entry.get("publishers");
                if (pubs.isArray() && pubs.size() > 0 && pubs.get(0).has("name"))
                    meta.setPublisher(pubs.get(0).get("name").asText());
            }

            // Publish date
            if (entry.has("publish_date"))
                meta.setPublishDate(entry.get("publish_date").asText());

            // Page count
            if (entry.has("number_of_pages"))
                meta.setPageCount(entry.get("number_of_pages").asInt());

            // Description
            if (entry.has("notes")) {
                JsonNode notes = entry.get("notes");
                meta.setDescription(notes.isTextual() ? notes.asText() : notes.path("value").asText());
            }

            // Cover image URL (large)
            if (entry.has("cover") && entry.get("cover").has("large")) {
                meta.setCoverUrl(entry.get("cover").get("large").asText());
            } else {
                meta.setCoverUrl(String.format(COVERS_URL, clean));
            }

            // Subjects / categories
            if (entry.has("subjects") && entry.get("subjects").isArray()) {
                JsonNode subs = entry.get("subjects");
                if (subs.size() > 0 && subs.get(0).has("name"))
                    meta.setCategory(subs.get(0).get("name").asText());
            }

            LOG.info("OpenLibrary: fetched metadata for ISBN {} — {}", clean, meta.getTitle());
            return Optional.of(meta);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("OpenLibrary request interrupted for ISBN {}", clean);
            return Optional.empty();
        } catch (Exception e) {
            LOG.warn("OpenLibrary fetch failed for ISBN {}: {}", clean, e.getMessage());
            return Optional.empty();
        }
    }

    /** Returns the URL for a book cover image (does not download it). */
    public static String coverUrl(String isbn) {
        if (isbn == null) return null;
        return String.format(COVERS_URL, isbn.replaceAll("[\\s\\-]", ""));
    }
}
