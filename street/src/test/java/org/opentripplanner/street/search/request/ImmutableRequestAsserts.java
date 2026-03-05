package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ImmutableRequestAsserts {

  public static <T> void assertEqualsAndHashCode(T subject, T other, T same) {
    assertEqualsAndHashCode(subject, same);
    assertNotEqualsAndHashCode(subject, other);
  }

  public static <T> void assertEqualsAndHashCode(T subject, T same) {
    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());
  }

  public static <T> void assertNotEqualsAndHashCode(T subject, T other) {
    assertNotEquals(subject, other);
    assertNotEquals(subject.hashCode(), other.hashCode());
  }
}
