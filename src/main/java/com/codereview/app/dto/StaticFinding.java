package com.codereview.app.dto;

public class StaticFinding {
    private String severity;
    private String message;
    private String source;
    private int lineNumber;

    public StaticFinding(String severity, String message, String source, int lineNumber) {
        this.severity = severity;
        this.message = message;
        this.source = source;
        this.lineNumber = lineNumber;
    }

    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getSource() { return source; }
    public int getLineNumber() { return lineNumber; }
}