package misc.fontscale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

public class RasterGlyph implements Cloneable {
    public static RasterGlyph fromUnifontHex(final String hex) {
        final byte[] content = DatatypeConverter.parseHexBinary(hex);
        final int height = 16;
        final int width = (content.length * 8) / height;
        final boolean[][] data = new boolean[height][width];
        for (int i = 0; i < (width * height); i++) {
            final int x = i % width;
            final int y = i / width;
            final int curBit = i % 8;
            final int curByte = i / 8;
            data[y][x] = ((content[curByte] >> (7 - curBit)) & 1) == 1;
        }
        return new RasterGlyph(width, height, data);
    }

    private final boolean[][] data;

    private final int height;

    private final int width;

    /**
     * New, blank raster glyph.
     *
     * @param width
     *            Width of raster
     * @param height
     *            Height of raster
     */
    public RasterGlyph(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.data = new boolean[height][width];
        this.clear();
    }

    public RasterGlyph(final int width, final int height, final boolean[][] data) {
        this.width = width;
        this.height = height;
        this.data = new boolean[height][width];
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.data[y][x] = data[y][x];
            }
        }
    }

    public RasterGlyph(RasterGlyph baseGlyph) {
        this(baseGlyph.width, baseGlyph.height, baseGlyph.data);
    }

    /**
     * Clear all pixels in raster
     */
    public void clear() {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.data[y][x] = false;
            }
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final RasterGlyph other = (RasterGlyph) obj;
        if (!Arrays.deepEquals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean getPixel(final int x, final int y) {
        return this.data[y][x];
    }

    public int getWidth() {
        return this.width;
    }

    public void setPixel(final int x, final int y, final boolean value) {
        if ((x < 0) || (x >= this.width)) {
            throw new IndexOutOfBoundsException("x out of range");
        }
        if ((y < 0) || (y >= this.height)) {
            throw new IndexOutOfBoundsException("y out of range");
        }
        this.data[y][x] = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder((this.width + 1) * this.height);
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                sb.append(this.getPixel(x, y) ? '#' : '-');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public VectorGlyph toVectorGlyph() {
        final VectorGlyph glyph = new VectorGlyph(this.width, this.height);
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (data[y][x]) {
                    glyph.addVertex(x, y);
                }
            }
        }
        return glyph;
    }

    public void line(int x0, int x1, int y0, int y1) {
        // https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
        if (Math.abs(y1 - y0) < Math.abs(x1 - x0)) {
            if (x0 > x1) {
                lineLow(x1, y1, x0, y0);
            } else {
                lineLow(x0, y0, x1, y1);
            }
        } else {
            if (y0 > y1) {
                lineHigh(x1, y1, x0, y0);
            } else {
                lineHigh(x0, y0, x1, y1);
            }
        }
    }

    private void lineHigh(int x0, int y0, int x1, int y1) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int xi = 1;
        if (dx < 0) {
            xi = -1;
            dx = -dx;
        }
        int D = 2 * dx - dy;
        int x = x0;
        for (int y = y0; y <= y1; y++) {
            this.setPixel(x, y, true);
            if (D > 0) {
                x = x + xi;
                D = D - 2 * dy;
            }
            D = D + 2 * dx;
        }
    }

    private void lineLow(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int yi = 1;
        if (dy < 0) {
            yi = -1;
            dy = -dy;
        }
        int D = 2 * dy - dx;
        int y = y1;
        for (int x = x1; x <= x2; x++) {
            this.setPixel(x, y, true);
            if (D > 0) {
                y = y + yi;
                D = D - 2 * dx;
            }
            D = D + 2 * dy;
        }
    }
    // if (x2 == x1) {
    // return lineVertical(x1, y1, y2);
    // }
    // if (x2 < x1) {
    // // Reverse
    // return line(x2, x1, y2, y1);
    // }
    // if (y2 == y1) {
    // // Shortcut for straight horizontal
    //// for (int x = x1; x <= x2; x++) {
    //// this.setPixel(x, y1, true);
    //// }
    // return;
    // }

    public void invert() {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                data[y][x] = !data[y][x];
            }
        }
    }

    public void writeAsPbm(String dstFile) throws IOException {
        // Construct file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("P4\n" + width + " " + height + "\n").getBytes());
        for(int y = 0; y < height; y++) {
            byte[] row = new byte[(width + 7) / 8];
            for(int x = 0; x < width; x++) {
                int byteNum = x / 8;
                int bitNum = x % 8;
                int val = this.getPixel(x, y) ? 1 : 0;
                row[byteNum] |= (val << (7 - bitNum));
            }
            baos.write(row);
        }
        // Write output
        Path path = Paths.get(dstFile);
        Files.write(path, baos.toByteArray());
    }

    // https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    // int dx = x2 - x1;
    // int dy = y2 - y1;
    // int D = 2 * dy - dx;
    // int y = y1;
    // for (int x = x1; x <= x2; x++) {
    // this.setPixel(x, y, true);
    // if (D > 0) {
    // y = y + 1;
    // D = D - 2 * dx;
    // }
    // D = D + 2 * dy;
    // }
    // double deltax = x2 - x1;
    // double deltay = y2 - y1;
    // double deltaerr = Math.abs(deltay / deltax); // Assume deltax != 0 (line is
    // not vertical),
    // // note that this division needs to be done in a way that preserves the
    // // fractional part
    // double error = 0.0D; // No error at start
    // int y = y1;
    // for (int x = x1; x <= x2; x++) {
    // this.setPixel(x, y, true);
    // error = error + deltaerr;
    // while (error >= 0.5D) {
    // y = y + (deltay < 0 ? -1 : 1);
    // error = error - 1.0D;
    // }
    // }
    // return 0;
    // }
    //
    // private int lineVertical(int x, int y1, int y2) {
    // // Shortcut for straight vertical
    // if (y2 < y1) {
    // return lineVertical(x, y2, y1);
    // }
    // for (int y = y1; y <= y2; y++) {
    // this.setPixel(x, y, true);
    // }
    // return y2 - y1 + 1;
    // }
}
