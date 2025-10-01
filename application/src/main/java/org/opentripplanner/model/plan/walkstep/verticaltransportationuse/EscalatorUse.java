package org.opentripplanner.model.plan.walkstep.verticaltransportationuse;

import javax.annotation.Nullable;

/**
 * Represents information about an escalator stored in
 * {@WalkStep}.
 */
public class EscalatorUse extends InclinedVerticalTransportationUse {

  public EscalatorUse(
    @Nullable Float fromLevel,
    @Nullable String fromLevelName,
    InclineType inclineType,
    @Nullable Float toLevel,
    @Nullable String toLevelName
  ) {
    super(fromLevel, fromLevelName, inclineType, toLevel, toLevelName);
  }
}
