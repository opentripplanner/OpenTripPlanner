package org.opentripplanner.mmri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

@Disabled("Requires departing onboard trip")
public class OnTripTest extends GtfsTest {
    @Override
    public final String getFeedName() {
        return "mmri/2f";
    }

    @Test
    public void test2f1() {
        Itinerary itinerary = plan(
                +1388530920L, null, "2f2", "2f|intercity", false,
                false,
                null, "", "", 2
        );

        Leg[] legs = itinerary.legs.toArray(new Leg[2]);

        validateLeg(legs[0], 1388530920000L, 1388531040000L, "2f3", null, null);
        validateLeg(legs[1], 1388531160000L, 1388531340000L, "2f2", "2f3", null);

        assertEquals("", itinerary.toStr());
    }
}
