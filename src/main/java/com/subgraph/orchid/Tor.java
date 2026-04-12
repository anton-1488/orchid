package com.subgraph.orchid;

import com.subgraph.orchid.circuits.CircuitManager;
import com.subgraph.orchid.circuits.CircuitManagerImpl;
import com.subgraph.orchid.circuits.TorInitializationTracker;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.connections.ConnectionCache;
import com.subgraph.orchid.connections.ConnectionCacheImpl;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.directory.DirectoryImpl;
import com.subgraph.orchid.directory.DirectoryStore;
import com.subgraph.orchid.directory.downloader.DirectoryDownloaderImpl;
import com.subgraph.orchid.socks.SocksPortListener;
import com.subgraph.orchid.socks.SocksPortListenerImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The <code>Tor</code> class is a collection of static methods for instantiating
 * various subsystem modules.
 */
public class Tor {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Tor.class);
    public final static String TOR_IMPLEMENTATION = "PlovDev/Orchid";
    public final static String TOR_VERSION = "2.0";

    private final static Charset defaultCharset = createDefaultCharset();

    private static Charset createDefaultCharset() {
        return StandardCharsets.ISO_8859_1;
    }

    public static Charset getDefaultCharset() {
        return defaultCharset;
    }

    /**
     * Create and return a new <code>TorConfig</code> instance.
     *
     * @return A new <code>TorConfig</code> instance.
     * @see TorConfig
     */
    @Contract(" -> new")
    public static @NotNull TorConfig createConfig() {
        return new TorConfig();
    }

    @Contract(" -> new")
    public static @NotNull TorInitializationTracker createInitalizationTracker() {
        return new TorInitializationTracker();
    }

    /**
     * Create and return a new <code>Directory</code> instance.
     *
     * @param config This is a required dependency. You must create a <code>TorConfig</code> before
     *               calling this method to create a <code>Directory</code>
     * @return A new <code>Directory</code> instance.
     * @see Directory
     */
    @Contract("_, _ -> new")
    public static @NotNull Directory createDirectory(TorConfig config, DirectoryStore customDirectoryStore) {
        return new DirectoryImpl(config, customDirectoryStore);
    }

    @Contract("_, _ -> new")
    public static @NotNull ConnectionCache createConnectionCache(TorConfig config, TorInitializationTracker tracker) {
        return new ConnectionCacheImpl(config, tracker);
    }

    /**
     * Create and return a new <code>CircuitManager</code> instance.
     *
     * @return A new <code>CircuitManager</code> instance.
     * @see CircuitManager
     */
    @Contract("_, _, _, _, _ -> new")
    public static @NotNull CircuitManager createCircuitManager(TorConfig config, DirectoryDownloaderImpl directoryDownloader, Directory directory, ConnectionCache connectionCache, TorInitializationTracker tracker) {
        return new CircuitManagerImpl(config, directoryDownloader, directory, connectionCache, tracker);
    }

    /**
     * Create and return a new <code>SocksPortListener</code> instance.
     *
     * @param circuitManager This is a required dependency.  You must create a <code>CircuitManager</code>
     *                       before calling this method to create a <code>SocksPortListener</code>.
     * @return A new <code>SocksPortListener</code> instance.
     * @see SocksPortListener
     */
    @Contract("_, _ -> new")
    public static @NotNull SocksPortListener createSocksPortListener(TorConfig config, CircuitManager circuitManager) {
        return new SocksPortListenerImpl(config, circuitManager);
    }

    /**
     * Create and return a new <code>DirectoryDownloader</code> instance.
     *
     * @return A new <code>DirectoryDownloader</code> instance.
     * @see DirectoryDownloaderImpl
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull DirectoryDownloaderImpl createDirectoryDownloader(TorConfig config, TorInitializationTracker initializationTracker) {
        return new DirectoryDownloaderImpl(config, initializationTracker);
    }
}