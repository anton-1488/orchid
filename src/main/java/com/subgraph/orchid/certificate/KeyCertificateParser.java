package com.subgraph.orchid.certificate;

import com.subgraph.orchid.exceptions.TorParsingException;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.crypto.TorSignature;
import com.subgraph.orchid.parsing.BasicDocumentParsingResult;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.parsing.DocumentParsingHandler;
import com.subgraph.orchid.parsing.DocumentParsingResult;
import com.subgraph.orchid.parsing.DocumentParsingResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class KeyCertificateParser implements DocumentParser<KeyCertificate> {
    private final static int CURRENT_CERTIFICATE_VERSION = 3;
    private static final Logger log = LoggerFactory.getLogger(KeyCertificateParser.class);
    private final DocumentFieldParser fieldParser;
    private KeyCertificateImpl currentCertificate;
    private DocumentParsingResultHandler<KeyCertificate> resultHandler;

    public KeyCertificateParser(DocumentFieldParser fieldParser) {
        this.fieldParser = fieldParser;
        this.fieldParser.setHandler(createParsingHandler());
    }

    private DocumentParsingHandler createParsingHandler() {
        return new DocumentParsingHandler() {
            public void parseKeywordLine() {
                processKeywordLine();
            }

            public void endOfDocument() {
            }
        };
    }

    private void processKeywordLine() {
        KeyCertificateKeyword keyword = KeyCertificateKeyword.findKeyword(fieldParser.getCurrentKeyword());
        /*
         * dirspec.txt (1.2)
         * When interpreting a Document, software MUST ignore any KeywordLine that
         * starts with a keyword it doesn't recognize;
         */
        if (!keyword.equals(KeyCertificateKeyword.UNKNOWN_KEYWORD)) {
            processKeyword(keyword);
        }
    }

    private void startNewCertificate() {
        fieldParser.resetRawDocument();
        fieldParser.startSignedEntity();
        currentCertificate = new KeyCertificateImpl();
    }

    public boolean parse(DocumentParsingResultHandler<KeyCertificate> resultHandler) {
        this.resultHandler = resultHandler;
        startNewCertificate();
        try {
            fieldParser.processDocument();
            return true;
        } catch (TorParsingException e) {
            resultHandler.parsingError(e.getMessage());
            return false;
        }
    }

    public DocumentParsingResult<KeyCertificate> parse() {
        BasicDocumentParsingResult<KeyCertificate> result = new BasicDocumentParsingResult<>();
        parse(result);
        return result;
    }

    private void processKeyword(KeyCertificateKeyword keyword) {
        switch (keyword) {
            case DIR_KEY_CERTIFICATE_VERSION:
                processCertificateVersion();
                break;
            case DIR_ADDRESS:
                processDirectoryAddress();
                break;
            case FINGERPRINT:
                currentCertificate.setAuthorityFingerprint(fieldParser.parseHexDigest());
                break;
            case DIR_IDENTITY_KEY:
                currentCertificate.setAuthorityIdentityKey(fieldParser.parsePublicKey());
                break;
            case DIR_SIGNING_KEY:
                currentCertificate.setAuthoritySigningKey(fieldParser.parsePublicKey());
                break;
            case DIR_KEY_PUBLISHED:
                currentCertificate.setKeyPublishedTime(fieldParser.parseTimestamp());
                break;
            case DIR_KEY_EXPIRES:
                currentCertificate.setKeyExpiryTime(fieldParser.parseTimestamp());
                break;
            case DIR_KEY_CROSSCERT:
                verifyCrossSignature(fieldParser.parseSignature());
                break;
            case DIR_KEY_CERTIFICATION:
                processCertificateSignature();
                break;
            case UNKNOWN_KEYWORD:
                break;
        }
    }

    private void processCertificateVersion() {
        int version = fieldParser.parseInteger();
        if (version != CURRENT_CERTIFICATE_VERSION) {
            throw new TorParsingException("Unexpected certificate version: " + version);
        }
    }

    private void processDirectoryAddress() {
        try {
            String addrport = fieldParser.parseString();
            String[] args = addrport.split(":");
            if (args.length != 2) {
                throw new TorParsingException("Address/Port string incorrectly formed: " + addrport);
            }
            currentCertificate.setDirectoryAddress(InetAddress.getByName(args[0]));
            currentCertificate.setDirectoryPort(fieldParser.parsePort(args[1]));
        } catch (Exception e) {
            throw new TorParsingException(e);
        }
    }

    private void verifyCrossSignature(TorSignature crossSignature) {
        TorPublicKey identityKey = currentCertificate.getAuthorityIdentityKey();
        TorPublicKey signingKey = currentCertificate.getAuthoritySigningKey();
        if (!signingKey.verifySignature(crossSignature, identityKey.getFingerprint())) {
            throw new TorParsingException("Cross signature on certificate failed.");
        }
    }

    private boolean verifyCurrentCertificate(TorSignature signature) {
        if (!fieldParser.verifySignedEntity(currentCertificate.getAuthorityIdentityKey(), signature)) {
            resultHandler.documentInvalid(currentCertificate, "Signature failed");
            log.warn("Signature failed for certificate with fingerprint: {}", currentCertificate.getAuthorityFingerprint());
            return false;
        }
        currentCertificate.setValidSignature();
        boolean isValid = currentCertificate.isValidDocument();
        if (!isValid) {
            resultHandler.documentInvalid(currentCertificate, "Certificate data is invalid");
            log.warn("Certificate data is invalid for certificate with fingerprint: {}", currentCertificate.getAuthorityFingerprint());
        }
        return isValid;
    }

    private void processCertificateSignature() {
        fieldParser.endSignedEntity();
        if (verifyCurrentCertificate(fieldParser.parseSignature())) {
            currentCertificate.setRawDocumentData(fieldParser.getRawDocument());
            resultHandler.documentParsed(currentCertificate);
        }
        startNewCertificate();
    }
}