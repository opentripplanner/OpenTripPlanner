package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimePenalty;

public class AccessEgressPreferencesTest {

  private static final Duration MAX_ACCESS_EGRESS = Duration.ofMinutes(5);
  private static final int MAX_DEFAULT_STOP_COUNT = 245;
  private static final int MAX_CAR_STOP_COUNT = 0;
  private static final TimeAndCostPenalty CAR_TO_PARK_PENALTY = TimeAndCostPenalty.of(
    TimePenalty.of("2m + 1.5t"),
    3.5
  );

  private final AccessEgressPreferences subject = AccessEgressPreferences.of()
    .withPenalty(Map.of(StreetMode.CAR_TO_PARK, CAR_TO_PARK_PENALTY))
    .withMaxDuration(MAX_ACCESS_EGRESS, Map.of())
    .withMaxStopCount(MAX_DEFAULT_STOP_COUNT, Map.of(StreetMode.CAR, MAX_CAR_STOP_COUNT))
    .build();

  @Test
  void accessEgressPenalty() {
    assertEquals(TimeAndCostPenalty.ZERO, subject.penalty().valueOf(StreetMode.WALK));
    assertEquals(CAR_TO_PARK_PENALTY, subject.penalty().valueOf(StreetMode.CAR_TO_PARK));
  }

  @Test
  void maxAccessEgressDuration() {
    assertEquals(MAX_ACCESS_EGRESS, subject.maxDuration().defaultValue());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(AccessEgressPreferences.DEFAULT, AccessEgressPreferences.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withMaxStopCount(20, Map.of()).build();
    var copy = other
      .copyOf()
      .withMaxStopCount(MAX_DEFAULT_STOP_COUNT, Map.of(StreetMode.CAR, MAX_CAR_STOP_COUNT))
      .build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("StreetPreferences{}", StreetPreferences.DEFAULT.toString());
    assertEquals(
      "AccessEgressPreferences{" +
      "penalty: TimeAndCostPenaltyForEnum{" +
      "CAR_TO_PARK: " +
      CAR_TO_PARK_PENALTY +
      ", " +
      "CAR_PICKUP: (timePenalty: 20m + 2.0 t, costFactor: 1.50), " +
      "CAR_RENTAL: (timePenalty: 20m + 2.0 t, costFactor: 1.50), " +
      "CAR_HAILING: (timePenalty: 20m + 2.0 t, costFactor: 1.50), " +
      "FLEXIBLE: (timePenalty: 10m + 1.30 t, costFactor: 1.30)}, " +
      "maxDuration: DurationForStreetMode{default:5m}, " +
      "maxStopCount: MaxStopCountLimit{defaultLimit: 245, limitsForModes: {CAR=0}}" +
      "}",
      subject.toString()
    );
  }
}
