package com.subgraph.orchid.data;

import com.subgraph.orchid.GuardEntry;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

public class GuardEntryImpl implements GuardEntry {
    private static final Logger log = LoggerFactory.getLogger(GuardEntryImpl.class);
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final Directory directory;
    private final StateFile stateFile;
    private final String nickname;
    private final String identity;

    private String version;
    private Instant createdTime;
    private boolean isAdded;
    private Instant unlistedSince;
    private Instant downSince;
    private Instant lastConnectAttempt;

    public GuardEntryImpl(Directory directory, StateFile stateFile, String nickname, String identity) {
        this.directory = directory;
        this.stateFile = stateFile;
        this.nickname = nickname;
        this.identity = identity;
    }

    public void setAddedFlag() {
        isAdded = true;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setCreatedTime(Instant time) {
        this.createdTime = time;
    }

    public synchronized void setUnlistedSince(Instant time) {
        this.unlistedSince = time;
    }

    public synchronized void setDownSince(Instant downSince, Instant lastTried) {
        this.downSince = downSince;
        this.lastConnectAttempt = lastTried;
    }

    @Override
    public boolean isAdded() {
        return isAdded;
    }

    @Override
    public synchronized void markAsDown() {
        Instant now = Instant.now();
        if (downSince == null) {
            downSince = now;
        }
        lastConnectAttempt = now;
        if (isAdded) {
            stateFile.writeFile();
        }
    }

    @Override
    public synchronized void clearDownSince() {
        downSince = null;
        lastConnectAttempt = null;
        if (isAdded) {
            stateFile.writeFile();
        }
    }

    public synchronized void clearUnlistedSince() {
        unlistedSince = null;
        if (isAdded) {
            stateFile.writeFile();
        }
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Instant getCreatedTime() {
        return createdTime;
    }

    @Override
    public Instant getDownSince() {
        return downSince;
    }

    @Override
    public Instant getLastConnectAttempt() {
        return lastConnectAttempt;
    }

    @Override
    public Instant getUnlistedSince() {
        return unlistedSince;
    }

    @Override
    public Router getRouterForEntry() {
        HexDigest id = HexDigest.createFromString(identity);
        return directory.getRouterByIdentity(id);
    }

    @Override
    public boolean testCurrentlyUsable() {
        Router router = getRouterForEntry();
        boolean isUsable = router != null && router.isValid() && router.isPossibleGuard() && router.isRunning();
        if (isUsable) {
            markUsable();
        } else {
            markUnusable();
        }
        return isUsable;
    }

    private synchronized void markUsable() {
        if (unlistedSince != null) {
            unlistedSince = null;
            if (isAdded) {
                stateFile.writeFile();
            }
        }
    }

    private synchronized void markUnusable() {
        if (unlistedSince == null) {
            unlistedSince = Instant.now();
            if (isAdded) {
                stateFile.writeFile();
            }
        }
    }

    public synchronized String writeToString() {
        StringBuilder sb = new StringBuilder();
        appendEntryGuardLine(sb);
        appendEntryGuardAddedBy(sb);
        if (downSince != null) {
            appendEntryGuardDownSince(sb);
        }
        if (unlistedSince != null) {
            appendEntryGuardUnlistedSince(sb);
        }
        return sb.toString();
    }

    private void appendEntryGuardLine(@NotNull StringBuilder sb) {
        sb.append(StateFile.KEYWORD_ENTRY_GUARD).append(" ").append(nickname).append(" ").append(identity).append(LINE_SEPARATOR);
    }

    private void appendEntryGuardAddedBy(@NotNull StringBuilder sb) {
        sb.append(StateFile.KEYWORD_ENTRY_GUARD_ADDED_BY).append(" ").append(identity).append(" ").append(version).append(" ").append(stateFile.formatDate(createdTime)).append(LINE_SEPARATOR);
    }

    private void appendEntryGuardDownSince(StringBuilder sb) {
        if (downSince == null) return;

        sb.append(StateFile.KEYWORD_ENTRY_GUARD_DOWN_SINCE).append(" ").append(stateFile.formatDate(downSince));
        if (lastConnectAttempt != null) {
            sb.append(" ").append(stateFile.formatDate(lastConnectAttempt));
        }
        sb.append(LINE_SEPARATOR);
    }

    private void appendEntryGuardUnlistedSince(StringBuilder sb) {
        if (unlistedSince == null) return;
        sb.append(StateFile.KEYWORD_ENTRY_GUARD_UNLISTED_SINCE).append(" ").append(stateFile.formatDate(unlistedSince)).append(LINE_SEPARATOR);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuardEntryImpl that)) return false;
        return Objects.equals(identity, that.identity) && Objects.equals(nickname, that.nickname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity, nickname);
    }

    @Override
    public String toString() {
        return "GuardEntryImpl{" +
                "directory=" + directory +
                ", stateFile=" + stateFile +
                ", nickname='" + nickname + '\'' +
                ", identity='" + identity + '\'' +
                ", version='" + version + '\'' +
                ", createdTime=" + createdTime +
                ", isAdded=" + isAdded +
                ", unlistedSince=" + unlistedSince +
                ", downSince=" + downSince +
                ", lastConnectAttempt=" + lastConnectAttempt +
                '}';
    }
}