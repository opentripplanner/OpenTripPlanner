package org.opentripplanner.apis.gtfs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

public class StopInPatternModelTest {

  private static final String ID = "1";
  private static final String NAME = "short name";
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final Route ROUTE = TimetableRepositoryForTest.route("routeId").build();
  public static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  public static final RegularStop STOP_B = TEST_MODEL.stop("B").build();
  public static final RegularStop STOP_C = TEST_MODEL.stop("C").build();
  private static final StopPattern STOP_PATTERN = getStopPattern();

  private static StopPattern getStopPattern() {
    var builder = StopPattern.create(3);

    builder.stops.with(0, STOP_A);
    builder.stops.with(1, STOP_B);
    builder.stops.with(2, STOP_C);
    builder.pickups.with(0, PickDrop.SCHEDULED);
    builder.pickups.with(1, PickDrop.CALL_AGENCY);
    builder.pickups.with(2, PickDrop.NONE);
    builder.dropoffs.with(0, PickDrop.NONE);
    builder.dropoffs.with(1, PickDrop.COORDINATE_WITH_DRIVER);
    builder.dropoffs.with(2, PickDrop.SCHEDULED);
    return builder.build();
  }

  public static final TripPattern PATTERN = TripPattern.of(id(ID))
    .withName(NAME)
    .withRoute(ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();

  @Test
  public void fromPatternAndIndex() {
    assertEquals(
      new StopInPatternModel(STOP_A, 0, PickDrop.SCHEDULED, PickDrop.NONE),
      StopInPatternModel.fromPatternAndIndex(PATTERN, 0)
    );
    assertEquals(
      new StopInPatternModel(STOP_B, 1, PickDrop.CALL_AGENCY, PickDrop.COORDINATE_WITH_DRIVER),
      StopInPatternModel.fromPatternAndIndex(PATTERN, 1)
    );
    assertEquals(
      new StopInPatternModel(STOP_C, 2, PickDrop.NONE, PickDrop.SCHEDULED),
      StopInPatternModel.fromPatternAndIndex(PATTERN, 2)
    );
  }
}
