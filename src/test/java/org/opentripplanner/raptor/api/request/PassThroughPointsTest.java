package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PassThroughPointsTest {

  private static final int[] STOPS_1 = { 2, 7, 13 };
  private static final int[] STOPS_2 = { 12 };

  private final PassThroughPoints subject = new PassThroughPoints(
    List.of(new PassThroughPoint(STOPS_1, "PT1"), new PassThroughPoint(STOPS_2, "PT2"))
  );

  @Test
  void stream() {
    assertEquals(
      "(name: 'PT1', stops: [2, 7, 13]), (name: 'PT2', stops: [12])",
      subject.stream().map(Objects::toString).collect(Collectors.joining(", "))
    );
  }

  @Test
  void isEmpty() {
    assertFalse(subject.isEmpty());
    assertTrue(new PassThroughPoints(List.of()).isEmpty());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = new PassThroughPoints(
      List.of(new PassThroughPoint(STOPS_1, "PT1"), new PassThroughPoint(STOPS_2, "PT2"))
    );
    var other = new PassThroughPoints(
      List.of(new PassThroughPoint(STOPS_1, "PT1"), new PassThroughPoint(STOPS_1, "PT2"))
    );
    assertEquals(same, subject);
    assertNotEquals(other, subject);

    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals(
      "PassThroughPoints{points: [(name: 'PT1', stops: [2, 7, 13]), (name: 'PT2', stops: [12])]}",
      subject.toString()
    );
  }
}
