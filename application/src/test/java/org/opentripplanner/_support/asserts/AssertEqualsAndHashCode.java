package org.opentripplanner._support.asserts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class AssertEqualsAndHashCode {

  private static final Object otherClass = "Not the same class";
  private final Object subject;

  @SuppressWarnings("EqualsWithItself")
  public AssertEqualsAndHashCode(Object subject) {
    this.subject = subject;
    assertEquals(subject, subject);
  }

  public static AssertEqualsAndHashCode verify(Object subject) {
    assertEquals(subject, subject);
    assertFalse(subject.equals(otherClass), "Equals should handle other types, and return false");
    assertFalse(subject.equals(null), "Equals should handle null, and return false");
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
