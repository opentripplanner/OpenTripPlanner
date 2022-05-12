package org.opentripplanner.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.util.I18NString;

/**
 * Acts as the supertype for all entities, except stations, created from the GTFS stops table. Most
 * of the fields are shared between the types, and eg. in pathways the namespace any of them can be
 * used as from and to.
 */
public abstract class StationElement extends TransitEntity {

  private final I18NString name;

  private final String code;

  private final I18NString description;

  private final WgsCoordinate coordinate;

  private final WheelchairBoarding wheelchairBoarding;

  private final StopLevel level;

  private Station parentStation;

  public StationElement(
    FeedScopedId id,
    I18NString name,
    String code,
    I18NString description,
    WgsCoordinate coordinate,
    WheelchairBoarding wheelchairBoarding,
    StopLevel level
  ) {
    super(id);
    this.name = name;
    this.code = code;
    this.description = description;
    this.coordinate = coordinate;
    this.wheelchairBoarding =
      Objects.requireNonNullElse(wheelchairBoarding, WheelchairBoarding.NO_INFORMATION);
    this.level = level;
  }

  /**
   * Name of the station element if provided.
   */
  @Nonnull
  public I18NString getName() {
    return name;
  }

  /**
   * Public facing stop code (short text or number).
   */
  public String getCode() {
    return code;
  }

  /**
   * Additional information about the station element (if needed).
   */
  public I18NString getDescription() {
    return description;
  }

  /**
   * Center point/location for the station element. Returns the coordinate of the parent station, if
   * the coordinate is not defined for this station element.
   */
  public WgsCoordinate getCoordinate() {
    if (coordinate != null) {
      return coordinate;
    }
    if (parentStation != null) {
      return parentStation.getCoordinate();
    }
    throw new IllegalStateException("Coordinate not set for: " + toString());
  }

  /**
   * Returns whether this station element is accessible for wheelchair users.
   */
  @Nonnull
  public WheelchairBoarding getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  /** Level name for elevator descriptions */
  public String getLevelName() {
    return level == null ? null : level.getName();
  }

  /** Level index for hop counts in elevators. Is {@code null} if not set. */
  public Double getLevelIndex() {
    return level == null ? null : level.getIndex();
  }

  /** Parent station for the station element */
  public Station getParentStation() {
    return parentStation;
  }

  public void setParentStation(Station parentStation) {
    this.parentStation = parentStation;
  }

  /** Return {@code true} if this stop (element) is part of a station, have a parent station. */
  public boolean isPartOfStation() {
    return parentStation != null;
  }

  /**
   * Return {@code true} if this stop (element) has the same parent station as the other stop
   * (element).
   */
  public boolean isPartOfSameStationAs(StopLocation other) {
    if (other == null) {
      return false;
    }

    return isPartOfStation() && parentStation.equals(other.getParentStation());
  }

  /**
   * The coordinate for the given stop element exist. The {@link #getCoordinate()} will use the
   * parent station coordinate if not set, but this method will return based on this instance; Hence
   * the {@link #getCoordinate()} might return a coordinate, while this method return {@code
   * false}.
   */
  boolean isCoordinateSet() {
    return coordinate != null;
  }
}
