package com.subgraph.orchid.consensus;

import com.subgraph.orchid.consensus.ConsensusDocumentParser.DocumentSection;
import com.subgraph.orchid.document.ConsensusDocument.ConsensusFlavor;
import com.subgraph.orchid.exceptions.TorParsingException;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import com.subgraph.orchid.parsing.NameIntegerParameter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreambleSectionParser extends ConsensusDocumentSectionParser {
    private final static int CURRENT_DOCUMENT_VERSION = 3;
    private static final Logger log = LoggerFactory.getLogger(PreambleSectionParser.class);
    private final AtomicBoolean isFirstLine = new AtomicBoolean(true);

    public PreambleSectionParser(DocumentFieldParser parser, ConsensusDocumentImpl document) {
        super(parser, document);
    }

    @Override
    public String getNextStateKeyword() {
        return "dir-source";
    }

    @Override
    public DocumentSection getSection() {
        return DocumentSection.PREAMBLE;
    }

    @Override
    public DocumentSection nextSection() {
        return DocumentSection.AUTHORITY;
    }

    @Override
    public void parseLine(DocumentKeyword keyword) {
        if (isFirstLine.get()) {
            parseFirstLine(keyword);
        } else {
            processKeyword(keyword);
        }
    }

    private void processKeyword(@NotNull DocumentKeyword keyword) {
        switch (keyword) {
            case NETWORK_STATUS_VERSION:
                throw new TorParsingException("Network status version may only appear on the first line of status document");
            case VOTE_STATUS:
                final String voteStatus = fieldParser.parseString();
                if (!voteStatus.equals("consensus")) {
                    throw new TorParsingException("Unexpected vote-status type: " + voteStatus);
                }
                break;
            case CONSENSUS_METHOD:
                document.setConsensusMethod(fieldParser.parseInteger());
                break;
            case VALID_AFTER:
                document.setValidAfter(fieldParser.parseTimestamp());
                break;
            case FRESH_UNTIL:
                document.setFreshUntil(fieldParser.parseTimestamp());
                break;
            case VALID_UNTIL:
                document.setValidUntil(fieldParser.parseTimestamp());
                break;
            case VOTING_DELAY:
                document.setVoteDelaySeconds(fieldParser.parseInteger());
                document.setDistDelaySeconds(fieldParser.parseInteger());
                break;
            case CLIENT_VERSIONS:
                for (String version : parseVersions(fieldParser.parseString())) {
                    document.addClientVersion(version);
                }
                break;
            case SERVER_VERSIONS:
                for (String version : parseVersions(fieldParser.parseString())) {
                    document.addServerVersion(version);
                }
                break;
            case KNOWN_FLAGS:
                while (fieldParser.argumentsRemaining() > 0) {
                    document.addKnownFlag(fieldParser.parseString());
                }
                break;
            case PARAMS:
                parseParams();
                break;
        }
    }

    private void parseFirstLine(DocumentKeyword keyword) {
        if (keyword != DocumentKeyword.NETWORK_STATUS_VERSION) {
            throw new TorParsingException("network-status-version not found at beginning of consensus document as expected.");
        }
        int documentVersion = fieldParser.parseInteger();
        if (documentVersion != CURRENT_DOCUMENT_VERSION) {
            throw new TorParsingException("Unexpected consensus document version number: " + documentVersion);
        }
        if (fieldParser.argumentsRemaining() > 0) {
            parseConsensusFlavor();
        }
        isFirstLine.set(false);
    }

    private void parseConsensusFlavor() {
        String flavor = fieldParser.parseString();
        if ("ns".equals(flavor)) {
            document.setConsensusFlavor(ConsensusFlavor.NS);
        } else if ("microdesc".equals(flavor)) {
            document.setConsensusFlavor(ConsensusFlavor.MICRODESC);
        } else {
            log.warn("Unknown consensus flavor: {}", flavor);
        }
    }

    private @NotNull List<String> parseVersions(@NotNull String versions) {
        return Arrays.asList(versions.split(","));
    }

    private void parseParams() {
        int remaining = fieldParser.argumentsRemaining();
        for (int i = 0; i < remaining; i++) {
            NameIntegerParameter p = fieldParser.parseParameter();
            document.addParameter(p.name(), p.value());
        }
    }
}