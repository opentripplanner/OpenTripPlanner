package org.opentripplanner.transit.raptor.service;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.opentripplanner.transit.raptor.service.HeuristicToRunResolver.resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters;

public class HeuristicToRunResolverTest {

    public static final boolean FWD = true;
    public static final boolean REV = true;
    public static final boolean DEST = true;
    public static final boolean EDT = true;
    public static final boolean LAT = true;
    public static final boolean WIN = true;
    public static final boolean _x_ = false;

    private boolean forward = false;
    private boolean reverse = false;

    // Request to test
    private RaptorRequest<TestRaptorTripSchedule> request;
    private String msg;


    @Test
    public void resolveHeuristicToRun() {
        // Alternatives with both EAT & LAT FALSE is skipped.
        // Either EAT or LAT is required and request is not possible to create.

        given(DEST, EDT, LAT, WIN).expect(_x_, REV);
        given(DEST, EDT, LAT, _x_).expect(_x_, REV);
        given(DEST, EDT, _x_, WIN).expect(FWD, REV);
        given(DEST, EDT, _x_, _x_).expect(FWD, REV);
        given(DEST, _x_, LAT, WIN).expect(_x_, REV);
        given(DEST, _x_, LAT, _x_).expect(_x_, REV);
        // Skip alternatives with both EAT & LAT off.
        given(_x_, EDT, LAT, WIN).expect(_x_, _x_);
        given(_x_, EDT, LAT, _x_).expect(FWD, _x_);
        given(_x_, EDT, _x_, WIN).expect(_x_, _x_);
        given(_x_, EDT, _x_, _x_).expect(FWD, _x_);
        given(_x_, _x_, LAT, WIN).expect(_x_, REV);
        given(_x_, _x_, LAT, _x_).expect(_x_, REV);
        // Skip alternatives with both EAT & LAT off.
    }

    @Test
    public void resolveHeuristicOffForNoneRangeRaptorProfile() {
        RaptorRequestBuilder<TestRaptorTripSchedule> b = new RaptorRequestBuilder<>();
        b.profile(RaptorProfile.NO_WAIT_BEST_TIME);
        // Add some dummy legs
        b.searchParams().accessLegs().add(dummyLeg());
        b.searchParams().egressLegs().add(dummyLeg());
        b.searchParams().earliestDepartureTime(10_000);

        resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters(
                b.build(),
                this::enableForward,
                this::enableReverse
        );
        assertFalse(forward);
        assertFalse(reverse);
    }

    private HeuristicToRunResolverTest given(boolean dest, boolean edt, boolean lat, boolean win) {
        RaptorRequestBuilder<TestRaptorTripSchedule> b = new RaptorRequestBuilder<>();
        b.profile(RaptorProfile.MULTI_CRITERIA);
        // Add some dummy legs
        b.searchParams().accessLegs().add(dummyLeg());
        b.searchParams().egressLegs().add(dummyLeg());
        msg = "Params:";

        if (dest) {
            msg += " DEST";
            b.enableOptimization(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
        }

        if (edt) {
            msg += " EDT";
            b.searchParams().earliestDepartureTime(10_000);
        }
        if (lat) {
            msg += " LAT";
            b.searchParams().latestArrivalTime(20_000);
        }
        if (win) {
            msg += " WIN";
            b.searchParams().searchWindowInSeconds(6_000);
        }
        request = b.build();
        return this;
    }

    private void expect(boolean forwardExpected, boolean reverseExpected) {
        forward = false;
        reverse = false;

        resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters(
                request,
                this::enableForward,
                this::enableReverse
        );
        assertEquals(msg + " - Forward", forwardExpected, forward);
        assertEquals(msg + " - Reverse", reverseExpected, reverse);
    }

    private void enableForward() {
        forward = true;
    }

    private void enableReverse() {
        reverse = true;
    }

    private RaptorTransfer dummyLeg() {
        return new RaptorTransfer() {
            @Override public int stop() { return 1; }
            @Override public int earliestDepartureTime(int requestedDepartureTime) { return requestedDepartureTime; }
            @Override public int latestArrivalTime(int requestedArrivalTime) { return requestedArrivalTime; }
            @Override public int durationInSeconds() { return 10; }
        };
    }
}