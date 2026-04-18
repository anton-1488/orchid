package com.subgraph.orchid.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import com.subgraph.orchid.exceptions.TorException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Objects;

public class CountryCodeService {
    // FROM: https://raw.githubusercontent.com/wp-statistics/GeoLite2-Country/refs/heads/master/GeoLite2-Country.mmdb.gz
    private final static String DATABASE_FILENAME = "/data/GeoLite2-Country.mmdb";
    private final static CountryCodeService DEFAULT_INSTANCE = new CountryCodeService();
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CountryCodeService.class);

    public static CountryCodeService getInstance() {
        return DEFAULT_INSTANCE;
    }

    private final DatabaseReader reader;

    public CountryCodeService() {
        this.reader = loadDatabase();
    }

    private @NotNull DatabaseReader loadDatabase() {
        try (InputStream input = CountryCodeService.class.getResourceAsStream(DATABASE_FILENAME)) {
            Objects.requireNonNull(input);
            return new DatabaseReader.Builder(input).build();
        } catch (IOException e) {
            throw new TorException(e);
        }
    }

    public String getCountryCodeForAddress(InetAddress address) {
        if (reader == null || address == null) {
            return "??";
        }

        try {
            return reader.tryCountry(address)
                    .map(CountryResponse::country)
                    .map(Country::isoCode)
                    .orElse("??");
        } catch (AddressNotFoundException e) {
            return "??";
        } catch (Exception e) {
            log.debug("Country lookup failed for {}: {}", address, e.getMessage());
            return "??";
        }
    }

    public String getCountryCodeForAddress(String hostname) {
        try {
            return getCountryCodeForAddress(InetAddress.getByName(hostname));
        } catch (Exception e) {
            return "??";
        }
    }
}