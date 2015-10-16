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

package org.opentripplanner.analyst.request;

import static org.apache.commons.math3.util.FastMath.toRadians;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.common.geometry.AccumulativeGridSampler;
import org.opentripplanner.common.geometry.AccumulativeGridSampler.AccumulativeMetric;
import org.opentripplanner.common.geometry.ZSampleGrid.ZSamplePoint;
import org.opentripplanner.common.geometry.IsolineBuilder;
import org.opentripplanner.common.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.ZSampleGrid;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.SPTWalker;
import org.opentripplanner.routing.spt.SPTWalker.SPTVisitor;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Compute a sample grid from a SPT request.
 * 
 * First compute a shortest-path-tree from the given routing request. It then build the sample grid
 * (a regular grid of samples covering the whole SPT area) using an accumulative grid sampling
 * process.
 * 
 * @see ZSampleGrid
 * @see AccumulativeGridSampler
 * 
 * @author laurent
 */
public class SampleGridRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(SampleGridRenderer.class);

    private Graph graph;

    public SampleGridRenderer(Graph graph) {
        this.graph = graph;
    }

    /**
     * @param spgRequest
     * @param sptRequest
     * @return
     */
    public ZSampleGrid<WTWD> getSampleGrid(SampleGridRequest spgRequest, RoutingRequest sptRequest) {

        final double offRoadDistanceMeters = spgRequest.offRoadDistanceMeters;
        final double offRoadWalkSpeedMps = 1.00; // m/s, off-road walk speed

        // 1. Compute the Shortest Path Tree.
        long t0 = System.currentTimeMillis();
        long tOvershot = (long) (2 * offRoadDistanceMeters / offRoadWalkSpeedMps);
        sptRequest.worstTime = (sptRequest.dateTime + (sptRequest.arriveBy ? -spgRequest.maxTimeSec
                - tOvershot : spgRequest.maxTimeSec + tOvershot));
        sptRequest.batch = (true);
        sptRequest.setRoutingContext(graph);
        // TODO swap in different state dominance logic (earliest arrival, pareto, etc.)
        final ShortestPathTree spt = new AStar().getShortestPathTree(sptRequest);

        // 3. Create a sample grid based on the SPT.
        long t1 = System.currentTimeMillis();
        Coordinate coordinateOrigin = spgRequest.coordinateOrigin;
        if (coordinateOrigin == null)
            coordinateOrigin = sptRequest.from.getCoordinate();
        final double gridSizeMeters = spgRequest.precisionMeters;
        final double cosLat = FastMath.cos(toRadians(coordinateOrigin.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;

        SparseMatrixZSampleGrid<WTWD> sampleGrid = new SparseMatrixZSampleGrid<WTWD>(16,
                spt.getVertexCount(), dX, dY, coordinateOrigin);
        sampleSPT(spt, sampleGrid, gridSizeMeters, offRoadDistanceMeters, offRoadWalkSpeedMps,
                sptRequest.getMaxWalkDistance(), spgRequest.maxTimeSec, cosLat);
        sptRequest.cleanup();

        long t2 = System.currentTimeMillis();
        LOG.info("Computed SPT in {}msec, {}msec for sampling ({} msec total)", (int) (t1 - t0),
                (int) (t2 - t1), (int) (t2 - t0));

        return sampleGrid;
    }

    /**
     * Sample a SPT using a SPTWalker and an AccumulativeGridSampler.
     */
    public static void sampleSPT(final ShortestPathTree spt, ZSampleGrid<WTWD> sampleGrid,
            final double gridSizeMeters, final double offRoadDistanceMeters, final double offRoadWalkSpeedMps,
            final double maxWalkDistance, final int maxTimeSec, final double cosLat) {

        AccumulativeMetric<WTWD> accMetric = new WTWDAccumulativeMetric(cosLat, offRoadDistanceMeters, offRoadWalkSpeedMps, gridSizeMeters);
        final AccumulativeGridSampler<WTWD> gridSampler = new AccumulativeGridSampler<WTWD>(sampleGrid, accMetric);

        // At which distance we split edges along the geometry during sampling.
        // For best results, this should be slighly lower than the grid size.
        double walkerSplitDistanceMeters = gridSizeMeters * 0.5;

        SPTWalker johnny = new SPTWalker(spt);
        johnny.walk(new SPTVisitor() {
            @Override
            public final boolean accept(Edge e) {
                return e instanceof StreetEdge;
            }

            @Override
            public final void visit(Edge e, Coordinate c, State s0, State s1, double d0, double d1, double speedAlongEdge) {
                double wd0 = s0.getWalkDistance() + d0;
                double wd1 = s0.getWalkDistance() + d1;
                double t0 = wd0 > maxWalkDistance ? Double.POSITIVE_INFINITY : s0.getActiveTime()
                        + d0 / speedAlongEdge;
                double t1 = wd1 > maxWalkDistance ? Double.POSITIVE_INFINITY : s1.getActiveTime()
                        + d1 / speedAlongEdge;
                if (t0 < maxTimeSec || t1 < maxTimeSec) {
                    if (!Double.isInfinite(t0) || !Double.isInfinite(t1)) {
                        WTWD z = new WTWD();
                        z.w = 1.0;
                        z.d = 0.0;
                        if (t0 < t1) {
                            z.wTime = t0;
                            z.wBoardings = s0.getNumBoardings();
                            z.wWalkDist = s0.getWalkDistance() + d0;
                        } else {
                            z.wTime = t1;
                            z.wBoardings = s1.getNumBoardings();
                            z.wWalkDist = s1.getWalkDistance() + d1;
                        }
                        gridSampler.addSamplingPoint(c, z, offRoadWalkSpeedMps);
                    }
                }
            }
        }, walkerSplitDistanceMeters);
        gridSampler.close();
    }

    /**
     * The default TZ data we keep for each sample: Weighted Time and Walk Distance
     * 
     * For now we keep all possible values in the vector; we may want to remove the values that will
     * not be used in the process (for example # of boardings). Currently the filtering is done
     * afterwards, it may be faster and surely less memory-intensive to do the filtering when
     * processing.
     * 
     * @author laurent
     */
    public static class WTWD {
        /* Total weight */
        public double w;

        // TODO Add generalized cost

        /* Weighted sum of time in seconds */
        public double wTime;

        /* Weighted sum of number of boardings (no units) */
        public double wBoardings;

        /* Weighted sum of walk distance in meters */
        public double wWalkDist;

        /* Minimum off-road distance to any sample */
        public double d;

        @Override
        public String toString() {
            return String.format("[t/w=%f,w=%f,d=%f]", wTime / w, w, d);
        }

        public static class IsolineMetric implements IsolineBuilder.ZMetric<WTWD> {
            @Override
            public int cut(WTWD zA, WTWD zB, WTWD z0) {
                double t0 = z0.wTime / z0.w;
                double tA = zA.d > z0.d ? Double.POSITIVE_INFINITY : zA.wTime / zA.w;
                double tB = zB.d > z0.d ? Double.POSITIVE_INFINITY : zB.wTime / zB.w;
                if (tA < t0 && t0 <= tB)
                    return 1;
                if (tB < t0 && t0 <= tA)
                    return -1;
                return 0;
            }

            @Override
            public double interpolate(WTWD zA, WTWD zB, WTWD z0) {
                if (zA.d > z0.d || zB.d > z0.d) {
                    if (zA.d > z0.d && zB.d > z0.d)
                        throw new AssertionError("dA > d0 && dB > d0");
                    // Interpolate on d
                    double k = zA.d == zB.d ? 0.5 : (z0.d - zA.d) / (zB.d - zA.d);
                    return k;
                } else {
                    // Interpolate on t
                    double tA = zA.wTime / zA.w;
                    double tB = zB.wTime / zB.w;
                    double t0 = z0.wTime / z0.w;
                    double k = tA == tB ? 0.5 : (t0 - tA) / (tB - tA);
                    return k;
                }
            }
        }
    }

    /**
     * Any given sample is weighted according to the inverse of the squared normalized distance
     * + 1 to the grid sample. We add to the sampling time a default off-road walk distance to
     * account for off-road sampling. TODO how does this "account" for off-road sampling ?
     */
    public static class WTWDAccumulativeMetric implements AccumulativeGridSampler.AccumulativeMetric<WTWD> {

        private double cosLat, offRoadDistanceMeters, offRoadSpeed, gridSizeMeters;

        public WTWDAccumulativeMetric (double cosLat, double offRoadDistanceMeters, double offRoadSpeed, double gridSizeMeters) {
            this.cosLat = cosLat;
            this.offRoadDistanceMeters = offRoadDistanceMeters;
            this.offRoadSpeed = offRoadSpeed;
            this.gridSizeMeters = gridSizeMeters;
        }

        @Override
        public WTWD cumulateSample(Coordinate C0, Coordinate Cs, WTWD z, WTWD zS, double offRoadSpeed) {
            double t = z.wTime / z.w;
            double b = z.wBoardings / z.w;
            double wd = z.wWalkDist / z.w;
            double d = SphericalDistanceLibrary.fastDistance(C0, Cs, cosLat);
            // additionnal time
            double dt = d / offRoadSpeed;
            /*
             * Compute weight for time. The weight function to distance here is somehow arbitrary.
             * It only purpose is to weight the samples when there is various samples within the
             * same "cell", giving more weight to the closests samples to the cell center.
             */
            double w = 1 / ((d + gridSizeMeters) * (d + gridSizeMeters));
            if (zS == null) {
                zS = new WTWD();
                zS.d = Double.MAX_VALUE;
            }
            zS.w = zS.w + w;
            zS.wTime = zS.wTime + w * (t + dt);
            zS.wBoardings = zS.wBoardings + w * b;
            zS.wWalkDist = zS.wWalkDist + w * (wd + d);
            if (d < zS.d)
                zS.d = d;
            return zS;
        }

        /**
         * A Generated closing sample take 1) as off-road distance, the minimum of the off-road
         * distance of all enclosing samples, plus the grid size, and 2) as time the minimum
         * time of all enclosing samples plus the grid size * off-road walk speed as additional
         * time. All this are approximations.
         *
         * TODO Is there a better way of computing this? Here the computation will be different
         * based on the order where we close the samples.
         */
        @Override
        public boolean closeSample(ZSamplePoint<WTWD> point){
            double dMin = Double.MAX_VALUE;
            double tMin = Double.MAX_VALUE;
            double bMin = Double.MAX_VALUE;
            double wdMin = Double.MAX_VALUE;
            List<WTWD> zz = new ArrayList<>(4);
            if (point.up() != null)
                zz.add(point.up().getZ());
            if (point.down() != null)
                zz.add(point.down().getZ());
            if (point.right() != null)
                zz.add(point.right().getZ());
            if (point.left() != null)
                zz.add(point.left().getZ());
            for (WTWD z : zz) {
                if (z.d < dMin)
                    dMin = z.d;
                double t = z.wTime / z.w;
                if (t < tMin)
                    tMin = t;
                double b = z.wBoardings / z.w;
                if (b < bMin)
                    bMin = b;
                double wd = z.wWalkDist / z.w;
                if (wd < wdMin)
                    wdMin = wd;
            }
            WTWD z = new WTWD();
            z.w = 1.0;
                /*
                 * The computations below are approximation, but we are on the edge anyway and the
                 * current sample does not correspond to any computed value.
                 */
            z.wTime = tMin + gridSizeMeters / offRoadSpeed;
            z.wBoardings = bMin;
            z.wWalkDist = wdMin + gridSizeMeters;
            z.d = dMin + gridSizeMeters;
            point.setZ(z);
            return dMin > offRoadDistanceMeters;
        }
    }

}
