package org.opentripplanner.analyst.request;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.opentripplanner.common.geometry.RecursiveGridIsolineBuilder;
import org.opentripplanner.common.geometry.RecursiveGridIsolineBuilder.ZFunc;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Compute isochrones out of a shortest path tree request.
 * 
 * @author laurent
 */
@Component
public class IsoChroneSPTRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(IsoChroneSPTRenderer.class);

    @Autowired
    private GraphService graphService;

    @Autowired
    private SPTService sptService;

    @Autowired
    private SampleSource sampleSource;

    /**
     * @param isoChroneRequest
     * @param sptRequest
     * @return
     * @throws Exception
     */
    public List<IsochroneData> getIsochrones(IsoChroneRequest isoChroneRequest,
            RoutingRequest sptRequest) throws Exception {

        // 1. Compute the Shortest Path Tree.
        sptRequest.setWorstTime(sptRequest.dateTime
                + (sptRequest.arriveBy ? -isoChroneRequest.getMaxCutoffSec() : isoChroneRequest
                        .getMaxCutoffSec()));
        sptRequest.setBatch(true);
        sptRequest.setRoutingContext(graphService.getGraph(sptRequest.getRouterId()));
        long t0 = System.currentTimeMillis();
        final ShortestPathTree spt = sptService.getShortestPathTree(sptRequest);
        long t1 = System.currentTimeMillis();
        sptRequest.cleanup();

        // 2. Compute the set of initial points
        List<Coordinate> initialPoints = computeInitialPoints(spt);

        // 3. Compute the isochrone based on the SPT.
        ZFunc timeFunc = new ZFunc() {
            @Override
            public long z(Coordinate c) {
                // TODO Make the sample source multi-router compatible
                Sample sample = sampleSource.getSample(c.x, c.y);
                if (sample == null) {
                    return Long.MAX_VALUE;
                }
                Long z = sample.eval(spt);
                return z;
            }
        };
        // TODO Snap the center as XYZ tile grid for better sample-reuse (if using sample cache).
        Coordinate center = sptRequest.getFrom().getCoordinate();
        double gridSizeMeters = isoChroneRequest.getPrecisionMeters();
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / Math.cos(center.x);
        LOG.info("dX={}, dY={}", dX, dY);
        RecursiveGridIsolineBuilder isolineBuilder = new RecursiveGridIsolineBuilder(dX, dY,
                center, timeFunc, initialPoints);
        isolineBuilder.setDebugCrossingEdges(isoChroneRequest.isIncludeDebugGeometry());
        isolineBuilder.setDebugSeedGrid(isoChroneRequest.isIncludeDebugGeometry());
        long t2 = System.currentTimeMillis();
        List<IsochroneData> isochrones = new ArrayList<IsochroneData>();
        for (Integer cutoffSec : isoChroneRequest.getCutoffSecList()) {
            IsochroneData isochrone = new IsochroneData(cutoffSec,
                    isolineBuilder.computeIsoline(cutoffSec));
            if (isoChroneRequest.isIncludeDebugGeometry())
                isochrone.setDebugGeometry(isolineBuilder.getDebugGeometry());
            isochrones.add(isochrone);
        }
        long t3 = System.currentTimeMillis();
        LOG.info("Computed SPT in {}msec, {} isochrones in {}msec", (int) (t1 - t0),
                isochrones.size(), (int) (t3 - t2));

        return isochrones;
    }

    /**
     * Compute a set of initial coordinates for the given SPT
     * 
     * @param spt
     * @return
     */
    private List<Coordinate> computeInitialPoints(ShortestPathTree spt) {
        List<Coordinate> retval = new ArrayList<Coordinate>(spt.getVertexCount());
        for (State s : spt.getAllStates()) {
            Vertex v = s.getVertex();
            // Take only street
            if (v instanceof StreetVertex) {
                retval.add(v.getCoordinate());
            }
        }
        LOG.debug("Created {} initial points from {} vertexes.", retval.size(),
                spt.getVertexCount());
        return retval;
    }
}
