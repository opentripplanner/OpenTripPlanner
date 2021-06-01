package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;

@ExtendWith(SnapshotExtension.class)
public class BikeRentalSnapshotTest
        extends SnapshotTestBase {

    static GenericLocation p1 = new GenericLocation("SW Johnson St. & NW 24th Ave. (P1)", null,
            45.52832, -122.70059);

    static GenericLocation p2 = new GenericLocation("NW Hoyt St. & NW 20th Ave. (P2)", null,
            45.52704, -122.69240);

    static GenericLocation p3 = new GenericLocation("NW Everett St. & NW 5th Ave. (P3)", null,
            45.52523, -122.67525);

    @BeforeAll
    public static void beforeClass() {
        loadGraphBeforeClass();
    }

    @DisplayName("Direct BIKE_RENTAL")
    @Test
    public void directBikeRental() {
        RoutingRequest request = createTestRequest(2009, 9, 21, 16, 10, 0);

        request.modes = new RequestModes(null, null, StreetMode.BIKE_RENTAL, Set.of());
        request.from = p1;
        request.to = p2;

        expectArriveByToMatchDepartAtAndSnapshot(request, (departAt, arriveBy) -> {
            /* The cost for switching between walking/biking is added the edge where switching occurs,
             * because of this the times for departAt / arriveBy itineraries differ.
             */
            arriveBy.legs.get(1).endTime = departAt.legs.get(1).endTime;
            arriveBy.legs.get(2).startTime = departAt.legs.get(2).startTime;
        });
    }

    @DisplayName("Direct BIKE_RENTAL while keeping the bicycle at the destination")
    @Test public void directBikeRentalArrivingAtDestination() {
        RoutingRequest request = createTestRequest(2009, 9, 21, 16, 10, 0);

        request.modes = new RequestModes(null, null, StreetMode.BIKE_RENTAL, Set.of());
        request.allowKeepingRentedBicycleAtDestination = true;
        request.from = p1;
        request.to = p2;

        expectArriveByToMatchDepartAtAndSnapshot(request, (departAt, arriveBy) -> {
            /* The cost for switching between walking/biking is added the edge where switching occurs,
             * because of this the times for departAt / arriveBy itineraries differ.
             */
            arriveBy.legs.get(1).endTime = departAt.legs.get(1).endTime;
            arriveBy.legs.get(2).startTime = departAt.legs.get(2).startTime;
        });
    }

    @DisplayName("Access BIKE_RENTAL")
    @Test public void accessBikeRental() {
        RoutingRequest request = createTestRequest(2009, 9, 21, 16, 14, 0);

        request.modes = new RequestModes(StreetMode.BIKE_RENTAL, StreetMode.WALK, null, Set.of(TransitMode.values()));
        request.from = p1;
        request.to = p3;

        expectArriveByToMatchDepartAtAndSnapshot(request);
    }

    @DisplayName("Egress BIKE_RENTAL")
    @Test public void egressBikeRental() {
        RoutingRequest request = createTestRequest(2009, 9, 21, 16, 10, 0);

        request.modes = new RequestModes(StreetMode.WALK, StreetMode.BIKE_RENTAL, null, Set.of(TransitMode.values()));
        request.from = p3;
        request.to = p1;

        expectArriveByToMatchDepartAtAndSnapshot(request);
    }
}
