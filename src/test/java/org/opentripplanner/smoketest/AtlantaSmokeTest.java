package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opentripplanner.api.model.ApiItinerary;

@Category(SmokeTest.class)
public class AtlantaSmokeTest {

    String centralAtlanta = "33.714059324224124,-84.37225341796875";
    String powderSpringsFlexZone1 = "33.882387270695105,-84.51576232910155";

    @Test
    public void regularRouteFromCentralAtlantaToPowderSprings() {
        var params = Map.of(
                "fromPlace", centralAtlanta,
                "toPlace", powderSpringsFlexZone1,
                "time", "1:00pm",
                "date", SmokeTest.nextMonday().toString(),
                "mode", "TRANSIT,WALK",
                "showIntermediateStops", "true",
                "locale", "en"
        );
        var otpResponse = SmokeTest.sendPlanRequest(params);
        var itineraries = otpResponse.getPlan().itineraries;

        assertTrue(itineraries.size() > 1);

        var expectedModes = List.of("WALK", "BUS", "WALK", "SUBWAY", "WALK", "BUS", "BUS", "WALK");
        // the assertion is a little fuzzy as more detailed ones would be hard to maintain over time
        assertThatItineraryHasModes(itineraries, expectedModes);
    }

    private static void assertThatItineraryHasModes(
            List<ApiItinerary> itineraries,
            List<String> expectedModes
    ) {
        var itineraryModes = itineraries.stream()
                .map(i -> i.legs.stream().map(l -> l.mode).collect(Collectors.toList()))
                .collect(Collectors.toList());
        assertTrue(
                itineraryModes.contains(expectedModes),
                String.format(
                        "Could not find an mode combination '%s' in itineraries %s",
                        expectedModes,
                        itineraryModes
                )
        );
    }
}
