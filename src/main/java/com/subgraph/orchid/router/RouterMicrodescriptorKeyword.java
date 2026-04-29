package com.subgraph.orchid.router;

public enum RouterMicrodescriptorKeyword {
    ONION_KEY("onion-key", 0),
    NTOR_ONION_KEY("ntor-onion-key", 1),
    A("a", 1),
    FAMILY("family"),
    P("p", 2),
    UNKNOWN_KEYWORD("KEYWORD NOT FOUNE");

    public final static int VARIABLE_ARGUMENT_COUNT = -1;

    private final String keyword;
    private final int argumentCount;

    RouterMicrodescriptorKeyword(String keyword) {
        this(keyword, VARIABLE_ARGUMENT_COUNT);
    }

    RouterMicrodescriptorKeyword(String keyword, int argumentCount) {
        this.keyword = keyword;
        this.argumentCount = argumentCount;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    public static RouterMicrodescriptorKeyword findKeyword(String keyword) {
        for (RouterMicrodescriptorKeyword k : values()) {
            if (k.getKeyword().equals(keyword)) {
                return k;
            }
        }
        return UNKNOWN_KEYWORD;
    }
}