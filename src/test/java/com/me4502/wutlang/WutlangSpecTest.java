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
}
