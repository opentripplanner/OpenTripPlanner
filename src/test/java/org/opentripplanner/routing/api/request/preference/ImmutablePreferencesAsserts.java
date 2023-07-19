package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ImmutablePreferencesAsserts {

  public static <T> void assertEqualsAndHashCode(T subject, T other, T same) {
    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());
    assertNotEquals(subject, other);
    assertNotEquals(subject.hashCode(), other.hashCode());
  }
}
