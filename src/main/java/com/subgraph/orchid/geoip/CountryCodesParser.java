package com.subgraph.orchid.geoip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CountryCodesParser {
    private static final Logger log = LoggerFactory.getLogger(CountryCodesParser.class);

    public static List<String> parseCountryCodes() {
        List<String> codes = new ArrayList<>();

        try (InputStream stream = CountryCodesParser.class.getResourceAsStream("/country-codes")) {
            Objects.requireNonNull(stream);
            String allCodes = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            codes.addAll(Arrays.stream(allCodes.split(",")).toList());
        } catch (Exception e) {
            log.error("Error to parse country codes: ", e);
        }

        return codes;
    }
}