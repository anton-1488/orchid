package com.subgraph.orchid.circuits;

import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class CircuitNodeImpl implements CircuitNode {
    private final static int CIRCWINDOW_START = 1000;
    private final static int CIRCWINDOW_INCREMENT = 100;

    public static @NotNull CircuitNode createAnonymous(CircuitNode previous, byte[] keyMaterial, byte[] verifyDigest) {
        return createNode(null, previous, keyMaterial, verifyDigest);
    }

    public static @NotNull CircuitNode createFirstHop(Router r, byte[] keyMaterial, byte[] verifyDigest) {
        return createNode(r, null, keyMaterial, verifyDigest);
    }

    public static @NotNull CircuitNode createNode(Router r, CircuitNode previous, byte[] keyMaterial, byte[] verifyDigest) {
        CircuitNodeCryptoState cs = CircuitNodeCryptoState.createFromKeyMaterial(keyMaterial, verifyDigest);
        return new CircuitNodeImpl(r, previous, cs);
    }

    private final Router router;
    private final CircuitNodeCryptoState cryptoState;
    private final CircuitNode previousNode;
    private final Object windowLock = new Object();

    private final AtomicInteger packageWindow = new AtomicInteger(CIRCWINDOW_START);
    private final AtomicInteger deliverWindow = new AtomicInteger(CIRCWINDOW_START);

    private CircuitNodeImpl(Router router, CircuitNode previous, CircuitNodeCryptoState cryptoState) {
        previousNode = previous;
        this.router = router;
        this.cryptoState = cryptoState;
    }

    @Override
    public Router getRouter() {
        return router;
    }

    @Override
    public CircuitNode getPreviousNode() {
        return previousNode;
    }

    @Override
    public void encryptForwardCell(RelayCell cell) {
        cryptoState.encryptForwardCell(cell);
    }

    @Override
    public boolean decryptBackwardCell(Cell cell) {
        return cryptoState.decryptBackwardCell(cell);
    }

    @Override
    public void updateForwardDigest(RelayCell cell) {
        cryptoState.updateForwardDigest(cell);
    }

    @Override
    public byte[] getForwardDigestBytes() {
        return cryptoState.getForwardDigestBytes();
    }

    @Override
    public void decrementDeliverWindow() {
        deliverWindow.decrementAndGet();
    }

    @Override
    public synchronized boolean considerSendingSendme() {
        if (deliverWindow.get() <= (CIRCWINDOW_START - CIRCWINDOW_INCREMENT)) {
            deliverWindow.addAndGet(CIRCWINDOW_INCREMENT);
            return true;
        }
        return false;
    }

    @Override
    public void waitForSendWindow() {
        waitForSendWindow(false);
    }

    @Override
    public void waitForSendWindowAndDecrement() {
        waitForSendWindow(true);
    }

    private synchronized void waitForSendWindow(boolean decrement) {
        while (packageWindow.get() == 0) {
            try {
                windowLock.wait();
            } catch (InterruptedException e) {
                throw new TorException("Thread interrupted while waiting for circuit send window");
            }
        }
        if (decrement) {
            packageWindow.decrementAndGet();
        }
    }

    @Override
    public void incrementSendWindow() {
        packageWindow.addAndGet(CIRCWINDOW_INCREMENT);
        windowLock.notifyAll();
    }

    @Override
    public String toString() {
        return "CircuitNodeImpl{" +
                "router=" + router +
                ", cryptoState=" + cryptoState +
                ", previousNode=" + previousNode +
                ", packageWindow=" + packageWindow +
                ", deliverWindow=" + deliverWindow +
                '}';
    }
}