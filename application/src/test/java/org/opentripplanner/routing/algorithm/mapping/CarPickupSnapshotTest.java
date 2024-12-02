package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;

@ExtendWith(SnapshotExtension.class)
public class CarPickupSnapshotTest extends SnapshotTestBase {

  static GenericLocation p0 = new GenericLocation(
    "SE Stark    St. & SE 17th Ave. (P0)",
    null,
    45.519320,
    -122.648567
  );

  static GenericLocation p1 = new GenericLocation(
    "SE Morrison St. & SE 17th Ave. (P1)",
    null,
    45.51726,
    -122.64847
  );

  static GenericLocation p2 = new GenericLocation(
    "NW Northrup St. & NW 22nd Ave. (P2)",
    null,
    45.53122,
    -122.69659
  );

  @BeforeAll
  public static void beforeClass() {
    loadGraphBeforeClass(false);
  }

  @Test
  public void test_trip_planning_with_car_pickup_only() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0);

    request.journey().direct().setMode(StreetMode.CAR_PICKUP);
    request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));

    request.setFrom(p0);
    request.setTo(p2);

    expectRequestResponseToMatchSnapshot(request);
  }

  @Test
  public void test_trip_planning_with_car_pickup_transfer() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0);

    request.journey().access().setMode(StreetMode.WALK);
    request.journey().egress().setMode(StreetMode.WALK);
    request.journey().direct().setMode(StreetMode.WALK);
    request.journey().transfer().setMode(StreetMode.CAR_PICKUP);
    request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));

    request.setFrom(p0);
    request.setTo(p2);

    expectRequestResponseToMatchSnapshot(request);
  }
}
