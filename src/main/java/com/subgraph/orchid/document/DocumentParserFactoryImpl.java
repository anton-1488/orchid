package com.subgraph.orchid.document;

import java.nio.ByteBuffer;

import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.router.RouterDescriptor;
import com.subgraph.orchid.router.RouterMicrodescriptor;
import com.subgraph.orchid.certificate.KeyCertificateParser;
import com.subgraph.orchid.consensus.ConsensusDocumentParser;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.parsing.DocumentParserFactory;
import com.subgraph.orchid.router.RouterDescriptorParser;
import com.subgraph.orchid.router.RouterMicrodescriptorParser;

public class DocumentParserFactoryImpl implements DocumentParserFactory {
	
	public DocumentParser<KeyCertificate> createKeyCertificateParser(ByteBuffer buffer) {
		return new KeyCertificateParser(new DocumentFieldParserImpl(buffer));
	}

	public DocumentParser<RouterDescriptor> createRouterDescriptorParser(ByteBuffer buffer, boolean verifySignatures) {
		return new RouterDescriptorParser(new DocumentFieldParserImpl(buffer), verifySignatures);
	}

	public DocumentParser<RouterMicrodescriptor> createRouterMicrodescriptorParser(ByteBuffer buffer) {
		buffer.rewind();
		DocumentFieldParser dfp = new DocumentFieldParserImpl(buffer);
		return new RouterMicrodescriptorParser(dfp);
	}

	public DocumentParser<ConsensusDocument> createConsensusDocumentParser(ByteBuffer buffer) {
		return new ConsensusDocumentParser(new DocumentFieldParserImpl(buffer));
	}
}
