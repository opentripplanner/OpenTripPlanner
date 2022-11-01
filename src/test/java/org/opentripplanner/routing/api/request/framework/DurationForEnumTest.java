package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;

class DurationForEnumTest {

  private static final Duration D10s = Duration.ofSeconds(10);

  private static final Duration DEFAULT = Duration.ofSeconds(7);
  private static final Duration WALK_VALUE = Duration.ofSeconds(3);

  private final DurationForEnum<StreetMode> subject = DurationForEnum
    .of(StreetMode.class)
    .withDefault(DEFAULT)
    .withValues(Map.of(StreetMode.WALK, WALK_VALUE))
    .build();

  @Test
  void testToString() {
    assertEquals("DurationForStreetMode{default:7s, WALK:3s}", subject.toString());
  }

  @Test
  void defaultValue() {
    assertEquals(DEFAULT, subject.defaultValue());
    assertEquals(DEFAULT.toSeconds(), subject.defaultValueSeconds());

    assertThrows(
      NullPointerException.class,
      () -> DurationForEnum.of(StreetMode.class).withDefault(null).build()
    );
  }

  @Test
  void valueOf() {
    assertEquals(DEFAULT, subject.valueOf(StreetMode.BIKE));
    assertEquals(WALK_VALUE, subject.valueOf(StreetMode.WALK));
  }

  @Test
  void isSet() {
    assertTrue(subject.isSet(StreetMode.WALK));
    assertFalse(subject.isSet(StreetMode.BIKE));
  }

  @Test
  void equalsAndHashCode() {
    var sameValue = DurationForEnum
      .of(StreetMode.class)
      .withDefault(DEFAULT)
      .with(StreetMode.WALK, WALK_VALUE)
      .build();

    assertEquals(subject, sameValue);
    assertEquals(subject.hashCode(), sameValue.hashCode());

    var different = DurationForEnum.of(StreetMode.class).withDefault(D10s).build();
    assertNotEquals(subject, different);
    assertNotEquals(subject.hashCode(), different.hashCode());
  }

  @Test
  void reuseBuilderToMakeDiffrentObjects() {
    var builder = DurationForEnum.of(StreetMode.class);
    var defaultValue = builder.build();
    builder.with(StreetMode.WALK, D10s);
    var withWalkSet = builder.build();

    assertNotSame(defaultValue, withWalkSet);
    assertEquals(D10s, withWalkSet.valueOf(StreetMode.WALK));
  }

  @Test
  void copyOf() {
    // with new default, keep map
    var copy = subject.copyOf().apply(b3 -> b3.withDefaultSec(10)).build();
    assertEquals(D10s, copy.valueOf(StreetMode.BIKE));
    assertEquals(WALK_VALUE, copy.valueOf(StreetMode.WALK));

    // with new map values, keep walk and default
    copy = subject.copyOf().apply(b2 -> b2.with(StreetMode.BIKE, D10s)).build();
    assertEquals(WALK_VALUE, copy.valueOf(StreetMode.WALK));
    assertEquals(D10s, copy.valueOf(StreetMode.BIKE));
    assertEquals(DEFAULT, copy.valueOf(StreetMode.CAR));

    // with override map value
    copy = subject.copyOf().apply(b1 -> b1.with(StreetMode.WALK, D10s)).build();
    assertEquals(D10s, copy.valueOf(StreetMode.WALK));

    // with no "real" changes -> return original
    copy =
      subject.copyOf().apply(b -> b.withDefault(DEFAULT).with(StreetMode.WALK, WALK_VALUE)).build();
    assertSame(subject, copy);
  }
}
