package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

@Disabled("Requires stop banning")
public class ExcludedStopsTest extends GtfsTest {
    @Override
    public final String getFeedName() {
        return "mmri/3f";
    }

    @Test
    public void test3f1() {
        Itinerary itinerary = plan(
                +1388530860L, "3f1", "3f3", null, false,
                false,
                null, "", "3f2", 1
        );

        Leg leg = itinerary.legs.toArray(new Leg[1])[0];

        validateLeg(leg, 1388530860000L, 1388531040000L, "3f3", "3f1", null);

        assertEquals("", itinerary.toStr());
    }
}
