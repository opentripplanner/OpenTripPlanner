package org.opentripplanner.routing.algorithm.raptor.street_router;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StationStopEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AccessEgressRouter {
    private static Logger LOG = LoggerFactory.getLogger(InterleavedBidirectionalHeuristic.class);


    // TODO Switch with generic A star
    public static Map<Stop, Transfer> streetSearch (RoutingRequest rr, boolean fromTarget, long abortTime) {
        rr.maxWalkDistance = 2000;
        rr.softWalkLimiting = false;
        rr.softPreTransitLimiting = false;
        Map<TransitStop, Transfer> transitStopsFound = new HashMap<>();
        BinHeap<Vertex> transitQueue = new BinHeap<>();
        double maxWeightSeen = 0;
        LOG.debug("Heuristic street search around the {}.", fromTarget ? "target" : "origin");
        rr = rr.clone();
        if (fromTarget) {
            rr.setArriveBy(!rr.arriveBy);
        }

        ShortestPathTree spt = new DominanceFunction.MinimumWeight().getNewShortestPathTree(rr);
        // TODO use normal OTP search for this.
        BinHeap<State> pq = new BinHeap<State>();
        Vertex initVertex = fromTarget ? rr.rctx.target : rr.rctx.origin;
        State initState = new State(initVertex, rr);
        pq.insert(initState, 0);
        while ( ! pq.empty()) {
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
                //return null;
            }
            State s = pq.extract_min();
            Vertex v = s.getVertex();

            boolean initialStop = v instanceof TransitStop && s.backEdge instanceof StationStopEdge;

            // At this point the vertex is closed (pulled off heap).
            // This is the lowest cost we will ever see for this vertex. We can record the cost to reach it.
            if (v instanceof TransitStop) {
                transitStopsFound.put((TransitStop)v, createTransfer(s, fromTarget));
                // We don't want to continue into the transit network yet, but when searching around the target
                // place vertices on the transit queue so we can explore the transit network backward later.
                if (fromTarget) {
                    double weight = s.getWeight();
                    transitQueue.insert(v, weight);
                    if (weight > maxWeightSeen) {
                        maxWeightSeen = weight;
                    }
                }
                if (!initialStop) continue;
            }
            for (Edge e : rr.arriveBy ? v.getIncoming() : v.getOutgoing()) {
                if (v instanceof TransitStop && !(e instanceof StreetTransitLink)) {
                    continue;
                }
                // arriveBy has been set to match actual directional behavior in this subsearch.
                // Walk cutoff will happen in the street edge traversal method.
                State s1 = e.traverse(s);
                if (s1 == null) {
                    continue;
                }
                if (spt.add(s1)) {
                    pq.insert(s1,  s1.getWeight());
                }
            }
        }
        LOG.debug("Street search hit {} vertices.", transitStopsFound.size());
        LOG.debug("Street search hit {} transit stops.", transitQueue.size());

        Map<Stop, Transfer> result = new HashMap<>();
        for (Map.Entry<TransitStop, Transfer> entry : transitStopsFound.entrySet()) {
            result.put(entry.getKey().getStop(), entry.getValue());
        }

        return result;
    }

    private static Transfer createTransfer(State state, boolean fromTarget) {
        State originalState = state;
        List<Coordinate> points = new ArrayList<>();
        do {
            if (state.backEdge != null && state.backEdge.getGeometry() != null) {
                List<Coordinate> edgeCoordinates = Arrays.asList(state.backEdge.getGeometry().getCoordinates());
                if (!fromTarget) {
                    Collections.reverse(edgeCoordinates);
                }
                points.addAll(edgeCoordinates);
            }
            state = state.getBackState();
        } while (state != null);
        Collections.reverse(points);

        Transfer transfer = new Transfer(
                0,
                (int)originalState.getWalkDistance(),
                points);

        return transfer;
    }
}
