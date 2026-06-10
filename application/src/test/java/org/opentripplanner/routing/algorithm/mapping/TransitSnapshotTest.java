package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;

@ExtendWith(SnapshotExtension.class)
public class TransitSnapshotTest extends SnapshotTestBase {

  static GenericLocation ptc = GenericLocation.fromStopId(
    new FeedScopedId("prt", "79-tc"),
    "Rose Quarter Transit Center"
  );

  static GenericLocation ps = GenericLocation.fromStopId(
    new FeedScopedId("prt", "6577"),
    "NE 12th & Couch"
  );

  static GenericLocation p0 = GenericLocation.fromCoordinate(
    45.519320,
    -122.648567,
    "SE Stark    St. & SE 17th Ave. (P0)"
  );

  static GenericLocation p1 = GenericLocation.fromCoordinate(
    45.51726,
    -122.64847,
    "SE Morrison St. & SE 17th Ave. (P1)"
  );

  static GenericLocation p2 = GenericLocation.fromCoordinate(
    45.53122,
    -122.69659,
    "NW Northrup St. & NW 22nd Ave. (P2)"
  );

  static GenericLocation p3 = GenericLocation.fromCoordinate(
    45.53100,
    -122.70029,
    "NW Northrup St. & NW 24th Ave. (P3)"
  );

  static GenericLocation p4 = GenericLocation.fromCoordinate(
    45.53896,
    -122.64699,
    "NE Thompson St. & NE 18th Ave. (P4)"
  );

  @BeforeAll
  public static void beforeClass() {
    loadGraphBeforeClass(false);
  }

  @Test
  public void test_trip_planning_with_walk_only() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0)
      .withJourney(jb -> jb.withTransit(b -> b.disable()))
      .withFrom(p0)
      .withTo(p2)
      .buildRequest();

    expectRequestResponseToMatchSnapshot(request);
  }

  @Test
  public void test_trip_planning_with_walk_only_stop() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0)
      .withJourney(jb -> {
        jb.withAllModes(StreetMode.WALK);
        jb.withTransit(b -> b.disable());
      })
      .withFrom(ps)
      .withTo(p2)
      .buildRequest();

    expectRequestResponseToMatchSnapshot(request);
  }

  @Test
  public void test_trip_planning_with_walk_only_stop_collection() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0)
      .withJourney(jb -> {
        jb.withAllModes(StreetMode.WALK);
        jb.withTransit(b -> b.disable());
      })
      .withFrom(ptc)
      .withTo(p3)
      .buildRequest();

    expectRequestResponseToMatchSnapshot(request);
    // not equal - expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  @Test
  public void test_trip_planning_with_transit() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0)
      .withJourney(jb -> {
        jb.withAllModes(StreetMode.WALK);
      })
      .withFrom(p1)
      .withTo(p2)
      .buildRequest();

    expectRequestResponseToMatchSnapshot(request);
  }

  @Test
  public void test_trip_planning_with_transit_stop() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0)
      .withJourney(jb -> jb.withAllModes(StreetMode.WALK))
      .withFrom(ps)
      .withTo(p3)
      .buildRequest();

    expectRequestResponseToMatchSnapshot(request);
  }

  @Test
  @Disabled
  public void test_trip_planning_with_transit_stop_collection() {
    RouteRequest request = createTestRequest(2009, 11, 17, 10, 0, 0)
      .withJourney(jb -> jb.withAllModes(StreetMode.WALK))
      .withFrom(ptc)
      .withTo(p3)
      .buildRequest();

    expectRequestResponseToMatchSnapshot(request);
  }
}
