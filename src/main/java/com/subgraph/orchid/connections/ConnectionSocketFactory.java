package com.subgraph.orchid.connections;

import org.jetbrains.annotations.NotNull;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class ConnectionSocketFactory {
    private static final String[] ENABLED_CIPHERS = {
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
    };

    private static final TrustManager[] NULL_TRUST = {
            new X509TrustManager() {
                private final X509Certificate[] empty = {};

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return empty;
                }
            }
    };

    private static final SSLSocketFactory socketFactory = createSSLContext().getSocketFactory();

    private ConnectionSocketFactory() {
    }

    private static @NotNull SSLContext createSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, NULL_TRUST, null);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }

    public static @NotNull SSLSocket createSocket() {
        try {
            SSLSocket socket = (SSLSocket) socketFactory.createSocket();
            socket.setEnabledCipherSuites(ENABLED_CIPHERS);
            socket.setUseClientMode(true);
            return socket;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SSL socket", e);
        }
    }
}