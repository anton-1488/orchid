package com.subgraph.orchid.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import com.subgraph.orchid.data.IPv4Address;
import com.subgraph.orchid.exceptions.TorException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Objects;

public class CountryCodeService {
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

    private DatabaseReader loadDatabase() {
        try (InputStream input = CountryCodeService.class.getResourceAsStream(DATABASE_FILENAME)) {
            Objects.requireNonNull(input);
            return new DatabaseReader.Builder(input).build();
        } catch (IOException e) {
            throw new TorException(e);
        }
    }

    public String getCountryCodeForAddress(IPv4Address address) {
        if (reader == null) {
            return "??";
        }

        try {
            byte[] bytes = new byte[4];
            int addr = address.getAddressData();
            bytes[0] = (byte) ((addr >>> 24) & 0xFF);
            bytes[1] = (byte) ((addr >>> 16) & 0xFF);
            bytes[2] = (byte) ((addr >>> 8) & 0xFF);
            bytes[3] = (byte) (addr & 0xFF);

            InetAddress inetAddress = InetAddress.getByAddress(bytes);
            return reader.tryCountry(inetAddress).map(CountryResponse::country).map(Country::isoCode).orElse("??");
        } catch (AddressNotFoundException e) {
            return "??";
        } catch (Exception e) {
            log.debug("Country lookup failed for {}: {}", address, e.getMessage());
            return "??";
        }
    }
}