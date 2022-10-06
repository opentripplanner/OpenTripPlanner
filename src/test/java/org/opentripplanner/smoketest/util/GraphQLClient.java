package org.opentripplanner.smoketest.util;

import static org.opentripplanner.smoketest.SmokeTest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.opentripplanner.smoketest.SmokeTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQLClient {

  static final Logger LOG = LoggerFactory.getLogger(GraphQLClient.class);

  static final HttpClient httpClient = HttpClient.newHttpClient();

  public static VehiclePositionResponse patternWithVehiclePositionsQuery() {
    var json = GraphQLClient.sendGraphQLRequest(
      """
        query {
        	patterns {
        		vehiclePositions {
        			vehicleId
        			lastUpdated
        			trip {
        				id
        				gtfsId
        			}
        			stopRelationship {
        				status
        				stop {
        					name
        				}
        			}
        		}
        	}
        }
                
          """
    );

    try {
      return SmokeTest.mapper.treeToValue(json, VehiclePositionResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<VehicleRentalStation> vehicleRentalStations() {
    var json = GraphQLClient.sendGraphQLRequest(
      """
        query {
        	vehicleRentalStations{
        		name
        		lat
        		lon
        		realtime
        		vehiclesAvailable
        	}
        }
          """
    );

    try {
      List<VehicleRentalStation> stations = SmokeTest.mapper.readValue(
        json.path("vehicleRentalStations").toString(),
        new TypeReference<>() {}
      );

      LOG.info("Fetched {} vehicle rental stations", stations.size());
      return stations;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

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

  public record VehicleRentalStation(String name, float lat, float lon) {}

  public record Position(String vehicleId) {}

  public record Pattern(List<Position> vehiclePositions) {}

  public record VehiclePositionResponse(List<Pattern> patterns) {}
}
