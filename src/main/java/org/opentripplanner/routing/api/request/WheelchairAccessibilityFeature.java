package org.opentripplanner.routing.api.request;

public record WheelchairAccessibilityFeature(
  boolean onlyConsiderAccessible,
  int unknownCost,
  int inaccessibleCost
) {}
