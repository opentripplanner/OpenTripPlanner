package org.opentripplanner.routing.algorithm.astar.strategies;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class BikeToStopSkipEdgeStrategy
        implements SkipEdgeStrategy {

    private static final int LIMIT = 100;
    private static final double MAX_FACTOR = 1.2;

    private final Function<Stop, Collection<Trip>> getTripsForStop;

    int numberOfBikeableTripsReached = 0;
    double distanceLimit = Double.MAX_VALUE;

    public BikeToStopSkipEdgeStrategy(Function<Stop, Collection<Trip>> getTripsForStop) {
        this.getTripsForStop = getTripsForStop;
    }

    @Override
    public boolean shouldSkipEdge(
            Set<Vertex> origins,
            Set<Vertex> targets,
            State current,
            Edge edge,
            ShortestPathTree spt,
            RoutingRequest traverseOptions
    ) {
        if (current.getVertex() instanceof TransitStopVertex && distanceLimit == Double.MAX_VALUE) {
            var stopVertex = (TransitStopVertex) current.getVertex();
            var stop = stopVertex.getStop();
            numberOfBikeableTripsReached += getTripsForStop.apply(stop).stream().filter(
                    BikeToStopSkipEdgeStrategy::bikeAccessForTrip).count();
            if (numberOfBikeableTripsReached >= LIMIT) {
                distanceLimit = current.getWalkDistance() * MAX_FACTOR;
            }
        }
        return current.getWalkDistance() > distanceLimit;
    }


    public static boolean bikeAccessForTrip(Trip trip) {
        if (trip.getBikesAllowed() != BikeAccess.UNKNOWN) {
            return trip.getBikesAllowed() == BikeAccess.ALLOWED;
        }

        return trip.getRoute().getBikesAllowed() == BikeAccess.ALLOWED;
    }
}
