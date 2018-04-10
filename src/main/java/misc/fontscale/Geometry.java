package misc.fontscale;

public class Geometry {
    private int width;
    private int height;
    private int offsetX;
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    private int offsetY;

    public Geometry(String spec) {
      String[] part = spec.split("[x\\+]");
      this.width = Integer.parseInt(part[0]);
      this.height = Integer.parseInt(part[1]);
      if(part.length > 2) {
          this.offsetX = Integer.parseInt(part[2]);
      }
      if(part.length > 3) {
          this.offsetY = Integer.parseInt(part[3]);
      }
    }

    public Geometry(int width, int height) {
        this(width, height, 0, 0);
    }

    public Geometry(int width, int height, int offsetX, int offsetY) {
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
    
    public String toString() {
        return width + "x" + height + "+" + offsetX + "+" + offsetY; 
    }
    
    public DiscretePoint transformPoint(int x, int y, Geometry dst) {
        int newX = transformPoint(x - this.offsetX, this.width, dst.width) + dst.offsetX;
        int newY = transformPoint(y - this.offsetY, this.height, dst.height) + dst.offsetY;
        return new DiscretePoint(newX, newY);
    }

    private int transformPoint(int val, int oldSize, int newSize) {
        // Linear interpolation: 0 => 0, (oldWidth - 1) => (newWidth - 1).
        if(oldSize == 1) {
            return 0;
        }
        return (int)(val * (newSize - 1) / (oldSize - 1));
    }
}
