package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.error.RoutingValidationException;

@ExtendWith(SnapshotExtension.class)
@ResourceLock(Resources.LOCALE)
public class BikeRentalSnapshotTest
        extends SnapshotTestBase {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    static GenericLocation p1 = new GenericLocation("SW Johnson St. & NW 24th Ave. (P1)", null,
            45.52832, -122.70059);

    static GenericLocation p2 = new GenericLocation("NW Hoyt St. & NW 20th Ave. (P2)", null,
            45.52704, -122.69240);

    static GenericLocation p3 = new GenericLocation("NW Everett St. & NW 5th Ave. (P3)", null,
            45.52523, -122.67525);

    @BeforeAll
    public static void beforeClass() {
        Locale.setDefault(Locale.US);
        loadGraphBeforeClass();
    }

    @AfterAll
    public static void afterClass() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @DisplayName("Direct BIKE_RENTAL")
    @Test
    public void directBikeRental() {
        RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

        request.modes = new RequestModes(null, null, null, StreetMode.BIKE_RENTAL, Set.of());
        request.from = p1;
        request.to = p2;

        expectArriveByToMatchDepartAtAndSnapshot(request);
    }

    /**
     * The next to two tests are an example where departAt and arriveBy searches return different
     * (but still correct) results.
     *
     * It's probably down to the intersection traversal because when you use a constant cost
     * they become the same route again.
     *
     * More discussion: https://github.com/opentripplanner/OpenTripPlanner/pull/3574
     */
    @DisplayName("Direct BIKE_RENTAL while keeping the bicycle at the destination with departAt")
    @Test public void directBikeRentalArrivingAtDestinationWithDepartAt() {
        RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

        request.modes = new RequestModes(null, null, null, StreetMode.BIKE_RENTAL, Set.of());
        request.allowKeepingRentedVehicleAtDestination = true;
        request.from = p1;
        request.to = p2;

        expectRequestResponseToMatchSnapshot(request);
    }

    @DisplayName("Direct BIKE_RENTAL while keeping the bicycle at the destination with arriveBy")
    @Test public void directBikeRentalArrivingAtDestinationWithArriveBy() {
        RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

        request.modes = new RequestModes(null, null, null, StreetMode.BIKE_RENTAL, Set.of());
        request.allowKeepingRentedVehicleAtDestination = true;
        request.from = p1;
        request.to = p2;
        request.arriveBy = true;

        expectRequestResponseToMatchSnapshot(request);
    }

    @DisplayName("Access BIKE_RENTAL")
    @Test public void accessBikeRental() {
        RoutingRequest request = createTestRequest(2009, 10, 21, 16, 14, 0);

        request.modes = new RequestModes(StreetMode.BIKE_RENTAL, StreetMode.WALK,  StreetMode.WALK, null, AllowedTransitMode.getAllTransitModes());
        request.from = p1;
        request.to = p3;

        try {
            expectArriveByToMatchDepartAtAndSnapshot(request);
        } catch (CompletionException e) {
            RoutingValidationException.unwrapAndRethrowCompletionException(e);
        }
    }

    @DisplayName("Egress BIKE_RENTAL")
    @Test public void egressBikeRental() {
        RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

        request.modes = new RequestModes(StreetMode.WALK, StreetMode.WALK, StreetMode.BIKE_RENTAL, null, AllowedTransitMode.getAllTransitModes());
        request.from = p3;
        request.to = p1;

        expectArriveByToMatchDepartAtAndSnapshot(request);
    }
}
