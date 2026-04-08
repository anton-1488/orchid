package com.subgraph.orchid.geoip;

import com.subgraph.orchid.data.IPv4Address;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Objects;

public class CountryCodeService {
    private final static String DATABASE_FILENAME = "data/GeoIP.dat";
    private final static int COUNTRY_BEGIN = 16776960;
    private final static int STANDARD_RECORD_LENGTH = 3;
    private final static int MAX_RECORD_LENGTH = 4;
    private final static List<String> COUNTRY_CODES = CountryCodesParser.parseCountryCodes();

    private final static CountryCodeService DEFAULT_INSTANCE = new CountryCodeService();
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CountryCodeService.class);

    public static CountryCodeService getInstance() {
        return DEFAULT_INSTANCE;
    }

    private final byte[] database;

    public CountryCodeService() {
        this.database = loadDatabase();
    }

    private static byte[] loadDatabase() {
        try (InputStream input = CountryCodeService.class.getResourceAsStream("/data/" + DATABASE_FILENAME)) {
            Objects.requireNonNull(input);
            return loadEntireStream(input);
        } catch (IOException e) {
            log.warn("IO error reading database file for country code lookups");
            return null;
        }
    }

    private static byte[] loadEntireStream(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        input.transferTo(output);
        return output.toByteArray();
    }

    public String getCountryCodeForAddress(IPv4Address address) {
        return COUNTRY_CODES.get(seekCountry(address));
    }

    private int seekCountry(IPv4Address address) {
        if (database == null) {
            return 0;
        }
        final byte[] record = new byte[2 * MAX_RECORD_LENGTH];
        final int[] x = new int[2];
        final long ip = address.getAddressData() & 0xFFFFFFFFL;

        int offset = 0;
        for (int depth = 31; depth >= 0; depth--) {
            loadRecord(offset, record);

            x[0] = unpackRecordValue(record, 0);
            x[1] = unpackRecordValue(record, 1);

            int xx = ((ip & (1L << depth)) > 0) ? (x[1]) : (x[0]);

            if (xx >= COUNTRY_BEGIN) {
                final int idx = xx - COUNTRY_BEGIN;
                if (idx > COUNTRY_CODES.size()) {
                    log.warn("Invalid index calculated looking up country code record for ({}) idx = {}", address, idx);
                    return 0;
                } else {
                    return idx;
                }
            } else {
                offset = xx;
            }

        }
        log.warn("No record found looking up country code record for ({})", address);
        return 0;
    }

    private void loadRecord(int offset, byte[] recordBuffer) {
        final int dbOffset = 2 * STANDARD_RECORD_LENGTH * offset;
        System.arraycopy(database, dbOffset, recordBuffer, 0, recordBuffer.length);
    }

    private int unpackRecordValue(byte[] record, int idx) {
        final int valueOffset = idx * STANDARD_RECORD_LENGTH;
        int value = 0;
        for (int i = 0; i < STANDARD_RECORD_LENGTH; i++) {
            int octet = record[valueOffset + i] & 0xFF;
            value += (octet << (i * 8));
        }
        return value;
    }
}