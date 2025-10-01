package org.opentripplanner.model.plan.walkstep.verticaltransportationuse;

import javax.annotation.Nullable;

/**
 * Represents information about inclined vertical transportation equipment stored in
 * {@WalkStep}.
 */
public abstract class InclinedVerticalTransportationUse implements VerticalTransportationUse {

  @Nullable
  private final Double fromLevel;

  @Nullable
  private final String fromLevelName;

  private final InclineType inclineType;

  @Nullable
  private final Double toLevel;

  @Nullable
  private final String toLevelName;

  public InclinedVerticalTransportationUse(
    @Nullable Float fromLevel,
    @Nullable String fromLevelName,
    InclineType inclineType,
    @Nullable Float toLevel,
    @Nullable String toLevelName
  ) {
    this.fromLevel = toNullableDouble(fromLevel);
    this.fromLevelName = fromLevelName;
    this.inclineType = inclineType;
    this.toLevel = toNullableDouble(toLevel);
    this.toLevelName = toLevelName;
  }

  private static Double toNullableDouble(Float f) {
    return f != null ? f.doubleValue() : null;
  }

  public Double fromLevel() {
    return this.fromLevel;
  }

  public String fromLevelName() {
    return this.fromLevelName;
  }

  public InclineType inclineType() {
    return this.inclineType;
  }

  public Double toLevel() {
    return this.toLevel;
  }

  public String toLevelName() {
    return this.toLevelName;
  }
}
