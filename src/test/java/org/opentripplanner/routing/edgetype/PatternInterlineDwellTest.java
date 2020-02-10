package org.opentripplanner.routing.edgetype;

import org.junit.Ignore;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Leg;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class PatternInterlineDwellTest extends GtfsTest {

    @Override
    public boolean isLongDistance() { return true; } // retrying wrecks the tests

    @Override
    public String getFeedName() {
        return "gtfs/interlining";
    }

    // TODO Allow using Calendar or ISOdate for testing, interpret it in the given graph's timezone.

    public void testInterlining() {
        Calendar calendar = new GregorianCalendar(2014, Calendar.JANUARY, 01, 00, 05, 00);
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        long time = calendar.getTime().getTime() / 1000;
        // We should arrive at the destination using two legs, both of which are on
        // the same route and with zero transfers.
        Leg[] legs = plan(time, "stop0", "stop3", null, false, false, null, null, null, 2);
        assertEquals(legs[0].routeId.getId(), "route1");
        assertEquals(legs[1].routeId.getId(), "route1");
        assertTrue(itinerary.nTransfers == 0);
    }

    // TODO test for trips on the same block with no transfer allowed (Trimet special case)

}
