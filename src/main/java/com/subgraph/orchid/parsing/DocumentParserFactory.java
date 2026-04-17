package com.subgraph.orchid.parsing;

import java.nio.ByteBuffer;

import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.router.RouterDescriptor;
import com.subgraph.orchid.router.RouterMicrodescriptor;

public interface DocumentParserFactory {
    DocumentParser<RouterDescriptor> createRouterDescriptorParser(ByteBuffer buffer, boolean verifySignatures);

    DocumentParser<RouterMicrodescriptor> createRouterMicrodescriptorParser(ByteBuffer buffer);

    DocumentParser<KeyCertificate> createKeyCertificateParser(ByteBuffer buffer);

    DocumentParser<ConsensusDocument> createConsensusDocumentParser(ByteBuffer buffer);
}