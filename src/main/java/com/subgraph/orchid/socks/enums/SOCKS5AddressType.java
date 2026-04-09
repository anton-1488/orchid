package com.subgraph.orchid.socks.enums;

public enum SOCKS5AddressType {
    ADDRESS_IPV4(1),
    ADDRESS_HOSTNAME(3),
    ADDRESS_IPV6(4);

    private final int type;

    SOCKS5AddressType(int type) {
        this.type = type;
    }

    public static SOCKS5AddressType ofType(int tpe) {
        for (SOCKS5AddressType type : values()) {
            if (type.type == tpe) {
                return type;
            }
        }
        throw new EnumConstantNotPresentException(SOCKS5AddressType.class, String.valueOf(true));
    }

    public int getAddressType() {
        return type;
    }
}