package com.subgraph.orchid.consensus;

import com.subgraph.orchid.consensus.ConsensusDocumentParser.DocumentSection;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthoritySectionParser extends ConsensusDocumentSectionParser {
    private static final Logger log = LoggerFactory.getLogger(AuthoritySectionParser.class);
    private volatile VoteAuthorityEntryImpl currentEntry = null;

    public AuthoritySectionParser(DocumentFieldParser parser, ConsensusDocumentImpl document) {
        super(parser, document);
        startEntry();
    }

    @Override
    void parseLine(@NotNull DocumentKeyword keyword) {
        switch (keyword) {
            case DIR_SOURCE:
                parseDirSource();
                break;
            case CONTACT:
                currentEntry.setContact(fieldParser.parseConcatenatedString());
                break;
            case VOTE_DIGEST:
                currentEntry.setVoteDigest(fieldParser.parseHexDigest());
                addCurrentEntry();
                break;
            default:
                break;
        }
    }

    private synchronized void startEntry() {
        currentEntry = new VoteAuthorityEntryImpl();
    }

    private void addCurrentEntry() {
        document.addVoteAuthorityEntry(currentEntry);
        startEntry();
    }

    private synchronized void parseDirSource() {
        try {
            currentEntry.setNickname(fieldParser.parseNickname());
            currentEntry.setIdentity(fieldParser.parseHexDigest());
            currentEntry.setHostname(fieldParser.parseString());
            currentEntry.setAddress(fieldParser.parseAddress());
            currentEntry.setDirectoryPort(fieldParser.parsePort());
            currentEntry.setRouterPort(fieldParser.parsePort());
        } catch (Exception e) {
            log.error("Error parse dir source: ", e);
        }
    }

    @Override
    public String getNextStateKeyword() {
        return "r";
    }

    @Override
    public DocumentSection getSection() {
        return DocumentSection.AUTHORITY;
    }

    public DocumentSection nextSection() {
        return DocumentSection.ROUTER_STATUS;
    }
}