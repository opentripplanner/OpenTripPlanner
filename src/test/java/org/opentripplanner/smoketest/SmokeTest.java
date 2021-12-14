package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.api.json.JSONObjectMapperProvider;
import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.api.resource.TripPlannerResponse;
import org.opentripplanner.routing.core.Fare;
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
    static final Set<DayOfWeek> weekend = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    /**
     * The Fare class is a little hard to deserialize so we have a custom deserializer as we don't
     * run any assertions against the fares. (That is done during unit tests.)
     */
    static class FareDeserializer extends JsonDeserializer<Fare> {

        @Override
        public Fare deserialize(
                JsonParser jsonParser, DeserializationContext deserializationContext
        ) throws IOException, JsonProcessingException {
            return null;
        }
    }

    static {
        var provider = new JSONObjectMapperProvider();

        SimpleModule module = new SimpleModule("SmokeTests");
        module.addDeserializer(Fare.class, new FareDeserializer());

        mapper = provider.getContext(null);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        mapper.registerModule(module);
    }

    /**
     * Return the closest non-weekend day.
     * <p>
     * On Saturday and Sunday, this returns the following Monday. During the work week it returns
     * the current day.
     */
    static LocalDate closestWorkDay() {
        var today = LocalDate.now();
        if (weekend.contains(today.getDayOfWeek())) {
            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        else {
            return today;
        }
    }

    /**
     * Builds an HTTP request for sending to an OTP instance.
     */
    static HttpRequest buildPlanRequest(Map<String, String> params) {
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

    /**
     * Sends an HTTP request to the OTP plan endpoint and deserializes the response.
     */
    static TripPlannerResponse sendPlanRequest(Map<String, String> params) {
        var request = SmokeTest.buildPlanRequest(params);
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

    /**
     * Given a list of itineraries assert that at least one of them has legs that have the expected
     * modes.
     */
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
