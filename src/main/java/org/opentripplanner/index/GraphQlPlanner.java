package org.opentripplanner.index;

import java.util.Collections;
import java.util.List;

import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;

import graphql.schema.DataFetchingEnvironment;

public class GraphQlPlanner {
    private GraphIndex index;

    public GraphQlPlanner(GraphIndex index) {
        this.index = index;
    }

    public TripPlan plan(DataFetchingEnvironment environment) {
        RoutingRequest request = createRequest(environment);
        Router router = index.graph.router;
        GraphPathFinder gpFinder = new GraphPathFinder(router);
        List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(request);
        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);        
        return plan;
    }

    private RoutingRequest createRequest(DataFetchingEnvironment environment) {
        RoutingRequest request = new RoutingRequest();
        request.from.lat = ((Number)environment.getArgument("fromLat")).doubleValue();
        request.from.lng = ((Number)environment.getArgument("fromLon")).doubleValue();
        request.to.lat = ((Number)environment.getArgument("toLat")).doubleValue();
        request.to.lng = ((Number)environment.getArgument("toLon")).doubleValue();
        request.setPreferredAgencies("HSL");
        QualifiedModeSet modes = new QualifiedModeSet("WALK,BUS");
        modes.applyToRoutingRequest(request);
        request.setModes(request.modes);
        request.setWheelchairAccessible(false);
        request.setMaxWalkDistance(2500.0);
        request.setWalkReluctance(2.0);
        request.walkSpeed = 1.2;
        request.setArriveBy(false);
        request.showIntermediateStops = true;
        request.setIntermediatePlacesFromStrings(Collections.emptyList());
        request.setWalkBoardCost(600);
        request.transferSlack = 180;
        request.disableRemainingWeightHeuristic = false;
        if (environment.containsArgument("numItineraries")) {
            request.setNumItineraries(((Number)environment.getArgument("numItineraries")).intValue());
        }
        return request;
    }
}
