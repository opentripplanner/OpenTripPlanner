package org.opentripplanner.model.plan.walkstep;

import javax.annotation.Nullable;

/**
 * Represents information about vertical transportation equipment stored in
 * {@WalkStep}.
 */
public record VerticalTransportationUse(
  @Nullable Double fromLevel,
  @Nullable String fromLevelName,
  InclineType inclineType,
  @Nullable Double toLevel,
  @Nullable String toLevelName
) {}
