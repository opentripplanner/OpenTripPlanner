/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.common.geometry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.opentripplanner.analyst.request.SampleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Compute isoline based on a zFunc and a set of initial coverage points P0={(x,y)} to seed the
 * computation.
 * 
 * This assume we have a z = Fz(x,y) function which can gives a z value for any given point (x,y), z
 * can be undefined for some region.
 * 
 * There are many tricks to reduce to the minimum the numbers of Fz samplings, explained in the code
 * below.
 * 
 * It will compute an isoline for a given z0 value. The isoline is composed of a list of n polygons,
 * CW for normal polygons, CCW for "holes". The isoline computation can be called multiple times on
 * the same builder for different z0 values: this will reduce the number of Fz sampling as they are
 * cached in the builder. Please note that the initial covering points must touch all isolines you
 * want to cover.
 * 
 * @author laurent
 */
public class RecursiveGridIsolineBuilder {

    public interface ZFunc {
        public long z(Coordinate c);
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;
    }

    /**
     * A fast XY-index for tiles, allowing to use maps.
     */
    private static class XYIndex {

        private int xIndex;

        private int yIndex;

        private XYIndex(int xIndex, int yIndex) {
            this.xIndex = xIndex;
            this.yIndex = yIndex;
        }

        private final XYIndex translated(int dx, int dy) {
            return new XYIndex(this.xIndex + dx, this.yIndex + dy);
        }

        @Override
        public final int hashCode() {
            return xIndex >> 16 | yIndex;
        }

        @Override
        public final boolean equals(Object other) {
            if (other instanceof XYIndex) {
                XYIndex otherTileIndex = (XYIndex) other;
                return otherTileIndex.xIndex == this.xIndex && otherTileIndex.yIndex == this.yIndex;
            }
            return false;
        }

        @Override
        public final String toString() {
            return "(" + xIndex + "," + yIndex + ")";
        }
    }

    private static class GridDot {
        private XYIndex index;

        private long z;

        private GridEdge up, down, left, right;

        private GridDot(XYIndex index, long z) {
            this.index = index;
            this.z = z;
        }

        @Override
        public final int hashCode() {
            return index.hashCode();
        }

        @Override
        public final boolean equals(Object other) {
            if (other instanceof GridDot) {
                // Only compare position, not Z value which should be identical
                return this.index.equals(((GridDot) other).index);
            }
            return false;
        }

        @Override
        public final String toString() {
            return "[Dot" + index + "," + z + "]";
        }
    }

    private static class GridEdge {
        // For horizontal edges, A is left and B is right
        // For vertical edges, A is bottom and B is top
        private GridDot A, B;

        boolean horizontal;

        int size;

        boolean used = false;

        private GridEdge(GridDot A, GridDot B, int size, boolean horizontal) {
            // We accept A and B in any order. They must be correctly placed however
            if (horizontal && A.index.xIndex > B.index.xIndex || !horizontal
                    && A.index.yIndex > B.index.yIndex) {
                // Must swap A and B
                this.A = B;
                this.B = A;
            } else {
                this.A = A;
                this.B = B;
            }
            this.size = size;
            this.horizontal = horizontal;
            if (horizontal) {
                if (this.A.index.yIndex != this.B.index.yIndex)
                    throw new AssertionError(
                            "Building horizontal edge with non horizontally-aligned dots");
                if (this.B.index.xIndex - this.A.index.xIndex != size)
                    throw new AssertionError(
                            "Building horizontal edge with incorrect size vs dot spacing");
            } else {
                if (this.A.index.xIndex != this.B.index.xIndex)
                    throw new AssertionError(
                            "Building vertical edge with non vertically-aligned dots");
                if (this.B.index.yIndex - this.A.index.yIndex != size)
                    throw new AssertionError(
                            "Building vertical edge with incorrect size vs dot spacing");
            }
        }

        private final void indexEndPoints() {
            if (size != 1)
                throw new AssertionError("Can't dot-index edge with size != 1");
            if (horizontal) {
                A.right = this;
                B.left = this;
            } else {
                A.up = this;
                B.down = this;
            }
        }

        private final int cut(long z0) {
            if (A.z < z0 && z0 <= B.z)
                return 1;
            if (B.z < z0 && z0 <= A.z)
                return -1;
            return 0;
        }

        @Override
        public final int hashCode() {
            // Normally size does not matter as we won't
            // keep edges from different sizes in the same set/map
            // horizontality matter, though.
            return A.hashCode() + size + (horizontal ? 0x8000 : 0);
        }

        @Override
        public final boolean equals(Object other) {
            if (other instanceof GridEdge) {
                // Only compare A position, size and horizontality
                GridEdge otherEdge = (GridEdge) other;
                return A.equals(otherEdge.A) && size == otherEdge.size
                        && horizontal == otherEdge.horizontal;
            }
            return false;
        }

        @Override
        public final String toString() {
            return "[" + (horizontal ? "H" : "V") + "-Edge" + A + "-" + B + " L=" + size + "]";
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RecursiveGridIsolineBuilder.class);

    /**
     * Size (in index-unit) of initial (seed) sampling. This *MUST* be a power of 2. Good values are
     * 4 (slower but miss less small islands) or 8 (faster but miss more small islands).
     */
    private static final int SIZE_0 = 4;

    private ZFunc fz;

    private int fzInterpolateCount;

    private double dX, dY;

    private Coordinate center;

    private Map<XYIndex, GridDot> allDots;

    private Set<GridDot> initialDots;

    private List<GridEdge> initialEdges;

    private boolean debugSeedGrid = false;

    private boolean debugCrossingEdges = false;

    private Geometry debugGeometry = null;
        
    // private List<Coordinate> __p0List;

    /**
     * Create an object to compute isochrones. One may call several time isochronify on the same
     * IsoChronificator object, this will re-use the z = f(x,y) sampling if possible, as they are
     * kept in cache.
     * 
     * @param request Parameters for the computation
     * @param center Center point (eg origin)
     * @param fz Function returning the z-value for a xy-coordinate
     * @param p0List Initial set of coverage points to seed the heuristics
     */
    public RecursiveGridIsolineBuilder( double dX, double dY, Coordinate center, ZFunc fz,
            List<Coordinate> p0List) {
       
    	
    	
    	this.dX = dX;
        this.dY = dY;
        /*
         * Center position only purpose is to serve as a reference value to the XY integer indexing,
         * so it only needs to be not too far off to prevent int indexes from overflowing.
         */
        this.center = center;
        // this.__p0List = p0List;

        LOG.debug("Center={} dX={} dY={}", this.center, dX, dY);
        this.fz = fz;
        /* Step 1. SEED (1). Compute initial set of dots. */
        allDots = new HashMap<XYIndex, GridDot>(p0List.size() / 2);
        initialDots = new HashSet<GridDot>(p0List.size() / 2);
        for (Coordinate p0 : p0List) {
            XYIndex index0 = getIndex(p0, SIZE_0);
            for (int dx = -SIZE_0; dx <= SIZE_0; dx += SIZE_0) {
                for (int dy = -SIZE_0; dy <= SIZE_0; dy += SIZE_0) {
                    // This will always create initial dots
                    GridDot A = getOrCreateDot(index0.translated(dx, dy));
                    initialDots.add(A);
                }
            }
        }
        /*
         * Step 2. SEED (2). Create initial edges from initial dots. There will be slightly less
         * edges than dots.
         */
        initialEdges = new ArrayList<GridEdge>(initialDots.size());
        for (GridDot A : initialDots) {
            // Horizontal
            GridDot B = allDots.get(A.index.translated(SIZE_0, 0));
            if (B != null) {
                GridEdge e = new GridEdge(A, B, SIZE_0, true);
                initialEdges.add(e);
            }
            // Vertical
            B = allDots.get(A.index.translated(0, SIZE_0));
            if (B != null) {
                GridEdge e = new GridEdge(A, B, SIZE_0, false);
                initialEdges.add(e);
            }
        }
        LOG.debug("Created {} dots and {} edges out of {} initial points.", initialDots.size(),
                initialEdges.size(), p0List.size());
    }

    public void setDebugSeedGrid(boolean debugSeedGrid) {
        this.debugSeedGrid = debugSeedGrid;
    }

    public void setDebugCrossingEdges(boolean debugCrossingEdges) {
        this.debugCrossingEdges = debugCrossingEdges;
    }

    public Geometry computeIsoline(long z0) {
        fzInterpolateCount = 0;
        GeometryFactory geomFactory = new GeometryFactory();

        /*
         * Step 3. DIVIDE. While there are cutting edges from size > 1 to divide, divide them in
         * two. Note: edgesToExpand only contains cutting edges.
         */
        Queue<GridEdge> edgesToDivide = new ArrayDeque<GridEdge>();
        edgesToDivide.addAll(initialEdges);
        Queue<GridEdge> edgesToExpand = new ArrayDeque<GridEdge>();
        while (!edgesToDivide.isEmpty()) {
            GridEdge e = edgesToDivide.remove();
            if (e.cut(z0) == 0)
                continue;
            int size2 = e.size / 2;
            XYIndex iC = e.horizontal ? e.A.index.translated(size2, 0) : e.A.index.translated(0,
                    size2);
            GridDot C = getOrCreateDot(iC);
            GridEdge e1 = new GridEdge(e.A, C, size2, e.horizontal);
            GridEdge e2 = new GridEdge(C, e.B, size2, e.horizontal);
            GridEdge eCut = e1.cut(z0) != 0 ? e1 : e2;
            if (eCut.cut(z0) == 0)
                throw new AssertionError("Edge MUST cut!");
            if (size2 == 1) {
                edgesToExpand.add(eCut);
            } else {
                edgesToDivide.add(eCut);
            }
        }

        /*
         * Step 4. EXPAND. While there are unprocessed cutting edges, expand them by looking around
         * them for cutting edges.
         */
        Set<GridEdge> finalEdges = new HashSet<GridEdge>();
        Set<GridEdge> finalNonCuttingEdges = new HashSet<GridEdge>();
        while (!edgesToExpand.isEmpty()) {
            GridEdge e = edgesToExpand.remove();
            if (finalEdges.add(e) == false) {
                // Since we may have duplicates in the toExpand Q,
                // we prune-out early processed elements.
                continue;
            }
            /**
             * Build the 6 remaining edges (e1..e6) around the 2 squares ABDC and DBFE touching e
             * 
             * <pre>
             *   [Horizontal e]     [Vertical e]
             *   
             *    C--(e2)--D       D--(e3)--B--(e6)--F
             *    |        |       |        |        |
             *   (e1)     (e3)    (e2)     (e)      (e5)   
             *    |        |       |        |        |
             *    A--(e)---B       C--(e1)--A--(e4)--E   
             *    |        |       
             *   (e4)     (e6)   
             *    |        |
             *    E--(e5)--F
             * </pre>
             */
            XYIndex iC = e.horizontal ? e.A.index.translated(0, 1) : e.A.index.translated(-1, 0);
            XYIndex iD = e.horizontal ? e.B.index.translated(0, 1) : e.B.index.translated(-1, 0);
            XYIndex iE = e.horizontal ? e.A.index.translated(0, -1) : e.A.index.translated(1, 0);
            XYIndex iF = e.horizontal ? e.B.index.translated(0, -1) : e.B.index.translated(1, 0);
            GridDot C = getOrCreateDot(iC);
            GridDot D = getOrCreateDot(iD);
            GridDot E = getOrCreateDot(iE);
            GridDot F = getOrCreateDot(iF);
            GridEdge[] e2List = new GridEdge[] { new GridEdge(e.A, C, 1, !e.horizontal),
                    new GridEdge(C, D, 1, e.horizontal), new GridEdge(e.B, D, 1, !e.horizontal),
                    new GridEdge(e.A, E, 1, !e.horizontal), new GridEdge(E, F, 1, e.horizontal),
                    new GridEdge(e.B, F, 1, !e.horizontal) };
            for (GridEdge e2 : e2List) {
                if (e2.cut(z0) == 0) {
                    finalNonCuttingEdges.add(e2);
                    continue; // Do not cut, OK
                }
                if (finalEdges.contains(e2))
                    continue; // Already processed
                edgesToExpand.add(e2);
            }
        }
        /*
         * Note: Here finalEdges only contains cutting edges, and should end up containing all
         * cutting edges that are discoverable.
         */
        for (GridEdge e : finalEdges) {
            e.indexEndPoints();
        }
        for (GridEdge e : finalNonCuttingEdges) {
            e.indexEndPoints();
        }
        // For later logs.
        int finalEdgesSize = finalEdges.size();
        int finalNonCuttingEdgesSize = finalNonCuttingEdges.size();

        /*
         * Step 5. BUILD. Build polygons from finalEdges set.
         */
        List<Geometry> debugGeom = new ArrayList<Geometry>();
        if (debugSeedGrid) {
            for (GridEdge e : initialEdges) {
                Coordinate A = getCoordinate(e.A.index);
                Coordinate B = getCoordinate(e.B.index);
                debugGeom.add(geomFactory.createLineString(new Coordinate[] { A, B }));
            }
            // for (Coordinate p0 : __p0List) {
            // debugGeom.add(geomFactory.createPoint(p0));
            // }
        }
        if (debugCrossingEdges) {
            for (GridEdge e : finalEdges) {
                Coordinate A = getCoordinate(e.A.index);
                Coordinate B = getCoordinate(e.B.index);
                Coordinate C = interpolate(A, B, e.A.z, e.B.z, z0);
                Coordinate C1 = new Coordinate(C.x + (B.y - A.y) * 0.1, C.y + (B.x - A.x) * 0.1);
                Coordinate C2 = new Coordinate(C.x - (B.y - A.y) * 0.1, C.y - (B.x - A.x) * 0.1);
                debugGeom
                        .add(geomFactory.createLineString(new Coordinate[] { A, C, C1, C2, C, B }));
            }
        }

        List<Geometry> retval = new ArrayList<Geometry>();
        List<LinearRing> rings = new ArrayList<LinearRing>();
        while (!finalEdges.isEmpty()) {
            GridEdge e0 = finalEdges.iterator().next();
            List<Coordinate> polyPoints = new ArrayList<Coordinate>();
            int cut = e0.cut(z0);
            Direction direction = e0.horizontal ? (cut > 0 ? Direction.UP : Direction.DOWN)
                    : (cut > 0 ? Direction.LEFT : Direction.RIGHT);
            GridEdge e = e0;
            while (true) {
                // Add a point to polyline
                Coordinate cA = getCoordinate(e.A.index);
                Coordinate cB = getCoordinate(e.B.index);
                Coordinate cC = interpolate(cA, cB, e.A.z, e.B.z, z0);
                polyPoints.add(cC);
                e.used = true;
                finalEdges.remove(e);
                // Compute next edge from adjacent tile
                // Here e1 is always on e.A side, e2 on e.B side
                // and e3 on opposite side of tile from e.
                GridEdge e1, e2, e3;
                Direction d1, d2; // d3: same direction.
                switch (direction) {
                default: // Never happen
                case UP:
                    e1 = e.A.up;
                    d1 = Direction.LEFT;
                    e2 = e.B.up;
                    d2 = Direction.RIGHT;
                    e3 = e1.B.right;
                    break;
                case DOWN:
                    e1 = e.A.down;
                    d1 = Direction.LEFT;
                    e2 = e.B.down;
                    d2 = Direction.RIGHT;
                    e3 = e1.A.right;
                    break;
                case LEFT:
                    e1 = e.A.left;
                    d1 = Direction.DOWN;
                    e2 = e.B.left;
                    d2 = Direction.UP;
                    e3 = e1.A.up;
                    break;
                case RIGHT:
                    e1 = e.A.right;
                    d1 = Direction.DOWN;
                    e2 = e.B.right;
                    d2 = Direction.UP;
                    e3 = e1.B.up;
                    break;
                }
                boolean ok1 = e1.cut(z0) != 0 && !e1.used;
                boolean ok2 = e2.cut(z0) != 0 && !e2.used;
                boolean ok3 = e3.cut(z0) != 0 && !e3.used;
                if (ok1 && ok2) {
                    /*
                     * We can go either turn, pick the best one from e1 or e2 by looking if C lies
                     * closer to e.A or e.B. Please note this is approximate only, as we should take
                     * into account real interpolated position on e1 and e2 to compute segment
                     * lenght. But this gives good approximated results and is probably sufficient
                     * given the approximated solution anyway.
                     */
                    double dA = Math.max(Math.abs(cA.x - cC.x), Math.abs(cA.y - cC.y));
                    double dB = Math.max(Math.abs(cB.x - cC.x), Math.abs(cB.y - cC.y));
                    if (dA <= dB) {
                        // C closer to A
                        e = e1;
                        direction = d1;
                    } else {
                        // C closer to B
                        e = e2;
                        direction = d2;
                    }
                } else if (ok1) {
                    e = e1;
                    direction = d1;
                } else if (ok2) {
                    e = e2;
                    direction = d2;
                } else if (ok3) {
                    e = e3;
                    // Same direction as before
                } else {
                    // This must be the end of the polyline...
                    break;
                }
            }
            // Close the polyline
            polyPoints.add(polyPoints.get(0));
            LinearRing ring = geomFactory.createLinearRing(polyPoints
                    .toArray(new Coordinate[polyPoints.size()]));
            rings.add(ring);
        }
        retval.addAll(punchHoles(geomFactory, rings));

        LOG.info("Isochrones: {}+{} Fz samples, {} cutting edges, {} non-cutting edges",
                allDots.size(), fzInterpolateCount, finalEdgesSize, finalNonCuttingEdgesSize);

        /*
         * Step 6. CLEAN. Remove edge index from dots.
         */
        for (GridDot A : allDots.values()) {
            A.up = A.down = A.right = A.left = null;
        }
        debugGeometry = geomFactory.createGeometryCollection(debugGeom
                .toArray(new Geometry[debugGeom.size()]));
        return geomFactory.createGeometryCollection(retval.toArray(new Geometry[retval.size()]));
    }

    public final Geometry getDebugGeometry() {
        return debugGeometry;
    }

    private final XYIndex getIndex(Coordinate p, int size) {
        int xIndex = (int) Math.round((p.x - center.x) / dX);
        int yIndex = (int) Math.round((p.y - center.y) / dY);
        if (size != 1) {
            int size2 = size / 2;
            xIndex = (xIndex + (xIndex > 0 ? size2 : -size2)) / size * size;
            yIndex = (yIndex + (yIndex > 0 ? size2 : -size2)) / size * size;
        }
        return new XYIndex(xIndex, yIndex);
    }

    private final Coordinate getCoordinate(XYIndex index) {
        return new Coordinate(index.xIndex * dX + center.x, index.yIndex * dY + center.y);
    }

    private GridDot getOrCreateDot(XYIndex xyIndex) {
        GridDot A = allDots.get(xyIndex);
        if (A == null) {
            A = new GridDot(xyIndex, fz.z(getCoordinate(xyIndex)));
            allDots.put(xyIndex, A);
        }
        return A;
    }

    private final Coordinate interpolate(Coordinate A, Coordinate B, long zA, long zB, long z0) {
        int n = 0;
        while (n < 3 && (zA == Long.MAX_VALUE || zB == Long.MAX_VALUE)) {
            Coordinate C = new Coordinate((A.x + B.x) / 2.0, (A.y + B.y) / 2.0);
            long zC = fz.z(A);
            fzInterpolateCount++;
            if (zA == Long.MAX_VALUE && z0 <= zC) {
                A = C;
                zA = zC;
            } else {
                B = C;
                zB = zC;
            }
            n++;
        }
        // Take as fallback position if we are still at +inf at one end z0 * 2
        if (zA == Long.MAX_VALUE)
            zA = z0 * 2;
        if (zB == Long.MAX_VALUE)
            zB = z0 * 2;
        double k = zB == zA ? 0.5 : (z0 - zA) / (double) (zB - zA);
        Coordinate C = new Coordinate(A.x * (1.0 - k) + B.x * k, A.y * (1.0 - k) + B.y * k);
        return C;
    }

    @SuppressWarnings("unchecked")
    private final List<Polygon> punchHoles(GeometryFactory geomFactory, List<LinearRing> rings) {
        List<Polygon> shells = new ArrayList<Polygon>(rings.size());
        List<LinearRing> holes = new ArrayList<LinearRing>(rings.size() / 2);
        // 1. Split the polygon list in two: shells and holes (CCW and CW)
        for (LinearRing ring : rings) {
            if (CGAlgorithms.signedArea(ring.getCoordinateSequence()) > 0.0)
                holes.add(ring);
            else
                shells.add(geomFactory.createPolygon(ring));
        }
        // 2. Sort the shells based on number of points to optimize step 3.
        Collections.sort(shells, new Comparator<Polygon>() {
            @Override
            public int compare(Polygon o1, Polygon o2) {
                return o2.getNumPoints() - o1.getNumPoints();
            }
        });
        for (Polygon shell : shells) {
            shell.setUserData(new ArrayList<LinearRing>());
        }
        // 3. For each hole, determine which shell it fits in.
        for (LinearRing hole : holes) {
            outer: {
                // Probably most of the time, the first shell will be the one
                for (Polygon shell : shells) {
                    if (shell.contains(hole)) {
                        ((List<LinearRing>) shell.getUserData()).add(hole);
                        break outer;
                    }
                }
                // This should not happen, but do not break bad here
                // as loosing a hole is not critical, we still have
                // sensible data to return.
                LOG.error("Cannot find fitting shell for a hole!");
            }
        }
        // 4. Build the list of punched polygons
        List<Polygon> punched = new ArrayList<Polygon>(shells.size());
        for (Polygon shell : shells) {
            List<LinearRing> shellHoles = ((List<LinearRing>) shell.getUserData());
            punched.add(geomFactory.createPolygon((LinearRing) (shell.getExteriorRing()),
                    shellHoles.toArray(new LinearRing[shellHoles.size()])));
        }
        return punched;
    }
}