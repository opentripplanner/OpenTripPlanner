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
) {
  public VerticalTransportationUse(
    @Nullable Float fromLevel,
    @Nullable String fromLevelName,
    InclineType inclineType,
    @Nullable Float toLevel,
    @Nullable String toLevelName
  ) {
    this(
      toNullableDouble(fromLevel),
      fromLevelName,
      inclineType,
      toNullableDouble(toLevel),
      toLevelName
    );
  }

  private static Double toNullableDouble(Float f) {
    return f != null ? f.doubleValue() : null;
  }
}
