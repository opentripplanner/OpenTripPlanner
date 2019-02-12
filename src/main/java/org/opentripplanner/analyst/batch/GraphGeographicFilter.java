package org.opentripplanner.analyst.batch;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class GraphGeographicFilter implements IndividualFilter {

    private static final Logger LOG = LoggerFactory.getLogger(GraphGeographicFilter.class);

    private RoutingRequest prototypeRoutingRequest;
    private GraphService graphService;

    public GraphGeographicFilter(RoutingRequest prototypeRoutingRequest, GraphService graphService) {
        this.prototypeRoutingRequest = prototypeRoutingRequest;
        this.graphService = graphService;
        findHull();
    }

    private double bufferMeters = 2000;
    private boolean useOnlyStops = true;
    private static GeometryFactory gf = new GeometryFactory();
    private Geometry hull;
    
    public void findHull() {
        LOG.info("finding hull of graph...");
        LOG.debug("using only stops? {}", useOnlyStops);
        if (bufferMeters < prototypeRoutingRequest.maxWalkDistance)
            LOG.warn("geographic filter buffer is smaller than max walk distance, this will probably yield incorrect results.");
        Graph graph= graphService.getRouter(prototypeRoutingRequest.routerId).graph;
        List<Geometry> geometries = new ArrayList<Geometry>();
        for (Vertex v : graph.getVertices()) {
            if (useOnlyStops && ! (v instanceof TransitStop))
                continue;
            Point pt = gf.createPoint(v.getCoordinate());
            Geometry geom = crudeProjectedBuffer(pt, bufferMeters);
            geometries.add(geom);
        }
        Geometry multiGeom = gf.buildGeometry(geometries);
        LOG.info("unioning hull...");
        hull = multiGeom.union();
        LOG.trace("hull is {}", hull.toText());
        // may lead to false rejections
        // DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier();
    }
    
    private Geometry crudeProjectedBuffer(Point pt, double distanceMeters) {
        final double mPerDegreeLat = 111111.111111;
        double lat = pt.getY();
        double lonScale = Math.cos(Math.PI * lat / 180);
        double latExpand = distanceMeters / mPerDegreeLat;
        double lonExpand = latExpand / lonScale;
        Envelope env = pt.getEnvelopeInternal();
        env.expandBy(lonExpand, latExpand);
        return gf.toGeometry(env);
    }
    
    @Override
    public boolean filter(Individual individual) {
        Coordinate coord = new Coordinate(individual.lon, individual.lat);
        Point pt = gf.createPoint(coord);
        boolean accept = hull.contains(pt);
        //LOG.debug("label {} accept {}", individual.label, accept);
        return accept;
    }

}
