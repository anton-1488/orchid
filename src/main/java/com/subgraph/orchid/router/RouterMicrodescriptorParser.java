package com.subgraph.orchid.router;

import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.exceptions.TorParsingException;
import com.subgraph.orchid.parsing.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterMicrodescriptorParser implements DocumentParser<RouterMicrodescriptor> {
    private static final Logger log = LoggerFactory.getLogger(RouterMicrodescriptorParser.class);
    private final DocumentFieldParser fieldParser;
    private RouterMicrodescriptorImpl currentDescriptor;
    private DocumentParsingResultHandler<RouterMicrodescriptor> resultHandler;

    public RouterMicrodescriptorParser(DocumentFieldParser fieldParser) {
        this.fieldParser = fieldParser;
        this.fieldParser.setHandler(createParsingHandler());
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull DocumentParsingHandler createParsingHandler() {
        return new DocumentParsingHandler() {
            @Override
            public void parseKeywordLine() {
                processKeywordLine();
            }

            @Override
            public void endOfDocument() {
                if (currentDescriptor != null) {
                    finalizeDescriptor(currentDescriptor);
                }
            }
        };
    }

    @Override
    public boolean parse(DocumentParsingResultHandler<RouterMicrodescriptor> resultHandler) {
        this.resultHandler = resultHandler;
        try {
            fieldParser.processDocument();
            return true;
        } catch (TorParsingException e) {
            resultHandler.parsingError(e.getMessage());
            return false;
        }
    }

    @Override
    public DocumentParsingResult<RouterMicrodescriptor> parse() {
        BasicDocumentParsingResult<RouterMicrodescriptor> result = new BasicDocumentParsingResult<RouterMicrodescriptor>();
        parse(result);
        return result;
    }

    private void processKeywordLine() {
        RouterMicrodescriptorKeyword keyword = RouterMicrodescriptorKeyword.findKeyword(fieldParser.getCurrentKeyword());
        if (!keyword.equals(RouterMicrodescriptorKeyword.UNKNOWN_KEYWORD)) {
            processKeyword(keyword);
        }
        if (currentDescriptor != null) {
            currentDescriptor.setRawDocumentData(fieldParser.getRawDocument());
        }
    }


    private void processKeyword(@NotNull RouterMicrodescriptorKeyword keyword) {
        fieldParser.verifyExpectedArgumentCount(keyword.getKeyword(), keyword.getArgumentCount());
        switch (keyword) {
            case ONION_KEY:
                processOnionKeyLine();
                break;
            case NTOR_ONION_KEY:
                if (currentDescriptor != null) {
                    currentDescriptor.setNtorOnionKey(fieldParser.parseNtorPublicKey());
                }
                break;
            case FAMILY:
                while (fieldParser.argumentsRemaining() > 0 && currentDescriptor != null) {
                    currentDescriptor.addFamilyMember(fieldParser.parseString());
                }
                break;
            case P:
                processP();
                break;
        }
    }

    private void processOnionKeyLine() {
        if (currentDescriptor != null) {
            finalizeDescriptor(currentDescriptor);
        }
        currentDescriptor = new RouterMicrodescriptorImpl();
        fieldParser.resetRawDocument(RouterMicrodescriptorKeyword.ONION_KEY.getKeyword() + "\n");
        currentDescriptor.setOnionKey(fieldParser.parsePublicKey());
    }

    private void finalizeDescriptor(@NotNull RouterMicrodescriptorImpl descriptor) {
        TorMessageDigest digest = new TorMessageDigest(true);
        digest.update(descriptor.getRawDocumentData());
        descriptor.setDescriptorDigest(digest.getHexDigest());
        if (!descriptor.isValidDocument()) {
            resultHandler.documentInvalid(descriptor, "Microdescriptor data invalid");
        } else {
            resultHandler.documentParsed(descriptor);
        }
    }

    private void processP() {
        if (currentDescriptor == null) {
            return;
        }
        String ruleType = fieldParser.parseString();

        if ("accept".equals(ruleType)) {
            currentDescriptor.addAcceptPorts(fieldParser.parseString());
        } else if ("reject".equals(ruleType)) {
            currentDescriptor.addRejectPorts(fieldParser.parseString());
        } else {
            log.warn("Unexpected P field in microdescriptor: {}", ruleType);
        }
    }
}