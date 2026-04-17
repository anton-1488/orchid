package com.subgraph.orchid.path.filters;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.InetAddressUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilterFactory {
    private final static Pattern NETMASK_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)/(\\d+)$");
    private final static Pattern ADDRESS_BITS_PATTERN = Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+)/(\\d+)$");
    private final static Pattern IDENTITY_PATTERN = Pattern.compile("^[A-Fa-f0-9]{40}$");
    private final static Pattern COUNTRYCODE_PATTERN = Pattern.compile("^\\{([A-Za-z]{2})}$");
    private final static Pattern ROUTERNAME_PATTERN = Pattern.compile("^\\w{1,19}$");

    private FilterFactory() {

    }

    public static @Nullable RouterFilter createFilterFor(String s) {
        if (isAddressString(s)) {
            return newAddressFilter(s);
        } else if (isCountryCodeString(s)) {
            return newCountryCodeFilter(s);
        } else if (isIdentityString(s)) {
            return newIdentityFilter(s);
        } else if (isNameString(s)) {
            return newNameFilter(s);
        }
        return null;
    }

    @Contract("_ -> new")
    private static @NotNull RouterFilter newAddressFilter(String s) {
        Matcher matcher = ADDRESS_BITS_PATTERN.matcher(s);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        InetAddress address = InetAddressUtils.createAddressFromString(matcher.group(1));
        int bits = Integer.parseInt(matcher.group(2));
        return new MaskFilter(Objects.requireNonNull(address), bits);
    }

    @Contract(pure = true)
    private static @NotNull RouterFilter newIdentityFilter(String identity) {
        if (!isIdentityString(identity)) {
            throw new IllegalArgumentException();
        }

        return router -> router.getIdentityHash().equals(HexDigest.createFromString(identity));
    }

    @Contract(pure = true)
    private static @NotNull RouterFilter newNameFilter(String name) {
        if (!isNameString(name)) {
            throw new IllegalArgumentException();
        }

        return router -> router.getNickname().equals(name);
    }

    @Contract(pure = true)
    private static @NotNull RouterFilter newCountryCodeFilter(String cc) {
        Matcher matcher = COUNTRYCODE_PATTERN.matcher(cc);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }

        return router -> router.getCountryCode().equalsIgnoreCase(cc);
    }

    private static boolean isAddressString(String s) {
        Matcher matcher = NETMASK_PATTERN.matcher(s);
        if (!matcher.matches()) {
            return false;
        }
        try {
            for (int i = 1; i < 5; i++) {
                if (!isValidOctetString(matcher.group(i))) {
                    return false;
                }
            }
            return isValidMaskValue(matcher.group(5));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isValidOctetString(String s) {
        int n = Integer.parseInt(s);
        return n >= 0 && n <= 255;
    }

    private static boolean isValidMaskValue(String s) {
        int n = Integer.parseInt(s);
        return n > 0 && n <= 32;
    }

    private static boolean isIdentityString(String s) {
        return IDENTITY_PATTERN.matcher(s).matches();
    }

    private static boolean isCountryCodeString(String s) {
        return COUNTRYCODE_PATTERN.matcher(s).matches();
    }

    private static boolean isNameString(String s) {
        return ROUTERNAME_PATTERN.matcher(s).matches();
    }
}