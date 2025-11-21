package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

class AccessibilityRequestTest {

  public static final int UNKNOWN_COST = 190;
  public static final int INACCESSIBLE_COST = 350;
  private final AccessibilityRequest subjectOnlyAccessible =
    AccessibilityRequest.ofOnlyAccessible();
  private final AccessibilityRequest subject = AccessibilityRequest.ofCost(
    UNKNOWN_COST,
    INACCESSIBLE_COST
  );

  @Test
  void onlyConsiderAccessible() {
    assertTrue(subjectOnlyAccessible.onlyConsiderAccessible());
    assertFalse(subject.onlyConsiderAccessible());
  }

  @Test
  void unknownCost() {
    assertEquals(UNKNOWN_COST, subject.unknownCost());
  }

  @Test
  void inaccessibleCost() {
    assertEquals(INACCESSIBLE_COST, subject.inaccessibleCost());
  }

  @Test
  void testFactoryMethods() {
    // Return same object if no value is set
    assertSame(AccessibilityRequest.ofOnlyAccessible(), subjectOnlyAccessible);
    assertNotEquals(subject, subjectOnlyAccessible);
    assertNotEquals(subject.hashCode(), subjectOnlyAccessible.hashCode());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = AccessibilityRequest.ofCost(UNKNOWN_COST, INACCESSIBLE_COST);
    var other = AccessibilityRequest.ofCost(2, 2);
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("OnlyConsiderAccessible", subjectOnlyAccessible.toString());
    assertEquals(
      "AccessibilityRequest{unknownCost: $190, inaccessibleCost: $350}",
      subject.toString()
    );
  }
}
