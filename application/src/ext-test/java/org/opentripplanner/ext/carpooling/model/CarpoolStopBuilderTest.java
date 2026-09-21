package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

public class CarpoolStopBuilderTest {

  public static final ZonedDateTime AIMED_ARRIVAL_TIME = ZonedDateTime.of(
    2026,
    3,
    9,
    10,
    12,
    0,
    0,
    ZoneId.systemDefault()
  );
  public static final ZonedDateTime EXPECTED_ARRIVAL_TIME = ZonedDateTime.of(
    2026,
    3,
    9,
    10,
    17,
    0,
    0,
    ZoneId.systemDefault()
  );
  public static final ZonedDateTime AIMED_DEPARTURE_TIME = ZonedDateTime.of(
    2026,
    3,
    9,
    8,
    17,
    0,
    0,
    ZoneId.systemDefault()
  );
  public static final ZonedDateTime EXPECTED_DEPARTURE_TIME = ZonedDateTime.of(
    2026,
    3,
    9,
    8,
    12,
    0,
    0,
    ZoneId.systemDefault()
  );

  @Test
  void buildFromValues_usingWith_buildToCorrectValues() {
    var builder = new CarpoolStopBuilder(new FeedScopedId("feed", "id"));
    builder
      .withOnboardCount(2)
      .withCoordinate(OSLO_NORTH)
      .withAimedArrivalTime(AIMED_ARRIVAL_TIME)
      .withExpectedArrivalTime(EXPECTED_ARRIVAL_TIME)
      .withAimedDepartureTime(AIMED_DEPARTURE_TIME)
      .withExpectedDepartureTime(EXPECTED_DEPARTURE_TIME);
    var stop = builder.buildFromValues();

    assertEquals(2, stop.getOnboardCount());
    assertEquals(OSLO_NORTH, stop.getCoordinate());
    assertEquals(AIMED_ARRIVAL_TIME, stop.getAimedArrivalTime());
    assertEquals(EXPECTED_ARRIVAL_TIME, stop.getExpectedArrivalTime());
    assertEquals(AIMED_DEPARTURE_TIME, stop.getAimedDepartureTime());
    assertEquals(EXPECTED_DEPARTURE_TIME, stop.getExpectedDepartureTime());
  }

  @Test
  void buildFromValues_usingCarPoolStop_buildsCorrectValues() {
    var originalBuilder = new CarpoolStopBuilder(new FeedScopedId("feed", "id"));
    originalBuilder
      .withOnboardCount(3)
      .withCoordinate(OSLO_CENTER)
      .withAimedArrivalTime(AIMED_ARRIVAL_TIME)
      .withExpectedArrivalTime(EXPECTED_ARRIVAL_TIME)
      .withAimedDepartureTime(AIMED_DEPARTURE_TIME)
      .withExpectedDepartureTime(EXPECTED_DEPARTURE_TIME);
    var original = originalBuilder.buildFromValues();

    var copyBuilder = new CarpoolStopBuilder(original);
    var copy = copyBuilder.buildFromValues();

    assertEquals(original.getOnboardCount(), copy.getOnboardCount());
    assertEquals(original.getCoordinate(), copy.getCoordinate());
    assertEquals(original.getAimedArrivalTime(), copy.getAimedArrivalTime());
    assertEquals(original.getExpectedArrivalTime(), copy.getExpectedArrivalTime());
    assertEquals(original.getAimedDepartureTime(), copy.getAimedDepartureTime());
    assertEquals(original.getExpectedDepartureTime(), copy.getExpectedDepartureTime());
  }
}
