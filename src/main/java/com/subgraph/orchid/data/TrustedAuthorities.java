package com.subgraph.orchid.data;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.directory.DirectoryAuthorityStatus;
import com.subgraph.orchid.directory.DirectoryServer;
import com.subgraph.orchid.directory.DirectoryServerImpl;
import com.subgraph.orchid.document.DocumentFieldParserImpl;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import com.subgraph.orchid.parsing.DocumentParsingHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/*
 * This class contains the hardcoded 'bootstrap' directory authority
 * server information.
 */
public class TrustedAuthorities {
    private final static TrustedAuthorities INSTANCE = new TrustedAuthorities();
    private static final Logger log = LoggerFactory.getLogger(TrustedAuthorities.class);

    private final List<DirectoryServer> directoryServers = new ArrayList<>();
    private final int v3ServerCount;

    public static TrustedAuthorities getInstance() {
        return INSTANCE;
    }

    private TrustedAuthorities() {
        initialize();
        v3ServerCount = countV3Servers();
    }

    private int countV3Servers() {
        int n = 0;
        for (DirectoryServer ds : directoryServers) {
            if (ds.getV3Identity() != null) {
                n += 1;
            }
        }
        return n;
    }

    private void initialize() {
        StringBuilder builder = new StringBuilder();
        for (String entry : TrustedAuthoritiesParser.loadDirServers()) {
            builder.append(entry);
            builder.append('\n');
        }
        ByteBuffer buffer = ByteBuffer.wrap(builder.toString().getBytes(Tor.getDefaultCharset()));
        DocumentFieldParser parser = new DocumentFieldParserImpl(buffer);

        parser.setHandler(new DocumentParsingHandler() {
            @Override
            public void endOfDocument() {
                log.debug("End of document...");
            }

            @Override
            public void parseKeywordLine() {
                processKeywordLine(parser);
            }
        });
        parser.processDocument();
    }

    private void processKeywordLine(@NotNull DocumentFieldParser fieldParser) {
        DirectoryAuthorityStatus status = new DirectoryAuthorityStatus();
        status.setNickname(fieldParser.parseNickname());
        while (fieldParser.argumentsRemaining() > 0) {
            processArgument(fieldParser, status);
        }
    }

    private void processArgument(@NotNull DocumentFieldParser fieldParser, DirectoryAuthorityStatus status) {
        String item = fieldParser.parseString();
        if (Character.isDigit(item.charAt(0))) {
            parseAddressPort(fieldParser, item, status);
            status.setIdentity(fieldParser.parseFingerprint());
            DirectoryServerImpl server = new DirectoryServerImpl(status);
            if (status.getV3Ident() != null) {
                server.setV3Ident(status.getV3Ident());
            }
            log.debug("Adding trusted authority: {}", server);
            directoryServers.add(server);
        } else {
            parseFlag(fieldParser, item, status);
        }
    }

    private void parseAddressPort(@NotNull DocumentFieldParser parser, @NotNull String item, @NotNull DirectoryAuthorityStatus status) {
        String[] args = item.split(":");
        status.setAddress(InetAddressUtils.createAddressFromString(args[0]));
        status.setDirectoryPort(parser.parsePort(args[1]));
    }

    private void parseFlag(DocumentFieldParser parser, @NotNull String flag, DirectoryAuthorityStatus status) {
        if (flag.equals("hs")) {
            status.setHiddenServiceAuthority();
        } else if (flag.equals("no-hs")) {
            status.unsetHiddenServiceAuthority();
        } else if (flag.equals("bridge")) {
            status.setBridgeAuthority();
        } else if (flag.startsWith("orport=")) {
            status.setRouterPort(parser.parsePort(flag.substring(7)));
        } else if (flag.startsWith("v3ident=")) {
            status.setV3Ident(HexDigest.createFromString(flag.substring(8)));
        }
    }

    public int getV3AuthorityServerCount() {
        return v3ServerCount;
    }

    public List<DirectoryServer> getAuthorityServers() {
        return directoryServers;
    }

    public DirectoryServer getAuthorityServerByIdentity(HexDigest identity) {
        for (DirectoryServer ds : directoryServers) {
            if (identity.equals(ds.getV3Identity())) {
                return ds;
            }
        }
        return null;
    }
}