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

@Tag("smoke-test")
@Tag("seattle")
public class SeattleSmokeTest {

  WgsCoordinate sodo = new WgsCoordinate(47.5811, -122.3290);
  WgsCoordinate clydeHill = new WgsCoordinate(47.6316, -122.2173);

  @Test
  public void acrossTheCity() {
    SmokeTest.basicRouteTest(
      sodo,
      clydeHill,
      Set.of("TRANSIT", "WALK"),
      List.of("WALK", "BUS", "WALK", "BUS", "WALK")
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
