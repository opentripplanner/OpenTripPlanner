package org.opentripplanner.smoketest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.api.resource.TripPlannerResponse;
import org.opentripplanner.smoketest.SmokeTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestClient {

  static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

  static HttpClient client = HttpClient.newHttpClient();

  /**
   * Builds an HTTP request for sending to an OTP instance.
   */
  static HttpRequest buildPlanRequest(Map<String, String> params) {
    var urlParams = params
      .entrySet()
      .stream()
      .map(kv -> kv.getKey() + "=" + kv.getValue())
      .collect(Collectors.joining("&"));

    var uri = URI.create("http://localhost:8080/otp/routers/default/plan?" + urlParams);

    return HttpRequest.newBuilder().uri(uri).GET().build();
  }

  /**
   * Sends an HTTP request to the OTP plan endpoint and deserializes the response.
   */
  public static TripPlannerResponse sendPlanRequest(SmokeTestRequest req) {
    var request = buildPlanRequest(req.toMap());
    LOG.info("Sending request to {}", request.uri());
    TripPlannerResponse otpResponse;
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      assertEquals(200, response.statusCode(), "Status code returned by OTP server was not 200");
      otpResponse = SmokeTest.mapper.readValue(response.body(), TripPlannerResponse.class);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    LOG.info(
      "Request to {} returned {} itineraries",
      request.uri(),
      otpResponse.getPlan().itineraries.size()
    );

    return otpResponse;
  }
}
