package com.subgraph.orchid.stream;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.circuits.ExitCircuit;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

public class OpenExitStreamTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(OpenExitStreamTask.class);
    private final ExitCircuit circuit;
    private final StreamExitRequest exitRequest;

    public OpenExitStreamTask(ExitCircuit circuit, StreamExitRequest exitRequest) {
        this.circuit = circuit;
        this.exitRequest = exitRequest;
    }

    @Override
    public void run() {
        log.debug("Attempting to open stream to {}", exitRequest);
        try {
            exitRequest.setCompletedSuccessfully(tryOpenExitStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exitRequest.setInterrupted();
        } catch (TimeoutException e) {
            circuit.markForClose();
            exitRequest.setCompletedTimeout();
        } catch (StreamConnectFailedException e) {
            circuit.markForClose();
            exitRequest.setStreamOpenFailure(e.getReason());
        }
    }

    private Stream tryOpenExitStream() throws InterruptedException, TimeoutException, StreamConnectFailedException {
        if (exitRequest.isAddressTarget()) {
            return circuit.openExitStream(exitRequest.getAddress(), exitRequest.port(), exitRequest.getStreamTimeout());
        } else {
            return circuit.openExitStream(exitRequest.getHostname(), exitRequest.port(), exitRequest.getStreamTimeout());
        }
    }
}