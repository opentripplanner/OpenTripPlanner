package org.opentripplanner.routing.edgetype;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class RentABikeEdgeTest {

    private enum Direction {ARRIVE_BY, DEPART_AT};

    private Graph graph;
    private RoutingRequest proto;
    private BikeRentalStationVertex stationVertex;
    private Set<String> networkSet = new HashSet<String>();

    @Before
    public void before() {
        graph = new Graph();

        proto = new RoutingRequest();
        proto.useBikeRentalAvailabilityInformation = true;
        proto.setModes(TraverseModeSet.allModes());
        proto.setRoutingContext(graph, stationVertex,null);

        BikeRentalStation station = new BikeRentalStation();
        station.id = "bikeRentalTestId";
        station.name = new NonLocalizedString("BikeRentalName Test");
        station.x = 0.0;
        station.y = 0.0;
        // default test station has bikes and spaces available
        station.spacesAvailable = 1;
        station.bikesAvailable = 1;
        station.realTimeData = true;

        networkSet.addAll(Arrays.asList(new String[]{"testNetwork"}));
        stationVertex = new BikeRentalStationVertex(graph, station);

        new RentABikeOffEdge(stationVertex, stationVertex, networkSet);
    }

    @Test
    public void rentABikeOnEdge_traverse_departAt_walking_bikesAvailable_shouldSucceed() {
        RentABikeOnEdge rentABikeOnEdge = new RentABikeOnEdge(stationVertex, stationVertex, networkSet);
        State s0 = createState(Direction.DEPART_AT, TraverseMode.WALK);

        State s1 = rentABikeOnEdge.traverse(s0);

        assertNotNull(s1);
    }

    @Test
    public void rentABikeOnEdge_traverse_departAt_walking_bikesUnavailable_shouldFail() {
        RentABikeOnEdge rentABikeOnEdge = new RentABikeOnEdge(stationVertex, stationVertex, networkSet);
        State s0 = createState(Direction.DEPART_AT, TraverseMode.WALK);
        stationVertex.setBikesAvailable(0);

        State s1 = rentABikeOnEdge.traverse(s0);

        assertNull("Rented though no bikes available", s1);
    }

    @Test
    public void rentABikeOnEdge_traverse_arriveBy_biking_bikesUnavailable_shouldFail() {
        RentABikeOnEdge rentABikeOnEdge = new RentABikeOnEdge(stationVertex, stationVertex, networkSet);
        State s0 = createState(Direction.DEPART_AT, TraverseMode.BICYCLE);
        stationVertex.setBikesAvailable(0);

        State s1 = rentABikeOnEdge.traverse(s0);

        assertNull("Rented though no bikes available", s1);
    }

    @Test
    public void rentABikeOffEdge_traverse_departAt_walking_spacesUnavailable_shouldFail() {
        RentABikeOffEdge rentABikeOffEdge = new RentABikeOffEdge(stationVertex, stationVertex, networkSet);
        State s0 = createState(Direction.DEPART_AT, TraverseMode.BICYCLE);
        stationVertex.setSpacesAvailable(0);

        State s1 = rentABikeOffEdge.traverse(s0);

        assertNull("Bike returned though no spaces available", s1);
    }

    @Test
    public void rentABikeOffEdge_traverse_arriveBy_walking_spacesUnavailable_shouldFail() {
        RentABikeOffEdge rentABikeOffEdge = new RentABikeOffEdge(stationVertex, stationVertex, networkSet);
        State s0 = createState(Direction.ARRIVE_BY, TraverseMode.WALK);
        stationVertex.setSpacesAvailable(0);

        State s1 = rentABikeOffEdge.traverse(s0);

        assertNull("Bike returned though no spaces available", s1);
    }

    private State createState(Direction direction, TraverseMode mode) {
        RoutingRequest options = proto.clone();
        if (direction == Direction.DEPART_AT) {
            options.arriveBy = false;
        } else {
            options.arriveBy = true;
        }
        options.setModes(TraverseModeSet.allModes());
        options.setRoutingContext(graph, stationVertex,null);

        StateEditor stateEditor = new StateEditor(options, stationVertex);
        if (mode != TraverseMode.WALK) {
            stateEditor.beginVehicleRenting(mode);
        }
        return stateEditor.makeState();
    }

}
