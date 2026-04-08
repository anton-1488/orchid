package com.subgraph.orchid.circuits.cells.enums;

public enum AddressType {
    /**
     * HOSTNAME Address Type.
     */
    HOSTNAME(0x00),

    /**
     * IPv4 Address Type.
     */
    IPv4(0x04),

    /**
     * IPv6 Address Type.
     */
    IPv6(0x06);

    private final int value;

    AddressType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AddressType fromValue(int value) {
        for (AddressType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown address type: " + value);
    }
}