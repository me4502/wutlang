package com.me4502.wutlang;

public class ParsingException extends Exception {

    public ParsingException(String message, WutlangParser parser) {
        super(message + " [" + (parser.getLineNum() + 1) + ":" + (parser.getColumnNum() + 1) + "]");
    }
}