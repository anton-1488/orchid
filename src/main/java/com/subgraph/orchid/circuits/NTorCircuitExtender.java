package com.subgraph.orchid.circuits;

import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.cells.enums.RelayCellCommand;
import com.subgraph.orchid.cells.io.CellReader;
import com.subgraph.orchid.cells.io.CellWriter;
import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.crypto.TorNTorKeyAgreement;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NTorCircuitExtender {
    private static final Logger log = LoggerFactory.getLogger(NTorCircuitExtender.class);
    private static final int NTOR_REPLY_LEN = 64;

    private final CircuitExtender extender;
    private final Router router;
    private final TorNTorKeyAgreement kex;

    public NTorCircuitExtender(CircuitExtender extender, @NotNull Router router) {
        this.extender = extender;
        this.router = router;
        this.kex = new TorNTorKeyAgreement(router.getIdentityHash(), router.getNTorOnionKey());
    }

    public CircuitNode extendTo() {
        byte[] onion = kex.createOnionSkin();
        if (targetRouterSupportsExtend2()) {
            log.debug("Extending circuit to {} with NTor inside RELAY_EXTEND2", router.getNickname());
            return extendWithExtend2(onion);
        } else {
            log.debug("Extending circuit to {} with NTor inside RELAY_EXTEND", router.getNickname());
            return extendWithTunneledExtend(onion);
        }
    }

    private CircuitNode extendWithExtend2(byte[] onion) {
        RelayCell cell = createExtend2Cell(onion);
        extender.sendRelayCell(cell);
        RelayCell response = extender.receiveRelayResponse(RelayCellCommand.EXTENDED2, router);
        return processExtended2(response);
    }

    private CircuitNode extendWithTunneledExtend(byte[] onion) {
        RelayCell cell = createExtendCell(onion, kex.getNtorCreateMagic());
        extender.sendRelayCell(cell);
        RelayCell response = extender.receiveRelayResponse(RelayCellCommand.EXTENDED, router);
        return processExtended(response);
    }

    private boolean targetRouterSupportsExtend2() {
        return router.getNTorOnionKey() != null;
    }

    private @NotNull RelayCell createExtend2Cell(byte @NotNull [] ntorOnionskin) {
        RelayCell cell = extender.createRelayCell(RelayCellCommand.EXTEND2);
        CellWriter writer = cell.getCellWriter();
        writer.putByte((byte) 2);

        writer.putByte((byte) 0);
        writer.putByte((byte) 6);
        writer.putByteArray(router.getAddress().getAddress());
        writer.putShort((short) router.getOnionPort());

        writer.putByte((byte) 2);
        writer.putByte((byte) 20);
        writer.putByteArray(router.getIdentityHash().getRawBytes());

        writer.putShort((short) 0x0002);
        writer.putShort((short) ntorOnionskin.length);
        writer.putByteArray(ntorOnionskin);
        return cell;
    }

    private @NotNull RelayCell createExtendCell(byte[] ntorOnionskin, byte[] ntorMagic) {
        RelayCell cell = extender.createRelayCell(RelayCellCommand.EXTEND);
        CellWriter writer = cell.getCellWriter();
        writer.putByteArray(router.getAddress().getAddress());
        writer.putShort((short) router.getOnionPort());
        writer.putByteArray(ntorOnionskin);
        writer.putByteArray(router.getIdentityHash().getRawBytes());
        return cell;
    }

    private CircuitNode processExtended(@NotNull RelayCell cell) {
        byte[] payload = new byte[NTOR_REPLY_LEN];
        cell.getCellReader().getByteArray(payload);

        return processPayload(payload);
    }

    private CircuitNode processExtended2(@NotNull RelayCell cell) {
        CellReader reader = cell.getCellReader();

        int payloadLength = reader.getShort();
        if (payloadLength > reader.remaining()) {
            throw new TorException("Incorrect payload length value in RELAY_EXTENED2 cell");
        }
        byte[] payload = new byte[payloadLength];
        reader.getByteArray(payload);

        return processPayload(payload);
    }

    private @Nullable CircuitNode processPayload(byte[] payload) {
        byte[] keyMaterial = new byte[CircuitNodeCryptoState.KEY_MATERIAL_SIZE];
        byte[] verifyDigest = new byte[TorMessageDigest.TOR_DIGEST_SIZE];
        if (!kex.deriveKeysFromHandshakeResponse(payload, keyMaterial, verifyDigest)) {
            return null;
        }
        return extender.createNewNode(router, keyMaterial, verifyDigest);
    }
}