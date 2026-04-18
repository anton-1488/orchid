package com.subgraph.orchid.circuits;

import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.cells.io.CellWriter;
import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.crypto.TorStreamCipher;
import com.subgraph.orchid.data.HexDigest;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CircuitNodeCryptoState {
    public final static int KEY_MATERIAL_SIZE = TorMessageDigest.TOR_DIGEST_SIZE * 2 + TorStreamCipher.KEY_LEN * 2;

    @Contract("_, _ -> new")
    public static @NotNull CircuitNodeCryptoState createFromKeyMaterial(byte[] keyMaterial, byte[] verifyDigest) {
        return new CircuitNodeCryptoState(keyMaterial, verifyDigest);
    }

    private final HexDigest checksumDigest;
    private final TorMessageDigest forwardDigest;
    private final TorMessageDigest backwardDigest;
    private final TorStreamCipher forwardCipher;
    private final TorStreamCipher backwardCipher;

    private static byte @NotNull [] extractDigestBytes(byte[] keyMaterial, int offset) {
        byte[] digestBytes = new byte[TorMessageDigest.TOR_DIGEST_SIZE];
        System.arraycopy(keyMaterial, offset, digestBytes, 0, TorMessageDigest.TOR_DIGEST_SIZE);
        return digestBytes;
    }

    private static byte @NotNull [] extractCipherKey(byte[] keyMaterial, int offset) {
        byte[] keyBytes = new byte[TorStreamCipher.KEY_LEN];
        System.arraycopy(keyMaterial, offset, keyBytes, 0, TorStreamCipher.KEY_LEN);
        return keyBytes;
    }

    private CircuitNodeCryptoState(byte[] keyMaterial, byte[] verifyDigest) {
        checksumDigest = HexDigest.createFromDigestBytes(verifyDigest);
        int offset = 0;

        forwardDigest = new TorMessageDigest();
        forwardDigest.update(extractDigestBytes(keyMaterial, offset));
        offset += TorMessageDigest.TOR_DIGEST_SIZE;

        backwardDigest = new TorMessageDigest();
        backwardDigest.update(extractDigestBytes(keyMaterial, offset));
        offset += TorMessageDigest.TOR_DIGEST_SIZE;

        forwardCipher = TorStreamCipher.createFromKeyBytes(extractCipherKey(keyMaterial, offset));
        offset += TorStreamCipher.KEY_LEN;

        backwardCipher = TorStreamCipher.createFromKeyBytes(extractCipherKey(keyMaterial, offset));
    }

    public boolean verifyPacketDigest(HexDigest packetDigest) {
        return checksumDigest.equals(packetDigest);
    }

    public void encryptForwardCell(@NotNull Cell cell) {
        forwardCipher.encrypt(cell.getCellBytes(), Cell.CELL_HEADER_LEN, Cell.CELL_PAYLOAD_LEN);
    }

    public boolean decryptBackwardCell(@NotNull Cell cell) {
        backwardCipher.encrypt(cell.getCellBytes(), Cell.CELL_HEADER_LEN, Cell.CELL_PAYLOAD_LEN);
        return isRecognizedCell(cell);
    }

    public void updateForwardDigest(@NotNull Cell cell) {
        forwardDigest.update(cell.getCellBytes(), Cell.CELL_HEADER_LEN, Cell.CELL_PAYLOAD_LEN);
    }

    public byte[] getForwardDigestBytes() {
        return forwardDigest.getDigestBytes();
    }

    private boolean isRecognizedCell(@NotNull Cell cell) {
        if (cell.getCellReader().getShortAt(RelayCell.RECOGNIZED_OFFSET) != 0) {
            return false;
        }

        byte[] digest = extractRelayDigest(cell);
        byte[] peek = backwardDigest.peekDigest(cell.getCellBytes(), Cell.CELL_HEADER_LEN, Cell.CELL_PAYLOAD_LEN);

        for (int i = 0; i < 4; i++) {
            if (digest[i] != peek[i]) {
                replaceRelayDigest(cell, digest);
                return false;
            }
        }
        backwardDigest.update(cell.getCellBytes(), Cell.CELL_HEADER_LEN, Cell.CELL_PAYLOAD_LEN);
        replaceRelayDigest(cell, digest);
        return true;
    }

    private byte @NotNull [] extractRelayDigest(@NotNull Cell cell) {
        CellWriter writer = cell.getCellWriter();
        byte[] digest = new byte[4];
        for (int i = 0; i < 4; i++) {
            digest[i] = cell.getCellReader().getByteAt(i + RelayCell.DIGEST_OFFSET);
            writer.putByteAt(i + RelayCell.DIGEST_OFFSET, (byte) 0);
        }
        return digest;
    }

    private void replaceRelayDigest(Cell cell, byte[] digest) {
        for (int i = 0; i < 4; i++) {
            cell.getCellWriter().putByteAt(i + RelayCell.DIGEST_OFFSET, (byte) (digest[i] & 0xFF));
        }
    }
}