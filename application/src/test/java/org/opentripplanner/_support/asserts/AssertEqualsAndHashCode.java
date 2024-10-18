package org.opentripplanner._support.asserts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class AssertEqualsAndHashCode {

  private final Object subject;

  @SuppressWarnings("EqualsWithItself")
  public AssertEqualsAndHashCode(Object subject) {
    this.subject = subject;
    assertEquals(subject, subject);
  }

  public static AssertEqualsAndHashCode verify(Object subject) {
    return new AssertEqualsAndHashCode(subject);
  }

  public AssertEqualsAndHashCode sameAs(Object same) {
    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());
    return this;
  }

  public AssertEqualsAndHashCode differentFrom(Object... others) {
    for (Object other : others) {
      assertNotEquals(subject, other);
      assertNotEquals(subject.hashCode(), other.hashCode());
    }
    return this;
  }
}
