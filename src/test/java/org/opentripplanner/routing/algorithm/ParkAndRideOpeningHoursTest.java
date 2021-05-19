package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.OsmOpeningHours;
import org.opentripplanner.routing.core.TimeRestriction;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class ParkAndRideOpeningHoursTest extends GraphRoutingTest {

    // A thursday
    private static final ZonedDateTime START_OF_TIME = ZonedDateTime.of(2021, 5, 20, 12, 0, 0, 0, ZoneId.of("GMT"));
    private static final int CAR_PARK_TIME = 180;

    private Graph graph;
    private StreetVertex A;
    private TransitStopVertex S;

    public void createGraph(TimeRestriction openingHours) {
        graph = graphOf(new Builder() {
            @Override
            public void build() {
                A = intersection("A", 47.500, 19.000);

                S = stop("S1", 47.500, 18.999);

                biLink(A, S);

                vehicleParking("CarPark #1", 47.500, 19.001, false, true,
                        openingHours,
                        List.of(vehicleParkingEntrance(A, "CarPark #1 Entrance A", true, true))
                );
            }
        });

        graph.index();

        graph.setTransitLayer(TransitLayerMapper.map(TransitTuningParameters.FOR_TEST, graph));
    }

    @Test
    public void testVehicleParkingAlwaysOpen() throws Exception {
        createGraph(OsmOpeningHours.parseFromOsm("24/7"));
        assertParkAndRideAccess(0, 0);
        assertParkAndRideTraversal(CAR_PARK_TIME, -CAR_PARK_TIME);
    }

    @Test
    public void testVehicleParkingPartiallyOpenForward() throws Exception {
        createGraph(OsmOpeningHours.parseFromOsm("Mo-Su 09:00-12:00;Mo-Su 13:00-15:00"));
        assertParkAndRideAccess(60 * 60, 0);
        assertParkAndRideTraversal(null, -CAR_PARK_TIME);
    }

    @Test
    public void testVehicleParkingPartiallyOpenReverse() throws Exception {
        createGraph(OsmOpeningHours.parseFromOsm("Mo-Su 09:00-11:00,12:00-15:00"));
        assertParkAndRideAccess(0, -60 * 60);
        assertParkAndRideTraversal(CAR_PARK_TIME, -(60 * 60 + CAR_PARK_TIME));
    }

    @Test
    public void testVehicleParkingClosed() throws Exception {
        createGraph(OsmOpeningHours.parseFromOsm("Mo-Su 09:00-11:00,14:00-16:00"));
        assertParkAndRideAccess(2 * 60 * 60, -1 * 60 * 60);
        assertParkAndRideTraversal(null, -(60 * 60 + CAR_PARK_TIME));
    }

    private void assertParkAndRideAccess(int earliestDepartureTime, int latestArrivalTime) {
        var rr = new RoutingRequest().getStreetSearchRequest(StreetMode.CAR_TO_PARK);
        rr.carParkTime = 60;
        rr.setRoutingContext(graph, A, null);

        var stops = AccessEgressRouter.streetSearch(rr, StreetMode.CAR_TO_PARK, false);
        assertEquals(1, stops.size(), "nearby access stops");

        var accessEgress =
                new AccessEgressMapper(graph.getTransitLayer().getStopIndex())
                        .mapNearbyStop(stops.iterator().next(), START_OF_TIME, false);

        assertEquals(earliestDepartureTime, accessEgress.earliestDepartureTime(0), "access earliestDepartureTime");
        assertEquals(latestArrivalTime, accessEgress.latestArrivalTime(0), "access latestArrivalTime");
    }

    private void assertParkAndRideTraversal(Integer departAtDuration, Integer arriveByDuration) {
        assertEquals(departAtDuration, parkAndRideDuration(false), "departAt duration");
        assertEquals(arriveByDuration, parkAndRideDuration(true), "arriveBy duration");
    }

    private Integer parkAndRideDuration(boolean arriveBy) {
        var options = new RoutingRequest().getStreetSearchRequest(StreetMode.CAR_TO_PARK);
        options.setDateTime(START_OF_TIME.toInstant());
        options.carParkTime = CAR_PARK_TIME;
        options.arriveBy = arriveBy;
        options.setRoutingContext(graph, A, S);

        var tree = new AStar().getShortestPathTree(options);
        var path = tree.getPath(
                arriveBy ? A : S,
                false
        );

        if (path == null) {
            return null;
        }

        return (int) Duration.between(
                START_OF_TIME,
                Instant.ofEpochSecond(options.arriveBy ? path.getStartTime() : path.getEndTime()).atZone(START_OF_TIME.getZone())
        ).getSeconds();
    }
}
