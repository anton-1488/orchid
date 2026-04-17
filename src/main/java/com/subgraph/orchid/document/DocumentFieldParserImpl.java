package com.subgraph.orchid.document;

import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.crypto.TorNTorKeyAgreement;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.crypto.TorSignature;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.exceptions.TorParsingException;
import com.subgraph.orchid.parsing.DocumentFieldParser;
import com.subgraph.orchid.parsing.DocumentObject;
import com.subgraph.orchid.parsing.DocumentParsingHandler;
import com.subgraph.orchid.parsing.NameIntegerParameter;
import jdk.jfr.Percentage;
import org.bouncycastle.util.encoders.Base64;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DocumentFieldParserImpl implements DocumentFieldParser {
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final static String BEGIN_TAG = "-----BEGIN";
    private final static String END_TAG = "-----END";
    private final static String TAG_DELIMITER = "-----";
    private final static String DEFAULT_DELIMITER = " ";
    private final ByteBuffer inputBuffer;
    private String delimiter = DEFAULT_DELIMITER;
    private String currentKeyword;
    private List<String> currentItems;
    private int currentItemsPosition;
    private boolean recognizeOpt;
    /* If a line begins with this string do not include it in the current signature. */
    private String signatureIgnoreToken;
    private boolean isProcessingSignedEntity = false;
    private TorMessageDigest signatureDigest;
    private TorMessageDigest signatureDigest256;
    private StringBuilder rawDocumentBuffer;

    private DocumentParsingHandler callbackHandler;

    public DocumentFieldParserImpl(ByteBuffer buffer) {
        buffer.rewind();
        this.inputBuffer = buffer;
        rawDocumentBuffer = new StringBuilder();
    }

    @Override
    public String parseNickname() {
        return getItem();
    }

    @Override
    public String parseString() {
        return getItem();
    }

    @Override
    public void setRecognizeOpt() {
        recognizeOpt = true;
    }

    @Override
    public void setHandler(DocumentParsingHandler handler) {
        callbackHandler = Objects.requireNonNull(handler);
    }

    @Override
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public int argumentsRemaining() {
        return currentItems.size() - currentItemsPosition;
    }

    /**
     * Return a string containing all remaining arguments concatenated together
     */
    @Override
    public String parseConcatenatedString() {
        StringBuilder result = new StringBuilder();
        while (argumentsRemaining() > 0) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(getItem());
        }
        return result.toString();
    }

    @Override
    public int parseInteger() {
        return parseInteger(getItem());
    }

    @Override
    public int parseInteger(String item) {
        return Integer.parseInt(item);
    }

    @Override
    public int[] parseIntegerList() {
        String item = getItem();
        String[] ns = item.split(",");
        int[] result = new int[ns.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = parseInteger(ns[i]);
        }
        return result;
    }

    @Override
    public int parsePort() {
        return parsePort(getItem());
    }

    @Override
    public int parsePort(String item) {
        int port = parseInteger(item);
        if (port < 0 || port > 65535) {
            throw new TorParsingException("Illegal port value: " + port);
        }
        return port;
    }


    @Override
    public Instant parseTimestamp() {
        String timeAndDate = getItem() + " " + getItem();
        TemporalAccessor date = dateFormat.parse(timeAndDate);
        return Instant.from(date);
    }

    @Override
    public HexDigest parseHexDigest() {
        return HexDigest.createFromString(parseString());
    }

    @Override
    public HexDigest parseBase32Digest() {
        return HexDigest.createFromBase32String(parseString());
    }

    @Override
    public HexDigest parseFingerprint() {
        return HexDigest.createFromString(parseConcatenatedString());
    }

    @Override
    public void verifyExpectedArgumentCount(String keyword, int argumentCount) {
        verifyExpectedArgumentCount(keyword, argumentCount, argumentCount);
    }

    @Override
    public byte[] parseBase64Data() {
        StringBuilder string = new StringBuilder(getItem());
        switch (string.length() % 4) {
            case 2:
                string.append("==");
                break;
            case 3:
                string.append("=");
                break;
        }
        return Base64.decode(string.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    @Override
    public InetAddress parseAddress() throws UnknownHostException {
        return InetAddress.getByName(getItem());
    }

    @Override
    public TorPublicKey parsePublicKey() {
        return new TorPublicKey(parseObject().getContent());
    }

    @Override
    public byte[] parseNtorPublicKey() {
        byte[] key = parseBase64Data();
        if (key.length != TorNTorKeyAgreement.CURVE25519_PUBKEY_LEN) {
            throw new TorParsingException("NTor public key was not expected length after base64 decoding.  Length is " + key.length);
        }
        return key;
    }

    @Override
    public TorSignature parseSignature() {
        return TorSignature.createFromPEMBuffer(parseObject().getContent());
    }

    @Override
    public NameIntegerParameter parseParameter() {
        String item = getItem();
        int eq = item.indexOf('=');
        if (eq == -1) {
            throw new TorParsingException("Parameter not in expected form name=value");
        }
        String name = item.substring(0, eq);
        validateParameterName(name);
        int value = parseInteger(item.substring(eq + 1));

        return new NameIntegerParameter(name, value);
    }

    @Override
    public DocumentObject parseObject() {
        String line = readLine();
        String keyword = parseObjectHeader(line);
        DocumentObject object = new DocumentObject(keyword, line);
        parseObjectBody(object, keyword);
        return object;
    }

    @Override
    public String getCurrentKeyword() {
        return currentKeyword;
    }

    @Override
    public void processDocument() {
        Objects.requireNonNull(callbackHandler);

        while (true) {
            String line = readLine();
            if (line == null) {
                callbackHandler.endOfDocument();
                return;
            }
            if (processLine(line)) {
                callbackHandler.parseKeywordLine();
            }
        }
    }

    @Override
    public synchronized void startSignedEntity() {
        isProcessingSignedEntity = true;
        signatureDigest = new TorMessageDigest();
        signatureDigest256 = new TorMessageDigest(true);
    }

    @Override
    public void endSignedEntity() {
        isProcessingSignedEntity = false;
    }

    @Override
    public void setSignatureIgnoreToken(String token) {
        signatureIgnoreToken = token;
    }

    @Override
    public TorMessageDigest getSignatureMessageDigest() {
        return signatureDigest;
    }

    @Override
    public TorMessageDigest getSignatureMessageDigest256() {
        return signatureDigest256;
    }

    @Override
    public String getRawDocument() {
        return rawDocumentBuffer.toString();
    }

    @Override
    public void resetRawDocument() {
        rawDocumentBuffer = new StringBuilder();
    }

    @Override
    public void resetRawDocument(String initialContent) {
        rawDocumentBuffer = new StringBuilder();
        rawDocumentBuffer.append(initialContent);
    }

    @Percentage
    public boolean verifySignedEntity(@NotNull TorPublicKey publicKey, TorSignature signature) {
        isProcessingSignedEntity = false;
        return publicKey.verifySignature(signature, signatureDigest);
    }

    private void validateParameterName(@NotNull String name) {
        if (name.isEmpty()) {
            throw new TorParsingException("Parameter name cannot be empty");
        }
        for (char c : name.toCharArray()) {
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                throw new TorParsingException("Parameter name can only contain letters.  Rejecting: " + name);
            }
        }
    }

    public DocumentObject parseTypedObject(@NotNull String type) {
        DocumentObject object = parseObject();
        if (!type.equals(object.getKeyword())) {
            throw new TorParsingException("Unexpected object type.  Expecting: " + type + ", but got: " + object.getKeyword());
        }
        return object;
    }

    private @NotNull String parseObjectHeader(@NotNull String headerLine) {
        if (!(headerLine.startsWith(BEGIN_TAG) && headerLine.endsWith(TAG_DELIMITER))) {
            throw new TorParsingException("Did not find expected object start tag.");
        }
        return headerLine.substring(BEGIN_TAG.length() + 1, headerLine.length() - TAG_DELIMITER.length());
    }

    private void parseObjectBody(DocumentObject object, String keyword) {
        String endTag = String.format("%s %s%s", END_TAG, keyword, TAG_DELIMITER);
        while (true) {
            String line = readLine();
            if (line == null) {
                throw new TorParsingException("EOF reached before end of '" + keyword + "' object.");
            }
            if (line.equals(endTag)) {
                object.addFooterLine(line);
                return;
            }
            parseObjectContent(object, line);
        }
    }

    private void parseObjectContent(@NotNull DocumentObject object, String content) {
        object.addContent(content);
    }

    private void updateRawDocument(String line) {
        rawDocumentBuffer.append(line);
        rawDocumentBuffer.append('\n');
    }

    private String readLine() {
        String line = nextLineFromInputBuffer();
        if (line != null) {
            updateCurrentSignature(line);
            updateRawDocument(line);
        }
        return line;
    }

    private String nextLineFromInputBuffer() {
        if (!inputBuffer.hasRemaining()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        while (inputBuffer.hasRemaining()) {
            char c = (char) (inputBuffer.get() & 0xFF);
            if (c == '\n') {
                return sb.toString();
            } else if (c != '\r') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void updateCurrentSignature(String line) {
        if ((!isProcessingSignedEntity) || (signatureIgnoreToken != null && line.startsWith(signatureIgnoreToken))) {
            return;
        }
        signatureDigest.update(line + "\n");
        signatureDigest256.update(line + "\n");
    }

    private boolean processLine(@NotNull String line) {
        List<String> lineItems = Arrays.asList(line.split(delimiter));
        if (lineItems.isEmpty() || lineItems.getFirst().isEmpty()) {
            return false;
        }

        currentKeyword = lineItems.getFirst();
        currentItems = lineItems;
        currentItemsPosition = 1;
        if (recognizeOpt && currentKeyword.equals("opt") && lineItems.size() > 1) {
            currentKeyword = lineItems.get(1);
            currentItemsPosition = 2;
        }
        return true;
    }

    private String getItem() {
        if (currentItemsPosition >= currentItems.size()) {
            throw new TorParsingException("Overrun while reading arguments");
        }
        return currentItems.get(currentItemsPosition++);
    }

    private void verifyExpectedArgumentCount(String keyword, int expectedMin, int expectedMax) {
        int argumentCount = argumentsRemaining();
        if (expectedMin != -1 && argumentCount < expectedMin) {
            throw new TorParsingException("Not enough arguments for keyword '" + keyword + "' expected " + expectedMin + " and got " + argumentCount);
        }
        if (expectedMax != -1 && argumentCount > expectedMax) {
            throw new TorParsingException("Too many arguments for keyword '" + keyword + "' expected " + expectedMax + " and got " + argumentCount);
        }
    }
}