package com.me4502.wutlang;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
 *  'c' - Set console as output.
 *  '$' - Open network connections on localhost at port specified as characters until 0
 *  '|' - Read from network stream at context specified.
 *
 *  Miscellaneous
 *
 *  '#' - Comment character. This line is a comment.
 *          Supports inline comments.
 */
public class Wutlang {

    private static int cursor = 0;

    private static char[] heap = new char[1];
    private static int heapLength = 1;

    private static Stack<Character> stack = new Stack<>();

    private static PrintStream fileOutput;
    private static PrintStream netOutput;

    private static PrintStream output;
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
                output.print(heap[cursor]);
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
                String port = "";
                char read;
                while ((read = heap[cursor]) != 0) {
                    cursor ++;
                    port = port + read;
                }
                try {
                    server = HttpServer.create(new InetSocketAddress("localhost", Integer.parseInt(port)), 0);
                } catch (IOException e) {
                    throw new ParsingException("Failed to create webserver. " + e.getMessage());
                }
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

        public LinePos(int line, int column) {
            this.line = line;
            this.column = column;
        }
    }
}