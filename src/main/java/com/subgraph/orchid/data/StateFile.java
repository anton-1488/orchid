package com.subgraph.orchid.data;

import com.subgraph.orchid.GuardEntry;
import com.subgraph.orchid.Tor;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.directory.DirectoryStore;
import com.subgraph.orchid.directory.DirectoryStore.CacheFile;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class StateFile {
    public static final String KEYWORD_ENTRY_GUARD = "EntryGuard";
    public static final String KEYWORD_ENTRY_GUARD_ADDED_BY = "EntryGuardAddedBy";
    public static final String KEYWORD_ENTRY_GUARD_DOWN_SINCE = "EntryGuardDownSince";
    public static final String KEYWORD_ENTRY_GUARD_UNLISTED_SINCE = "EntryGuardUnlistedSince";

    private final static int DATE_LENGTH = 19;
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(StateFile.class);

    private final List<GuardEntryImpl> guardEntries = new CopyOnWriteArrayList<>();

    private static class Line {
        final String line;
        int offset;

        public Line(String line) {
            this.line = line;
            offset = 0;
        }

        private boolean hasChars() {
            return offset < line.length();
        }

        private char getChar() {
            return line.charAt(offset);
        }

        private void incrementOffset(int n) {
            offset += n;
            if (offset > line.length()) {
                offset = line.length();
            }
        }

        private void skipWhitespace() {
            while (hasChars() && Character.isWhitespace(getChar())) {
                offset += 1;
            }
        }

        public @Nullable String nextToken() {
            skipWhitespace();
            if (!hasChars()) {
                return null;
            }

            StringBuilder token = new StringBuilder();
            while (hasChars() && !Character.isWhitespace(getChar())) {
                token.append(getChar());
                offset += 1;
            }
            return token.toString();
        }

        public @Nullable Instant parseDate() {
            skipWhitespace();
            if (!hasChars()) {
                return null;
            }
            TemporalAccessor date = dateFormat.parse(line.substring(offset));
            incrementOffset(DATE_LENGTH);
            return Instant.from(date);
        }
    }

    public String formatDate(Instant date) {
        return dateFormat.format(date);
    }

    private final DirectoryStore directoryStore;
    private final Directory directory;

    public StateFile(DirectoryStore store, Directory directory) {
        this.directoryStore = store;
        this.directory = directory;
    }

    public GuardEntry createGuardEntryFor(Router router) {
        GuardEntryImpl entry = new GuardEntryImpl(directory, this, router.getNickname(), router.getIdentityHash().toString());
        String version = Tor.TOR_IMPLEMENTATION + "-" + Tor.TOR_VERSION;
        entry.setVersion(version);
        /*
         * "Choose expiry time smudged over the last month."
         *
         * See add_an_entry_guard() in entrynodes.c
         */
        long createTime = Instant.now().toEpochMilli() - (TorRandom.nextInt(3600 * 24 * 30) * 1000L);
        entry.setCreatedTime(Instant.ofEpochMilli(createTime));
        return entry;
    }

    public synchronized List<GuardEntry> getGuardEntries() {
        return List.copyOf(guardEntries);
    }

    public synchronized void removeGuardEntry(GuardEntry entry) {
        guardEntries.remove(entry);
        writeFile();
    }

    public void addGuardEntry(GuardEntry entry) {
        addGuardEntry(entry, true);
    }

    private synchronized void addGuardEntry(GuardEntry entry, boolean writeFile) {
        if (guardEntries.contains(entry)) {
            return;
        }
        GuardEntryImpl impl = (GuardEntryImpl) entry;
        guardEntries.add(impl);
        impl.setAddedFlag();
        if (writeFile) {
            writeFile();
        }
    }

    public void writeFile() {
        directoryStore.writeData(CacheFile.STATE, getFileContents());
    }

    public synchronized ByteBuffer getFileContents() {
        StringBuilder sb = new StringBuilder();
        for (GuardEntryImpl entry : guardEntries) {
            sb.append(entry.writeToString());
        }
        return ByteBuffer.wrap(sb.toString().getBytes(Tor.getDefaultCharset()));
    }

    public synchronized void parseBuffer(ByteBuffer buffer) {
        guardEntries.clear();
        loadGuardEntries(buffer);
    }

    private void loadGuardEntries(ByteBuffer buffer) {
        GuardEntryImpl currentEntry = null;
        while (true) {
            Line line = readLine(buffer);
            if (line == null) {
                addEntryIfValid(currentEntry);
                return;
            }
            currentEntry = processLine(line, currentEntry);
        }
    }

    private GuardEntryImpl processLine(@NotNull Line line, GuardEntryImpl current) {
        String keyword = line.nextToken();
        if (keyword == null) {
            return current;
        } else if (keyword.equals(KEYWORD_ENTRY_GUARD)) {
            addEntryIfValid(current);
            GuardEntryImpl newEntry = processEntryGuardLine(line);
            if (newEntry == null) {
                return current;
            } else {
                return newEntry;
            }
        } else if (keyword.equals(KEYWORD_ENTRY_GUARD_ADDED_BY)) {
            processEntryGuardAddedBy(line, current);
            return current;
        } else if (keyword.equals(KEYWORD_ENTRY_GUARD_DOWN_SINCE)) {
            processEntryGuardDownSince(line, current);
            return current;
        } else if (keyword.equals(KEYWORD_ENTRY_GUARD_UNLISTED_SINCE)) {
            processEntryGuardUnlistedSince(line, current);
            return current;
        } else {
            return current;
        }
    }

    private @Nullable GuardEntryImpl processEntryGuardLine(@NotNull Line line) {
        String name = line.nextToken();
        String identity = line.nextToken();
        if (name == null || name.isEmpty() || identity == null || identity.isEmpty()) {
            log.warn("Failed to parse EntryGuard line: {}", line.line);
            return null;
        }
        return new GuardEntryImpl(directory, this, name, identity);
    }

    private void processEntryGuardAddedBy(Line line, GuardEntryImpl current) {
        if (current == null) {
            log.warn("EntryGuardAddedBy line seen before EntryGuard in state file");
            return;
        }
        String identity = line.nextToken();
        String version = line.nextToken();
        Instant created = line.parseDate();
        if (identity == null || identity.isEmpty() || version == null || version.isEmpty() || created == null) {
            log.warn("Missing EntryGuardAddedBy field in state file");
            return;
        }
        current.setVersion(version);
        current.setCreatedTime(created);
    }

    private void processEntryGuardDownSince(Line line, GuardEntryImpl current) {
        if (current == null) {
            log.warn("EntryGuardDownSince line seen before EntryGuard in state file");
            return;
        }

        Instant downSince = line.parseDate();
        Instant lastTried = line.parseDate();
        if (downSince == null) {
            log.warn("Failed to parse date field in EntryGuardDownSince line in state file");
            return;
        }
        current.setDownSince(downSince, lastTried);
    }

    private void processEntryGuardUnlistedSince(Line line, GuardEntryImpl current) {
        if (current == null) {
            log.warn("EntryGuardUnlistedSince line seen before EntryGuard in state file");
            return;
        }
        Instant unlistedSince = line.parseDate();
        if (unlistedSince == null) {
            log.warn("Failed to parse date field in EntryGuardUnlistedSince line in state file");
            return;
        }
        current.setUnlistedSince(unlistedSince);
    }

    private void addEntryIfValid(GuardEntryImpl entry) {
        if (isValidEntry(entry)) {
            addGuardEntry(entry, false);
        }
    }

    private boolean isValidEntry(GuardEntryImpl entry) {
        return entry != null && entry.getNickname() != null && entry.getIdentity() != null && entry.getVersion() != null && entry.getCreatedTime() != null;
    }

    private @Nullable Line readLine(@NotNull ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            char c = (char) (buffer.get() & 0xFF);
            if (c == '\n') {
                return new Line(sb.toString());
            } else if (c != '\r') {
                sb.append(c);
            }
        }
        return new Line(sb.toString());
    }
}