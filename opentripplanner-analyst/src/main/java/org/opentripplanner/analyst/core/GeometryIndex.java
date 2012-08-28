package org.opentripplanner.analyst.core;

import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;

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
        // build a spatial index of road geometries
        pedestrianIndex = new STRtree();
        index = new STRtree();
        for (StreetVertex vertex : IterableLibrary.filter(graph.getVertices(), StreetVertex.class)) {
            for (StreetEdge e: IterableLibrary.filter(vertex.getOutgoing(), StreetEdge.class)) {
                Geometry geom = e.getGeometry();
                if (e.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
                    pedestrianIndex.insert(geom.getEnvelopeInternal(), e);
                }
                index.insert(geom.getEnvelopeInternal(), e);
            }
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
