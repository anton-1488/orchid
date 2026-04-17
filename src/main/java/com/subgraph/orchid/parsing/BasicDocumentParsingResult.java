package com.subgraph.orchid.parsing;

import java.util.ArrayList;
import java.util.List;

public class BasicDocumentParsingResult<T> implements DocumentParsingResultHandler<T>, DocumentParsingResult<T> {
    private final List<T> documents = new ArrayList<>();
    private T invalidDocument;
    private boolean isOkay = false;
    private boolean isInvalid = false;
    private boolean isError = false;
    private String message = "";

    public BasicDocumentParsingResult() {
    }

    @Override
    public T getDocument() {
        if (documents.isEmpty()) {
            throw new IllegalStateException("Documents list is empty");
        }
        return documents.getFirst();
    }

    @Override
    public List<T> getParsedDocuments() {
        return List.copyOf(documents);
    }

    @Override
    public boolean isOkay() {
        return isOkay;
    }

    @Override
    public boolean isInvalid() {
        return isInvalid;
    }

    @Override
    public T getInvalidDocument() {
        return invalidDocument;
    }

    @Override
    public boolean isError() {
        return isError;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void documentParsed(T document) {
        documents.add(document);
    }

    @Override
    public synchronized void documentInvalid(T document, String message) {
        isOkay = false;
        isInvalid = true;
        invalidDocument = document;
        this.message = message;
    }

    @Override
    public synchronized void parsingError(String message) {
        isOkay = false;
        isError = true;
        this.message = message;
    }
}