package org.opentripplanner.model.plan.pagecursor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.pagecursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.pagecursor.PageType.PREVIOUS_PAGE;

public class PageCursorTest {

    private static final ZoneId ZONE_ID = ZoneId.of("GMT");
    private static final String EDT_STR = "2021-01-31T12:20:00Z";
    private static final String LAT_STR = "2021-01-31T15:00:00Z";
    private static final Instant EDT = Instant.parse(EDT_STR);
    private static final Instant LAT = Instant.parse(LAT_STR);
    private static final Duration SEARCH_WINDOW = Duration.parse("PT2h");

    private TimeZone originalTimeZone;
    private PageCursor subjectDepartAfter;
    private PageCursor subjectArriveBy;

    @Before
    public void setup() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE_ID));

        subjectDepartAfter = new PageCursor(NEXT_PAGE, STREET_AND_ARRIVAL_TIME, EDT, null, SEARCH_WINDOW);
        subjectArriveBy = new PageCursor(PREVIOUS_PAGE, STREET_AND_DEPARTURE_TIME, EDT, LAT, SEARCH_WINDOW);
    }

    @After
    public void teardown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void testToString() {
        assertEquals(
                "PageCursor{type: NEXT_PAGE, sortOrder: STREET_AND_ARRIVAL_TIME, "
                        + "edt: " + EDT_STR + ", searchWindow: 2h}",
                subjectDepartAfter.toString()
        );
        assertEquals(
                "PageCursor{type: PREVIOUS_PAGE, sortOrder: STREET_AND_DEPARTURE_TIME, "
                        + "edt: " + EDT_STR + ", lat: " + LAT_STR + ", searchWindow: 2h}",
                subjectArriveBy.toString()
        );
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void encodeAndDecode() {
        // depart after
        String buf = subjectDepartAfter.encode();
        var before = PageCursor.decode(buf);
        assertEquals(subjectDepartAfter.toString(), before.toString());

        // Arrive by
        buf = subjectArriveBy.encode();
        before = PageCursor.decode(buf);
        assertEquals(subjectArriveBy.toString(), before.toString());
    }
}