package com.subgraph.orchid.data;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class InetAddressUtils {
    private static final Logger log = LoggerFactory.getLogger(InetAddressUtils.class);

    public static @Nullable InetAddress createAddressFromString(String address) {
        try {
            return InetAddress.getByName(address);
        } catch (Exception e) {
            log.error("Error to create InetAddress from string: ", e);
            return null;
        }
    }
}