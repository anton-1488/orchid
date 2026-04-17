package com.subgraph.orchid.directory;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.document.Document;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectoryStoreImpl implements DirectoryStore {
    private final TorConfig config;
    private final Map<CacheFile, DirectoryStoreFile> fileMap;

    public DirectoryStoreImpl(TorConfig config) {
        this.config = config;
        this.fileMap = new HashMap<>();
    }

    @Override
    public ByteBuffer loadCacheFile(CacheFile cacheFile) {
        return getStoreFile(cacheFile).loadContents();
    }

    @Override
    public void writeData(CacheFile cacheFile, ByteBuffer data) {
        getStoreFile(cacheFile).writeData(data);
    }

    @Override
    public void writeDocument(CacheFile cacheFile, Document document) {
        writeDocumentList(cacheFile, List.of(document));
    }

    @Override
    public void writeDocumentList(CacheFile cacheFile, List<? extends Document> documents) {
        getStoreFile(cacheFile).writeDocuments(documents);
    }

    @Override
    public void appendDocumentList(CacheFile cacheFile, List<? extends Document> documents) {
        getStoreFile(cacheFile).appendDocuments(documents);
    }

    @Override
    public void removeCacheFile(CacheFile cacheFile) {
        getStoreFile(cacheFile).remove();
    }

    @Override
    public void removeAllCacheFiles() {
        for (CacheFile cf : CacheFile.values()) {
            getStoreFile(cf).remove();
        }
    }

    private DirectoryStoreFile getStoreFile(CacheFile cacheFile) {
        if (!fileMap.containsKey(cacheFile)) {
            fileMap.put(cacheFile, new DirectoryStoreFile(config, cacheFile.getFilename()));
        }
        return fileMap.get(cacheFile);
    }
}