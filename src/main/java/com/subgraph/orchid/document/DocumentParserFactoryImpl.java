package com.subgraph.orchid.document;

import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.certificate.KeyCertificateParser;
import com.subgraph.orchid.consensus.ConsensusDocumentParser;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.parsing.DocumentParserFactory;
import com.subgraph.orchid.router.RouterDescriptor;
import com.subgraph.orchid.router.RouterDescriptorParser;
import com.subgraph.orchid.router.RouterMicrodescriptor;
import com.subgraph.orchid.router.RouterMicrodescriptorParser;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class DocumentParserFactoryImpl implements DocumentParserFactory {
    @Override
    public DocumentParser<KeyCertificate> createKeyCertificateParser(ByteBuffer buffer) {
        return new KeyCertificateParser(new DocumentFieldParserImpl(buffer));
    }

    @Override
    public DocumentParser<RouterDescriptor> createRouterDescriptorParser(ByteBuffer buffer, boolean verifySignatures) {
        return new RouterDescriptorParser(new DocumentFieldParserImpl(buffer), verifySignatures);
    }

    @Override
    public DocumentParser<RouterMicrodescriptor> createRouterMicrodescriptorParser(@NotNull ByteBuffer buffer) {
        buffer.rewind();
        DocumentFieldParser dfp = new DocumentFieldParserImpl(buffer);
        return new RouterMicrodescriptorParser(dfp);
    }

    @Override
    public DocumentParser<ConsensusDocument> createConsensusDocumentParser(ByteBuffer buffer) {
        return new ConsensusDocumentParser(new DocumentFieldParserImpl(buffer));
    }
}