package org.opentripplanner.api.thrift.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;

import org.apache.thrift.TException;
import org.opentripplanner.api.thrift.OTPServerTask;
import org.opentripplanner.api.thrift.definition.BulkFindNearestEdgesRequest;
import org.opentripplanner.api.thrift.definition.BulkFindNearestEdgesResponse;
import org.opentripplanner.api.thrift.definition.BulkFindNearestVertexRequest;
import org.opentripplanner.api.thrift.definition.BulkFindNearestVertexResponse;
import org.opentripplanner.api.thrift.definition.BulkPathsRequest;
import org.opentripplanner.api.thrift.definition.BulkPathsResponse;
import org.opentripplanner.api.thrift.definition.FindNearestEdgesRequest;
import org.opentripplanner.api.thrift.definition.FindNearestEdgesResponse;
import org.opentripplanner.api.thrift.definition.FindNearestVertexRequest;
import org.opentripplanner.api.thrift.definition.FindNearestVertexResponse;
import org.opentripplanner.api.thrift.definition.FindPathsRequest;
import org.opentripplanner.api.thrift.definition.FindPathsResponse;
import org.opentripplanner.api.thrift.definition.GraphEdge;
import org.opentripplanner.api.thrift.definition.GraphEdgesRequest;
import org.opentripplanner.api.thrift.definition.GraphEdgesResponse;
import org.opentripplanner.api.thrift.definition.GraphVertex;
import org.opentripplanner.api.thrift.definition.GraphVerticesRequest;
import org.opentripplanner.api.thrift.definition.GraphVerticesResponse;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.api.thrift.definition.NearestEdgesQuery;
import org.opentripplanner.api.thrift.definition.NearestEdgesResult;
import org.opentripplanner.api.thrift.definition.OTPService;
import org.opentripplanner.api.thrift.definition.PathOptions;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.api.thrift.definition.TripPaths;
import org.opentripplanner.api.thrift.definition.VertexQuery;
import org.opentripplanner.api.thrift.definition.VertexResult;
import org.opentripplanner.api.thrift.util.EdgeMatchExtension;
import org.opentripplanner.api.thrift.util.GraphEdgeExtension;
import org.opentripplanner.api.thrift.util.GraphVertexExtension;
import org.opentripplanner.api.thrift.util.LatLngExtension;
import org.opentripplanner.api.thrift.util.RoutingRequestBuilder;
import org.opentripplanner.api.thrift.util.TravelModeSet;
import org.opentripplanner.api.thrift.util.TripPathsExtension;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdge;
import org.opentripplanner.routing.impl.CandidateEdge.CandidateEdgeScoreComparator;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Concrete implementation of the Thrift interface.
 * 
 * @author flamholz
 */
@Data
public class OTPServiceImpl implements OTPService.Iface {

    private static Logger LOG = LoggerFactory.getLogger(OTPServerTask.class);

    private GraphService graphService;

    private PathService pathService;
    
    private RoutingRequest prototypeRoutingRequest = new RoutingRequest();

    /**
     * Convenience getter for street index.
     * 
     * @return
     */
    private StreetVertexIndexService getStreetIndex() {
        return graphService.getGraph().streetIndex;
    }

    /**
     * Returns all vertices in the graph as GraphVertices.
     * 
     * @param g
     * @return
     */
    private static List<GraphVertex> makeGraphVertices(Graph g) {
        Collection<Vertex> verts = g.getVertices();
        List<GraphVertex> l = new ArrayList<GraphVertex>(verts.size());
        for (Vertex v : verts) {
            l.add(new GraphVertexExtension(v));
        }
        return l;
    }
    
    @Override
    public GraphVerticesResponse GetVertices(GraphVerticesRequest req) throws TException {
        LOG.info("GetVertices called");
        long startTime = System.currentTimeMillis();

        GraphVerticesResponse res = new GraphVerticesResponse();
        Graph g = graphService.getGraph();
        res.setVertices(makeGraphVertices(g));
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);
        return res;
    }

    /**
     * Returns all edges in the graph as GraphEdges.
     * 
     * @param g
     * @return
     */
    private static List<GraphEdge> makeGraphEdges(Graph g, GraphEdgesRequest req) {
        Collection<Edge> edges;
        if (!req.isSetStreet_edges_only()) {
            // Get all edges.
            edges = g.getEdges();
        } else {
            // Get only street edges
            Collection<StreetEdge> ses = g.getStreetEdges();
            
            // Filter the StreetEdges by allowed traversal modes.
            TraverseModeSet allowedModes = TraverseModeSet.allModes();
            if (req.isSetCan_be_traversed_by()) {
                allowedModes = new TravelModeSet(req.getCan_be_traversed_by()).toTraverseModeSet();
            }
            
            Set<Edge> traversableStreetEdges = new HashSet<Edge>();
            for (StreetEdge se : ses) {
                if (se.canTraverse(allowedModes)) {
                    traversableStreetEdges.add(se);
                }
            }
            edges = traversableStreetEdges;
        }
        
        List<GraphEdge> l = new ArrayList<GraphEdge>(edges.size());
        for (Edge e : edges) {
            l.add(new GraphEdgeExtension(e));
        }
        return l;
    }
    
    @Override
    public GraphEdgesResponse GetEdges(GraphEdgesRequest req) throws TException {
        LOG.info("GetEdges called");
        long startTime = System.currentTimeMillis();

        GraphEdgesResponse res = new GraphEdgesResponse();
        Graph g = graphService.getGraph();      
        res.setEdges(makeGraphEdges(g, req));
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);
        return res;
    }
    
    private VertexResult findNearbyVertex(VertexQuery q) {
        // NOTE(flamholz): can't set the graph here because we are not
        // actually doing any routing and don't have a to/from. From the
        // perspective of the street indes, RoutingRequest is really just
        // a container for the TraversalModes, which is a weird design
        // but it's what we've got to work with.
        RoutingRequestBuilder builder = new RoutingRequestBuilder(prototypeRoutingRequest);
        if (q.isSetAllowed_modes()) {
            builder.setTravelModes(q.getAllowed_modes());
        }
        RoutingRequest rr = builder.build();

        // Get the nearest vertex
        StreetVertexIndexService streetVertexIndex = getStreetIndex();
        GenericLocation gl = new LatLngExtension(q.getLocation().getLat_lng()).toGenericLocation();
        // NOTE(flamholz): We don't currently provide a name.
        // I guess this would speed things up somewhat?
        Vertex closest = streetVertexIndex.getVertexForLocation(gl, rr);
        VertexResult result = new VertexResult();
        result.setNearest_vertex(new GraphVertexExtension(closest));
        
        rr.cleanup();
        return result;
    }
    
    @Override
    public FindNearestVertexResponse FindNearestVertex(FindNearestVertexRequest req)
            throws TException {
        LOG.info("FindNearestVertex called");
        long startTime = System.currentTimeMillis();
        VertexQuery q = req.getQuery();
        VertexResult result = findNearbyVertex(q);
        
        FindNearestVertexResponse res = new FindNearestVertexResponse();
        res.setResult(result);
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);
        return res;
    }
    
    @Override
    public BulkFindNearestVertexResponse BulkFindNearestVertex(BulkFindNearestVertexRequest req)
            throws TException {
        LOG.info("BulkFindNearestVertex called");
        long startTime = System.currentTimeMillis();
        
        BulkFindNearestVertexResponse res = new BulkFindNearestVertexResponse();
        for (VertexQuery q : req.getQueries()) {
            VertexResult result = findNearbyVertex(q);
            res.addToResults(result);
        }
        
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);
        return res;
    }

    private NearestEdgesResult findNearestEdges(NearestEdgesQuery q) {
        // Set up the TraversalRequirements.
        TraversalRequirements requirements = new TraversalRequirements();
        requirements.setModes(new TravelModeSet(q.getAllowed_modes()).toTraverseModeSet());
        
        // Set up the LocationObservation.
        Location queryLoc = q.getLocation();
        GenericLocation loc = new LatLngExtension(queryLoc.getLat_lng()).toGenericLocation();
        if (queryLoc.isSetHeading()) loc.setHeading(queryLoc.getHeading());
                
        // Find the candidate edges.
        // NOTE(flamholz): for now this will return at smallish number of edges because of
        // the internal binning that's going on. I'd rather get more edges just in case...
        StreetVertexIndexService streetVertexIndex = getStreetIndex();
        CandidateEdgeBundle edges = streetVertexIndex.getClosestEdges(loc, requirements);

        // Sort them by score.
        CandidateEdgeScoreComparator comp = new CandidateEdgeScoreComparator();
        Collections.sort(edges, comp);
        
        // Add matches to the response.
        NearestEdgesResult result = new NearestEdgesResult();
        int maxEdges = q.getMax_edges();
        for (CandidateEdge e : edges) {
            if (result.getNearest_edgesSize() >= maxEdges) break;
            result.addToNearest_edges(new EdgeMatchExtension(e));
        }
        
        return result;
    }
    
    @Override
    public FindNearestEdgesResponse FindNearestEdges(FindNearestEdgesRequest req) throws TException {
        LOG.info("FindNearestEdges called");
        long startTime = System.currentTimeMillis();

        NearestEdgesQuery q = req.getQuery();
        NearestEdgesResult result = findNearestEdges(q);

        FindNearestEdgesResponse res = new FindNearestEdgesResponse();
        res.setResult(result);
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);
        return res;
    }
    
    @Override
    public BulkFindNearestEdgesResponse BulkFindNearestEdges(BulkFindNearestEdgesRequest req)
            throws TException {
        LOG.info("BulkFindNearestEdges called");
        long startTime = System.currentTimeMillis();

        BulkFindNearestEdgesResponse res = new BulkFindNearestEdgesResponse();
        for (NearestEdgesQuery q : req.getQueries()) {
            NearestEdgesResult result = findNearestEdges(q);
            res.addToResults(result);
        }
        
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);
        return res;
    }

    /**
     * Computes the GraphPath for the given trip.
     * 
     * @param trip
     * @return
     */
    private TripPaths computePaths(TripParameters trip, PathOptions pathOptions) {
        // Build the RoutingRequest. For now, get only one itinerary.
        RoutingRequest options = (new RoutingRequestBuilder(prototypeRoutingRequest))
                .addTripParameters(trip)
                .setGraph(graphService.getGraph())
                .setNumItineraries(pathOptions.getNum_paths())
                .build();

        // For now, always use the default router.
        options.setRouterId("");

        List<GraphPath> paths = pathService.getPaths(options);
        if (paths == null || paths.size() == 0) {
            LOG.warn("Found 0 paths for trip {}", trip);
        }
        TripPathsExtension tripPaths = new TripPathsExtension(trip, paths);

        // Need to call RoutingRequest.cleanup() to cleanup the temp edges.
        options.cleanup();
        return tripPaths;
    }

    @Override
    public FindPathsResponse FindPaths(FindPathsRequest req) throws TException {
        LOG.info("FindPaths called");
        long startTime = System.currentTimeMillis();

        TripParameters trip = req.getTrip();
        TripPaths outPaths = new TripPaths();
        outPaths.setTrip(trip);

        TripPaths tripPaths = computePaths(trip, req.getOptions());

        FindPathsResponse res = new FindPathsResponse();
        res.setPaths(tripPaths);
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);

        return res;
    }

    @Override
    public BulkPathsResponse BulkFindPaths(BulkPathsRequest req) throws TException {
        LOG.info("BulkFindPaths called");
        long startTime = System.currentTimeMillis();

        PathOptions pathOptions = req.getOptions();
        BulkPathsResponse res = new BulkPathsResponse();
        for (TripParameters trip : req.getTrips()) {
            TripPaths tripPaths = computePaths(trip, pathOptions);
            res.addToPaths(tripPaths);
        }
        res.setCompute_time_millis(System.currentTimeMillis() - startTime);
        return res;
    }

}