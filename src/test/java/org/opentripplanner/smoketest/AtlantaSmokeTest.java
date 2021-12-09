package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SmokeTest.class)
public class AtlantaSmokeTest {

    HttpClient client = HttpClient.newHttpClient();

    @Test
    public void shouldFail() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/otp/routers/default/plan?fromPlace=33.747751389895576,-84.38529968261719&toPlace=33.90176649398624,-84.53704833984375&time=12:23pm&date=12-09-2021&mode=TRANSIT,WALK&maxWalkDistance=4828.032&arriveBy=false&wheelchair=false&showIntermediateStops=true&debugItineraryFilter=false&locale=en"))
                .GET()
                .build();

        var response = client.send(request, BodyHandlers.discarding());

        assertEquals(200, response.statusCode(), "Status code returned by OTP server was not 200");
    }
}
