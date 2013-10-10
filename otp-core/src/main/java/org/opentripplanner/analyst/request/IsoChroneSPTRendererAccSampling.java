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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.common.geometry.AccSamplingGridIsolineBuilder;
import org.opentripplanner.common.geometry.AccSamplingGridIsolineBuilder.ZFunc;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

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
                + (sptRequest.arriveBy ? -isoChroneRequest.getMaxCutoffSec() - tOvershot
                        : isoChroneRequest.getMaxCutoffSec() + tOvershot));
        sptRequest.setBatch(true);
        sptRequest.setRoutingContext(graphService.getGraph(sptRequest.getRouterId()));
        final ShortestPathTree spt = sptService.getShortestPathTree(sptRequest);
        sptRequest.cleanup();

        // 3. Compute the isochrone based on the SPT.
        long t1 = System.currentTimeMillis();
        Coordinate center = sptRequest.getFrom().getCoordinate();
        final double gridSizeMeters = isoChroneRequest.getPrecisionMeters();
        final double cosLat = FastMath.cos(toRadians(center.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;

        ZFunc zFunc = new ZFunc() {
            // Note: zz[] = { w, t.w, d }.
            @Override
            public double[] cumulateSample(Coordinate C0, Coordinate Cs, double z, double[] zzS) {
                double t = z;
                double d = distanceLibrary.fastDistance(C0, Cs, cosLat);
                // additionnal time
                double dt = d / V0;
                // t weight
                double w = 1 / (d + D0) * (d + D0);
                if (zzS == null) {
                    zzS = new double[3];
                    zzS[2] = Double.MAX_VALUE;
                }
                zzS[0] = zzS[0] + w;
                zzS[1] = zzS[1] + w * (t + dt);
                if (d < zzS[2])
                    zzS[2] = d;
                return zzS;
            }

            @Override
            public double[] closeSample(double[] zzUp, double[] zzDown, double[] zzRight,
                    double[] zzLeft) {
                double dMin = Double.MAX_VALUE;
                for (double[] zz : new double[][] { zzUp, zzDown, zzRight, zzLeft }) {
                    if (zz == null)
                        continue;
                    double d = zz[2] / zz[0];
                    if (d < dMin)
                        dMin = d;
                }
                double[] zz = new double[3];
                zz[0] = 1.0; // w
                zz[1] = Double.POSITIVE_INFINITY; // t
                zz[2] = dMin + gridSizeMeters; // d
                return zz;
            }

            @Override
            public int cut(double[] zzA, double[] zzB, double[] zz0) {
                double dA = zzA[2];
                double dB = zzB[2];
                double t0 = zz0[0];
                double d0 = zz0[1];
                double tA = dA > d0 ? Double.POSITIVE_INFINITY : zzA[1] / zzA[0];
                double tB = dB > d0 ? Double.POSITIVE_INFINITY : zzB[1] / zzB[0];
                if (tA < t0 && t0 <= tB)
                    return 1;
                if (tB < t0 && t0 <= tA)
                    return -1;
                return 0;
            }

            @Override
            public double interpolate(double[] zzA, double[] zzB, double[] zz0) {
                double dA = zzA[2];
                double dB = zzB[2];
                double t0 = zz0[0];
                double d0 = zz0[1];
                if (dA > d0 || dB > d0) {
                    if (dA > d0 && dB > d0)
                        throw new AssertionError("dA > d0 && dB > d0");
                    // Interpolate on d
                    double k = dA == dB ? 0.5 : (d0 - dA) / (dB - dA);
                    return k;
                } else {
                    // Interpolate on t
                    double tA = zzA[1] / zzA[0];
                    double tB = zzB[1] / zzB[0];
                    double k = tA == tB ? 0.5 : (t0 - tA) / (tB - tA);
                    return k;
                }
            }
        };
        AccSamplingGridIsolineBuilder isolineBuilder = new AccSamplingGridIsolineBuilder(dX, dY,
                center, zFunc, spt.getVertexCount());
        isolineBuilder.setDebug(isoChroneRequest.isIncludeDebugGeometry());
        computeInitialPoints(spt, isolineBuilder, gridSizeMeters * 0.7, V0,
                isoChroneRequest.getMaxCutoffSec() + tOvershot, cosLat);

        long t2 = System.currentTimeMillis();
        List<IsochroneData> isochrones = new ArrayList<IsochroneData>();
        for (Integer cutoffSec : isoChroneRequest.getCutoffSecList()) {
            IsochroneData isochrone = new IsochroneData(cutoffSec,
                    isolineBuilder.computeIsoline(new double[] { cutoffSec, D0 }));
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
     * Compute a set of initial coordinates for the given SPT
     * 
     * @param spt
     * @return
     */
    private void computeInitialPoints(ShortestPathTree spt,
            AccSamplingGridIsolineBuilder isolineBuilder, double d0, double v0, long tMax,
            double cosLat) {
        int n = 0;
        int nSkippedDupEdge = 0, nSkippedTimeOut = 0;
        Collection<? extends State> allStates = spt.getAllStates();
        Set<Edge> processedEdges = new HashSet<Edge>(allStates.size());
        for (State s0 : allStates) {
            for (Edge e : s0.getVertex().getIncoming()) {
                // Take only street
                if (e != null && e instanceof StreetEdge) {
                    State s1 = spt.getState(e.getFromVertex());
                    if (s1 == null)
                        continue;
                    long t0 = s0.getActiveTime();
                    long t1 = s1.getActiveTime();
                    if (t0 > tMax && t1 > tMax) {
                        nSkippedTimeOut++;
                        continue;
                    }
                    if (e.getFromVertex() != null && e.getToVertex() != null) {
                        // Hack alert: e.hashCode() throw NPE
                        if (processedEdges.contains(e)) {
                            nSkippedDupEdge++;
                            continue;
                        }
                        processedEdges.add(e);
                    }
                    Vertex vx0 = s0.getVertex();
                    Vertex vx1 = s1.getVertex();
                    LineString lineString = e.getGeometry();

                    isolineBuilder.addSample(vx0.getCoordinate(), t0);
                    isolineBuilder.addSample(vx1.getCoordinate(), t1);
                    n += 2;
                    Coordinate[] pList = lineString.getCoordinates();
                    boolean reverse = vx1.getCoordinate().equals(pList[0]);
                    // Length of linestring
                    double lineStringLen = distanceLibrary.fastLength(lineString, cosLat);
                    // Split the linestring in nSteps
                    if (lineStringLen > d0) {
                        int nSteps = (int) Math.floor(lineStringLen / d0) + 1; // Number of steps
                        double stepLen = lineStringLen / nSteps; // Length of step
                        double startLen = 0; // Distance at start of current seg
                        double curLen = stepLen; // Distance cursor
                        int ns = 1;
                        for (int i = 0; i < pList.length - 1; i++) {
                            Coordinate p0 = pList[i];
                            Coordinate p1 = pList[i + 1];
                            double segLen = distanceLibrary.fastDistance(p0, p1);
                            while (curLen - startLen < segLen) {
                                double k = (curLen - startLen) / segLen;
                                Coordinate p = new Coordinate(p0.x * (1 - k) + p1.x * k, p0.y
                                        * (1 - k) + p1.y * k);
                                double t0b = (reverse ? t1 : t0) + curLen / v0;
                                double t1b = (reverse ? t0 : t1) + (lineStringLen - curLen) / v0;
                                isolineBuilder.addSample(p, t0b < t1b ? t0b : t1b);
                                n++;
                                curLen += stepLen;
                                ns++;
                            }
                            startLen += segLen;
                            if (ns >= nSteps)
                                break;
                        }
                    }
                }
            }
        }
        LOG.info("Created {} initial points ({} dup edges, {} out time) from {} states.", n,
                nSkippedDupEdge, nSkippedTimeOut, allStates.size());
    }
}
