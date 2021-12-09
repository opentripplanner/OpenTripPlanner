package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.smoketest.SmokeTest.assertThatItineraryHasModes;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SmokeTest.class)
public class AtlantaSmokeTest {

    String nearGeorgiaStateStation = "33.74139944890028,-84.38607215881348";
    String powderSpringsInsideFlexZone1 = "33.86425088555784,-84.67141628265381";

    @Test
    public void regularRouteFromCentralAtlantaToPowderSprings() {
        var params = Map.of(
                "fromPlace", nearGeorgiaStateStation,
                "toPlace", powderSpringsInsideFlexZone1,
                "time", "1:00pm",
                "date", SmokeTest.nextMonday().toString(),
                "mode", "TRANSIT,WALK",
                "showIntermediateStops", "true",
                "locale", "en"
        );
        var otpResponse = SmokeTest.sendPlanRequest(params);
        var itineraries = otpResponse.getPlan().itineraries;

        assertTrue(itineraries.size() > 1);

        var expectedModes = List.of("WALK", "SUBWAY", "WALK", "BUS", "WALK", "BUS", "WALK");
        assertThatItineraryHasModes(itineraries, expectedModes);
    }

    @Test
    public void flexRouteFromCentralAtlantaToPowderSprings() {
        var params = Map.of(
                "fromPlace", nearGeorgiaStateStation,
                "toPlace", powderSpringsInsideFlexZone1,
                "time", "1:00pm",
                "date", SmokeTest.nextMonday().toString(),
                "mode", "FLEX_EGRESS,WALK,TRANSIT",
                "showIntermediateStops", "true",
                "locale", "en",
                "searchWindow", Long.toString(Duration.ofHours(2).toSeconds())
        );
        var otpResponse = SmokeTest.sendPlanRequest(params);
        var itineraries = otpResponse.getPlan().itineraries;

        assertTrue(itineraries.size() > 0);

        var expectedModes = List.of("WALK", "SUBWAY", "WALK", "BUS", "WALK", "BUS");
        assertThatItineraryHasModes(itineraries, expectedModes);

        var transitLegs = itineraries.stream()
                .flatMap(i -> i.legs.stream().filter(l -> l.transitLeg))
                .collect(Collectors.toList());

        var hasFlexRoute = transitLegs.stream().map(l -> l.routeShortName).anyMatch(name -> name.equals("Zone 1"));

        assertTrue(hasFlexRoute);

    }
}
