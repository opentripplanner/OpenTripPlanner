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
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.common.geometry.AccumulativeGridSampler;
import org.opentripplanner.common.geometry.AccumulativeGridSampler.AccumulativeMetric;
import org.opentripplanner.common.geometry.DelaunayIsolineBuilder;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.IsolineBuilder.ZMetric;
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
 * Compute isochrones out of a shortest path tree request (AccSampling isoline algorithm).
 * 
 * @author laurent
 */
@Component
public class IsoChroneSPTRendererAccSampling implements IsoChroneSPTRenderer {

    private static final Logger LOG = LoggerFactory
            .getLogger(IsoChroneSPTRendererAccSampling.class);

    @Autowired
    private GraphService graphService;

    @Autowired
    private SPTService sptService;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    /**
     * @param isoChroneRequest
     * @param sptRequest
     * @return
     */
    @Override
    public List<IsochroneData> getIsochrones(IsoChroneRequest isoChroneRequest,
            RoutingRequest sptRequest) {

        // Off-road max distance MUST be APPROX EQUALS to the grid precision
        final double D0 = isoChroneRequest.getPrecisionMeters() * 0.8;
        final double V0 = 1.00; // m/s, off-road walk speed

        // 1. Compute the Shortest Path Tree.
        long t0 = System.currentTimeMillis();
        long tOvershot = (long) (2 * D0 / V0);
        sptRequest.setWorstTime(sptRequest.dateTime
                + (sptRequest.arriveBy ? -isoChroneRequest.getMaxTimeSec() - tOvershot
                        : isoChroneRequest.getMaxTimeSec() + tOvershot));
        sptRequest.setBatch(true);
        sptRequest.setRoutingContext(graphService.getGraph(sptRequest.getRouterId()));
        final ShortestPathTree spt = sptService.getShortestPathTree(sptRequest);

        // 3. Create a sample grid based on the SPT.
        long t1 = System.currentTimeMillis();
        Coordinate center = sptRequest.getFrom().getCoordinate();
        final double gridSizeMeters = isoChroneRequest.getPrecisionMeters();
        final double cosLat = FastMath.cos(toRadians(center.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;

        SparseMatrixZSampleGrid<WTWD> sampleGrid = new SparseMatrixZSampleGrid<WTWD>(16,
                spt.getVertexCount(), dX, dY, center);
        sampleSPT(spt, sampleGrid, gridSizeMeters * 0.7, gridSizeMeters, V0,
                sptRequest.getMaxWalkDistance(), cosLat);
        sptRequest.cleanup();

        // 4. Compute isolines
        ZMetric<WTWD> zMetric = new ZMetric<WTWD>() {
            @Override
            public int cut(WTWD zA, WTWD zB, WTWD z0) {
                double t0 = z0.tw / z0.w;
                double tA = zA.d > z0.d ? Double.POSITIVE_INFINITY : zA.tw / zA.w;
                double tB = zB.d > z0.d ? Double.POSITIVE_INFINITY : zB.tw / zB.w;
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
                    double tA = zA.tw / zA.w;
                    double tB = zB.tw / zB.w;
                    double t0 = z0.tw / z0.w;
                    double k = tA == tB ? 0.5 : (t0 - tA) / (tB - tA);
                    return k;
                }
            }
        };
        DelaunayIsolineBuilder<WTWD> isolineBuilder = new DelaunayIsolineBuilder<WTWD>(sampleGrid,
                zMetric);
        isolineBuilder.setDebug(isoChroneRequest.isIncludeDebugGeometry());

        long t2 = System.currentTimeMillis();
        List<IsochroneData> isochrones = new ArrayList<IsochroneData>();
        for (Integer cutoffSec : isoChroneRequest.getCutoffSecList()) {
            WTWD z0 = new WTWD();
            z0.w = 1.0;
            z0.tw = cutoffSec;
            z0.d = D0;
            IsochroneData isochrone = new IsochroneData(cutoffSec,
                    isolineBuilder.computeIsoline(z0));
            if (isoChroneRequest.isIncludeDebugGeometry())
                isochrone.setDebugGeometry(isolineBuilder.getDebugGeometry());
            isochrones.add(isochrone);
        }

        long t3 = System.currentTimeMillis();
        LOG.info("Computed SPT in {}msec, {} isochrones in {}msec ({}msec for sampling)",
                (int) (t1 - t0), isochrones.size(), (int) (t3 - t1), (int) (t2 - t1));

        return isochrones;
    }

    /**
     * Sample a SPT using a SPTWalker and an AccumulativeGridSampler.
     * 
     * @param spt
     * @return
     */
    private void sampleSPT(ShortestPathTree spt, ZSampleGrid<WTWD> sampleGrid, final double d0,
            final double gridSizeMeters, final double v0, final double maxWalkDistance,
            final double cosLat) {

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
                for (WTWD z : new WTWD[] { zUp, zDown, zRight, zLeft }) {
                    if (z == null)
                        continue;
                    if (z.d < dMin)
                        dMin = z.d;
                }
                WTWD z = new WTWD();
                z.w = 1.0;
                z.tw = Double.POSITIVE_INFINITY;
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

    private static class WTWD {
        private double w;

        private double tw;

        private double d;

        @Override
        public String toString() {
            return String.format("[t/w=%f,w=%f,d=%f]", tw / w, w, d);
        }
    }
}
