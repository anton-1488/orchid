package com.subgraph.orchid.data;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TrustedAuthoritiesParser {
    private static final Logger log = LoggerFactory.getLogger(TrustedAuthoritiesParser.class);
    private static final String DIR_SERVERS_PATH = "/data/directory-servers.list";

    private TrustedAuthoritiesParser() {
    }

    public static @NotNull List<String> loadDirServers() {
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TrustedAuthoritiesParser.class.getResourceAsStream(DIR_SERVERS_PATH)), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to load directory servers list", e);
            return List.of();
        }
    }
}