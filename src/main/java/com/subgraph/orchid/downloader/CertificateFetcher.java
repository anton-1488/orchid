package com.subgraph.orchid.downloader;

import java.nio.ByteBuffer;
import java.util.Set;

import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.document.ConsensusDocument.RequiredCertificate;
import com.subgraph.orchid.downloader.request.TorRequest;
import com.subgraph.orchid.parsing.DocumentParser;

public class CertificateFetcher extends DocumentFetcher<KeyCertificate> {
    private final Set<RequiredCertificate> requiredCertificates;

    public CertificateFetcher(Set<RequiredCertificate> requiredCertificates) {
        this.requiredCertificates = requiredCertificates;
    }

    @Override
    public TorRequest getRequest() {
        return TorRequest.get("/tor/keys/fp-sk/" + getRequiredCertificatesRequestString());
    }

    private String getRequiredCertificatesRequestString() {
        StringBuilder sb = new StringBuilder();
        for (RequiredCertificate rc : requiredCertificates) {
            if (!sb.isEmpty()) {
                sb.append("+");
            }
            sb.append(rc.getAuthorityIdentity().toString());
            sb.append("-");
            sb.append(rc.getSigningKey().toString());
        }
        return sb.toString();
    }

    @Override
    DocumentParser<KeyCertificate> createParser(ByteBuffer response) {
        return PARSER_FACTORY.createKeyCertificateParser(response);
    }
}