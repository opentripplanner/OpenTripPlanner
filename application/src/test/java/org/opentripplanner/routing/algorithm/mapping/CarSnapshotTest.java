package org.opentripplanner.routing.algorithm.mapping;

import au.com.origin.snapshots.junit5.SnapshotExtension;
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
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;

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
    RouteRequest request = createTestRequest(2009, 10, 21, 16, 10, 0)
      .withJourney(jb -> {
        jb.withDirect(new StreetRequest(StreetMode.CAR_TO_PARK));
        jb.withTransit(b -> b.disable());
      })
      .withFrom(p1)
      .withTo(p2)
      .buildRequest();

    expectRequestResponseToMatchSnapshot(request);
  }
}
