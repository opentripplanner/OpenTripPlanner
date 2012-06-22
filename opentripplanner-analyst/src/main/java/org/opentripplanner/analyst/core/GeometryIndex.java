package org.opentripplanner.analyst.core;

import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.distance.DistanceOp;

@Component
public class GeometryIndex implements GeometryIndexService {
    
    private static final Logger LOG = LoggerFactory.getLogger(GeometryIndex.class);
    private static final double SEARCH_RADIUS_M = 100; // meters
    private static final double SEARCH_RADIUS_DEG = SphericalDistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);
    
    private STRtree pedestrianIndex;
    private STRtree index;
    
    @Autowired
    public void setGraphService(GraphService graphService) {
        Graph graph = graphService.getGraph();
        if (graph == null) // analyst currently depends on there being a single default graph
        	return;
        // build a spatial index of road geometries (not individual edges)
        pedestrianIndex = new STRtree();
        index = new STRtree();
        for (TurnVertex tv : IterableLibrary.filter(graph.getVertices(), TurnVertex.class)) {
            Geometry geom = tv.getGeometry();
            if (tv.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
                pedestrianIndex.insert(geom.getEnvelopeInternal(), tv);
            }
            index.insert(geom.getEnvelopeInternal(), tv);
        }
        pedestrianIndex.build();
        index.build();
        LOG.debug("spatial index size: {}", pedestrianIndex.size());
    }

    @SuppressWarnings("rawtypes")
    public List queryPedestrian(Envelope env) {
        return pedestrianIndex.query(env);
    }
    
    @SuppressWarnings("rawtypes")
    public List query(Envelope env) {
        return index.query(env);
    }

    @Override
    public Vertex getNearestPedestrianStreetVertex(double lon, double lat) {
        Coordinate c = new Coordinate(lon, lat);
        Point p = GeometryUtils.getGeometryFactory().createPoint(c);

        // track best two turn vertices
        TurnVertex closestVertex = null;
        double bestDistance = Double.MAX_VALUE;

        // query
        Envelope env = new Envelope(c);
        env.expandBy(SEARCH_RADIUS_DEG, SEARCH_RADIUS_DEG);
        @SuppressWarnings("unchecked")
        List<TurnVertex> vs = (List<TurnVertex>) pedestrianIndex.query(env);
        // query always returns a (possibly empty) list, but never null

        // find two closest among nearby geometries
        for (TurnVertex v : vs) {
            Geometry g = v.getGeometry();
            DistanceOp o = new DistanceOp(p, g);
            double d = o.distance();
            if (d > SEARCH_RADIUS_DEG)
                continue;
            if (d < bestDistance) {
                closestVertex = v;
                bestDistance = d;
            }
        }

        return closestVertex;
    }

    @Override
    public BoundingBox getBoundingBox(CoordinateReferenceSystem crs) {
        try {
            Envelope bounds = (Envelope) index.getRoot().getBounds();
            ReferencedEnvelope refEnv = new ReferencedEnvelope(bounds, CRS.decode("EPSG:4326", true));
            return refEnv.toBounds(crs);
        } catch (Exception e) {
            LOG.error("error transforming graph bounding box to request CRS : {}", crs);
            return null;
        }
    }
    
}
