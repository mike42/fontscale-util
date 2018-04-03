package misc.fontscale;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class RasterGlyphTest {
    final String AT_SYMBOL = "000000001C224A565252524E201E0000";
    final String EMPTY_SYMBOL = "00000000000000000000000000000000";
    final String NUL = "AAAA00018000000180004A51EA505A51C99E0001800000018000000180005555";

    @Test
    void testToVectorGlyph() {
        RasterGlyph foo = RasterGlyph.fromUnifontHex(AT_SYMBOL);
        VectorGlyph bar = foo.toVectorGlyph();
        assertEquals(30, bar.getVertices().size());
    }
    
    @Test
    void testFromUnifontHexSingleWidth() {
        RasterGlyph foo = RasterGlyph.fromUnifontHex(AT_SYMBOL);
        assertEquals(8,  foo.getWidth());
        assertEquals(16,  foo.getHeight());
        assertEquals("--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "---###--\n" + 
                "--#---#-\n" + 
                "-#--#-#-\n" + 
                "-#-#-##-\n" + 
                "-#-#--#-\n" + 
                "-#-#--#-\n" + 
                "-#-#--#-\n" + 
                "-#--###-\n" + 
                "--#-----\n" + 
                "---####-\n" + 
                "--------\n" + 
                "--------\n", foo.toString());
    }

    @Test
    void testFromUnifontHexDoubleWidth() {
        RasterGlyph foo = RasterGlyph.fromUnifontHex(NUL);
        assertEquals(16,  foo.getWidth());
        assertEquals(16,  foo.getHeight());
        assertEquals(
                "#-#-#-#-#-#-#-#-\n" + 
                "---------------#\n" + 
                "#---------------\n" + 
                "---------------#\n" + 
                "#---------------\n" + 
                "-#--#-#--#-#---#\n" + 
                "###-#-#--#-#----\n" + 
                "-#-##-#--#-#---#\n" + 
                "##--#--##--####-\n" + 
                "---------------#\n" + 
                "#---------------\n" + 
                "---------------#\n" + 
                "#---------------\n" + 
                "---------------#\n" + 
                "#---------------\n" + 
                "-#-#-#-#-#-#-#-#\n", foo.toString());
    }
    
    @Test
    void testEquals() {
        RasterGlyph foo = RasterGlyph.fromUnifontHex(NUL);
        RasterGlyph bar = RasterGlyph.fromUnifontHex(AT_SYMBOL);
        assertFalse(foo.equals(bar) || bar.equals(foo));
        // Wrong size still
        foo.clear();
        bar.clear();
        assertFalse(foo.equals(bar) || bar.equals(foo));
        // But blank symbols of same size are equal
        RasterGlyph baz = RasterGlyph.fromUnifontHex(EMPTY_SYMBOL);
        assertTrue(bar.equals(baz));
        assertFalse(foo.equals(baz));
    }
    
    @Test
    void testLineVertical() {
        RasterGlyph foo = new RasterGlyph(8, 8);
        foo.line(0, 0, 0, 7);
        foo.line(6, 6, 7, 0);
        assertEquals(
                "#-----#-\n" + 
                "#-----#-\n" + 
                "#-----#-\n" + 
                "#-----#-\n" + 
                "#-----#-\n" + 
                "#-----#-\n" + 
                "#-----#-\n" + 
                "#-----#-\n", foo.toString());
    }
    
    @Test
    void testLineHorizontal() {
        RasterGlyph foo = new RasterGlyph(8, 8);
        foo.line(0, 7, 0, 0);
        foo.line(7, 0, 6, 6);
        assertEquals(
                "########\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "########\n" + 
                "--------\n", foo.toString());
    }
    
    @Test
    void testLineDiagonalDown() {
        RasterGlyph foo = new RasterGlyph(8, 8);
        // increasing Y, increasing x
        foo.line(0, 7, 0, 7);
        // increasing Y, decreasing X
        foo.line(6, 0, 0, 6);
        assertEquals(
                "#-----#-\n" + 
                "-#---#--\n" + 
                "--#-#---\n" + 
                "---#----\n" + 
                "--#-#---\n" + 
                "-#---#--\n" + 
                "#-----#-\n" + 
                "-------#\n", foo.toString());
    }
    
    @Test
    void testLineDiagonalUp() {
        RasterGlyph foo = new RasterGlyph(8, 8);
        // decreasing Y, increasing x
        foo.line(7, 0, 7, 0);
        // decreasing Y, decreasing X
        foo.line(0, 6, 6, 0);
        assertEquals(
                "#-----#-\n" + 
                "-#---#--\n" + 
                "--#-#---\n" + 
                "---#----\n" + 
                "--#-#---\n" + 
                "-#---#--\n" + 
                "#-----#-\n" + 
                "-------#\n", foo.toString());
    }
    
    @Test
    void testLineSteep() {
        RasterGlyph baz = new RasterGlyph(8, 16);
        baz.line(2, 1, 9, 12);
        assertEquals("--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--#-----\n" + 
                "--#-----\n" + 
                "-#------\n" + 
                "-#------\n" + 
                "--------\n" + 
                "--------\n" + 
                "--------\n", baz.toString());
    }
}
