package misc.fontscale;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

public class VectorGlyph {
    public class Vertex implements Comparable<Vertex> {
        boolean mark = false;

        private final Set<Vertex> neighbours = new TreeSet<>();

        final int x;

        final int y;

        public Vertex(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(final Vertex o) {
            final int a = Integer.compare(this.y, o.y);
            if (a != 0) {
                return a;
            }
            return Integer.compare(this.x, o.x);
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
            final Vertex other = (Vertex) obj;
            if (!this.getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (this.x != other.x) {
                return false;
            }
            if (this.y != other.y) {
                return false;
            }
            return true;
        }

        public Set<Vertex> getNeighbours() {
            return this.neighbours;
        }

        private VectorGlyph getOuterType() {
            return VectorGlyph.this;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = (prime * result) + this.getOuterType().hashCode();
            result = (prime * result) + this.x;
            result = (prime * result) + this.y;
            return result;
        }

        public boolean isMarked() {
            return this.mark;
        }

        public void join(final Vertex other) {
            this.neighbours.add(other);
            other.neighbours.add(this);
        }

        public void setMark(final boolean mark) {
            this.mark = mark;
        }

        public void unjoin(final Vertex other) {
            this.neighbours.remove(other);
            other.neighbours.remove(this);
        }

        public void unjoinAll(final Collection<Vertex> other) {
            for (final Vertex o : other) {
                this.unjoin(o);
            }
        }
    }

    private String debugPrefix = null;

    final int height;

    private int revision = 0;

    final Map<Integer, Vertex> vertices = new TreeMap<>();

    final int width;

    public VectorGlyph(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    public void addVertex(final int x, final int y) {
        final int key = (y * this.width) + x;
        this.vertices.put(key, new Vertex(x, y));
    }

    private List<List<Vertex>> allCandidates() {
        final List<List<Vertex>> ret = new ArrayList<>();
        final Stack<Vertex> subList = new Stack<>();
        for (final Vertex v1 : this.vertices.values()) {
            this.allCandidates(ret, subList, v1, v1);
        }
        return ret;
    }

    private void allCandidates(final List<List<Vertex>> ret, final Stack<Vertex> subList, final Vertex base,
            final Vertex v1) {
        if (ret.size() > 100000) {
            System.err.println(
                    "Glyph has a lot of paths, trace is no good. Can you invert it or make the lines thinner?");
            // Combinatorial explosion, give up.
            return;
        }
        v1.setMark(true);
        subList.push(v1);
        final double baseDistance = this.geometricLen(base, v1);
        for (final Vertex v2 : v1.getNeighbours()) {
            if (v2.isMarked()) {
                continue;
            }
            if ((this.geometricLen(base, v2) > baseDistance) && (subList.size() < 16)) {
                this.allCandidates(ret, subList, base, v2);
            }
        }
        if (subList.size() > 2) {
            ret.add(new ArrayList<>(subList));
        }
        v1.setMark(false);
        subList.pop();
    }

    private void collapse(final List<Vertex> vtx) {
        final RasterGlyph before = this.toRasterGlyph();
        // Look for nodes that can be eliminated completely
        final List<Vertex> newLine = new ArrayList<>();
        final List<Vertex> deleteMe = new ArrayList<>();
        for (final Vertex v1 : vtx) {
            if (this.isRedundant(v1, vtx)) {
                deleteMe.add(v1);
            } else {
                newLine.add(v1);
            }
        }
        // Clear unwanted nodes
        for (final Vertex v1 : deleteMe) {
            final List<Vertex> neighbours = new ArrayList<>(v1.getNeighbours());
            v1.unjoinAll(neighbours);
        }
        this.vertices.values().removeAll(deleteMe);
        // Join new line together
        final Iterator<Vertex> lineIt = newLine.iterator();
        Vertex prev = lineIt.next();
        while (lineIt.hasNext()) {
            final Vertex cur = lineIt.next();
            prev.join(cur);
            prev = cur;
        }
        final RasterGlyph after = this.toRasterGlyph();
        if (!after.equals(before)) {
            // Merge changed raster output. This usually involves
            // diagonals that are crossing things, so we can try cutting through more
            // severely to
            // salvage the glyph.
            // We also flag this as a warning, because a human mau prefer NOT to join
            // non-45 degree diagonals in such a configuration depending on how complex the
            // glyph is.
            System.err.println("Warning: Had some trouble tracing this glyph. Manually review trace result.");
            this.collapseFully(newLine);
            return;
        }
    }

    void collapseFully(final List<Vertex> vtx) {
        final List<Vertex> newLine = new ArrayList<>();
        final List<Vertex> deleteMe = new ArrayList<>();
        final int firstIndex = 0;
        final int lastIndex = vtx.size() - 1;
        for (int i = 0; i <= lastIndex; i++) {
            if ((i == firstIndex) || (i == lastIndex)) {
                newLine.add(vtx.get(i));
            } else {
                deleteMe.add(vtx.get(i));
            }
        }
        // Clear unwanted nodes
        for (final Vertex v1 : deleteMe) {
            final List<Vertex> neighbours = new ArrayList<>(v1.getNeighbours());
            v1.unjoinAll(neighbours);
        }
        this.vertices.values().removeAll(deleteMe);
        // Join new line together
        newLine.get(0).join(newLine.get(1));
    }

    private void combineEdge() {
        final int size = this.getVertices().size();
        // Every possible way to walk through current structure (hundreds)
        final List<List<Vertex>> paths = this.allCandidates();
        // Look at longest path first
        Collections.sort(paths, (lhs, rhs) -> this.compareVertexCandidate(lhs, rhs));
        // Filter for first candidate that does not modify the glyph
        for (final List<Vertex> vtx : paths) {
            final Vertex vtx1 = vtx.get(0);
            final Vertex vtx2 = vtx.get(vtx.size() - 1);
            if (this.isDiagonal(vtx)) {
                // Completely exclude 3-segment diagonals that are not a 45 degree angle.
                if ((Math.abs(vtx1.getX() - vtx2.getX()) < 2) || (Math.abs(vtx1.getY() - vtx2.getY()) < 2)) {
                    continue;
                }
            }

            // Draw to a single line from start to finish
            final RasterGlyph nuGlyph = new RasterGlyph(this.width, this.height);
            nuGlyph.line(vtx1.getX(), vtx2.getX(), vtx1.getY(), vtx2.getY());
            // Draw the entire chain of vertices as lines
            final RasterGlyph baseGlyph = new RasterGlyph(this.width, this.height);
            for (int i = 0; i < (vtx.size() - 1); i++) {
                baseGlyph.line(vtx.get(i).getX(), vtx.get(i + 1).getX(), vtx.get(i).getY(), vtx.get(i + 1).getY());
            }
            // Skip now if output would change
            if (!nuGlyph.equals(baseGlyph)) {
                continue;
            }
            if (this.debugPrefix != null) {
                // Illustrates process for debugging...
                if (this.revision == 0) {
                    // Initial snapshot
                    final String svg = this.toSvg();
                    final String fn = String.format("%s-%03d.svg", this.debugPrefix, this.revision);
                    try (PrintWriter out = new PrintWriter(fn)) {
                        out.println(svg);
                    } catch (final FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                for (final Vertex v : vtx) {
                    v.setMark(true);
                }
            }
            this.collapse(vtx);
            if (this.debugPrefix != null) {
                // Set up for monitoring w/ breakpoint
                final String svg = this.toSvg();
                final String fn = String.format("%s-%03d.svg", this.debugPrefix, this.revision + 1);
                try (PrintWriter out = new PrintWriter(fn)) {
                    out.println(svg);
                } catch (final FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                for (final Vertex v : vtx) {
                    v.setMark(false);
                }
            }
            // Ho humm
            if (size != this.vertices.size()) {
                return;
            }
        }
    }

    public void combineEdges() {
        int len;
        do {
            len = this.vertices.size();
            this.combineEdge();
            this.revision++;
        } while (len != this.vertices.size());
    }

    private int compareVertexCandidate(final List<Vertex> lhs, final List<Vertex> rhs) {
        // // Compare on diagonality - diagonal first
        // if (lhsDiagonal != rhsDiagonal) {
        // return -Boolean.compare(lhsDiagonal, rhsDiagonal);
        // }
        // Compare on length - longer chain of vertices first
        final int vertexSizeCompare = Integer.compare(rhs.size(), lhs.size());
        if (vertexSizeCompare != 0) {
            return vertexSizeCompare;
        }
        // Compare on geometric distance - slight handicap for diagonals since they have
        // an advantage but will claim pixels in non-intuitive ways.
        final boolean lhsDiagonal = this.isDiagonal(lhs);
        final boolean rhsDiagonal = this.isDiagonal(rhs);
        final double lhsLen = this.geometricLen(lhs) - (lhsDiagonal ? 1 : 0);
        final double rhsLen = this.geometricLen(rhs) - (rhsDiagonal ? 1 : 0);
        return Double.compare(rhsLen, lhsLen);
    }

    public void copyFrom(final VectorGlyph originalVector, final Geometry srcGeometry,
            final Geometry dstGeometry) {
        // Avoiding problems if you copy in twice (unjoined edges)
        this.vertices.clear();
        // Vertices
        for (final Vertex v1 : originalVector.vertices.values()) {
            DiscretePoint target = srcGeometry.transformPoint(v1.getX(), v1.getY(), dstGeometry);
            this.addVertex(target.getX(), target.getY());
        }
        // Lines as SVG
        for (final Vertex v1 : originalVector.vertices.values()) {
            for (final Vertex v2 : v1.getNeighbours()) {
                // v1 is joined to v2 on original graph
                DiscretePoint target1 = srcGeometry.transformPoint(v1.getX(), v1.getY(), dstGeometry);
                DiscretePoint target2 = srcGeometry.transformPoint(v2.getX(), v2.getY(), dstGeometry);
                // v3, v4 will be joined on new graph
                Vertex v3 = this.getVertex(target1.getX(), target1.getY());
                Vertex v4 = this.getVertex(target2.getX(), target2.getY());
                v3.join(v4);
            }
        }
    }

    public void disconnect(final Vertex v1) {
        if (v1 == null) {
            return;
        }
        final List<Vertex> neighbours = new ArrayList<>(v1.getNeighbours());
        v1.unjoinAll(neighbours);
        v1.join(v1);
    }

    public void disconnectDottedOutline() {
        // Specific unifont heuristic
        if ((this.width != 16) || (this.height != 16)) {
            return;
        }
        if ((this.getVertex(0, 0) == null) || (this.getVertex(15, 15) == null) || (this.getVertex(15, 0) != null)
                || (this.getVertex(15, 0) != null)) {
            return;
        }
        for (int i = 1; i < 15; i++) {
            this.disconnect(this.getVertex(i, 0));
            this.disconnect(this.getVertex(15, i));
            this.disconnect(this.getVertex(15 - i, 15));
            this.disconnect(this.getVertex(0, 15 - i));
        }
    }

    public void disconnectFilledAreas() {
        for (final Vertex v1 : this.getVertices()) {
            if (v1.getNeighbours().size() >= 8) {
                v1.unjoinAll(new ArrayList<>(v1.getNeighbours()));
                v1.join(v1);
            }
        }
    }

    private double geometricLen(final List<Vertex> vtx) {
        final Vertex vtx1 = vtx.get(0);
        final Vertex vtx2 = vtx.get(vtx.size() - 1);
        return this.geometricLen(vtx1, vtx2);
    }

    private double geometricLen(final Vertex vtx1, final Vertex vtx2) {
        final double a = vtx1.getX() - vtx2.getX();
        final double b = vtx1.getY() - vtx2.getY();
        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
    }

    private Set<Vertex> getAdjacent(final Vertex v) {
        final Set<Vertex> ret = new TreeSet<>();
        for (int y = -1; y < 2; y++) {
            for (int x = -1; x < 2; x++) {
                if ((x == 0) && (y == 0)) {
                    continue;
                }
                final Vertex neighbour = this.getVertex(v.getX() + x, v.getY() + y);
                if (neighbour != null) {
                    ret.add(neighbour);
                }
            }
        }
        // If no neighbours, join to self
        if (ret.size() == 0) {
            ret.add(v);
        }
        return ret;
    }

    public Geometry getGeometry() {
        return new Geometry(this.width, this.height);
    }

    public Geometry getInternalGeometry() {
        if (this.vertices.size() == 0) {
            return this.getGeometry();
        }
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (final Vertex v1 : this.getVertices()) {
            if (v1.getX() < minX) {
                minX = v1.getX();
            }
            if (v1.getX() > maxX) {
                maxX = v1.getX();
            }
            if (v1.getY() < minY) {
                minY = v1.getY();
            }
            if (v1.getY() > maxY) {
                maxY = v1.getY();
            }
        }
        final int internalWidth = (maxX - minX) + 1;
        final int internalHeight = (maxY - minY) + 1;
        return new Geometry(internalWidth, internalHeight, minX, minY);
    }

    private Vertex getVertex(final int x, final int y) {
        if ((x < 0) || (x >= this.width)) {
            return null;
        }
        if ((y < 0) || (y >= this.height)) {
            return null;
        }
        return this.vertices.get((y * this.width) + x);
    }

    public Collection<Vertex> getVertices() {
        return this.vertices.values();
    }

    private boolean isDiagonal(final List<Vertex> vtx) {
        final Vertex vtx1 = vtx.get(0);
        final Vertex vtx2 = vtx.get(vtx.size() - 1);
        return ((vtx1.getX() != vtx2.getX()) && (vtx1.getY() != vtx2.getY()));
    }

    private boolean isLinear(final List<Vertex> ls, final Vertex v1) {
        // Planning to check here for evidence of not being able to fit a line to
        // this set of points, for pruning.
        if (ls.size() < 1) {
            return true;
        }
        if (this.geometricLen(ls.get(0), v1) > this.geometricLen(ls)) {
            return true;
        }
        return false;
        // // Determine reference points
        // final Vertex ref1 = ls.get(0);
        // final Vertex ref2 = ls.get(ls.size() - 1);
        // // X directions
        // if (ref2.getX() < ref1.getX()) {
        // return v1.getX() <= ref2.getX();
        // }
        // if (ref2.getX() > ref1.getX()) {
        // return v1.getX() >= ref2.getX();
        // }
        // // Y directions
        // if (ref2.getY() < ref1.getY()) {
        // return v1.getY() <= ref2.getY();
        // }
        // if (ref2.getY() > ref1.getY()) {
        // return v1.getY() >= ref2.getY();
        // }
        // return true;
    }

    private boolean isRedundant(final Vertex v1, final List<Vertex> vtx) {
        final Vertex first = vtx.get(0);
        final Vertex last = vtx.get(vtx.size() - 1);
        if (v1.equals(first) || v1.equals(last)) {
            // Ends are never redundant
            return false;
        }
        if (vtx.containsAll(v1.getNeighbours())) {
            // Joined only to other line components - always redundant
            return true;
        }
        // Determine correct vertex for each external neighbour - this
        // vertex can be deleted if we are not the answer for any of these
        final List<Vertex> neighbours = new ArrayList<>(v1.getNeighbours());
        neighbours.removeAll(vtx);
        boolean redundant = true;
        for (final Vertex v2 : neighbours) {
            if (this.shouldConnect(v1, v2, vtx)) {
                redundant = false;
            }
        }
        // If no hits, we are redundant.
        return redundant;
    }

    public void joinAdjacentVertices() {
        // Move from series of dots to use lazily joined lines
        for (final Vertex v : this.vertices.values()) {
            for (final Vertex neighbour : this.getAdjacent(v)) {
                v.join(neighbour);
            }
        }
    }

    public void setDebug(final String substring) {
        this.debugPrefix = substring;
    }

    // Determine whether v1 this is the correct connection place for v2 vertex in
    // the list of vertices vtx.
    // v1 must be in vtx, and joined to v2.
    private boolean shouldConnect(final Vertex v1, final Vertex v2, final List<Vertex> vtx) {
        // Start by building the contiguous block of vertices from v1 that contain v2 as
        // a member.
        final Deque<Vertex> memberList = new ArrayDeque<>();
        // Handle actual vertex v1
        final int thisIndex = vtx.indexOf(v1);
        final int lastIndex = vtx.size() - 1;
        boolean containsFirst = thisIndex == 0;
        boolean containsLast = thisIndex == lastIndex;
        memberList.add(v1);
        // Handle vertices before
        for (int i = thisIndex - 1; i >= 0; i--) {
            final Vertex v3 = vtx.get(i);
            if (!v3.getNeighbours().contains(v2)) {
                break;
            }
            if (i == 0) {
                containsFirst = true;
            }
            memberList.addFirst(v3);
        }
        // Handle vertices after
        for (int i = thisIndex + 1; i < vtx.size(); i++) {
            final Vertex v3 = vtx.get(i);
            if (!v3.getNeighbours().contains(v2)) {
                break;
            }
            if (i == lastIndex) {
                containsLast = true;
            }
            memberList.addLast(v3);
        }
        // If only one item was found, it wins
        if (memberList.size() == 1) {
            return true;
        }
        // If the number is odd, the middle is selected.
        if ((memberList.size() % 2) == 1) {
            while (memberList.size() > 1) {
                memberList.removeLast();
                memberList.removeFirst();
            }
            return memberList.peekFirst().equals(v1);
        }
        // If exactly one member of the list is an end, that end is selected.
        if (containsFirst ^ containsLast) {
            if (containsFirst) {
                return thisIndex == 0;
            }
            return thisIndex == lastIndex;
        }

        // If the number is even, then the farthest from the edge is removed, and then
        // the middle is selected.
        // TODO on that one.
        return true;
    }

    public RasterGlyph toRasterGlyph() {
        return this.toRasterGlyph(RasterOption.ALL);
    }

    public RasterGlyph toRasterGlyph(final RasterOption option) {
        final RasterGlyph ret = new RasterGlyph(this.width, this.height);
        for (final Vertex v : this.vertices.values()) {
            if (option != RasterOption.LINES_ONLY) {
                ret.setPixel(v.getX(), v.getY(), true);
            }
            if (option != RasterOption.DOTS_ONLY) {
                for (final Vertex o : v.getNeighbours()) {
                    ret.line(v.getX(), o.getX(), v.getY(), o.getY());
                }
            }
        }
        return ret;
    }

    public String toSvg() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" standalone=\"no\"?>\n");
        sb.append("<svg width=\"" + (this.width * 10) + "\" height=\"" + (this.height * 10)
                + "\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">\n");
        // Raster rendering as backing
        final RasterGlyph glyph = this.toRasterGlyph(RasterOption.LINES_ONLY);
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (!glyph.getPixel(x, y)) {
                    continue;
                }
                sb.append("    <rect x=\"" + ((x * 10) + 1) + "\" y=\"" + ((y * 10) + 1)
                        + "\" width=\"8\" height=\"8\" stroke=\"none\" fill=\"#ccc\" stroke-width=\"0\"/>\n");
            }
        }

        // Lines as SVG
        for (final Vertex v : this.vertices.values()) {
            for (final Vertex o : v.getNeighbours()) {
                sb.append("    <line x1=\"" + ((v.getX() * 10) + 5) + "\" x2=\"" + ((o.getX() * 10) + 5) + "\" y1=\""
                        + ((v.getY() * 10) + 5) + "\" y2=\"" + ((o.getY() * 10) + 5)
                        + "\" stroke=\"blue\" stroke-width=\"1\"/>");
            }
        }
        // Vertices
        for (final Vertex v : this.vertices.values()) {
            String color = "red";
            if (v.isMarked()) {
                color = "yellow";
            }
            sb.append("    <circle cx=\"" + ((v.getX() * 10) + 5) + "\" cy=\"" + ((v.getY() * 10) + 5)
                    + "\" r=\"1\" stroke=\"" + color + "\" fill=\"" + color + "\" stroke-width=\"1\"/>\n");
        }
        sb.append("</svg>\n");
        return sb.toString();
    }

}
