package com.subgraph.orchid.parsing;

public class DocumentObject {
    private final StringBuilder stringContent = new StringBuilder();
    private final String keyword;
    private final String headerLine;
    private String footerLine;
    private String bodyContent;

    public DocumentObject(String keyword, String headerLine) {
        this.keyword = keyword;
        this.headerLine = headerLine;
    }

    public void addContent(String content) {
        stringContent.append(content);
        stringContent.append("\n");
    }

    public void addFooterLine(String footer) {
        footerLine = footer;
        bodyContent = stringContent.toString();
    }

    public String getKeyword() {
        return keyword;
    }

    public String getContent() {
        return getContent(true);
    }

    public String getContent(boolean includeHeaders) {
        if (includeHeaders) {
            return headerLine + "\n" + bodyContent + footerLine + "\n";
        } else {
            return bodyContent;
        }
    }
}