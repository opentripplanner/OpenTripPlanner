package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;

class DurationForEnumTest {

  private static final Duration DEFAULT = Duration.ofSeconds(7);
  private static final Duration WALK_VALUE = Duration.ofSeconds(3);

  private final DurationForEnum<StreetMode> subject = new DurationForEnum<>(
    StreetMode.class,
    DEFAULT,
    Map.of(StreetMode.WALK, WALK_VALUE)
  );

  @Test
  void testToString() {
    assertEquals("DurationForStreetMode{default:7s, WALK:3s}", subject.toString());
  }

  @Test
  void copyOf() {
    var copy = subject.copyOf();
    assertEquals(subject, copy);
    assertEquals(subject.hashCode(), copy.hashCode());
  }

  @Test
  void defaultValue() {
    assertEquals(DEFAULT, subject.defaultValue());
  }

  @Test
  void valueOf() {
    assertEquals(DEFAULT, subject.valueOf(StreetMode.BIKE));
    assertEquals(WALK_VALUE, subject.valueOf(StreetMode.WALK));
  }
}
