package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.SearchWindowUtils.calculateNewSearchWindow;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.common.model.P2;

class SearchWindowUtilsTest {

    private static final Duration D0s = Duration.ZERO;
    private static final Duration D30m = Duration.ofMinutes(30);
    private static final Duration D1h = Duration.ofHours(1);
    private static final Duration D1h20m = Duration.ofMinutes(80);
    private static final Duration D2h = Duration.ofHours(2);
    private final Instant time = Instant.parse("2022-01-15T12:00:00Z");

    @Test
    void calculateNewSearchWindowWithNItineraries() {
        var expectedList = List.of(
                duration(6,0),
                duration(4,0),
                duration(2,0),
                duration(1,0),
                duration(0,30),
                duration(0,20),
                duration(0,10),
                // n > 6 : then the search window passed in is expected without any change
                duration(0,0)
        );

        for (int n=0; n< expectedList.size(); ++n) {
            var expected = expectedList.get(n);
            assertEquals(expected, calculateNewSearchWindow(D0s, time, null, n), "n="+n);
            assertEquals(expected.plus(D30m), calculateNewSearchWindow(D30m, time, null, n), "n="+n);
        }
    }

    @Test
    void calculateNewSearchWindowWithCroppedSearchWindow() {
        assertEquals(duration(0, 40), calculateNewSearchWindow(D1h, time, time.plus(D30m), 4));
        assertEquals(duration(1, 30), calculateNewSearchWindow(D2h, time, time.plus(D1h20m), 4));
    }

    @Test
    void normalizeSearchWindow() {
        var cases = List.of(
                // Smallest searchWindow allowed is 10 min
                new P2<>(10, -100),
                new P2<>(10, 0),
                new P2<>(10, 10),
                // sw <= 4h, the round up to closest 10 min
                new P2<>(20, 11),
                new P2<>(230, 230),
                new P2<>(240, 231),
                new P2<>(240, 240),
                // sw > 4h, the round up to the closest 30 min
                new P2<>(270, 241),
                new P2<>(300, 300),
                new P2<>(330, 301),
                // Max is 24 hours
                new P2<>(24*60, 24*60),
                new P2<>(24*60, Integer.MAX_VALUE)
        );

        for (P2<Integer> tc : cases) {
            assertEquals(
                    Duration.ofMinutes(tc.first),
                    SearchWindowUtils.normalizeSearchWindow(tc.second)
            );
        }
    }

    @Test
    void ceiling() {
        assertEquals(-1, SearchWindowUtils.ceiling(-1,1));
        assertEquals(0, SearchWindowUtils.ceiling(0,1));
        assertEquals(1, SearchWindowUtils.ceiling(1,1));

        assertEquals(-2, SearchWindowUtils.ceiling(-2,2));
        assertEquals(0, SearchWindowUtils.ceiling(-1,2));
        assertEquals(0, SearchWindowUtils.ceiling(0,2));
        assertEquals(2, SearchWindowUtils.ceiling(2,2));
        assertEquals(4, SearchWindowUtils.ceiling(3,2));

        assertEquals(-3, SearchWindowUtils.ceiling(-3,3));
        assertEquals(0, SearchWindowUtils.ceiling(-2,3));
        assertEquals(0, SearchWindowUtils.ceiling(0,3));
        assertEquals(3, SearchWindowUtils.ceiling(3,3));
        assertEquals(6, SearchWindowUtils.ceiling(4,3));
    }


    Duration duration(int hours, int minutes) {
        return Duration.ofMinutes(60L * hours + minutes);
    }
}