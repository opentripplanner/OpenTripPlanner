package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SmokeTest.class)
public class AtlantaSmokeTest {

    @Test
    public void returnRouteFromCentralAtlantaToPowderSprings() {
        var params = Map.of(
                "fromPlace", "33.714059324224124,-84.37225341796875",
                "toPlace", "33.882387270695105,-84.51576232910155",
                "time", "1:00pm",
                "date", SmokeTest.nextMonday().toString(),
                "mode", "TRANSIT,WALK,FLEX_EGRESS",
                "showIntermediateStops", "true",
                "locale", "en"
        );
        var otpResponse = SmokeTest.sendPlanRequest(params);

        assertTrue(otpResponse.getPlan().itineraries.size() > 1);

        var itineraries = otpResponse.getPlan().itineraries;

        var itineraryModes = itineraries.stream()
                .map(i -> i.legs.stream().map(l -> l.mode).collect(Collectors.toList()))
                .collect(Collectors.toList());


        var expectedModes = List.of("WALK", "BUS", "WALK", "SUBWAY", "WALK", "BUS", "BUS", "WALK");

        // the assertion is a little fuzzy as more detailed ones would be hard to maintain over time
        assertTrue(
                itineraryModes.contains(expectedModes),
                String.format(
                        "Could not find expected mode %s in itineraries which had modes %s",
                        expectedModes,
                        itineraryModes
                )
        );


    }
}
