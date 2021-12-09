package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opentripplanner.api.resource.TripPlannerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(SmokeTest.class)
public class AtlantaSmokeTest {

    private static final Logger LOG = LoggerFactory.getLogger(AtlantaSmokeTest.class);

    HttpClient client = HttpClient.newHttpClient();

    @Test
    public void returnRouteFromCentralAtlantaToPowderSprings()
    throws IOException, InterruptedException {
        var params = Map.of(
                "fromPlace", "33.714059324224124,-84.37225341796875",
                "toPlace", "33.882387270695105,-84.51576232910155",
                "time", "1:00pm",
                "date", SmokeTest.nextMonday().toString(),
                "mode", "TRANSIT,WALK",
                "showIntermediateStops", "true",
                "locale", "en"
        );

        var request = SmokeTest.planRequest(params);

        var response = client.send(request, BodyHandlers.ofInputStream());

        assertEquals(200, response.statusCode(), "Status code returned by OTP server was not 200");

        var tripPlannerResponse =
                SmokeTest.mapper.readValue(response.body(), TripPlannerResponse.class);

        LOG.info(
                "Request to {} returned {} itineraries",
                request.uri(),
                tripPlannerResponse.getPlan().itineraries.size()
        );

        assertTrue(tripPlannerResponse.getPlan().itineraries.size() > 1);
    }
}
