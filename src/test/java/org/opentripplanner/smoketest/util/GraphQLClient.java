package org.opentripplanner.smoketest.util;

import static org.opentripplanner.smoketest.SmokeTest.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.opentripplanner.smoketest.SmokeTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQLClient {

  static final Logger LOG = LoggerFactory.getLogger(SmokeTest.class);

  static final HttpClient httpClient = HttpClient.newHttpClient();

  public static JsonNode sendGraphQLRequest(String query) {
    var body = mapper.createObjectNode();
    body.put("query", query);

    try {
      var bodyString = mapper.writeValueAsString(body);

      HttpRequest request = HttpRequest
        .newBuilder()
        .uri(URI.create("http://localhost:8080/otp/routers/default/index/graphql"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(bodyString))
        .build();
      HttpResponse<String> response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      );

      var responseJson = mapper.readTree(response.body());

      LOG.info("Response JSON: {}", responseJson);
      return responseJson.get("data");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
