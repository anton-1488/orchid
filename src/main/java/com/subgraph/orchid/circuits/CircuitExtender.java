package com.subgraph.orchid.circuits;

import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.cells.enums.CellCommand;
import com.subgraph.orchid.cells.enums.RelayCellCommand;
import com.subgraph.orchid.cells.impls.CellImpl;
import com.subgraph.orchid.cells.impls.RelayCellImpl;
import com.subgraph.orchid.crypto.TorNTorKeyAgreement;
import com.subgraph.orchid.crypto.TorStreamCipher;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircuitExtender {
    private static final Logger log = LoggerFactory.getLogger(CircuitExtender.class);
    private static final int CIPHER_KEY_LEN = TorStreamCipher.KEY_LEN;
    private static final int NTOR_ONIONSKIN_LEN = 2 * 32 + 20; // CURVE25519_PUBKEY_LEN * 2 + DIGEST_LEN

    private final CircuitImpl circuit;

    public CircuitExtender(CircuitImpl circuit) {
        this.circuit = circuit;
    }

    public CircuitNode extendTo(Router targetRouter) {
        if (circuit.getCircuitLength() == 0) {
            log.debug("Creating first hop to {}", targetRouter.getNickname());
            return createFirstHop(targetRouter);
        } else {
            log.debug("Extending circuit to {} through existing nodes", targetRouter.getNickname());
            NTorCircuitExtender extender = new NTorCircuitExtender(this, targetRouter);
            return extender.extendTo();
        }
    }

    private @NotNull CircuitNode createFirstHop(@NotNull Router targetRouter) {
        TorNTorKeyAgreement kex = new TorNTorKeyAgreement(targetRouter.getIdentityHash(), targetRouter.getNTorOnionKey());
        sendCreateCell(kex);
        return receiveAndProcessCreateResponse(targetRouter, kex);
    }

    private CircuitNode extendThroughCircuit(Router targetRouter) {
        NTorCircuitExtender ext = new NTorCircuitExtender(this, targetRouter);
        return ext.extendTo();
    }

    public CircuitNode createNewNode(Router router, byte[] keyMaterial, byte[] verifyDigest) {
        CircuitNode node = CircuitNodeImpl.createNode(router, circuit.getFinalCircuitNode(), keyMaterial, verifyDigest);
        circuit.appendNode(node);
        log.debug("Added new circuit node for {}", router.getNickname());
        return node;
    }

    private void sendCreateCell(@NotNull TorNTorKeyAgreement kex) {
        Cell cell = new CellImpl(circuit.getCircuitId(), CellCommand.CREATE2);
        cell.getCellWriter().putByteArray(kex.createOnionSkin());
        circuit.sendCell(cell);
    }

    private @NotNull CircuitNode receiveAndProcessCreateResponse(Router targetRouter, TorNTorKeyAgreement kex) {
        Cell cell = circuit.receiveControlCellResponse();
        if (cell == null) {
            throw new TorException("Timeout building circuit waiting for CREATED2 response from " + targetRouter);
        }
        return processCreatedCell(targetRouter, cell, kex);
    }

    private @NotNull CircuitNode processCreatedCell(Router targetRouter, @NotNull Cell cell, @NotNull TorNTorKeyAgreement kex) {
        byte[] keyMaterial = new byte[CircuitNodeCryptoState.KEY_MATERIAL_SIZE];
        byte[] verifyHash = new byte[32]; // SHA-256

        if (!kex.deriveKeysFromHandshakeResponse(cell.getCellBytes(), keyMaterial, verifyHash)) {
            throw new TorException("Failed to derive keys from handshake with " + targetRouter);
        }

        CircuitNode node = CircuitNodeImpl.createNode(targetRouter, circuit.getFinalCircuitNode(), keyMaterial, verifyHash);
        circuit.appendNode(node);
        log.debug("Successfully extended circuit to {}", targetRouter.getNickname());
        return node;
    }

    public void sendRelayCell(RelayCell cell) {
        circuit.sendRelayCell(cell);
    }

    public RelayCell receiveRelayResponse(RelayCellCommand expectedCommand, Router extendTarget) {
        RelayCell cell = circuit.receiveRelayCell();
        if (cell == null) {
            throw new TorException("Timeout building circuit");
        }

        if (cell.getRelayCommand() == RelayCellCommand.TRUNCATED) {
            int code = cell.getCellReader().getByte() & 0xFF;
            String source = getNodeName(cell.getCircuitNode());
            throw new TorException("Error from " + source + " extending to " + extendTarget.getNickname() + ": " + code);
        }

        if (cell.getRelayCommand() != expectedCommand) {
            throw new TorException("Expected " + expectedCommand + " but got " + cell.getRelayCommand());
        }

        return cell;
    }

    public RelayCell createRelayCell(RelayCellCommand command) {
        return new RelayCellImpl(circuit.getFinalCircuitNode(), circuit.getCircuitId(), 0, command, true);
    }

    private String getNodeName(CircuitNode node) {
        return node != null && node.getRouter() != null ? node.getRouter().getNickname() : "(null)";
    }
}