package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;

public class MaxStopCountLimitTest {

  private static final int MAX_DEFAULT_STOP_COUNT = 245;
  private static final int MAX_CAR_STOP_COUNT = 0;

  private final MaxStopCountLimit subject = MaxStopCountLimit.of()
    .withDefaultLimit(MAX_DEFAULT_STOP_COUNT)
    .withLimitsForModes(Map.of(StreetMode.CAR, MAX_CAR_STOP_COUNT))
    .build();

  @Test
  void maxAccessEgressStopCountLimit() {
    assertEquals(MAX_DEFAULT_STOP_COUNT, subject.defaultLimit());
    assertEquals(MAX_DEFAULT_STOP_COUNT, subject.limitForMode(StreetMode.BIKE));
    assertEquals(MAX_CAR_STOP_COUNT, subject.limitForMode(StreetMode.CAR));
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(MaxStopCountLimit.DEFAULT, MaxStopCountLimit.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withDefaultLimit(20).build();
    var copy = other.copyOf().withDefaultLimit(MAX_DEFAULT_STOP_COUNT).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("StreetPreferences{}", StreetPreferences.DEFAULT.toString());
    assertEquals(
      "MaxStopCountLimit{" + "defaultLimit: 245, " + "limitsForModes: {CAR=0}" + "}",
      subject.toString()
    );
  }
}
