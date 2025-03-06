package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

class AccessibilityPreferencesTest {

  public static final int UNKNOWN_COST = 190;
  public static final int INACCESSIBLE_COST = 350;
  private final AccessibilityPreferences subjectOnlyAccessible =
    AccessibilityPreferences.ofOnlyAccessible();
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
  void testFactoryMethods() {
    // Return same object if no value is set
    assertSame(AccessibilityPreferences.ofOnlyAccessible(), subjectOnlyAccessible);
    assertNotEquals(subject, subjectOnlyAccessible);
    assertNotEquals(subject.hashCode(), subjectOnlyAccessible.hashCode());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = AccessibilityPreferences.ofCost(UNKNOWN_COST, INACCESSIBLE_COST);
    var other = AccessibilityPreferences.ofCost(2, 2);
    assertEqualsAndHashCode(subject, other, same);
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
