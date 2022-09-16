package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessibilityPreferencesTest {

  public static final int UNKNOWN_COST = 190;
  public static final int INACCESSIBLE_COST = 350;
  private final AccessibilityPreferences subjectOnlyAccessible = AccessibilityPreferences.ofOnlyAccessible();
  private final AccessibilityPreferences subject = AccessibilityPreferences.ofCost(
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
  void testEqualsAndHashCode() {
    assertSame(AccessibilityPreferences.ofOnlyAccessible(), subjectOnlyAccessible);
    assertNotEquals(subject, subjectOnlyAccessible);
    assertNotEquals(subject.hashCode(), subjectOnlyAccessible.hashCode());

    var same = AccessibilityPreferences.ofCost(UNKNOWN_COST, INACCESSIBLE_COST);
    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());

    var other = AccessibilityPreferences.ofCost(2, 2);
    assertNotEquals(other, subjectOnlyAccessible);
    assertNotEquals(other.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("OnlyConsiderAccessible", subjectOnlyAccessible.toString());
    assertEquals(
      "AccessibilityPreferences{unknownCost: $190, inaccessibleCost: $350}",
      subject.toString()
    );
  }
}
