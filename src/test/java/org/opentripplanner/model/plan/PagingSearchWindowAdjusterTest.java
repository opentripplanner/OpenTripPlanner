package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.util.time.DurationUtils.duration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.common.model.P2;


class PagingSearchWindowAdjusterTest {
    private static final boolean CROP_TAIL = true;
    private static final boolean CROP_HEAD = false;

    private static final Duration D0s = Duration.ZERO;
    private static final Duration D10m = duration("10m");
    private static final Duration D30m = duration("30m");
    private static final Duration D50m = duration("50m");
    private static final Duration D1h = duration("1h");
    private static final Duration D1h10m = duration("1h10m");
    private static final Duration D1h20m = duration("1h20m");
    private static final Duration D2h = duration("2h");
    private static final Duration D4h = duration("4h");
    private static final Duration D5h = duration("5h");
    private static final Duration D6h = duration("6h");
    private static final Duration D1d = duration("1d");
    private static final List<Duration> LIST_OF_DURATIONS = List.of(D5h, D1h20m, D30m, D10m);

    private final Instant time = Instant.parse("2022-01-15T12:00:00Z");


    private final PagingSearchWindowAdjuster subject = new PagingSearchWindowAdjuster(
            (int)D10m.toMinutes(), (int)D1d.toMinutes(), LIST_OF_DURATIONS
    );


    @Test
    void decreaseSearchWindow() {
        assertEquals(D1h10m, subject.decreaseSearchWindow(D2h, time, time.plus(D1h10m), CROP_TAIL));
        assertEquals(D50m, subject.decreaseSearchWindow(D2h, time, time.plus(D1h10m), CROP_HEAD));

        assertEquals(D4h, subject.decreaseSearchWindow(D5h, time, time.plus(D4h), CROP_TAIL));
        assertEquals(D1h, subject.decreaseSearchWindow(D5h, time, time.plus(D4h), CROP_HEAD));
    }

    @Test
    void keepSearchWindow() {
        assertEquals(D30m, subject.increaseOrKeepSearchWindow(D30m,5, 5));
        assertEquals(D30m, subject.increaseOrKeepSearchWindow(D30m,1, 1));
        assertEquals(D30m, subject.increaseOrKeepSearchWindow(D30m,1, 3));
    }

    @Test
    void increaseSearchWindow() {
        var expectedList = new ArrayList<>(LIST_OF_DURATIONS);
        expectedList.add(D0s);

        for (int n=0; n< expectedList.size(); ++n) {
            var expected = expectedList.get(n);
            assertEquals(expected, subject.increaseOrKeepSearchWindow(D0s,20, n), "n="+n);
            assertEquals(expected.plus(D30m), subject.increaseOrKeepSearchWindow(D30m, 20, n), "n="+n);
        }
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
                new P2<>(24*60, 99_999_999)
        );

        for (P2<Integer> tc : cases) {
            assertEquals(
                    Duration.ofMinutes(tc.first),
                    subject.normalizeSearchWindow(tc.second * 60),
                    "[exp nSeconds, input nSeconds]: " + tc
            );
        }
    }

    @Test
    void ceiling() {
        assertEquals(-1, PagingSearchWindowAdjuster.ceiling(-1,1));
        assertEquals(0, PagingSearchWindowAdjuster.ceiling(0,1));
        assertEquals(1, PagingSearchWindowAdjuster.ceiling(1,1));

        assertEquals(-2, PagingSearchWindowAdjuster.ceiling(-2,2));
        assertEquals(0, PagingSearchWindowAdjuster.ceiling(-1,2));
        assertEquals(0, PagingSearchWindowAdjuster.ceiling(0,2));
        assertEquals(2, PagingSearchWindowAdjuster.ceiling(2,2));
        assertEquals(4, PagingSearchWindowAdjuster.ceiling(3,2));

        assertEquals(-3, PagingSearchWindowAdjuster.ceiling(-3,3));
        assertEquals(0, PagingSearchWindowAdjuster.ceiling(-2,3));
        assertEquals(0, PagingSearchWindowAdjuster.ceiling(0,3));
        assertEquals(3, PagingSearchWindowAdjuster.ceiling(3,3));
        assertEquals(6, PagingSearchWindowAdjuster.ceiling(4,3));
    }
}