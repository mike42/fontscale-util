package misc.fontscale.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import misc.fontscale.Geometry;
import misc.fontscale.RasterGlyph;
import misc.fontscale.VectorGlyph;
import picocli.CommandLine;


public class FontscaleApp {

    public static void main(String[] args) {
        System.out.println(Arrays.asList(args));
        if(args[0].equals("scale")) {
            String hex = args[1];
            String srcGeometrySpec = args[2];
            String dimensions = args[3];
            String dstGeometrySpec = args[4];
            String dstFile = args[5];
            RasterGlyph originalRaster = RasterGlyph.fromUnifontHex(args[1]);
            VectorGlyph originalVector = originalRaster.toVectorGlyph();
            originalVector.joinAdjacentVertices();
            originalVector.disconnectDottedOutline();
            originalVector.disconnectFilledAreas();
            originalVector.combineEdges();
            // Geometry work..
            Geometry srcGeometry = originalVector.getGeometry();
            if(srcGeometrySpec.equals("detect")) {
                srcGeometry = originalVector.getInternalGeometry();
            } else if(!srcGeometrySpec.equals("full")) {
                srcGeometry = new Geometry(srcGeometrySpec);
            }
            Geometry dstCanvas = new Geometry(dimensions);
            Geometry dstGeometry = dstCanvas;
            if(!dstGeometrySpec.equals("full")) {
                dstGeometry = new Geometry(dstGeometrySpec);
            }
            // Do the scaling..
            VectorGlyph newVector = new VectorGlyph(dstCanvas.getWidth(), dstCanvas.getHeight());
            newVector.copyFrom(originalVector, srcGeometry, dstGeometry);
            RasterGlyph newRaster = newVector.toRasterGlyph();
            try {
                Files.write(Paths.get(dstFile + "-unscaled.svg"), originalVector.toSvg().getBytes());
                Files.write(Paths.get(dstFile + "-scaled.svg"), newVector.toSvg().getBytes());
                Files.write(Paths.get(dstFile + "-scaled.txt"), newRaster.toString().getBytes());
                newRaster.writeAsPbm(dstFile);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        } else if(args[0] == "debug") {
            // TODO picocli CommandLine commandLine = new CommandLine(....);
            RasterGlyph foo = RasterGlyph.fromUnifontHex(args[1]);
            VectorGlyph bar = foo.toVectorGlyph();
            bar.joinAdjacentVertices();
            bar.disconnectDottedOutline();
            bar.disconnectFilledAreas();
            bar.setDebug(args[0].substring(0, args[0].indexOf('.')));
            bar.combineEdges();
            String svg = bar.toSvg();
            try (PrintWriter out = new PrintWriter(args[0])) {
              out.println(svg);
          } catch (FileNotFoundException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }
          return;
        }
        throw new RuntimeException("Not implemented..");
    }
}
