package org.opentripplanner.transit.transfer.regular.api.ways;

import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.core.model.doc.DocumentedEnum;

/// This enum is used to indicate the user's preference regarding the use of a specific street segment type.
public enum StreetSegmentTypeUsage
  implements DocumentedEnum<StreetSegmentTypeUsage>, NormalizedDistance<StreetSegmentTypeUsage>
{
  ALLOWED(
    "This value indicates that the user has no preference regarding the use of the street segment type."
  ),
  DISCOURAGED(
    "This value indicates that the user can use this but would prefer other alternatives. " +
      "A small time penalty and/or a moderate generalized cost are added."
  ),
  FORBIDDEN(
    "This value indicates that the user does not want this at all, unless no other option exists." +
      "A time penalty and a large generalized cost are added."
  );

  private final String description;

  StreetSegmentTypeUsage(String description) {
    this.description = description;
  }

  @Override
  public float normalizedDistance(StreetSegmentTypeUsage other) {
    return NormalizedDistance.calculateNormalizedDistance(this, other);
  }

  @Override
  public String typeDescription() {
    return (
      "This is used to indicate the user's preference regarding the use of a specific street segment type, " +
      "like stairs, escalators, or elevators."
    );
  }

  @Override
  public String enumValueDescription() {
    return description;
  }
}
