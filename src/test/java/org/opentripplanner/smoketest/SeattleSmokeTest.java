package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.smoketest.util.GraphQLClient;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("seattle")
public class SeattleSmokeTest {

  WgsCoordinate sodo = new WgsCoordinate(47.5811, -122.3290);
  WgsCoordinate clydeHill = new WgsCoordinate(47.6316, -122.2173);

  WgsCoordinate boeingCreekPark = new WgsCoordinate(47.755872, -122.361645);
  WgsCoordinate ronaldBogPark = new WgsCoordinate(47.75601664, -122.33141);

  @Test
  public void acrossTheCity() {
    Set<String> modes = Set.of("TRANSIT", "WALK");
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(sodo, clydeHill, modes),
      List.of("WALK", "BUS", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void flexAndTransit() {
    Set<String> modes = Set.of("WALK", "BUS", "FLEX", "FLEX_DIRECT", "FLEX_EGRESS", "FLEX_ACCESS");
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(boeingCreekPark, ronaldBogPark, modes),
      List.of("BUS")
    );
  }

  @Test
  public void monorailRoute() {
    var modes = GraphQLClient
      .routes()
      .stream()
      .map(GraphQLClient.Route::mode)
      .map(Objects::toString)
      .collect(Collectors.toSet());

    assertEquals(Set.of("MONORAIL", "TRAM", "FERRY", "BUS"), modes);
  }
}
