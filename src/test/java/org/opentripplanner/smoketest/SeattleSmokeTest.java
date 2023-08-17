package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.client.model.RequestMode.BUS;
import static org.opentripplanner.client.model.RequestMode.FLEX_ACCESS;
import static org.opentripplanner.client.model.RequestMode.FLEX_DIRECT;
import static org.opentripplanner.client.model.RequestMode.FLEX_EGRESS;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.Route;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("seattle")
public class SeattleSmokeTest {

  Coordinate sodo = new Coordinate(47.5811, -122.3290);
  Coordinate clydeHill = new Coordinate(47.6316, -122.2173);

  Coordinate boeingCreekPark = new Coordinate(47.755872, -122.361645);
  Coordinate ronaldBogPark = new Coordinate(47.75601664, -122.33141);

  @Test
  public void acrossTheCity() {
    var modes = Set.of(TRANSIT, WALK);
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(sodo, clydeHill, modes),
      List.of("WALK", "BUS", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void flexAndTransit() {
    var modes = Set.of(WALK, BUS, FLEX_DIRECT, FLEX_EGRESS, FLEX_ACCESS);
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(boeingCreekPark, ronaldBogPark, modes),
      List.of("BUS")
    );
  }

  @Test
  public void monorailRoute() throws IOException, InterruptedException {
    Set<Object> modes = SmokeTest.API_CLIENT
      .routes()
      .stream()
      .map(Route::mode)
      .map(Objects::toString)
      .collect(Collectors.toSet());
    assertEquals(Set.of("MONORAIL", "TRAM", "FERRY", "BUS"), modes);
  }

  @Test
  public void vehiclePositions() {
    SmokeTest.assertThereArePatternsWithVehiclePositions();
  }
}
