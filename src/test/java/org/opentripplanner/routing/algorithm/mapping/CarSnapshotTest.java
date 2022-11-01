package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;

@ExtendWith(SnapshotExtension.class)
@ResourceLock(Resources.LOCALE)
public class CarSnapshotTest extends SnapshotTestBase {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  static GenericLocation p1 = new GenericLocation(
    "NW Pettygrove Ave. & NW 24th Ave. (P1)",
    null,
    45.53261,
    -122.70075
  );

  static GenericLocation p2 = new GenericLocation(
    "NW Marshall St. & NW 24th Ave. (P2)",
    null,
    45.53046,
    -122.70067
  );

  static GenericLocation p3 = new GenericLocation(
    "Chapman Elementary School (P3)",
    null,
    45.53335,
    -122.70517
  );

  static GenericLocation p4 = new GenericLocation(
    "Legacy Good Samaritan Medical Center (P4)",
    null,
    45.53060,
    -122.69771
  );

  @BeforeAll
  public static void beforeClass() {
    Locale.setDefault(Locale.US);
    loadGraphBeforeClass(false);
  }

  @AfterAll
  public static void afterClass() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @DisplayName("Direct CAR_TO_PARK")
  @Test
  public void directCarPark() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    // TODO: 2022-08-30 VIA: Previously we were using RequestModesBuilder
    // maybe we should implement similar pattern for new models?
    request.journey().direct().setMode(StreetMode.CAR_TO_PARK);
    request.journey().transit().setModes(List.of());
    request.setFrom(p1);
    request.setTo(p2);

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }

  @DisplayName("Direct CAR_PICKUP (with walking both ends)")
  @Test
  public void directCarPickupWithWalking() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    // TODO: 2022-08-30 VIA: Previously we were using RequestModesBuilder
    // maybe we should implement similar pattern for new models?
    request.journey().direct().setMode(StreetMode.CAR_PICKUP);
    request.journey().transit().setModes(List.of());
    request.setFrom(p3);
    request.setTo(p4);

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Direct CAR_PICKUP (with walking both ends) - arriveBy")
  @Test
  public void directCarPickupWithWalkingArriveBy() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 16, 54);

    // TODO: 2022-08-30 VIA: Previously we were using RequestModesBuilder
    // maybe we should implement similar pattern for new models?
    request.journey().direct().setMode(StreetMode.CAR_PICKUP);
    request.journey().transit().setModes(List.of());
    request.setFrom(p3);
    request.setTo(p4);
    request.setArriveBy(true);

    expectRequestResponseToMatchSnapshot(request);
  }

  @DisplayName("Direct CAR_PICKUP (without walking at either end)")
  @Test
  public void directCarPickupWithoutWalking() {
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0);

    // TODO: 2022-08-30 VIA: Previously we were using RequestModesBuilder
    // maybe we should implement similar pattern for new models?
    request.journey().direct().setMode(StreetMode.CAR_PICKUP);
    request.journey().transit().setModes(List.of());
    request.setFrom(p1);
    request.setTo(p2);
    request.withPreferences(pref -> pref.withWalk(w -> w.withSpeed(1.0)));

    expectArriveByToMatchDepartAtAndSnapshot(request);
  }
}
