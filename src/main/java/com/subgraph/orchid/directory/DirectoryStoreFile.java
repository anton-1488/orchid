package com.subgraph.orchid.directory;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.document.Document;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DirectoryStoreFile implements AutoCloseable {
    private final static ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private static final Logger log = LoggerFactory.getLogger(DirectoryStoreFile.class);

    private final TorConfig config;
    private final String cacheFilename;
    private RandomAccessFile openFile;
    private boolean openFileFailed;
    private boolean directoryCreationFailed;

    public DirectoryStoreFile(TorConfig config, String cacheFilename) {
        this.config = config;
        this.cacheFilename = cacheFilename;
    }

    public synchronized void writeData(ByteBuffer data) {
        File tempFile = createTempFile();
        FileOutputStream fos = openFileOutputStream(tempFile);
        if (fos == null) {
            return;
        }
        try {
            writeAllToChannel(fos.getChannel(), data);
            quietClose(fos);
            installTempFile(tempFile);
        } catch (IOException e) {
            log.warn("I/O error writing to temporary cache file(write data) {}: ", tempFile, e);
        } finally {
            quietClose(fos);
            boolean deleted = tempFile.delete();
            log.debug("File deleted after write data: {}", deleted);
        }
    }

    public synchronized void writeDocuments(List<? extends Document> documents) {
        File tempFile = createTempFile();
        FileOutputStream fos = openFileOutputStream(tempFile);
        if (fos == null) {
            return;
        }
        try {
            writeDocumentsToChannel(fos.getChannel(), documents);
            quietClose(fos);
            installTempFile(tempFile);
        } catch (IOException e) {
            log.warn("I/O error writing to temporary cache file(write doc) {}:", tempFile, e);
        } finally {
            quietClose(fos);
            boolean deleted = tempFile.delete();
            log.debug("File deleted after write doc: {}", deleted);
        }
    }

    public synchronized void appendDocuments(List<? extends Document> documents) {
        if (!ensureOpened()) {
            return;
        }
        try {
            final FileChannel channel = openFile.getChannel();
            channel.position(channel.size());
            writeDocumentsToChannel(channel, documents);
            channel.force(true);
        } catch (IOException e) {
            log.warn("I/O error writing to cache file {}", cacheFilename);
        }
    }

    public ByteBuffer loadContents() {
        if (!(fileExists() && ensureOpened())) {
            return EMPTY_BUFFER;
        }

        try {
            return readAllFromChannel(openFile.getChannel());
        } catch (IOException e) {
            log.warn("I/O error reading cache file {}:", cacheFilename, e);
            return EMPTY_BUFFER;
        }
    }

    private @Nullable FileOutputStream openFileOutputStream(File file) {
        try {
            createDirectoryIfMissing();
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            log.warn("Failed to open file {}:", file, e);
            return null;
        }
    }

    private ByteBuffer readAllFromChannel(@NotNull FileChannel channel) throws IOException {
        channel.position(0);
        ByteBuffer buffer = createBufferForChannel(channel);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) == -1) {
                log.warn("Unexpected EOF reading from cache file");
                return EMPTY_BUFFER;
            }
        }
        buffer.rewind();
        return buffer;
    }

    private @NotNull ByteBuffer createBufferForChannel(@NotNull FileChannel channel) throws IOException {
        return ByteBuffer.allocateDirect((int) channel.size());
    }

    @Override
    public void close() {
        if (openFile != null) {
            quietClose(openFile);
            openFile = null;
        }
    }

    private boolean fileExists() {
        return getFile().exists();
    }

    private boolean ensureOpened() {
        if (openFileFailed) {
            return false;
        }
        if (openFile != null) {
            return true;
        }
        openFile = openFile();
        return openFile != null;
    }

    private synchronized @Nullable RandomAccessFile openFile() {
        try {
            File f = new File(config.dataDirectory().toFile(), cacheFilename);
            createDirectoryIfMissing();
            return new RandomAccessFile(f, "rw");
        } catch (FileNotFoundException e) {
            openFileFailed = true;
            log.warn("Failed to open cache file {}", cacheFilename);
            return null;
        }
    }

    private synchronized void installTempFile(File tempFile) {
        close();
        File target = getFile();
        if (target.exists() && !target.delete()) {
            log.warn("Failed to delete file {}", target);
        }
        if (!tempFile.renameTo(target)) {
            log.warn("Failed to rename temp file {} to {}", tempFile, target);
        }
        boolean deleted = tempFile.delete();
        log.debug("File deleted after install temp file: {}", deleted);
        ensureOpened();
    }

    private @NotNull File createTempFile() {
        int n = TorRandom.nextInt();
        File f = new File(config.dataDirectory().toFile(), cacheFilename + n);
        f.deleteOnExit();
        return f;
    }

    private void writeDocumentsToChannel(FileChannel channel, @NotNull List<? extends Document> documents) throws IOException {
        for (Document d : documents) {
            writeAllToChannel(channel, d.getRawDocumentBytes());
        }
    }

    private void writeAllToChannel(WritableByteChannel channel, @NotNull ByteBuffer data) throws IOException {
        data.rewind();
        while (data.hasRemaining()) {
            channel.write(data);
        }
    }

    private void quietClose(@NotNull Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log.error("Error to quietClose: ", e);
        }
    }

    @Contract(" -> new")
    private @NotNull File getFile() {
        return new File(config.dataDirectory().toFile(), cacheFilename);
    }

    public void remove() {
        close();
        boolean deleted = getFile().delete();
        log.debug("File removed: {}", deleted);
    }

    private void createDirectoryIfMissing() {
        if (directoryCreationFailed) {
            return;
        }
        Path dd = config.dataDirectory();
        try {
            if (Files.notExists(dd)) {
                if (Files.notExists(Files.createDirectories(dd))) {
                    directoryCreationFailed = true;
                    log.warn("Failed to create data directory {}", dd);
                }
            }
        } catch (Exception e) {
            log.warn("Error to create directory if missing: ", e);
        }
    }
}