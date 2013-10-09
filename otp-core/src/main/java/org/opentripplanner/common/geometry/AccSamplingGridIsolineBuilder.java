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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Compute isoline based on a set of z samplings.
 * 
 * This assume we have a set of z sampling points which give z for some (x,y) points. z can be
 * undefined for some region.
 * 
 * It will compute an isoline for a given z0 value. The isoline is composed of a list of n polygons,
 * CW for normal polygons, CCW for "holes". The isoline computation can be called multiple times on
 * the same builder for different z0 values: this will reduce the number of Fz sampling as they are
 * cached in the builder. Please note that the initial covering points must touch all isolines you
 * want to cover.
 * 
 * @author laurent
 */
public class AccSamplingGridIsolineBuilder implements IsolineBuilder {

    public interface ZFunc {
        /**
         * Callback function to handle a new added sample.
         * 
         * @param C0 The initial position of the sample, as given in the addSample() call.
         * @param Cs The position of the sample on the grid, never farther away than (dX,dY)
         * @param z The z value of the initial sample, as given in the addSample() call.
         * @param zzS The previous z values of the sample. Can be null if this is the first time,
         *        it's up to the caller to initialize the z values.
         * @return The modified z values for the sample.
         */
        public double[] cumulateSample(Coordinate C0, Coordinate Cs, double z, double[] zzS);

        /**
         * Callback function to handle a "closing" sample (that is a sample post-created to surround
         * existing samples and provide nice edges for the algorithm).
         * 
         * @param zzUp Sampled values of the up neighbor.
         * @param zzDown Idem
         * @param zzRight Idem
         * @param zzLeft Idem
         * @return The z values for the closing sample.
         */
        public double[] closeSample(double[] zzUp, double[] zzDown, double[] zzRight,
                double[] zzLeft);

        /**
         * Check if the edge [AB] between two samples A and B "intersect" the zz0 plane.
         * 
         * @param zzA z values for the A sample
         * @param zzB z values for the B sample
         * @param zz0 z values for the intersecting plane
         * @return 0 if no intersection, -1 or +1 if intersection (depending on which is lower, A or
         *         B).
         */
        public int cut(double[] zzA, double[] zzB, double[] zz0);

        /**
         * Interpolate a crossing point on an edge [AB].
         * 
         * @param zzA z values for the A sample
         * @param zzB z values for the B sample
         * @param zz0 z values for the intersecting plane
         * @return k values between 0 and 1, where the crossing occurs. 0=A, 1=B.
         */
        public double interpolate(double[] zzA, double[] zzB, double[] zz0);
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

    private static class GridSample {
        private XYIndex index;

        private double[] zz;

        private GridSample up, down, right, left;

        private GridSample(XYIndex index) {
            this.index = index;
        }

        @Override
        public final int hashCode() {
            return index.hashCode();
        }

        @Override
        public final boolean equals(Object other) {
            if (other instanceof GridSample) {
                // Only compare position, not Z value which should be identical
                return this.index.equals(((GridSample) other).index);
            }
            return false;
        }

        @Override
        public final String toString() {
            return "[Sample" + index + "," + zz + "]";
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccSamplingGridIsolineBuilder.class);

    private double dX, dY;

    private Coordinate center;

    private ZFunc zFunc;

    private Map<XYIndex, GridSample> allSamples;

    private boolean closed = false;

    private boolean debug = false;

    private GeometryFactory geometryFactory = new GeometryFactory();

    private List<Geometry> debugGeom = new ArrayList<Geometry>();

    /**
     * Create an object to compute isochrones. One may call several time isochronify on the same
     * IsoChronificator object.
     * 
     * @param dX, dY Grid size
     * @param center Center point (eg origin)
     * @param zFunc ZFunc giving z function "behavior" and "metric".
     * @param size Estimated grid size
     */
    public AccSamplingGridIsolineBuilder(double dX, double dY, Coordinate center, ZFunc zFunc,
            int size) {
        this.dX = dX;
        this.dY = dY;
        /*
         * Center position only purpose is to serve as a reference value to the XY integer indexing,
         * so it only needs to be not too far off to prevent int indexes from overflowing.
         */
        this.center = center;
        this.zFunc = zFunc;
        allSamples = new HashMap<XYIndex, GridSample>(size);
        LOG.debug("Center={} dX={} dY={}", this.center, dX, dY);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public final void addSample(Coordinate C0, double z) {
        if (closed) {
            throw new IllegalStateException("Cannot add sample once an isochrone is computed.");
        }
        // if (debug) {
        // debugGeom.add(geometryFactory.createPoint(C0));
        // debugGeom.add(geometryFactory.createLineString(new Coordinate[] { C0,
        // new Coordinate(C0.x + z / 1000000, C0.y + z / 1000000) }));
        // }
        XYIndex iA = getLowerLeftIndex(C0);
        GridSample[] ABCD = new GridSample[4];
        ABCD[0] = getOrCreateSample(iA);
        ABCD[1] = ABCD[0].right != null ? ABCD[0].right : getOrCreateSample(iA.translated(1, 0));
        ABCD[2] = ABCD[0].up != null ? ABCD[0].up : getOrCreateSample(iA.translated(0, 1));
        ABCD[3] = ABCD[1].up != null ? ABCD[1].up : getOrCreateSample(iA.translated(1, 1));
        for (GridSample P : ABCD) {
            Coordinate C = getCoordinate(P.index);
            P.zz = zFunc.cumulateSample(C0, C, z, P.zz);
        }
    }

    // TODO Add default for up, down, right and left if available
    private final GridSample getOrCreateSample(XYIndex index) {
        GridSample A = allSamples.get(index);
        if (A != null)
            return A;
        A = new GridSample(index);
        A.zz = null;
        GridSample Aright = allSamples.get(index.translated(1, 0));
        if (Aright != null) {
            Aright.left = A;
            A.right = Aright;
        }
        GridSample Aleft = allSamples.get(index.translated(-1, 0));
        if (Aleft != null) {
            Aleft.right = A;
            A.left = Aleft;
        }
        GridSample Aup = allSamples.get(index.translated(0, 1));
        if (Aup != null) {
            Aup.down = A;
            A.up = Aup;
        }
        GridSample Adown = allSamples.get(index.translated(0, -1));
        if (Adown != null) {
            Adown.up = A;
            A.down = Adown;
        }
        allSamples.put(index, A);
        return A;
    }

    /**
     * Surround all existing sample on the edge by 2 layers of closing samples.
     */
    private final void closeSamples() {
        List<GridSample> processList = new ArrayList<GridSample>(allSamples.size());
        processList.addAll(allSamples.values());
        int n = 0;
        final int N_STAGES = 1;
        for (int i = 0; i < N_STAGES; i++) {
            List<GridSample> newProcessList = new ArrayList<GridSample>(
                    i == 0 ? processList.size() / 10 : processList.size() * 5 / 4);
            for (GridSample A : processList) {
                if (A.right == null) {
                    newProcessList.add(closeSample(A.index.translated(1, 0)));
                    n++;
                }
                if (A.left == null) {
                    newProcessList.add(closeSample(A.index.translated(-1, 0)));
                    n++;
                }
                if (A.up == null) {
                    newProcessList.add(closeSample(A.index.translated(0, 1)));
                    n++;
                }
                if (A.down == null) {
                    newProcessList.add(closeSample(A.index.translated(0, -1)));
                    n++;
                }
            }
            processList = newProcessList;
        }
        LOG.info("Added {} closing samples to {} samples in {} stages.", n, processList.size(),
                N_STAGES);
    }

    private final GridSample closeSample(XYIndex index) {
        GridSample A = getOrCreateSample(index);
        A.zz = zFunc.closeSample(A.up != null ? A.up.zz : null, A.down != null ? A.down.zz : null,
                A.right != null ? A.right.zz : null, A.left != null ? A.left.zz : null);
        return A;
    }

    @Override
    public Geometry computeIsoline(double[] zz0) {
        if (!closed) {
            closeSamples();
            closed = true;
        }

        // TODO Transform this with a "open Q"
        // and two hProcessed,vProcessed boolean in GridSample
        Set<GridSample> h = new HashSet<GridSample>(allSamples.size());
        Set<GridSample> v = new HashSet<GridSample>(allSamples.size());
        for (GridSample A : allSamples.values()) {
            h.add(A);
            v.add(A);
        }

        if (debug)
            generateDebugGeometry(zz0);

        List<Geometry> retval = new ArrayList<Geometry>();
        List<LinearRing> rings = new ArrayList<LinearRing>();
        while (!(h.isEmpty() && v.isEmpty())) {
            boolean horizontal = v.isEmpty();
            GridSample A = horizontal ? h.iterator().next() : v.iterator().next();
            GridSample B = horizontal ? A.right : A.up;
            boolean ok = B != null;
            int cut = 0;
            if (ok) {
                cut = zFunc.cut(A.zz, B.zz, zz0);
                ok = (cut != 0);
            }
            if (!ok) {
                if (horizontal)
                    h.remove(A);
                else
                    v.remove(A);
                continue; // While
            }
            List<Coordinate> polyPoints = new ArrayList<Coordinate>();
            Direction direction = horizontal ? (cut > 0 ? Direction.UP : Direction.DOWN)
                    : (cut > 0 ? Direction.LEFT : Direction.RIGHT);
            while (true) {
                // Add a point to polyline
                Coordinate cA = getCoordinate(A.index);
                Coordinate cB = getCoordinate(B.index);
                double k = zFunc.interpolate(A.zz, B.zz, zz0);
                Coordinate cC = new Coordinate(cA.x * (1.0 - k) + cB.x * k, cA.y * (1.0 - k) + cB.y
                        * k);
                polyPoints.add(cC);
                horizontal = direction == Direction.UP || direction == Direction.DOWN;
                if (horizontal)
                    h.remove(A);
                else
                    v.remove(A);
                // Compute next samples from adjacent tile
                // C same side of B, D same side of A.
                GridSample C, D;
                Direction d1, d2; // d3: same direction.
                boolean invertAB;
                switch (direction) {
                default: // Never happen
                case UP:
                    d1 = Direction.LEFT;
                    d2 = Direction.RIGHT;
                    B = A.right;
                    C = B.up;
                    D = A.up;
                    invertAB = false;
                    break;
                case DOWN:
                    d1 = Direction.LEFT;
                    d2 = Direction.RIGHT;
                    B = A.right;
                    C = B.down;
                    D = A.down;
                    invertAB = true;
                    break;
                case LEFT:
                    d1 = Direction.DOWN;
                    d2 = Direction.UP;
                    B = A.up;
                    C = B.left;
                    D = A.left;
                    invertAB = true;
                    break;
                case RIGHT:
                    d1 = Direction.DOWN;
                    d2 = Direction.UP;
                    B = A.up;
                    C = B.right;
                    D = A.right;
                    invertAB = false;
                    break;
                }
                boolean ok1 = D != null
                        && zFunc.cut(A.zz, D.zz, zz0) != 0
                        && (horizontal ? (invertAB ? v.contains(D) : v.contains(A)) : (invertAB ? h
                                .contains(D) : h.contains(A)));
                boolean ok2 = C != null
                        && zFunc.cut(B.zz, C.zz, zz0) != 0
                        && (horizontal ? (invertAB ? v.contains(C) : v.contains(B)) : (invertAB ? h
                                .contains(C) : h.contains(B)));
                boolean ok3 = C != null && D != null && zFunc.cut(C.zz, D.zz, zz0) != 0
                        && (horizontal ? h.contains(D) : v.contains(D));
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
                        GridSample oA = A;
                        A = invertAB ? D : A;
                        B = invertAB ? oA : D;
                        direction = d1;
                    } else {
                        // C closer to B
                        A = invertAB ? C : B;
                        B = invertAB ? B : C;
                        direction = d2;
                    }
                } else if (ok1) {
                    GridSample oA = A;
                    A = invertAB ? D : A;
                    B = invertAB ? oA : D;
                    direction = d1;
                } else if (ok2) {
                    A = invertAB ? C : B;
                    B = invertAB ? B : C;
                    direction = d2;
                } else if (ok3) {
                    A = D;
                    B = C;
                    // Same direction as before
                } else {
                    // This must be the end of the polyline...
                    break;
                }
            }
            // Close the polyline
            polyPoints.add(polyPoints.get(0));
            LinearRing ring = geometryFactory.createLinearRing(polyPoints
                    .toArray(new Coordinate[polyPoints.size()]));
            rings.add(ring);
        }
        retval.addAll(punchHoles(rings));
        return geometryFactory
                .createGeometryCollection(retval.toArray(new Geometry[retval.size()]));
    }

    private final void generateDebugGeometry(double[] zz0) {
        debug = false;
        GeometryFactory geomFactory = new GeometryFactory();

        double[] zmax = null;
        for (GridSample A : allSamples.values()) {
            if (zmax == null) {
                zmax = new double[A.zz.length];
                for (int i = 0; i < zmax.length; i++)
                    zmax[i] = 0.0;
            }
            for (int i = 1; i < A.zz.length; i++) {
                double z = A.zz[i] / A.zz[0];
                if (A.zz[i] < Double.MAX_VALUE && z > zmax[i])
                    zmax[i] = z;
            }
        }
        for (GridSample A : allSamples.values()) {
            Coordinate C1 = getCoordinate(A.index);
            double[] zz = A.zz;
            for (int i = 1; i < zz.length - 1; i++) {
                double z = zz[i] / zz[0]; // TODO Make this more generic
                if (z != Double.POSITIVE_INFINITY) {
                    Coordinate C2 = new Coordinate(C1.x + z * dX / zmax[i] * 1 / 4, C1.y + z * dY
                            / zmax[i]);
                    debugGeom.add(geomFactory.createLineString(new Coordinate[] { C1, C2 }));
                }
            }
        }
    }

    public final Geometry getDebugGeometry() {
        return geometryFactory.createGeometryCollection(debugGeom.toArray(new Geometry[debugGeom
                .size()]));
    }

    private final XYIndex getLowerLeftIndex(Coordinate p) {
        int xIndex = (int) Math.round((p.x - center.x - dX / 2) / dX);
        int yIndex = (int) Math.round((p.y - center.y - dY / 2) / dY);
        return new XYIndex(xIndex, yIndex);
    }

    private final Coordinate getCoordinate(XYIndex index) {
        return new Coordinate(index.xIndex * dX + center.x, index.yIndex * dY + center.y);
    }

    @SuppressWarnings("unchecked")
    private final List<Polygon> punchHoles(List<LinearRing> rings) {
        List<Polygon> shells = new ArrayList<Polygon>(rings.size());
        List<LinearRing> holes = new ArrayList<LinearRing>(rings.size() / 2);
        // 1. Split the polygon list in two: shells and holes (CCW and CW)
        for (LinearRing ring : rings) {
            if (CGAlgorithms.signedArea(ring.getCoordinateSequence()) > 0.0)
                holes.add(ring);
            else
                shells.add(geometryFactory.createPolygon(ring));
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
            punched.add(geometryFactory.createPolygon((LinearRing) (shell.getExteriorRing()),
                    shellHoles.toArray(new LinearRing[shellHoles.size()])));
        }
        return punched;
    }
}