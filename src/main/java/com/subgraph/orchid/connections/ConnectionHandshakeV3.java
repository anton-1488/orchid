package com.subgraph.orchid.connections;

import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.cells.enums.CellCommand;
import com.subgraph.orchid.exceptions.ConnectionHandshakeException;
import com.subgraph.orchid.exceptions.ConnectionIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ConnectionHandshakeV3 extends ConnectionHandshake {
    private static final Logger log = LoggerFactory.getLogger(ConnectionHandshakeV3.class);

    private X509Certificate linkCertificate;
    private X509Certificate identityCertificate;

    public ConnectionHandshakeV3(ConnectionImpl connection, SSLSocket socket) {
        super(connection, socket);
    }

    @Override
    public void runHandshake() throws ConnectionIOException {
        try {
            log.debug("Starting V3 handshake");
            sendVersions((short) 3);
            receiveVersions();
            recvCerts();
            verifyCertificates();
            sendNetinfo();
            recvNetinfo();
            log.debug("V3 handshake completed");
        } catch (ConnectionHandshakeException e) {
            throw e;
        } catch (Exception e) {
            throw new ConnectionHandshakeException("Handshake failed", e);
        }
    }

    private void recvCerts() throws ConnectionHandshakeException {
        Cell cell = expectCell(CellCommand.CERTS);
        int ncerts = cell.getCellReader().getByte();

        if (ncerts != 2) {
            throw new ConnectionHandshakeException("Expected 2 certificates, got " + ncerts);
        }

        for (int i = 0; i < ncerts; i++) {
            int type = cell.getCellReader().getByte();
            X509Certificate cert = readCertificateFromCell(cell);

            if (type == 1) {
                linkCertificate = cert;
            } else if (type == 2) {
                identityCertificate = cert;
            } else {
                throw new ConnectionHandshakeException("Unexpected certificate type: " + type);
            }
        }
    }

    private X509Certificate readCertificateFromCell(Cell cell) throws ConnectionHandshakeException {
        try {
            int length = cell.getCellReader().getShort();
            byte[] data = new byte[length];
            cell.getCellReader().getByteArray(data);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(data));
        } catch (Exception e) {
            throw new ConnectionHandshakeException("Failed to read certificate", e);
        }
    }

    private void verifyCertificates() throws ConnectionHandshakeException {
        if (identityCertificate == null || linkCertificate == null) {
            throw new ConnectionHandshakeException("Missing certificates");
        }

        // Verify identity key
        verifyIdentityKey(identityCertificate.getPublicKey());

        // Verify certificate chain
        try {
            identityCertificate.checkValidity();
            identityCertificate.verify(identityCertificate.getPublicKey());
            linkCertificate.checkValidity();
            linkCertificate.verify(identityCertificate.getPublicKey());
        } catch (GeneralSecurityException e) {
            throw new ConnectionHandshakeException("Invalid certificate chain", e);
        }
    }
}