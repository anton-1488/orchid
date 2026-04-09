package com.subgraph.orchid;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Globals {
    public static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(VIRTUAL_EXECUTOR::close));
    }

    private Globals() {
        throw new UnsupportedOperationException();
    }
}