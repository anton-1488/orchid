package com.subgraph.orchid.document;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.directory.DirectoryStore;
import com.subgraph.orchid.directory.DirectoryStore.CacheFile;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.parsing.DocumentParsingResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class DescriptorCache<T extends Descriptor> {
    private static final Logger log = LoggerFactory.getLogger(DescriptorCache.class);
    private final DescriptorCacheData<T> data;
    private final DirectoryStore store;
    private final ScheduledExecutorService rebuildExecutor = Executors.newSingleThreadScheduledExecutor();
    private final CacheFile cacheFile;
    private final CacheFile journalFile;

    private int droppedBytes;
    private int journalLength;
    private int cacheLength;
    private boolean initiallyLoaded;

    public DescriptorCache(DirectoryStore store, CacheFile cacheFile, CacheFile journalFile) {
        this.data = new DescriptorCacheData<>();
        this.store = store;
        this.cacheFile = cacheFile;
        this.journalFile = journalFile;
        startRebuildTask();
    }

    public synchronized void initialLoad() {
        if (initiallyLoaded) {
            return;
        }
        reloadCache();
    }

    public void shutdown() {
        rebuildExecutor.shutdownNow();
    }

    public T getDescriptor(HexDigest digest) {
        return data.findByDigest(digest);
    }

    public synchronized void addDescriptors(@NotNull List<T> descriptors) {
        List<T> journalDescriptors = new ArrayList<>();
        int duplicateCount = 0;
        for (T d : descriptors) {
            if (data.addDescriptor(d)) {
                if (d.getCacheLocation() == Descriptor.CacheLocation.NOT_CACHED) {
                    journalLength += d.getBodyLength();
                    journalDescriptors.add(d);
                }
            } else {
                duplicateCount += 1;
            }
        }
        if (!journalDescriptors.isEmpty()) {
            store.appendDocumentList(journalFile, journalDescriptors);
        }
        if (duplicateCount > 0) {
            log.info("Duplicate descriptors added to journal, count = {}", duplicateCount);
        }
    }

    public void addDescriptor(T d) {
        List<T> descriptors = new ArrayList<>();
        descriptors.add(d);
        addDescriptors(descriptors);
    }

    private synchronized void clearMemoryCache() {
        data.clear();
        journalLength = 0;
        cacheLength = 0;
        droppedBytes = 0;
    }

    private synchronized void reloadCache() {
        clearMemoryCache();
        ByteBuffer[] buffers = loadCacheBuffers();
        loadCacheFileBuffer(buffers[0]);
        loadJournalFileBuffer(buffers[1]);
        if (!initiallyLoaded) {
            initiallyLoaded = true;
        }
    }

    private ByteBuffer @NotNull [] loadCacheBuffers() {
        ByteBuffer[] buffers = new ByteBuffer[2];
        buffers[0] = store.loadCacheFile(cacheFile);
        buffers[1] = store.loadCacheFile(journalFile);
        return buffers;
    }

    private void loadCacheFileBuffer(@NotNull ByteBuffer buffer) {
        cacheLength = buffer.limit();
        if (cacheLength == 0) {
            return;
        }
        DocumentParser<T> parser = createDocumentParser(buffer);
        DocumentParsingResult<T> result = parser.parse();
        if (result.isOkay()) {
            for (T d : result.getParsedDocuments()) {
                d.setCacheLocation(Descriptor.CacheLocation.CACHED_CACHEFILE);
                data.addDescriptor(d);
            }
        }
    }

    private void loadJournalFileBuffer(@NotNull ByteBuffer buffer) {
        journalLength = buffer.limit();
        if (journalLength == 0) {
            return;
        }
        DocumentParser<T> parser = createDocumentParser(buffer);
        DocumentParsingResult<T> result = parser.parse();
        if (result.isOkay()) {
            int duplicateCount = 0;
            log.debug("Loaded {} descriptors from journal", result.getParsedDocuments().size());
            for (T d : result.getParsedDocuments()) {
                d.setCacheLocation(Descriptor.CacheLocation.CACHED_JOURNAL);
                if (!data.addDescriptor(d)) {
                    duplicateCount += 1;
                }
            }
            if (duplicateCount > 0) {
                log.info("Found {} duplicate descriptors in journal file", duplicateCount);
            }
        } else if (result.isInvalid()) {
            log.warn("Invalid descriptor data parsing from journal file : {}", result.getMessage());
        } else if (result.isError()) {
            log.warn("Error parsing descriptors from journal file : {}", result.getMessage());
        }
    }

    abstract protected DocumentParser<T> createDocumentParser(ByteBuffer buffer);

    private @NotNull ScheduledFuture<?> startRebuildTask() {
        return rebuildExecutor.scheduleAtFixedRate(this::maybeRebuildCache, 5, 30, TimeUnit.MINUTES);
    }

    private synchronized void maybeRebuildCache() {
        if (!initiallyLoaded) {
            return;
        }
        droppedBytes += data.cleanExpired();
        if (!shouldRebuildCache()) {
            return;
        }
        rebuildCache();
    }

    private boolean shouldRebuildCache() {
        if (journalLength < 16384) {
            return false;
        }
        if (droppedBytes > (journalLength + cacheLength) / 3) {
            return true;
        }
        return journalLength > (cacheLength / 2);
    }

    private synchronized void rebuildCache() {
        store.writeDocumentList(cacheFile, data.getAllDescriptors());
        store.removeCacheFile(journalFile);
        reloadCache();
    }
}