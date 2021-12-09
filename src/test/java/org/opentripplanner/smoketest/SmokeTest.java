package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.api.json.JSONObjectMapperProvider;
import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.api.resource.TripPlannerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is both a utility class and a category to select or deselect smoke tests during test
 * execution.
 * <p>
 * By default, the smoke tests are not run when you execute `mvn test`.
 * <p>
 * If you want run them use the following command: `mvn test -P smoke-tests`
 */
public class SmokeTest {

    static final Logger LOG = LoggerFactory.getLogger(SmokeTest.class);
    static HttpClient client = HttpClient.newHttpClient();
    static final ObjectMapper mapper;

    static {
        var provider = new JSONObjectMapperProvider();
        mapper = provider.getContext(null);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
    }

    static LocalDate nextMonday() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }


    static HttpRequest planRequest(Map<String, String> params) {
        var urlParams = params.entrySet()
                .stream()
                .map(kv -> kv.getKey() + "=" + kv.getValue())
                .collect(Collectors.joining("&"));

        var uri = URI.create("http://localhost:8080/otp/routers/default/plan?" + urlParams);

        return HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

    }

    static TripPlannerResponse sendPlanRequest(Map<String, String> params) {
        var request = SmokeTest.planRequest(params);
        LOG.info("Sending request to {}", request.uri());
        TripPlannerResponse otpResponse;
        try {

            var response = client.send(request, BodyHandlers.ofInputStream());

            assertEquals(
                    200, response.statusCode(), "Status code returned by OTP server was not 200");
            otpResponse = SmokeTest.mapper.readValue(response.body(), TripPlannerResponse.class);
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        LOG.info(
                "Request to {} returned {} itineraries",
                request.uri(),
                otpResponse.getPlan().itineraries.size()
        );

        return otpResponse;
    }

    static void assertThatItineraryHasModes(
            List<ApiItinerary> itineraries,
            List<String> expectedModes
    ) {
        var itineraryModes = itineraries.stream()
                .map(i -> i.legs.stream().map(l -> l.mode).collect(Collectors.toList()))
                .collect(Collectors.toList());
        assertTrue(
                itineraryModes.contains(expectedModes),
                String.format(
                        "Could not find a mode combination '%s' in itineraries %s",
                        expectedModes,
                        itineraryModes
                )
        );
    }
}
