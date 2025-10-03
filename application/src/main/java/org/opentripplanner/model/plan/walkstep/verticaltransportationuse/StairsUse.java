package org.opentripplanner.model.plan.walkstep.verticaltransportationuse;

import javax.annotation.Nullable;

/**
 * Represents information about a set of stairs stored in
 * {@WalkStep}.
 */
public class StairsUse extends InclinedVerticalTransportationUse {

  public StairsUse(
    @Nullable Double fromLevel,
    @Nullable String fromLevelName,
    InclineType inclineType,
    @Nullable Double toLevel,
    @Nullable String toLevelName
  ) {
    super(fromLevel, fromLevelName, inclineType, toLevel, toLevelName);
  }
}
