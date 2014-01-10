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

import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.common.geometry.AccumulativeGridSampler;
import org.opentripplanner.common.geometry.AccumulativeGridSampler.AccumulativeMetric;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.ZSampleGrid;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.SPTWalker;
import org.opentripplanner.routing.spt.SPTWalker.SPTVisitor;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Compute a sample grid from a SPT request.
 * 
 * @author laurent
 */
@Component
public class SampleGridRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(SampleGridRenderer.class);

    @Autowired
    private GraphService graphService;

    @Autowired
    private SPTService sptService;

    /**
     * @param spgRequest
     * @param sptRequest
     * @return
     */
    public ZSampleGrid<WTWD> getSampleGrid(SampleGridRequest spgRequest, RoutingRequest sptRequest) {

        final double D0 = getOffRoadDistanceMeters(spgRequest.getPrecisionMeters());
        final double V0 = 1.00; // m/s, off-road walk speed

        // 1. Compute the Shortest Path Tree.
        long t0 = System.currentTimeMillis();
        long tOvershot = (long) (2 * D0 / V0);
        sptRequest.setWorstTime(sptRequest.dateTime
                + (sptRequest.arriveBy ? -spgRequest.getMaxTimeSec() - tOvershot : spgRequest
                        .getMaxTimeSec() + tOvershot));
        sptRequest.setBatch(true);
        sptRequest.setRoutingContext(graphService.getGraph(sptRequest.getRouterId()));
        final ShortestPathTree spt = sptService.getShortestPathTree(sptRequest);

        // 3. Create a sample grid based on the SPT.
        long t1 = System.currentTimeMillis();
        Coordinate center = sptRequest.getFrom().getCoordinate();
        final double gridSizeMeters = spgRequest.getPrecisionMeters();
        final double cosLat = FastMath.cos(toRadians(center.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;

        SparseMatrixZSampleGrid<WTWD> sampleGrid = new SparseMatrixZSampleGrid<WTWD>(16,
                spt.getVertexCount(), dX, dY, center);
        sampleSPT(spt, sampleGrid, gridSizeMeters * 0.7, gridSizeMeters, V0,
                sptRequest.getMaxWalkDistance(), cosLat);
        sptRequest.cleanup();

        long t2 = System.currentTimeMillis();
        LOG.info("Computed SPT in {}msec, {}msec for sampling ({} msec total)", (int) (t1 - t0),
                (int) (t2 - t1), (int) (t2 - t0));

        return sampleGrid;
    }

    /**
     * Sample a SPT using a SPTWalker and an AccumulativeGridSampler.
     * 
     * @param spt
     * @return
     */
    public void sampleSPT(ShortestPathTree spt, ZSampleGrid<WTWD> sampleGrid, final double d0,
            final double gridSizeMeters, final double v0, final double maxWalkDistance,
            final double cosLat) {

        final DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

        AccumulativeMetric<WTWD> accMetric = new AccumulativeMetric<WTWD>() {
            @Override
            public WTWD cumulateSample(Coordinate C0, Coordinate Cs, double z, WTWD zS) {
                double t = z;
                double d = distanceLibrary.fastDistance(C0, Cs, cosLat);
                // additionnal time
                double dt = d / v0;
                // t weight
                double w = 1 / ((d + d0) * (d + d0));
                if (zS == null) {
                    zS = new WTWD();
                    zS.d = Double.MAX_VALUE;
                }
                zS.w = zS.w + w;
                zS.tw = zS.tw + w * (t + dt);
                if (d < zS.d)
                    zS.d = d;
                return zS;
            }

            @Override
            public WTWD closeSample(WTWD zUp, WTWD zDown, WTWD zRight, WTWD zLeft) {
                double dMin = Double.MAX_VALUE;
                double tMin = Double.MAX_VALUE;
                for (WTWD z : new WTWD[] { zUp, zDown, zRight, zLeft }) {
                    if (z == null)
                        continue;
                    if (z.d < dMin)
                        dMin = z.d;
                    double t = z.tw / z.w;
                    if (t < tMin)
                        tMin = t;
                }
                WTWD z = new WTWD();
                z.w = 1.0;
                // Two method below are approximation,
                // but we are on the edge anyway and
                // the current sample does not correspond to any
                // computed value.
                z.tw = tMin + gridSizeMeters / v0;
                z.d = dMin + gridSizeMeters;
                return z;
            }
        };
        final AccumulativeGridSampler<WTWD> gridSampler = new AccumulativeGridSampler<WTWD>(
                sampleGrid, accMetric);

        SPTWalker johnny = new SPTWalker(spt);
        johnny.walk(new SPTVisitor() {
            @Override
            public final boolean accept(Edge e) {
                return e instanceof StreetEdge;
            }

            @Override
            public final void visit(Coordinate c, State s0, State s1, double d0, double d1) {
                double wd0 = s0.getWalkDistance() + d0;
                double wd1 = s0.getWalkDistance() + d1;
                double t0 = wd0 > maxWalkDistance ? Double.POSITIVE_INFINITY : s0.getActiveTime()
                        + d0 / v0;
                double t1 = wd1 > maxWalkDistance ? Double.POSITIVE_INFINITY : s1.getActiveTime()
                        + d1 / v0;
                if (!Double.isInfinite(t0) || !Double.isInfinite(t1))
                    gridSampler.addSamplingPoint(c, t0 < t1 ? t0 : t1);
            }
        }, d0);
        gridSampler.close();
    }

    public double getOffRoadDistanceMeters(double precisionMeters) {
        // Off-road max distance MUST be APPROX EQUALS to the grid precision
        // TODO: Loosen this restriction (by adding more closing sample).
        // Change the 0.8 magic factor here with caution.
        return 0.8 * precisionMeters;
    }

    public static class WTWD {
        public double w;

        public double tw;

        public double d;

        @Override
        public String toString() {
            return String.format("[t/w=%f,w=%f,d=%f]", tw / w, w, d);
        }
    }
}
