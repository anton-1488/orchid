package com.subgraph.orchid.stream;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.exceptions.OpenFailedException;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PendingExitStreams {
    private final Set<StreamExitRequest> pendingRequests = new CopyOnWriteArraySet<>();

    public PendingExitStreams() {
    }

    public Stream openExitStream(InetAddress address, int port) throws InterruptedException, OpenFailedException {
        StreamExitRequest request = new StreamExitRequest(address, port);
        return openExitStreamByRequest(request);
    }

    public Stream openExitStream(String hostname, int port) throws InterruptedException, OpenFailedException {
        StreamExitRequest request = new StreamExitRequest(hostname, port);
        return openExitStreamByRequest(request);
    }

    private Stream openExitStreamByRequest(@NotNull StreamExitRequest request) throws InterruptedException, OpenFailedException {
        pendingRequests.add(request);
        try {
            return handleRequest(request);
        } finally {
            pendingRequests.remove(request);
        }
    }

    private Stream handleRequest(@NotNull StreamExitRequest request) throws InterruptedException, OpenFailedException {
        while (true) {
            try {
                return request.getFuture().get(request.getStreamTimeout(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (!(cause instanceof TimeoutException || cause instanceof StreamConnectFailedException)) {
                    throw new OpenFailedException("Stream open failed", cause);
                }
                if (request.getRetryCount() > 5) {
                    throw new OpenFailedException("Too many retries", cause);
                }
                request.resetForRetry();
            } catch (TimeoutException e) {
                request.resetForRetry();
            }
        }
    }


    public synchronized List<StreamExitRequest> getUnreservedPendingRequests() {
        List<StreamExitRequest> result = new ArrayList<>();
        for (StreamExitRequest request : pendingRequests) {
            if (!request.isReserved()) {
                result.add(request);
            }
        }
        return result;
    }
}