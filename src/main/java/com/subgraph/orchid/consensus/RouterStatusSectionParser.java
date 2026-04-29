package com.subgraph.orchid.consensus;

import com.subgraph.orchid.consensus.ConsensusDocumentParser.DocumentSection;
import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.document.ConsensusDocument.ConsensusFlavor;
import com.subgraph.orchid.exceptions.TorParsingException;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterStatusSectionParser extends ConsensusDocumentSectionParser {
    private static final Logger log = LoggerFactory.getLogger(RouterStatusSectionParser.class);
    private RouterStatusImpl currentEntry = null;

    public RouterStatusSectionParser(DocumentFieldParser parser, ConsensusDocumentImpl document) {
        super(parser, document);
    }

    @Override
    void parseLine(@NotNull DocumentKeyword keyword) {
        if (!keyword.equals(DocumentKeyword.R)) {
            assertCurrentEntry();
        }
        switch (keyword) {
            case R:
                parseFirstLine();
                break;
            case S:
                parseFlags();
                break;
            case V:
                parseVersion();
                break;
            case W:
                parseBandwidth();
                break;
            case P:
                parsePortList();
                break;
            case M:
                parseMicrodescriptorHash();
                break;
        }
    }

    private void assertCurrentEntry() {
        if (currentEntry == null) {
            throw new TorParsingException("Router status entry must begin with an 'r' line");
        }
    }

    private void addCurrentEntry() {
        assertCurrentEntry();
        document.addRouterStatusEntry(currentEntry);
        currentEntry = null;
    }

    private void parseFirstLine() {
        try {
            if (currentEntry != null) {
                throw new TorParsingException("Unterminated router status entry.");
            }
            currentEntry = new RouterStatusImpl();
            currentEntry.setNickname(fieldParser.parseNickname());
            currentEntry.setIdentity(parseBase64Digest());
            if (document.getFlavor() != ConsensusFlavor.MICRODESC) {
                currentEntry.setDigest(parseBase64Digest());
            }
            currentEntry.setPublicationTime(fieldParser.parseTimestamp());
            currentEntry.setAddress(fieldParser.parseAddress());
            currentEntry.setRouterPort(fieldParser.parsePort());
            currentEntry.setDirectoryPort(fieldParser.parsePort());
        } catch (Exception e) {
            log.error("Error parse first line: ", e);
        }
    }

    @Contract(" -> new")
    private @NotNull HexDigest parseBase64Digest() {
        return HexDigest.createFromDigestBytes(fieldParser.parseBase64Data());
    }

    private void parseFlags() {
        while (fieldParser.argumentsRemaining() > 0) {
            currentEntry.addFlag(fieldParser.parseString());
        }
    }

    private void parseVersion() {
        currentEntry.setVersion(fieldParser.parseConcatenatedString());
    }

    private void parseBandwidth() {
        while (fieldParser.argumentsRemaining() > 0) {
            String[] parts = fieldParser.parseString().split("=");
            if (parts.length == 2) {
                parseBandwidthItem(parts[0], fieldParser.parseInteger(parts[1]));
            }
        }
        if (document.getFlavor() == ConsensusFlavor.MICRODESC) {
            addCurrentEntry();
        }
    }

    private void parseBandwidthItem(@NotNull String key, int value) {
        if (key.equals("Bandwidth")) {
            currentEntry.setEstimatedBandwidth(value);
        } else if (key.equals("Measured")) {
            currentEntry.setMeasuredBandwidth(value);
        }
    }

    private void parsePortList() {
        if (document.getFlavor() == ConsensusFlavor.MICRODESC) {
            throw new TorParsingException("'p' line does not appear in consensus flavor 'microdesc'");
        }
        String arg = fieldParser.parseString();
        if (arg.equals("accept")) {
            currentEntry.setAcceptedPorts(fieldParser.parseString());
        } else if (arg.equals("reject")) {
            currentEntry.setRejectedPorts(fieldParser.parseString());
        }
        addCurrentEntry();
    }

    private void parseMicrodescriptorHash() {
        if (document.getFlavor() != ConsensusFlavor.MICRODESC) {
            throw new TorParsingException("'m' line is invalid unless consensus flavor is microdesc");
        }
        byte[] hashBytes = fieldParser.parseBase64Data();
        if (hashBytes.length != TorMessageDigest.TOR_DIGEST256_SIZE) {
            throw new TorParsingException("'m' line has incorrect digest size " + hashBytes.length + " != " + TorMessageDigest.TOR_DIGEST256_SIZE);
        }
        currentEntry.setMicrodescriptorDigest(HexDigest.createFromDigestBytes(hashBytes));
    }

    @Override
    public String getNextStateKeyword() {
        return "directory-footer";
    }

    @Override
    public DocumentSection getSection() {
        return DocumentSection.ROUTER_STATUS;
    }

    public DocumentSection nextSection() {
        return DocumentSection.FOOTER;
    }
}