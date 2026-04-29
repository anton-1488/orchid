package com.subgraph.orchid.consensus;

import com.subgraph.orchid.consensus.ConsensusDocumentParser.DocumentSection;
import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.crypto.TorSignature;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import com.subgraph.orchid.parsing.NameIntegerParameter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class FooterSectionParser extends ConsensusDocumentSectionParser {
    private final AtomicBoolean seenFirstSignature = new AtomicBoolean(false);

    public FooterSectionParser(DocumentFieldParser parser, ConsensusDocumentImpl document) {
        super(parser, document);
    }

    @Override
    public String getNextStateKeyword() {
        return null;
    }

    @Override
    public DocumentSection getSection() {
        return DocumentSection.FOOTER;
    }

    public DocumentSection nextSection() {
        return DocumentSection.NO_SECTION;
    }

    @Override
    public void parseLine(@NotNull DocumentKeyword keyword) {
        switch (keyword) {
            case BANDWIDTH_WEIGHTS:
                processBandwidthWeights();
                break;
            case DIRECTORY_SIGNATURE:
                processSignature();
                break;
        }
    }

    private void doFirstSignature() {
        seenFirstSignature.set(true);
        fieldParser.endSignedEntity();
        TorMessageDigest messageDigest = fieldParser.getSignatureMessageDigest();
        messageDigest.update("directory-signature ");
        document.setSigningHash(messageDigest.getHexDigest());

        TorMessageDigest messageDigest256 = fieldParser.getSignatureMessageDigest256();
        messageDigest256.update("directory-signature ");
        document.setSigningHash256(messageDigest256.getHexDigest());
    }

    private void processSignature() {
        if (!seenFirstSignature.get()) {
            doFirstSignature();
        }
        String s = fieldParser.parseString();
        HexDigest identity;
        boolean useSha256 = false;
        if (s.length() < TorMessageDigest.TOR_DIGEST_SIZE) {
            useSha256 = ("sha256".equals(s));
            identity = fieldParser.parseHexDigest();
        } else {
            identity = HexDigest.createFromString(s);
        }
        HexDigest signingKey = fieldParser.parseHexDigest();
        TorSignature signature = fieldParser.parseSignature();
        document.addSignature(new DirectorySignature(identity, signingKey, signature, useSha256));
    }

    private void processBandwidthWeights() {
        int remaining = fieldParser.argumentsRemaining();
        for (int i = 0; i < remaining; i++) {
            NameIntegerParameter p = fieldParser.parseParameter();
            document.addBandwidthWeight(p.name(), p.value());
        }
    }
}