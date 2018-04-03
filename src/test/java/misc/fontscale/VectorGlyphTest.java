package misc.fontscale;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

import misc.fontscale.cli.FontscaleApp;

public class VectorGlyphTest {

    @Test
    void testBlank() {
        VectorGlyph foo = new VectorGlyph(2, 1);
        assertEquals(0, foo.getVertices().size());
        // Blank when converted to raster
        RasterGlyph bar = foo.toRasterGlyph();
        assertEquals("--\n", bar.toString());
        // Set to solid
        foo.addVertex(0, 0);
        bar = foo.toRasterGlyph();
        assertEquals("#-\n", bar.toString());
    }

    @Test
    void testSvg() {
        RasterGlyph foo = RasterGlyph.fromUnifontHex("000000001C224A565252524E201E0000");
        VectorGlyph bar = foo.toVectorGlyph();
        bar.joinAdjacentVertices();
        bar.combineEdges();
        String svg = bar.toSvg();
        // Can't really validate the SVG, but we know there's no exceptions at least.
    }
    
    @Test
    void tmp() {
    //    FontscaleApp.main(new String[] {"scale", "00000000182442464A52624224180000", "detect", "12x24", "11x20+0+1", "zero.pbm"});
    }
//        RasterGlyph foo = RasterGlyph.fromUnifontHex("00007FFE738E6DF66DC66DF6738E7FFE7FFE73CE6DB673B66DB673CE7FFE0000");
//        foo.invert();
//        VectorGlyph bar = foo.toVectorGlyph();
//        //bar.invert(); TODO detect and invert for placeholder letters
//        bar.joinAdjacentVertices();
//
//        bar.disconnectDottedOutline();
//        bar.setDebug("aaaa");
//        bar.combineEdges();
//        String svg = bar.toSvg();
//        try (PrintWriter out = new PrintWriter("aaaa.svg")) {
//          out.println(svg);
//      } catch (FileNotFoundException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }
//    }
}
