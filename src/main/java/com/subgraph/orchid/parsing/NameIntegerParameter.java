package com.subgraph.orchid.parsing;

import org.jetbrains.annotations.NotNull;

public record NameIntegerParameter(String name, int value) {
    @Override
    public @NotNull String toString() {
        return name + "=" + value;
    }
}