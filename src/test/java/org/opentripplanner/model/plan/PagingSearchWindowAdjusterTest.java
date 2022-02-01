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
    private static final boolean CROP_TAIL = false;
    private static final boolean CROP_HEAD = true;

    private static final Duration D0s = Duration.ZERO;
    private static final Duration D10m = duration("10m");
    private static final Duration D30m = duration("30m");
    private static final Duration D40m = duration("40m");
    private static final Duration D50m = duration("50m");
    private static final Duration D1h = duration("1h");
    private static final Duration D1h10m = duration("1h10m");
    private static final Duration D1h20m = duration("1h20m");
    private static final Duration D1h30m = duration("1h30m");
    private static final Duration D2h = duration("2h");
    private static final Duration D4h = duration("4h");
    private static final Duration D4h30m = duration("4h30m");
    private static final Duration D5h = duration("5h");
    private static final Duration D6h = duration("6h");
    private static final Duration D1d = duration("1d");
    private static final List<Duration> LIST_OF_DURATIONS = List.of(D5h, D1h20m, D30m, D10m);

    private final Instant time = Instant.parse("2022-01-15T12:00:00Z");


    private final PagingSearchWindowAdjuster subject = new PagingSearchWindowAdjuster(
            (int)D10m.toMinutes(), (int)D1d.toMinutes(), LIST_OF_DURATIONS
    );


    @Test
    void adjustSearchWindow() {
        // ACTUAL AND USED SEARCH-WINDOW IS THE SAME, EXPECT ADJUSTED TO BE THE SAME

       assertEquals(D30m, subject.adjustSearchWindow(D30m, 3, 3, null, null, CROP_HEAD));
       assertEquals(D1h, subject.adjustSearchWindow(D1h, 3, 3, null, null, CROP_TAIL));


        // ACTUAL < USED SEARCH-WINDOW, EXPECT AN INCREASED SEARCH-WINDOW

        assertEquals(D1h, subject.adjustSearchWindow(D30m, 2, 3, null, null, CROP_HEAD));
        assertEquals(D6h, subject.adjustSearchWindow(D1h, 0, 4, null, null, CROP_TAIL));


        // ACTUAL > USED SEARCH-WINDOW, CROPPED SEARCH-WINDOW, WE EXPECT REDUCED SEARCHWINDOWS

        // No itinerary filter (removed itinerary)
        assertEquals(D40m, subject.adjustSearchWindow(D1h, 2, 1, null, null, CROP_HEAD));
        assertEquals(D50m, subject.adjustSearchWindow(D2h, 20,  5, null, null, CROP_TAIL));

        // With itinerary filter (removed itinerary)
        assertEquals(D1h20m, subject.adjustSearchWindow(D2h, 99, 1, time, time.plus(D1h10m), CROP_TAIL));
        assertEquals(D1h, subject.adjustSearchWindow(D2h, 99,  1, time, time.plus(D1h10m), CROP_HEAD));

        // Search-window reduced by 20% from 5 to 4 hours give us 30 min reduction in time
        assertEquals(D4h30m, subject.adjustSearchWindow(D5h, 99, 1, time, time.plus(D4h), CROP_TAIL));
        assertEquals(D4h30m, subject.adjustSearchWindow(D5h, 99,  1, time, time.plus(D1h), CROP_HEAD));
    }

    @Test
    void increaseOrKeepSearchWindow() {
        var expectedList = new ArrayList<>(LIST_OF_DURATIONS);
        expectedList.add(D0s);

        for (int n=0; n< expectedList.size(); ++n) {
            var expected = expectedList.get(n);
            assertEquals(expected, subject.increaseOrKeepSearchWindow(D0s, n), "n="+n);
            assertEquals(expected.plus(D30m), subject.increaseOrKeepSearchWindow(D30m, n), "n="+n);
        }
    }

    @Test
    void reduceSearchWindow() {
        assertEquals(D30m, subject.reduceSearchWindow(D30m, D30m));
        assertEquals(D40m, subject.reduceSearchWindow(D30m, D1h20m));
        assertEquals(D40m, subject.reduceSearchWindow(D30m, D1h30m));
        assertEquals(D50m, subject.reduceSearchWindow(D30m, D2h));
        assertEquals(D1h30m, subject.reduceSearchWindow(D1h, D5h));
        assertEquals(D4h, subject.reduceSearchWindow(D1h, D1d));
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