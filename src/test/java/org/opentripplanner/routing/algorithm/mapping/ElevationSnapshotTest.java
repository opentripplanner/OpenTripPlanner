package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.network.MainAndSubMode;

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
    RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.modes = new RequestModes(null, null, null, StreetMode.WALK, List.of());
    request.from = p1;
    request.to = p4;

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Direct BIKE_RENTAL")
  @Test
  public void directBikeRental() {
    RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.modes = new RequestModes(null, null, null, StreetMode.BIKE_RENTAL, List.of());
    request.from = p1;
    request.to = p2;

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  @DisplayName("Direct BIKE")
  @Test
  public void directBike() {
    RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);
    request.bicycleOptimizeType = BicycleOptimizeType.TRIANGLE;
    request.bikeTriangleSafetyFactor = 0.3;
    request.bikeTriangleTimeFactor = 0.3;
    request.bikeTriangleSlopeFactor = 0.4;

    request.modes = new RequestModes(null, null, null, StreetMode.BIKE, List.of());
    request.from = p1;
    request.to = p4;
    request.arriveBy = true;

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Access BIKE_RENTAL")
  @Test
  public void accessBikeRental() {
    RoutingRequest request = createTestRequest(2009, 10, 21, 16, 14, 0);

    request.modes =
      new RequestModes(
        StreetMode.BIKE_RENTAL,
        StreetMode.WALK,
        StreetMode.WALK,
        null,
        MainAndSubMode.all()
      );
    request.from = p1;
    request.to = p3;

    try {
      expectArriveByToMatchDepartAtAndSnapshot(request);
    } catch (CompletionException e) {
      RoutingValidationException.unwrapAndRethrowCompletionException(e);
    }
  }

  @DisplayName("TRANSIT")
  @Test
  public void transit() {
    RoutingRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    request.modes =
      new RequestModes(
        StreetMode.WALK,
        StreetMode.WALK,
        StreetMode.WALK,
        null,
        MainAndSubMode.all()
      );
    request.from = p3;
    request.to = p1;

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  @Override
  protected Graph getGraph() {
    return ConstantsForTests.getInstance().getCachedPortlandGraphWithElevation();
  }
}
