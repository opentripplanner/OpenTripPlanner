package org.opentripplanner.routing.api.request;

public class WheelchairAccessibilityFeature {

  private static final int NOT_SET = -1;

  private static final WheelchairAccessibilityFeature ONLY_CONSIDER_ACCESSIBLE = new WheelchairAccessibilityFeature(
    true,
    NOT_SET,
    NOT_SET
  );

  private final boolean onlyConsiderAccessible;
  private final int unknownCost;
  private final int inaccessibleCost;

  private WheelchairAccessibilityFeature(
    boolean onlyConsiderAccessible,
    int unknownCost,
    int inaccessibleCost
  ) {
    this.onlyConsiderAccessible = onlyConsiderAccessible;
    this.unknownCost = unknownCost;
    this.inaccessibleCost = inaccessibleCost;
  }

  public static WheelchairAccessibilityFeature ofOnlyAccessible() {
    return ONLY_CONSIDER_ACCESSIBLE;
  }

  public static WheelchairAccessibilityFeature ofCost(int unknownCost, int inaccessibleCost) {
    return new WheelchairAccessibilityFeature(false, unknownCost, inaccessibleCost);
  }

  public boolean onlyConsiderAccessible() {
    return onlyConsiderAccessible;
  }

  public int unknownCost() {
    return unknownCost;
  }

  public int inaccessibleCost() {
    return inaccessibleCost;
  }
}
