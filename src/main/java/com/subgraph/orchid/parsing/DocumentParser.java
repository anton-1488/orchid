package com.subgraph.orchid.parsing;


public interface DocumentParser<T> {
    boolean parse(DocumentParsingResultHandler<T> resultHandler);

    DocumentParsingResult<T> parse();
}