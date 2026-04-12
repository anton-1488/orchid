package com.subgraph.orchid.directory.parsing;

import java.nio.ByteBuffer;

import com.subgraph.orchid.ConsensusDocument;
import com.subgraph.orchid.directory.certificate.KeyCertificate;
import com.subgraph.orchid.directory.router.RouterDescriptor;
import com.subgraph.orchid.directory.router.RouterMicrodescriptor;

public interface DocumentParserFactory {
	DocumentParser<RouterDescriptor> createRouterDescriptorParser(ByteBuffer buffer, boolean verifySignatures);
	
	DocumentParser<RouterMicrodescriptor> createRouterMicrodescriptorParser(ByteBuffer buffer);

	DocumentParser<KeyCertificate> createKeyCertificateParser(ByteBuffer buffer);

	DocumentParser<ConsensusDocument> createConsensusDocumentParser(ByteBuffer buffer);
}
