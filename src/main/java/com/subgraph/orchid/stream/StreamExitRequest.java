package com.subgraph.orchid.stream;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.data.exitpolicy.ExitTarget;
import com.subgraph.orchid.exceptions.OpenFailedException;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StreamExitRequest implements ExitTarget {

    private enum CompletionStatus {NOT_COMPLETED, SUCCESS, TIMEOUT, STREAM_OPEN_FAILURE, EXIT_FAILURE, INTERRUPTED}

    private final boolean isAddress;
    private final InetAddress address;
    private final String hostname;
    private final int port;
    private volatile CompletableFuture<Stream> future = new CompletableFuture<>();

    private CompletionStatus completionStatus;
    private Stream stream;
    private final AtomicInteger streamOpenFailReason = new AtomicInteger();
    private final AtomicBoolean isReserved = new AtomicBoolean();
    private final AtomicInteger retryCount = new AtomicInteger();
    private final AtomicLong specificTimeout = new AtomicLong();

    public StreamExitRequest(InetAddress address, int port) {
        this(true, "", address, port);
    }

    public StreamExitRequest(String hostname, int port) {
        this(false, hostname, null, port);
    }

    private StreamExitRequest(boolean isAddress, String hostname, InetAddress address, int port) {
        this.isAddress = isAddress;
        this.hostname = hostname;
        this.address = address;
        this.port = port;
        this.completionStatus = CompletionStatus.NOT_COMPLETED;
    }

    @Override
    public boolean isAddressTarget() {
        return isAddress;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public int port() {
        return port;
    }

    public void setStreamTimeout(long timeout) {
        specificTimeout.set(timeout);
    }

    public synchronized long getStreamTimeout() {
        if (specificTimeout.get() > 0) {
            return specificTimeout.get();
        } else if (retryCount.get() < 2) {
            return 10 * 1000;
        } else {
            return 15 * 1000;
        }
    }

    public void setCompletedTimeout() {
        newStatus(CompletionStatus.TIMEOUT);
    }

    public void setExitFailed() {
        newStatus(CompletionStatus.EXIT_FAILURE);
    }

    public void setStreamOpenFailure(int reason) {
        streamOpenFailReason.set(reason);
        newStatus(CompletionStatus.STREAM_OPEN_FAILURE);
    }

    public void setCompletedSuccessfully(Stream stream) {
        this.stream = stream;
        newStatus(CompletionStatus.SUCCESS);
    }

    public void setInterrupted() {
        newStatus(CompletionStatus.INTERRUPTED);
    }

    private synchronized void newStatus(CompletionStatus newStatus) {
        if (completionStatus != CompletionStatus.NOT_COMPLETED) {
            throw new IllegalStateException("Attempt to set completion state to " + newStatus + " while status is " + completionStatus);
        }
        completionStatus = newStatus;
        switch (newStatus) {
            case SUCCESS -> future.complete(stream);
            case TIMEOUT -> future.completeExceptionally(new TimeoutException());
            case EXIT_FAILURE -> future.completeExceptionally(new OpenFailedException("Failure at exit node"));
            case STREAM_OPEN_FAILURE ->
                    future.completeExceptionally(new StreamConnectFailedException("Connection failed", streamOpenFailReason.get()));
            case INTERRUPTED -> future.completeExceptionally(new InterruptedException());
        }
    }

    public synchronized void resetForRetry() {
        streamOpenFailReason.set(0);
        completionStatus = CompletionStatus.NOT_COMPLETED;
        retryCount.incrementAndGet();
        isReserved.set(false);
        future = new CompletableFuture<>();
    }

    public boolean isCompleted() {
        return completionStatus != CompletionStatus.NOT_COMPLETED;
    }

    public synchronized boolean reserveRequest() {
        if (isReserved.get()) {
            return false;
        }
        isReserved.set(true);
        return true;
    }

    public CompletableFuture<Stream> getFuture() {
        return future;
    }

    public int getRetryCount() {
        return retryCount.get();
    }

    public boolean isReserved() {
        return isReserved.get();
    }

    @Override
    public String toString() {
        if (isAddress) {
            return address + ":" + port;
        } else {
            return hostname + ":" + port;
        }
    }
}
