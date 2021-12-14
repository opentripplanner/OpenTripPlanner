package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.smoketest.SmokeTest.assertThatItineraryHasModes;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from central Atlanta to the flex zone in Powder
 * Springs can be planned. In order to guarantee somewhat predictable results over time it uses 1pm
 * on the next Monday relative to the day the test is run as the start time of the route.
 * <p>
 * The assertions are intentionally vague as more precise ones would probably cause false positives
 * when there are slight changes in the schedule.
 */
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
                "date", SmokeTest.closestWorkDay().toString(),
                "mode", "TRANSIT,WALK",
                "showIntermediateStops", "true",
                "locale", "en",
                "searchWindow", Long.toString(Duration.ofHours(2).toSeconds())
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
                "date", SmokeTest.closestWorkDay().toString(),
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

        var usesZone1Route = transitLegs.stream()
                .map(l -> l.routeShortName)
                .anyMatch(name -> name.equals("Zone 1"));

        assertTrue(usesZone1Route);

    }
}
