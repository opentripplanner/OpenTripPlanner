package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.error.RoutingValidationException;

@ExtendWith(SnapshotExtension.class)
@ResourceLock(Resources.LOCALE)
public class ElevationSnapshotTest extends SnapshotTestBase {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  static GenericLocation p1 = new GenericLocation(
    "SW Johnson St. & NW 24th Ave. (P1)",
    null,
    45.52832,
    -122.70059
  );

  static GenericLocation p2 = new GenericLocation(
    "NW Hoyt St. & NW 20th Ave. (P2)",
    null,
    45.52704,
    -122.69240
  );

  static GenericLocation p3 = new GenericLocation(
    "NW Everett St. & NW 5th Ave. (P3)",
    null,
    45.52523,
    -122.67525
  );

  static GenericLocation p4 = new GenericLocation("Sulzer Pump (P4)", null, 45.54549, -122.69659);

  @BeforeAll
  public static void beforeClass() {
    Locale.setDefault(Locale.US);
    loadGraphBeforeClass(true);
  }

  @AfterAll
  public static void afterClass() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @DisplayName("Direct WALK")
  @Test
  public void directWalk() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.journey().direct().setMode(StreetMode.WALK);
    request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));

    request.setFrom(p1);
    request.setTo(p4);

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Direct BIKE_RENTAL")
  @Test
  public void directBikeRental() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.journey().direct().setMode(StreetMode.BIKE_RENTAL);
    request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));

    request.setFrom(p1);
    request.setTo(p2);

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  @DisplayName("Direct BIKE")
  @Test
  public void directBike() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.withPreferences(pref ->
      pref.withBike(bike ->
        bike
          .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
          .withOptimizeTriangle(b -> b.withTime(0.3).withSlope(0.4).withSafety(0.3))
      )
    );

    request.journey().direct().setMode(StreetMode.BIKE);
    request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));

    request.setFrom(p1);
    request.setTo(p4);
    request.setArriveBy(true);

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Access BIKE_RENTAL")
  @Test
  @Disabled
  public void accessBikeRental() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 14, 0);

    request.journey().access().setMode(StreetMode.BIKE_RENTAL);
    request.journey().egress().setMode(StreetMode.WALK);
    request.journey().direct().setMode(StreetMode.NOT_SET);
    request.journey().transfer().setMode(StreetMode.WALK);

    request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));

    request.setFrom(p1);
    request.setTo(p3);

    try {
      expectArriveByToMatchDepartAtAndSnapshot(request);
    } catch (CompletionException e) {
      RoutingValidationException.unwrapAndRethrowCompletionException(e);
    }
  }

  @DisplayName("TRANSIT")
  @Test
  public void transit() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.journey().access().setMode(StreetMode.WALK);
    request.journey().egress().setMode(StreetMode.WALK);
    request.journey().transfer().setMode(StreetMode.WALK);
    request.journey().direct().setMode(StreetMode.NOT_SET);

    request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));

    request.setFrom(p3);
    request.setTo(p1);

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  @Override
  protected TestOtpModel getGraph() {
    return ConstantsForTests.getInstance().getCachedPortlandGraphWithElevation();
  }
}
