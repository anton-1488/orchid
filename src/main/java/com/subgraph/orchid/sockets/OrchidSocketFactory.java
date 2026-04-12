package com.subgraph.orchid.sockets;

import com.subgraph.orchid.TorClient;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.*;

public class OrchidSocketFactory extends SocketFactory {
    private final TorClient torClient;

    public OrchidSocketFactory(TorClient torClient) {
        this.torClient = torClient;
    }

    @Override
    public Socket createSocket() throws IOException {
        return createSocketInstance();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket s = createSocketInstance();
        return connectOrchidSocket(s, host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        Socket s = createSocketInstance();
        return connectOrchidSocket(s, address.getHostAddress(), port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(address, port);
    }

    private Socket connectOrchidSocket(Socket s, String host, int port) throws IOException {
        SocketAddress endpoint = InetSocketAddress.createUnresolved(host, port);
        s.connect(endpoint);
        return s;
    }

    private Socket createSocketInstance() throws SocketException {
        OrchidSocketImpl impl = new OrchidSocketImpl(torClient);
        return new Socket(impl) {
        };
    }
}
