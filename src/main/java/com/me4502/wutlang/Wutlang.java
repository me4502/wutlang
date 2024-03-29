/*
 * Copyright (c) 2016-2021 Me4502 (Maddy Miller)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.me4502.wutlang;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * **Wutlang version 1.1**
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
 *          In format: IP METHOD URL BODY
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

    public static void main(String[] args) {
        WutlangParser parser = null;
        try {
            String fileName = args[0];
            parser = new WutlangParser(new File(fileName));
            parser.parseProgram();
        } catch (IOException e) {
            System.out.println("Unknown file!");
        } catch (ParsingException e) {
            System.out.println("Failed to parse program: " + e.getMessage());
            if (parser != null) {
                System.out.println("Heap Dump: " + Arrays.toString(parser.getHeap()));
                System.out.println("Stack Dump: " + parser.getStack().toString());
                System.out.println("Cursor Position: " + parser.getCursor());
            }
        }
    }
}
