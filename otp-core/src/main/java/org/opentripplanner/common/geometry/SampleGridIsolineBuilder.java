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
 * the same builder for different z0 value: this will reduce the number of Fz sampling as they are
 * cached in the builder. Please note that the initial covering points must touch all isolines you
 * want to cover.
 * 
 * @author laurent
 */
public class SampleGridIsolineBuilder<TZ> implements IsolineBuilder<TZ> {

    public interface ZMetric<TZ> {
        /**
         * Check if the edge [AB] between two samples A and B "intersect" the zz0 plane.
         * 
         * @param zA z value for the A sample
         * @param zB z value for the B sample
         * @param z0 z value for the intersecting plane
         * @return 0 if no intersection, -1 or +1 if intersection (depending on which is lower, A or
         *         B).
         */
        public int cut(TZ zA, TZ zB, TZ z0);

        /**
         * Interpolate a crossing point on an edge [AB].
         * 
         * @param zA z value for the A sample
         * @param zB z value for the B sample
         * @param z0 z value for the intersecting plane
         * @return k value between 0 and 1, where the crossing occurs. 0=A, 1=B.
         */
        public double interpolate(TZ zA, TZ zB, TZ z0);
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;
    }

    private static class IsoLineUserData {
        private boolean vProcessed, hProcessed;
    }

    private static final Logger LOG = LoggerFactory.getLogger(SampleGridIsolineBuilder.class);

    private ZMetric<TZ> zMetric;

    private ZSampleGrid<TZ> sampleGrid;

    private boolean debug = false;

    private GeometryFactory geometryFactory = new GeometryFactory();

    private List<Geometry> debugGeom = new ArrayList<Geometry>();

    /**
     * Create an object to compute isochrones. One may call several time isochronify on the same
     * object.
     * 
     * @param sampleGrid The sample grid.
     * @param zMetric The Z metric (intersection detection and interpolation method).
     */
    public SampleGridIsolineBuilder(ZSampleGrid<TZ> sampleGrid, ZMetric<TZ> zMetric) {
        this.sampleGrid = sampleGrid;
        this.zMetric = zMetric;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public Geometry computeIsoline(TZ z0) {
        Queue<ZSamplePoint<TZ>> processQ = new ArrayDeque<ZSamplePoint<TZ>>(sampleGrid.size());
        for (ZSamplePoint<TZ> A : sampleGrid) {
            IsoLineUserData iud = (IsoLineUserData) A.getUserData();
            if (iud == null) {
                // Lazy-create user data.
                // Please note that this is re-used across calls.
                iud = new IsoLineUserData();
                A.setUserData(iud);
            }
            iud.hProcessed = false;
            iud.vProcessed = false;
            processQ.add(A);
        }

        if (debug)
            generateDebugGeometry(z0);

        List<LinearRing> rings = new ArrayList<LinearRing>();
        while (!processQ.isEmpty()) {
            ZSamplePoint<TZ> A = processQ.remove();
            IsoLineUserData uA = (IsoLineUserData) A.getUserData();
            if (uA.hProcessed && uA.vProcessed)
                continue;
            boolean horizontal = !uA.hProcessed;
            if (horizontal) {
                uA.hProcessed = true;
            } else {
                uA.vProcessed = true;
            }
            if (!(uA.vProcessed && uA.hProcessed)) {
                // Re-adding self to be processed again for perpendicular direction
                processQ.add(A);
            }
            ZSamplePoint<TZ> B = horizontal ? A.right() : A.up();
            boolean ok = B != null;
            int cut = 0;
            if (ok) {
                cut = zMetric.cut(A.getZ(), B.getZ(), z0);
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
                Coordinate cA = sampleGrid.getCoordinates(A);
                Coordinate cB = sampleGrid.getCoordinates(B);
                double k = zMetric.interpolate(A.getZ(), B.getZ(), z0);
                Coordinate cC = new Coordinate(cA.x * (1.0 - k) + cB.x * k, cA.y * (1.0 - k) + cB.y
                        * k);
                polyPoints.add(cC);
                horizontal = direction == Direction.UP || direction == Direction.DOWN;
                if (horizontal) {
                    ((IsoLineUserData) A.getUserData()).hProcessed = true;
                } else {
                    ((IsoLineUserData) A.getUserData()).vProcessed = true;
                }
                // Compute next samples from adjacent tile
                // C same side of B, D same side of A.
                ZSamplePoint<TZ> C, D;
                Direction d1, d2; // d3: same direction.
                boolean invertAB;
                switch (direction) {
                default: // Never happen
                case UP:
                    d1 = Direction.LEFT;
                    d2 = Direction.RIGHT;
                    B = A.right();
                    C = B.up();
                    D = A.up();
                    invertAB = false;
                    break;
                case DOWN:
                    d1 = Direction.LEFT;
                    d2 = Direction.RIGHT;
                    B = A.right();
                    C = B.down();
                    D = A.down();
                    invertAB = true;
                    break;
                case LEFT:
                    d1 = Direction.DOWN;
                    d2 = Direction.UP;
                    B = A.up();
                    C = B.left();
                    D = A.left();
                    invertAB = true;
                    break;
                case RIGHT:
                    d1 = Direction.DOWN;
                    d2 = Direction.UP;
                    B = A.up();
                    C = B.right();
                    D = A.right();
                    invertAB = false;
                    break;
                }
                boolean ok1 = D != null
                        && zMetric.cut(A.getZ(), D.getZ(), z0) != 0
                        && (horizontal ? (invertAB ? !((IsoLineUserData) D.getUserData()).vProcessed
                                : !((IsoLineUserData)A.getUserData()).vProcessed)
                                : (invertAB ? !((IsoLineUserData) D.getUserData()).hProcessed
                                        : !((IsoLineUserData) A.getUserData()).hProcessed));
                boolean ok2 = C != null
                        && zMetric.cut(B.getZ(), C.getZ(), z0) != 0
                        && (horizontal ? (invertAB ? !((IsoLineUserData) C.getUserData()).vProcessed
                                : !((IsoLineUserData) B.getUserData()).vProcessed)
                                : (invertAB ? !((IsoLineUserData) C.getUserData()).hProcessed
                                        : !((IsoLineUserData) B.getUserData()).hProcessed));
                boolean ok3 = C != null
                        && D != null
                        && zMetric.cut(C.getZ(), D.getZ(), z0) != 0
                        && (horizontal ? !((IsoLineUserData) D.getUserData()).hProcessed
                                : !((IsoLineUserData) D.getUserData()).vProcessed);
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
                        ZSamplePoint<TZ> oA = A;
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
                    ZSamplePoint<TZ> oA = A;
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

    private final void generateDebugGeometry(TZ z0) {
        debug = false;
        // TODO Map TZ to some debug geometry.
    }

    public final Geometry getDebugGeometry() {
        return geometryFactory.createGeometryCollection(debugGeom.toArray(new Geometry[debugGeom
                .size()]));
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