package org.opentripplanner.routing.api.request.preference;

import java.util.Objects;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.framework.Units;

/**
 * Preferences for how to treat trips or stops with accessibility restrictions, like wheelchair
 * accessibility.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class AccessibilityPreferences {

  /**
   * Set the unknown cost to a very high number, so in case it is used accidentally it
   * will not cause any harm.
   */
  private static final int NOT_SET = 9_999_999;

  private static final AccessibilityPreferences ONLY_CONSIDER_ACCESSIBLE = new AccessibilityPreferences(
    true,
    NOT_SET,
    NOT_SET
  );

  private final boolean onlyConsiderAccessible;
  private final int unknownCost;
  private final int inaccessibleCost;

  private AccessibilityPreferences(
    boolean onlyConsiderAccessible,
    int unknownCost,
    int inaccessibleCost
  ) {
    this.onlyConsiderAccessible = onlyConsiderAccessible;
    this.unknownCost = Units.cost(unknownCost);
    this.inaccessibleCost = Units.cost(inaccessibleCost);
  }

  /**
   * Create a feature which only considers wheelchair-accessible trips/stops.
   */
  public static AccessibilityPreferences ofOnlyAccessible() {
    return ONLY_CONSIDER_ACCESSIBLE;
  }

  /**
   * Create a feature which considers trips/stops that don't have an accessibility of POSSIBLE.
   */
  public static AccessibilityPreferences ofCost(int unknownCost, int inaccessibleCost) {
    return new AccessibilityPreferences(false, unknownCost, inaccessibleCost);
  }

  /**
   * Whether to include trips or stops which don't have a wheelchair accessibility of POSSIBLE
   */
  public boolean onlyConsiderAccessible() {
    return onlyConsiderAccessible;
  }

  /**
   * The extra cost to add when using a trip/stop which has an accessibility of UNKNOWN.
   */
  public int unknownCost() {
    return unknownCost;
  }

  /**
   * The extra cost to add when using a trip/stop which has an accessibility of NOT_POSSIBLE.
   */
  public int inaccessibleCost() {
    return inaccessibleCost;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessibilityPreferences that = (AccessibilityPreferences) o;
    return (
      onlyConsiderAccessible == that.onlyConsiderAccessible &&
      unknownCost == that.unknownCost &&
      inaccessibleCost == that.inaccessibleCost
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(onlyConsiderAccessible, unknownCost, inaccessibleCost);
  }

  @Override
  public String toString() {
    if (onlyConsiderAccessible) {
      return "OnlyConsiderAccessible";
    }

    return ToStringBuilder
      .of(AccessibilityPreferences.class)
      .addCost("unknownCost", unknownCost, NOT_SET)
      .addCost("inaccessibleCost", inaccessibleCost, NOT_SET)
      .toString();
  }
}
