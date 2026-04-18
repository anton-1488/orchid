package com.subgraph.orchid.downloader;

import com.subgraph.orchid.document.DocumentParserFactoryImpl;
import com.subgraph.orchid.downloader.request.TorHttpClient;
import com.subgraph.orchid.downloader.request.TorRequest;
import com.subgraph.orchid.exceptions.DirectoryRequestFailedException;
import com.subgraph.orchid.parsing.BasicDocumentParsingResult;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.parsing.DocumentParserFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public abstract class DocumentFetcher<T> {
    protected final static DocumentParserFactory PARSER_FACTORY = new DocumentParserFactoryImpl();

    public List<T> requestDocuments() {
        ByteBuffer body = makeRequest();
        if (body.hasRemaining()) {
            return processResponse(body);
        } else {
            return Collections.emptyList();
        }
    }

    protected ByteBuffer makeRequest() {
        return TorHttpClient.sendGetRequest(getRequest());
    }

    private List<T> processResponse(ByteBuffer response) throws DirectoryRequestFailedException {
        DocumentParser<T> parser = createParser(response);
        BasicDocumentParsingResult<T> result = new BasicDocumentParsingResult<>();
        boolean success = parser.parse(result);
        if (success) {
            return result.getParsedDocuments();
        }
        throw new DirectoryRequestFailedException("Failed to parse response from directory: " + result.getMessage());
    }

    //===== ABSTRACT METHODS =====\\

    abstract TorRequest getRequest();

    abstract DocumentParser<T> createParser(ByteBuffer response);
}