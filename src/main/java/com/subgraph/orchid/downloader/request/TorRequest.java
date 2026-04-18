package com.subgraph.orchid.downloader.request;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record TorRequest(URI path, String method, Map<String, String> headers, String body, Duration timeout) {
    private static final Map<String, String> DEFAULT_HEADERS = new HashMap<>();
    public static final TorRequest BRIDGE_DESCRIPTION = TorRequest.get("/tor/server/authority");

    static {
        DEFAULT_HEADERS.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        DEFAULT_HEADERS.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        DEFAULT_HEADERS.put("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        DEFAULT_HEADERS.put("upgrade-insecure-requests", "1");
        DEFAULT_HEADERS.put("Cache-Control", "max-age=0");
    }

    public TorRequest {
        Objects.requireNonNull(path);
        Objects.requireNonNull(method);
        Objects.requireNonNull(headers);
        Objects.requireNonNull(timeout);
        if (method.equals("POST")) {
            Objects.requireNonNull(body);
        }
    }

    @Contract("_ -> new")
    public static @NotNull TorRequest get(String url) {
        return get(URI.create(url));
    }

    @Contract("_ -> new")
    public static @NotNull TorRequest get(URI uri) {
        return new TorRequest(uri, "GET", DEFAULT_HEADERS, null, Duration.ofSeconds(30));
    }

    @Contract("_ -> new")
    public static @NotNull TorRequest head(String url) {
        return head(URI.create(url));
    }

    @Contract("_ -> new")
    public static @NotNull TorRequest head(URI uri) {
        return new TorRequest(uri, "HEAD", DEFAULT_HEADERS, null, Duration.ofSeconds(30));
    }

    @Contract("_, _ -> new")
    public static @NotNull TorRequest post(String url, String body) {
        return post(URI.create(url), body);
    }

    @Contract("_, _ -> new")
    public static @NotNull TorRequest post(URI url, String body) {
        return new TorRequest(url, "POST", DEFAULT_HEADERS, body, Duration.ofSeconds(30));
    }
}