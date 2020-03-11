package org.opentripplanner.routing.algorithm.raptor.router.street;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.graph_builder.module.NearbyStopFinder.StopAtDistance;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.BarrierVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This uses a street search to find paths to all the access/egress stop within range
 */
public class AccessEgressRouter {
    private static Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

    public AccessEgressRouter(RoutingRequest rr,int distanceMeters) {
        this.request = rr;
        this.nearbyStopFinder = new NearbyStopFinder(rr.rctx.graph, distanceMeters, true);
    }

    private RoutingRequest request;
    private NearbyStopFinder nearbyStopFinder;
    /**
     *
     * @param rr the current routing request
     * @param fromTarget whether to route from or towards the point provided in the routing request
     *                   (access or egress)
     * @param distanceMeters the maximum street distance to search for access/egress stops
     * @return Transfer objects by access/egress stop
     */
    public static Map<Stop, Transfer> streetSearch (RoutingRequest rr, boolean fromTarget, int distanceMeters) {
        Set<Vertex> vertices = fromTarget ? rr.rctx.toVertices : rr.rctx.fromVertices;

        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(rr.rctx.graph, distanceMeters, true);
        // We set removeTempEdges to false because this is a sub-request - the temporary edges for the origin and
        // target vertex will be cleaned up at the end of the super-request, and we don't want that to happen twice.
        List<NearbyStopFinder.StopAtDistance> stopAtDistanceList =
                nearbyStopFinder.findNearbyStopsViaStreets(vertices, fromTarget, false);

        Map<Stop, Transfer> result = new HashMap<>();
        for (NearbyStopFinder.StopAtDistance stopAtDistance : stopAtDistanceList) {
            result.put(
                    stopAtDistance.tstop.getStop(),
                    new Transfer(-1,
                            (int)stopAtDistance.edges.stream().map(Edge::getEffectiveWalkDistance)
                                    .collect(Collectors.summarizingDouble(Double::doubleValue)).getSum(),
                            stopAtDistance.edges));
        }

        LOG.debug("Found {} {} stops", result.size(), fromTarget ? "egress" : "access");

        return result;
    }

    public Map<Stop, Transfer> streetSearch(boolean fromTarget) {
        Set<Vertex> vertices = fromTarget ? request.rctx.toVertices : request.rctx.fromVertices;
        return streetSearch(fromTarget,vertices,true);
    }

    private Map<Stop,Transfer> streetSearch(boolean fromTarget,Set<Vertex> origin,boolean checkNearBy) {
        Collection<State> states = nearbyStopFinder
            .findNearbyStatesViaStreet(origin, fromTarget);
        Set<Vertex> vertexes = new HashSet<>();
        boolean transitStopFound = false;
        for(State s:states){
            if(!(s.getVertex() instanceof TemporaryStreetLocation)){
                vertexes.add(s.getVertex());
            }
            if(s.getVertex() instanceof TransitStopVertex){
                transitStopFound = true;
            }
        }
        // if there's no direct vertex to any stop, we need to update the request & try again.
        if (!transitStopFound && checkNearBy) {
            return streetSearch(fromTarget,vertexes,false);
        }

        List<StopAtDistance> stopsFound = nearbyStopFinder.getStopAtDistances(origin, states);
        if(!checkNearBy){
            Map<Vertex,List<StopAtDistance>> stopGroups = stopsFound.stream().collect(groupingBy(stop -> stop.edges.get(0).getFromVertex()));
            int count = 0;
            Vertex target = null;
            for(Vertex v: stopGroups.keySet()){
                List<StopAtDistance> value = stopGroups.get(v);
                if(value.size()>count){
                    count = value.size();
                    stopsFound = value;
                    target = v;
                }
            }
            if(target!=null) {
                TemporaryFreeEdge freeEdge = new TemporaryFreeEdge(
                    new TemporaryStreetLocation(target.getLabel(), target.getCoordinate(),
                        target.getOriginalName(), false), target);
                try {
                    this.setRequest(fromTarget, target.getCoordinate());
                } catch (Exception e) {
                    //set request failed
                    return Collections.emptyMap();
                }
                stopsFound = stopsFound.stream().map(stop -> {
                    List<Edge> newEdges = new ArrayList();
                    newEdges.add(freeEdge);
                    newEdges.addAll(stop.edges);
                    return new StopAtDistance(stop.tstop, stop.distance, newEdges, stop.geometry);
                }).collect(Collectors.toList());
            }
        }

        Map<Stop, Transfer> result = new HashMap<>();
        for (NearbyStopFinder.StopAtDistance stopAtDistance : stopsFound) {
            result.put(
                stopAtDistance.tstop.getStop(),
                new Transfer(-1,
                    (int) stopAtDistance.edges.stream().map(Edge::getEffectiveWalkDistance)
                        .collect(Collectors.summarizingDouble(Double::doubleValue)).getSum(),
                    stopAtDistance.edges));
        }
        LOG.debug("Found {} {} stops", result.size(), fromTarget ? "egress" : "access");
        return result;
    }

    public RoutingRequest getRequest() {
        return request;
    }

    private void setRequest(boolean fromTarget, Coordinate target) {
        this.request = request.clone();
        GenericLocation location = new GenericLocation(target.y, target.x);
        if (fromTarget) {
            this.request.to = location;
        } else {
            double distance = this.request.from.getCoordinate().distance(target);
            int walkTime = (int) (distance / request.walkSpeed);
            this.request.from = location;
            this.request.dateTime = this.request.dateTime + walkTime;
        }
        Graph reference = request.rctx.graph;
        request.rctx = null;
        request.setRoutingContext(reference);
    }
}
