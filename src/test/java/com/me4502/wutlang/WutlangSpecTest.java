/*
 * Copyright (c) 2016-2017 Me4502 (Madeline Miller)
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class WutlangSpecTest {

    @Test
    public void testCursorMovement() {
        WutlangParser parser = new WutlangParser(">><");
        assertTrue(parser.getCursor() == 0);
        try {
            parser.parseProgram();
        } catch (ParsingException e) {
            fail(e.getMessage());
        }
        assertTrue(parser.getCursor() == 1);
    }

    @Test(expected = ParsingException.class)
    public void testCursorOutOfBounds() throws ParsingException {
        WutlangParser parser = new WutlangParser("<");
        parser.parseProgram();
    }

    @Test
    public void testHeap() {
        WutlangParser parser = new WutlangParser("++>+>+++>++++");
        try {
            parser.parseProgram();
        } catch (ParsingException e) {
            fail(e.getMessage());
        }
        assertArrayEquals(parser.getHeap(), new char[] {2, 1, 3, 4});
    }

    @Test
    public void testHeap2() {
        WutlangParser parser = new WutlangParser("++--+>+----->+>--+");
        try {
            parser.parseProgram();
        } catch (ParsingException e) {
            fail(e.getMessage());
        }
        assertArrayEquals(parser.getHeap(), new char[] {1, (char) -4, 1, (char) -1});
    }

    @Test
    public void testForLoop() {
        WutlangParser parser = new WutlangParser("++++++++++[-]");
        try {
            parser.parseProgram();
        } catch (ParsingException e) {
            fail(e.getMessage());
        }
        assertTrue(parser.getHeap()[parser.getCursor()] == 0);
    }

    @Test
    public void testNestedForLoop() {
        WutlangParser parser = new WutlangParser("[[]]");
        try {
            parser.parseProgram();
        } catch (ParsingException e) {
            fail(e.getMessage());
        }
        assertTrue(parser.getHeap()[parser.getCursor()] == 0);
    }

    @Test
    public void testNestedForLoop2() {
        WutlangParser parser = new WutlangParser(">++>++>++[[-]<]");
        try {
            parser.parseProgram();
        } catch (ParsingException e) {
            fail(e.getMessage());
        }
        assertArrayEquals(parser.getHeap(), new char[]{0, 0, 0, 0});
    }
}
