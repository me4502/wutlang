package com.me4502.wutlang;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class WutlangParser {

    private List<String> lines;

    private int lineNum;
    private int columnNum;

    private int cursor = 0;

    private char[] heap = new char[1];
    private int heapLength = 1;

    private Stack<Character> stack = new Stack<>();

    private File file;

    private OutputStream fileOutput;
    private OutputStream netOutput;

    private InputStream fileInput;
    private InputStream netInput;

    private boolean networkStreamOpen = false;

    private OutputStream output;
    private InputStream input;

    private Stack<LinePos> loopStack = new Stack<>();
    private boolean loopSkipping;

    private HttpServer server;

    public WutlangParser(File file) throws IOException {
        this(Files.readAllLines(file.toPath()));
    }

    public WutlangParser(String lines) {
        this(Arrays.asList(lines.split("\n")));
    }

    public WutlangParser(List<String> lines) {
        this.lines = lines;
    }

    public int getLineNum() {
        return this.lineNum;
    }

    public int getColumnNum() {
        return this.columnNum;
    }

    public int getCursor() {
        return this.cursor;
    }

    public char[] getHeap() {
        return this.heap;
    }

    public Stack<Character> getStack() {
        return this.stack;
    }

    public void parseProgram() throws ParsingException {
        output = System.out;
        input = System.in;

        for (lineNum = 0; lineNum < lines.size(); lineNum ++) {
            for (columnNum = 0; columnNum < lines.get(lineNum).length(); columnNum ++) {
                String line = lines.get(lineNum);
                char instruction = line.toCharArray()[columnNum];
                if (instruction == '#') break;
                if (!loopSkipping || instruction == ']')
                    parseChar(instruction);
            }
        }
    }

    private void parseChar(char instruction) throws ParsingException {
        switch(instruction) {
            case '<':
                cursor --;
                if (cursor < 0) {
                    throw new ParsingException("Cursor pointing to negative heapspace.", this);
                }
                break;
            case '>':
                cursor ++;
                if (cursor >= heapLength) {
                    expandHeap();
                }
                break;
            case '.':
                try {
                    output.write(heap[cursor]);
                    System.out.println("Writing " + heap[cursor]);
                } catch (IOException e) {
                    throw new ParsingException("Failed to write to output: " + e.getMessage(), this);
                }
                break;
            case ',':
                try {
                    heap[cursor] = (char) input.read();
                } catch (IOException e) {
                    throw new ParsingException("Failed to read from input: " + e.getMessage(), this);
                }
                break;
            case '+':
                heap[cursor] ++;
                break;
            case '-':
                heap[cursor] --;
                break;
            case 'c':
                output = System.out;
                break;
            case 'r':
                input = System.in;
            case '^':
                stack.push(heap[cursor]);
                break;
            case 'V':
                heap[cursor] = stack.pop();
            case '[':
                if ((heap[cursor]) == 0) {
                    loopSkipping = true;
                } else {
                    loopStack.push(new LinePos(lineNum, columnNum));
                    loopSkipping = false;
                }
                break;
            case ']':
                if (loopSkipping) {
                    loopSkipping = false;
                } else {
                    if (loopStack.size() > 0) {
                        LinePos pos = loopStack.pop();
                        lineNum = pos.getLine();
                        columnNum = pos.getColumn() - 1;
                    } else {
                        throw new ParsingException("Found end of loop without beginning.", this);
                    }
                }
                break;
            case '$':
                StringBuilder port = new StringBuilder();
                char read;
                while ((read = heap[cursor]) != 0) {
                    cursor ++;
                    port.append(read);
                }
                cursor ++;
                try {
                    if (server != null) {
                        throw new ParsingException("Webserver is already running.", this);
                    }
                    server = HttpServer.create(new InetSocketAddress("localhost", Integer.parseInt(port.toString())), 0);
                    System.out.println(server.getAddress().toString());
                    server.createContext("/", httpExchange -> {
                        netInput = new SequenceInputStream(new ByteArrayInputStream(httpExchange.getLocalAddress().getAddress().getAddress()), httpExchange.getRequestBody());
                        netOutput = httpExchange.getResponseBody();
                        httpExchange.sendResponseHeaders(200, (int)heap[cursor]);
                        networkStreamOpen = true;
                    });
                    server.setExecutor(null);
                    server.start();
                } catch (IOException e) {
                    throw new ParsingException("Failed to create webserver. " + e.getMessage(), this);
                }
                break;
            case '@':
                if (server == null) {
                    throw new ParsingException("Webserver must be created before setting stream.", this);
                }
                if (!networkStreamOpen) {
                    columnNum--;
                    break;
                }
                input = netInput;
                break;
            case '!':
                if (server == null) {
                    throw new ParsingException("Webserver must be created before setting stream.", this);
                }
                if (!networkStreamOpen) {
                    columnNum--;
                    break;
                }
                output = netOutput;
                break;
            case '%':
                networkStreamOpen = false;
                try {
                    netOutput.close();
                    netInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case '~':
                if (server == null) {
                    throw new ParsingException("Webserver must be created before it can be shutdown.", this);
                }
                server.stop(2);
                server = null;
                break;
            case '&':
                StringBuilder filename = new StringBuilder();
                while ((read = heap[cursor]) != 0) {
                    cursor ++;
                    filename.append(read);
                }
                cursor ++;

                file = new File(filename.toString());
                try {
                    fileOutput = new FileOutputStream(file, true);
                    fileInput = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new ParsingException("Failed to access file. " + e.getMessage(), this);
                }
                break;
            case 'o':
                if (file == null) {
                    throw new ParsingException("File must be loaded before setting stream.", this);
                }
                output = fileOutput;
                break;
            case 'i':
                if (file == null) {
                    throw new ParsingException("File must be loaded before setting stream.", this);
                }
                input = fileInput;
                break;
            case 'p':
                if (file == null) {
                    throw new ParsingException("File must be loaded before clearing.", this);
                }

                try {
                    fileOutput.close();
                    fileInput.close();

                    PrintWriter out = new PrintWriter(file);
                    out.write("");
                    out.close();

                    fileOutput = new FileOutputStream(file, true);
                    fileInput = new FileInputStream(file);
                } catch (IOException e) {
                    throw new ParsingException("Failed to clear file. " + e.getMessage(), this);
                }

                break;
            case 'e':
                if (file == null) {
                    throw new ParsingException("File must be loaded before clearing.", this);
                }

                file = null;
                try {
                    fileOutput.close();
                    fileInput.close();
                } catch (IOException e) {
                    throw new ParsingException("Failed to close file. " + e.getMessage(), this);
                }
            case ':':
                System.out.println(Arrays.toString(heap));
                break;
        }
    }

    private void expandHeap() {
        heapLength <<= 1;
        heap = Arrays.copyOf(heap, heapLength);
    }
}
