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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

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

    private static class GridSample {
        private int x, y;

        private double[] zz;

        private GridSample up, down, right, left;

        private boolean vProcessed, hProcessed;

        private GridSample(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public final String toString() {
            return "[Sample(" + x + "," + y + ")," + Arrays.toString(zz) + "]";
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccSamplingGridIsolineBuilder.class);

    private double dX, dY;

    private Coordinate center;

    private ZFunc zFunc;

    private SparseMatrix<GridSample> allSamples;

    private boolean closed = false;

    private boolean debug = false;

    private GeometryFactory geometryFactory = new GeometryFactory();

    private List<Geometry> debugGeom = new ArrayList<Geometry>();

    /**
     * Create an object to compute isochrones. One may call several time isochronify on the same
     * object.
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
        allSamples = new SparseMatrix<GridSample>(16, size);
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
        int x = (int) Math.round((C0.x - center.x - dX / 2) / dX);
        int y = (int) Math.round((C0.y - center.y - dY / 2) / dY);

        GridSample[] ABCD = new GridSample[4];
        ABCD[0] = getOrCreateSample(x, y, null, null, null, null);
        ABCD[1] = ABCD[0].right != null ? ABCD[0].right : getOrCreateSample(x + 1, y, null, null,
                null, ABCD[0]);
        ABCD[2] = ABCD[0].up != null ? ABCD[0].up : getOrCreateSample(x, y + 1, null, ABCD[0],
                null, null);
        ABCD[3] = ABCD[1].up != null ? ABCD[1].up : getOrCreateSample(x + 1, y + 1, null, ABCD[1],
                null, ABCD[2]);
        for (GridSample P : ABCD) {
            Coordinate C = getCoordinate(P.x, P.y);
            P.zz = zFunc.cumulateSample(C0, C, z, P.zz);
        }
    }

    private final GridSample getOrCreateSample(int x, int y, GridSample Aup, GridSample Adown,
            GridSample Aright, GridSample Aleft) {
        GridSample A = allSamples.get(x, y);
        if (A != null)
            return A;
        A = new GridSample(x, y);
        A.zz = null;
        if (Aup == null)
            Aup = allSamples.get(x, y + 1);
        if (Aup != null) {
            Aup.down = A;
            A.up = Aup;
        }
        if (Adown == null)
            Adown = allSamples.get(x, y - 1);
        if (Adown != null) {
            Adown.up = A;
            A.down = Adown;
        }
        if (Aright == null)
            Aright = allSamples.get(x + 1, y);
        if (Aright != null) {
            Aright.left = A;
            A.right = Aright;
        }
        if (Aleft == null)
            Aleft = allSamples.get(x - 1, y);
        if (Aleft != null) {
            Aleft.right = A;
            A.left = Aleft;
        }
        allSamples.put(x, y, A);
        return A;
    }

    /**
     * Surround all existing sample on the edge by 2 layers of closing samples.
     */
    private final void closeSamples() {
        List<GridSample> processList = new ArrayList<GridSample>(allSamples.size());
        for (GridSample A : allSamples) {
            processList.add(A);
        }
        int n = 0;
        for (GridSample A : processList) {
            if (A.right == null) {
                closeSample(A.x + 1, A.y, null, null, null, A);
                n++;
            }
            if (A.left == null) {
                closeSample(A.x - 1, A.y, null, null, A, null);
                n++;
            }
            if (A.up == null) {
                closeSample(A.x, A.y + 1, null, A, null, null);
                n++;
            }
            if (A.down == null) {
                closeSample(A.x, A.y - 1, A, null, null, null);
                n++;
            }
        }
        LOG.info("Added {} closing samples to get a total of {}.", n, allSamples.size());
    }

    private final GridSample closeSample(int x, int y, GridSample up, GridSample down,
            GridSample right, GridSample left) {
        GridSample A = getOrCreateSample(x, y, up, down, right, left);
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

        Queue<GridSample> processQ = new ArrayDeque<GridSample>(allSamples.size());
        for (GridSample A : allSamples) {
            A.hProcessed = false;
            A.vProcessed = false;
            processQ.add(A);
        }

        if (debug)
            generateDebugGeometry(zz0);

        List<LinearRing> rings = new ArrayList<LinearRing>();
        while (!processQ.isEmpty()) {
            GridSample A = processQ.remove();
            if (A.hProcessed && A.vProcessed)
                continue;
            boolean horizontal = !A.hProcessed;
            if (horizontal) {
                A.hProcessed = true;
            } else {
                A.vProcessed = true;
            }
            if (!(A.vProcessed && A.hProcessed)) {
                // Re-adding self to be processed again for perpendicular direction
                processQ.add(A);
            }
            GridSample B = horizontal ? A.right : A.up;
            boolean ok = B != null;
            int cut = 0;
            if (ok) {
                cut = zFunc.cut(A.zz, B.zz, zz0);
                ok = (cut != 0);
            }
            if (!ok) {
                continue; // While
            }
            List<Coordinate> polyPoints = new ArrayList<Coordinate>();
            Direction direction = horizontal ? (cut > 0 ? Direction.UP : Direction.DOWN)
                    : (cut > 0 ? Direction.LEFT : Direction.RIGHT);
            while (true) {
                // Add a point to polyline
                Coordinate cA = getCoordinate(A.x, A.y);
                Coordinate cB = getCoordinate(B.x, B.y);
                double k = zFunc.interpolate(A.zz, B.zz, zz0);
                Coordinate cC = new Coordinate(cA.x * (1.0 - k) + cB.x * k, cA.y * (1.0 - k) + cB.y
                        * k);
                polyPoints.add(cC);
                horizontal = direction == Direction.UP || direction == Direction.DOWN;
                if (horizontal) {
                    A.hProcessed = true;
                } else {
                    A.vProcessed = true;
                }
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
                        && (horizontal ? (invertAB ? !D.vProcessed : !A.vProcessed)
                                : (invertAB ? !D.hProcessed : !A.hProcessed));
                boolean ok2 = C != null
                        && zFunc.cut(B.zz, C.zz, zz0) != 0
                        && (horizontal ? (invertAB ? !C.vProcessed : !B.vProcessed)
                                : (invertAB ? !C.hProcessed : !B.hProcessed));
                boolean ok3 = C != null && D != null && zFunc.cut(C.zz, D.zz, zz0) != 0
                        && (horizontal ? !D.hProcessed : !D.vProcessed);
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
            if (polyPoints.size() > 5) {
                // If the ring is smaller than 4 points do not add it,
                // that will remove too small islands or holes.
                LinearRing ring = geometryFactory.createLinearRing(polyPoints
                        .toArray(new Coordinate[polyPoints.size()]));
                rings.add(ring);
            }
        }
        List<Polygon> retval = punchHoles(rings);
        return geometryFactory
                .createGeometryCollection(retval.toArray(new Geometry[retval.size()]));
    }

    private final void generateDebugGeometry(double[] zz0) {
        debug = false;

        double[] zmax = new double[10]; // TODO size!
        for (GridSample A : allSamples) {
            for (int i = 1; i < A.zz.length; i++) {
                double z = A.zz[i] / A.zz[0];
                if (A.zz[i] < Double.MAX_VALUE && z > zmax[i])
                    zmax[i] = z;
            }
        }
        for (GridSample A : allSamples) {
            Coordinate C1 = getCoordinate(A.x, A.y);
            double[] zz = A.zz;
            for (int i = 1; i < zz.length - 1; i++) {
                double z = zz[i] / zz[0]; // TODO Make this more generic
                if (z != Double.POSITIVE_INFINITY) {
                    Coordinate C2 = new Coordinate(C1.x + z * dX / zmax[i] * 1 / 4, C1.y + z * dY
                            / zmax[i]);
                    debugGeom.add(geometryFactory.createLineString(new Coordinate[] { C1, C2 }));
                }
            }
        }
    }

    public final Geometry getDebugGeometry() {
        return geometryFactory.createGeometryCollection(debugGeom.toArray(new Geometry[debugGeom
                .size()]));
    }

    private final Coordinate getCoordinate(int x, int y) {
        return new Coordinate(x * dX + center.x, y * dY + center.y);
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