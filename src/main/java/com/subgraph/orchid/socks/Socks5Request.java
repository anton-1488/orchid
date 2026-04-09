package com.subgraph.orchid.socks;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.exceptions.SocksRequestException;
import com.subgraph.orchid.socks.enums.SOCKS5AddressType;
import com.subgraph.orchid.socks.enums.SOCKS5Command;
import com.subgraph.orchid.socks.enums.SOCKS5Status;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Socks5Request extends SocksRequest {
    private SOCKS5Command command;
    private SOCKS5AddressType addressType;

    private byte[] addressBytes = new byte[0];
    private byte[] portBytes = new byte[0];

    Socks5Request(TorConfig config, Socket socket) {
        super(config, socket);
    }

    private String addressBytesToHostname() {
        if (addressType != SOCKS5AddressType.ADDRESS_HOSTNAME) {
            throw new IllegalStateException("Not a hostname request");
        }
        return new String(addressBytes, 1, addressBytes.length - 1, StandardCharsets.UTF_8);
    }

    private byte[] readPortData() throws SocksRequestException {
        byte[] data = new byte[2];
        readAll(data);
        return data;
    }

    private void setIPv4AddressData(byte[] data) throws SocksRequestException {
        if (data.length != 4) throw new SocksRequestException("Invalid IPv4 data");
        try {
            InetAddress addr = InetAddress.getByAddress(data);
            setHostname(addr.getHostAddress());  // ✅ использовать родительский метод
        } catch (UnknownHostException e) {
            throw new SocksRequestException("Failed to parse IPv4 address", e);
        }
    }

    private byte[] readIPv4AddressData() throws SocksRequestException {
        byte[] data = new byte[4];
        readAll(data);
        return data;
    }

    private byte[] readIPv6AddressData() throws SocksRequestException {
        byte[] data = new byte[16];
        readAll(data);
        return data;
    }

    @Override
    public boolean isConnectRequest() {
        return command == SOCKS5Command.COMMAND_CONNECT;
    }

    @Override
    public int getCommandCode() {
        return command.getCommand();
    }

    @Override
    public void readRequest() throws SocksRequestException {
        if (!processAuthentication()) {
            throw new SocksRequestException("Failed to negotiate authentication");
        }
        if (readByte() != SOCKS5Command.VERSION.getCommand()) {
            throw new SocksRequestException();
        }

        command = SOCKS5Command.ofCommand(readByte());
        readByte(); // Reserved
        addressType = SOCKS5AddressType.ofType(readByte());
        addressBytes = readAddressBytes();
        portBytes = readPortData();

        if (addressType == SOCKS5AddressType.ADDRESS_IPV4) {
            setIPv4AddressData(addressBytes);
        } else if (addressType == SOCKS5AddressType.ADDRESS_HOSTNAME) {
            setHostname(addressBytesToHostname());
        } else {
            throw new SocksRequestException();
        }
        setPortData(portBytes);
    }

    @Override
    public void sendConnectionRefused() throws SocksRequestException {
        sendResponse(SOCKS5Status.CONNECTION_REFUSED);
    }

    @Override
    public void sendError(boolean isUnsupportedCommand) throws SocksRequestException {
        if (isUnsupportedCommand) {
            sendResponse(SOCKS5Status.COMMAND_NOT_SUPPORTED);
        } else {
            sendResponse(SOCKS5Status.FAILURE);
        }
    }

    @Override
    public void sendSuccess() throws SocksRequestException {
        sendResponse(SOCKS5Status.SUCCESS);
    }

    private void sendResponse(SOCKS5Status status) throws SocksRequestException {
        int responseLength = 4 + addressBytes.length + portBytes.length;
        ByteBuffer response = ByteBuffer.allocate(responseLength);

        response.put((byte) SOCKS5Command.VERSION.getCommand());
        response.put((byte) status.getStatus());
        response.put((byte) 0);
        response.put((byte) addressType.getAddressType());

        response.put(addressBytes);
        response.put(portBytes);

        socketWrite(response.array());
    }

    private boolean processAuthentication() throws SocksRequestException {
        int nmethods = readByte();
        boolean foundAuthNone = false;

        for (int i = 0; i < nmethods; i++) {
            SOCKS5Command meth = SOCKS5Command.ofCommand(readByte());
            if (meth == SOCKS5Command.AUTH_NONE) {
                foundAuthNone = true;
            }
        }

        if (foundAuthNone) {
            sendAuthenticationResponse(SOCKS5Command.AUTH_NONE);
            return true;
        } else {
            sendAuthenticationResponse(SOCKS5Command.NO_ACCEPTABLE);
            return false;
        }
    }


    private void sendAuthenticationResponse(SOCKS5Command method) throws SocksRequestException {
        byte[] response = new byte[2];
        response[0] = (byte) SOCKS5Command.VERSION.getCommand();
        response[1] = (byte) method.getCommand();

        socketWrite(response);
    }

    private byte[] readAddressBytes() throws SocksRequestException {
        return switch (addressType) {
            case SOCKS5AddressType.ADDRESS_IPV4 -> readIPv4AddressData();
            case SOCKS5AddressType.ADDRESS_IPV6 -> readIPv6AddressData();
            case SOCKS5AddressType.ADDRESS_HOSTNAME -> readHostnameData();
        };
    }

    private byte[] readHostnameData() throws SocksRequestException {
        int length = readByte();
        byte[] addrData = new byte[length + 1];

        addrData[0] = (byte) length;
        readAll(addrData, 1, length);

        return addrData;
    }
}
