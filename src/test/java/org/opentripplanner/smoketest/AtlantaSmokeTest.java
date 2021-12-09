package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opentripplanner.api.json.JSONObjectMapperProvider;
import org.opentripplanner.api.resource.TripPlannerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(SmokeTest.class)
public class AtlantaSmokeTest {
    private static final Logger LOG = LoggerFactory.getLogger(AtlantaSmokeTest.class);

    HttpClient client = HttpClient.newHttpClient();

    static LocalDate nextMonday() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    @Test
    public void returnRouteFromCentralAtlantaToPowerSprings() throws IOException, InterruptedException {
        var params = Map.of(
                "fromPlace", "33.714059324224124,-84.37225341796875",
                "toPlace", "33.882387270695105,-84.51576232910155",
                "time", "1:00pm",
                "date", nextMonday().toString(),
                "mode", "TRANSIT,WALK",
                "showIntermediateStops", "true",
                "locale", "en"
        )
                .entrySet()
                .stream()
                .map(kv -> kv.getKey() + "=" + kv.getValue())
                .collect(Collectors.joining("&"));

        var uri = URI.create("http://localhost:8080/otp/routers/default/plan?" + params);

        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        var response = client.send(request, BodyHandlers.ofInputStream());

        assertEquals(200, response.statusCode(), "Status code returned by OTP server was not 200");

        var provider = new JSONObjectMapperProvider();
        var mapper = provider.getContext(null);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

        var tripPlannerResponse = mapper.readValue(response.body(), TripPlannerResponse.class);

        LOG.info("Request to {} returned {} itineraries", uri, tripPlannerResponse.getPlan().itineraries.size());

        assertTrue(tripPlannerResponse.getPlan().itineraries.size() > 1);
    }
}
