package org.opentripplanner.raptor.moduletests;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

/**
 * FEATURE UNDER TEST
 * <p>
 * When transit can only give worse results than an access directly followed by an egress, RAPTOR
 * should return an empty result.
 */
public class F06_TransitWorseThanPureAccessEgressTest implements RaptorTestConstants {

    private final TestTransitData data = new TestTransitData();
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
    private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

    @BeforeEach
    void setup() {
        data
                .access("Walk 1m ~ A")
                .withTimetables(
                        """
                        -- R1
                        A      B
                        00:05  00:10
                        -- R2
                        B      A
                        00:15  00:20
                        """
                )
                .egress("A ~ Walk 1m");

        requestBuilder
                .searchParams()
                .earliestDepartureTime(T00_00)
                .latestArrivalTime(T00_30)
                .timetable(true);
    }

    static List<RaptorModuleTestCase> testCases() {
        var path = "";
        return RaptorModuleTestCase.of()
                .addMinDuration("13m", -1, T00_00, T00_30)
                .add(standard(), PathUtils.withoutCost(path))
                .add(multiCriteria(), path)
                .build();
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void testRaptor(RaptorModuleTestCase testCase) {
        assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
    }
}