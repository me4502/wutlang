package com.me4502.wutlang;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * **Wutlang version 1.0**
 *
 *  Wutlang is an esoteric programming language,
 *  with capabilities for netIO and fileIO.
 *
 *  This is heavily inspired by Brainf*ck.
 *
 *  Basic Functions
 *
 *  '<' - Takes the cursor back one spot.
 *  '>' - Takes the cursor forwards one spot.
 *  '.' - Output current heap cell to current output as character.
 *          Default output is console.
 *  ',' - Set current heap cell to current input.
 *          Default input is console.
 *  '+' - Increases current heap point.
 *  '-' - Decreases current heap point.
 *  '[' - Open loop. Skips past ']' if 0 at heap.
 *  ']' - Close loop. Returns to '[' when found.
 *
 *  Stack Functions
 *
 *  '^' - Push current input to stack.
 *  'V' - Drop current input from stack to heap.
 *
 *  Network Functions
 *
 *  '$' - Open network connections on localhost at port specified as characters until 0
 *          After the 0, the length should be specified as a raw number.
 *          Cursor will be on length afterwards.
 *  '@' - Set network stream as input.
 *  '!' - Set network stream as output
 *  '%' - Close network stream.
 *  '~' - End server.
 *
 *  File Functions
 *
 *  '&' - Open file connections. Filename is characters until 0.
 *  'o' - Set file as output.
 *  'i' - Set file as input. Appends.
 *  'p' - Clears file.
 *  'e' - Closes file streams.
 *
 *  Standard IO Functions
 *
 *  'c' - Set console as output.
 *  'r' - Set console as input.
 *
 *  Miscellaneous
 *
 *  '#' - Comment character. This line is a comment.
 *          Supports inline comments.
 *  ':' - Dump heap.
 */
public class Wutlang {

    private static int cursor = 0;

    private static char[] heap = new char[1];
    private static int heapLength = 1;

    private static Stack<Character> stack = new Stack<>();

    private static File file;

    private static OutputStream fileOutput;
    private static OutputStream netOutput;

    private static InputStream fileInput;
    private static InputStream netInput;

    private static boolean networkStreamOpen = false;

    private static OutputStream output;
    private static InputStream input;

    private static String fileName;
    private static int lineNum;
    private static int columnNum;

    private static Stack<LinePos> loopStack = new Stack<>();
    private static boolean loopSkipping;

    private static HttpServer server;

    public static void main(String[] args) {
        output = System.out;
        input = System.in;

        try {
            fileName = args[0];
            List<String> lines = Files.readAllLines(new File(fileName).toPath());
            for (lineNum = 0; lineNum < lines.size(); lineNum ++) {
                for (columnNum = 0; columnNum < lines.get(lineNum).length(); columnNum ++) {
                    String line = lines.get(lineNum);
                    char instruction = line.toCharArray()[columnNum];
                    if (instruction == '#') break;
                    if (!loopSkipping || instruction == ']')
                        parseChar(instruction);
                }
            }
        } catch (IOException e) {
            System.out.println("Unknown file!");
        } catch (ParsingException e) {
            System.out.println("Failed to parse program: " + e.getMessage());
            System.out.println("Heap Dump: " + Arrays.toString(heap));
        }
    }

    private static void parseChar(char instruction) throws ParsingException {
        switch(instruction) {
            case '<':
                cursor --;
                if (cursor < 0) {
                    throw new ParsingException("Cursor pointing to negative heapspace.");
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
                    throw new ParsingException("Failed to write to output: " + e.getMessage());
                }
                break;
            case ',':
                try {
                    heap[cursor] = (char) input.read();
                } catch (IOException e) {
                    throw new ParsingException("Failed to read from input: " + e.getMessage());
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
                        lineNum = pos.line;
                        columnNum = pos.column - 1;
                    } else {
                        throw new ParsingException("Found end of loop without beginning.");
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
                        throw new ParsingException("Webserver is already running.");
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
                    throw new ParsingException("Failed to create webserver. " + e.getMessage());
                }
                break;
            case '@':
                if (server == null) {
                    throw new ParsingException("Webserver must be created before setting stream.");
                }
                if (!networkStreamOpen) {
                    columnNum--;
                    break;
                }
                input = netInput;
                break;
            case '!':
                if (server == null) {
                    throw new ParsingException("Webserver must be created before setting stream.");
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
                    throw new ParsingException("Webserver must be created before it can be shutdown.");
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
                    throw new ParsingException("Failed to access file. " + e.getMessage());
                }
                break;
            case 'o':
                if (file == null) {
                    throw new ParsingException("File must be loaded before setting stream.");
                }
                output = fileOutput;
                break;
            case 'i':
                if (file == null) {
                    throw new ParsingException("File must be loaded before setting stream.");
                }
                input = fileInput;
                break;
            case 'p':
                if (file == null) {
                    throw new ParsingException("File must be loaded before clearing.");
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
                    throw new ParsingException("Failed to clear file. " + e.getMessage());
                }

                break;
            case 'e':
                if (file == null) {
                    throw new ParsingException("File must be loaded before clearing.");
                }

                file = null;
                try {
                    fileOutput.close();
                    fileInput.close();
                } catch (IOException e) {
                    throw new ParsingException("Failed to close file. " + e.getMessage());
                }
            case ':':
                System.out.println(Arrays.toString(heap));
                break;
        }
    }

    private static void expandHeap() {
        heapLength <<= 1;
        heap = Arrays.copyOf(heap, heapLength);
    }

    private static final class ParsingException extends Exception {
        ParsingException(String message) {
            super(message + " [" + fileName + ":" + (lineNum+1) + ":" + (columnNum+1) + "]");
        }
    }

    private static final class LinePos {
        int line;
        int column;

        LinePos(int line, int column) {
            this.line = line;
            this.column = column;
        }
    }
}
