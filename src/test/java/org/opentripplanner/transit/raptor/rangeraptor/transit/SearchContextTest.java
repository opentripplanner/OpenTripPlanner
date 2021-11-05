package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext.accessOrEgressPaths;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

class SearchContextTest implements RaptorTestConstants {
    private final boolean GET_ACCESS = true;
    private final boolean GET_EGRESS = false;
    private final RaptorTransfer PATH_A_10s = TestTransfer.walk(STOP_A, D10s);
    private final RaptorTransfer PATH_A_11s = TestTransfer.walk(STOP_A, D11s);
    private final RaptorTransfer PATH_B = TestTransfer.walk(STOP_B, D1m);
    private final RaptorTransfer PATH_C_30s = TestTransfer.walk(STOP_C, D30s);
    private final RaptorTransfer PATH_C_40s = TestTransfer.walk(STOP_C, D40s);
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();


    @BeforeEach
    void setup() {
        requestBuilder.searchParams().earliestDepartureTime(0);
    }

    @Test
    void accessOrEgressPathsHandlesOneElementPaths() {
        // given:
        var emptyPaths = requestBuilder
                .searchParams().addAccessPaths(PATH_A_10s).addEgressPaths(PATH_C_30s)
                .build().searchParams();

        assertEquals(List.of(PATH_A_10s), accessOrEgressPaths(GET_ACCESS, STANDARD, emptyPaths));
        assertEquals(List.of(PATH_A_10s), accessOrEgressPaths(GET_ACCESS, MULTI_CRITERIA, emptyPaths));
        assertEquals(List.of(PATH_C_30s), accessOrEgressPaths(GET_EGRESS, STANDARD, emptyPaths));
        assertEquals(List.of(PATH_C_30s), accessOrEgressPaths(GET_EGRESS, MULTI_CRITERIA, emptyPaths));
    }

    @Test
    void accessOrEgressPathsHandlesDuplicatesPaths() {
        var emptyPaths = requestBuilder
                .searchParams().addAccessPaths(PATH_A_10s, PATH_A_11s, PATH_B)
                .addEgressPaths(PATH_B, PATH_C_40s, PATH_C_30s)
                .build().searchParams();

        assertEquals(
                List.of(PATH_A_10s, PATH_B),
                sort(accessOrEgressPaths(GET_ACCESS, STANDARD, emptyPaths))
        );
        assertEquals(
                List.of(PATH_A_10s, PATH_A_11s, PATH_B),
                sort(accessOrEgressPaths(GET_ACCESS, MULTI_CRITERIA, emptyPaths))
        );
        assertEquals(
                List.of(PATH_B, PATH_C_30s),
                sort(accessOrEgressPaths(GET_EGRESS, STANDARD, emptyPaths))
        );
        assertEquals(
                List.of(PATH_B, PATH_C_30s, PATH_C_40s),
                sort(accessOrEgressPaths(GET_EGRESS, MULTI_CRITERIA, emptyPaths))
        );
    }

    private static List<RaptorTransfer> sort(Collection<RaptorTransfer> c) {
        return c.stream()
                .sorted(Comparator.comparingInt(it -> it.stop() * 10_000 + it.durationInSeconds()))
                .collect(Collectors.toList());
    }
}