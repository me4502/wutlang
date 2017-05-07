package com.me4502.wutlang;

public class LinePos {
    private int line;
    private int column;

    public LinePos(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
