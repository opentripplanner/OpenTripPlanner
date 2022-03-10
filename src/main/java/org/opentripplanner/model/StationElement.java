package org.opentripplanner.model;

import javax.annotation.Nonnull;

/**
 * Acts as the supertype for all entities, except stations, created from the GTFS stops table. Most
 * of the fields are shared between the types, and eg. in pathways the namespace any of them can be
 * used as from and to.
 */
public abstract class StationElement extends TransitEntity {

  private final String name;

  private final String code;

  private final String description;

  private final WgsCoordinate coordinate;

  private final WheelChairBoarding wheelchairBoarding;

  private final StopLevel level;

  private Station parentStation;

  public StationElement(
      FeedScopedId id,
      String name,
      String code,
      String description,
      WgsCoordinate coordinate,
      WheelChairBoarding wheelchairBoarding,
      StopLevel level
  ) {
    super(id);
    this.name = name;
    this.code = code;
    this.description = description;
    this.coordinate = coordinate;
    this.wheelchairBoarding = wheelchairBoarding;
    this.level = level;
  }

  /**
   * Name of the station element if provided.
   */
  @Nonnull
  public String getName() {
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
  public String getDescription() {
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
   * The coordinate for the given stop element exist. The {@link #getCoordinate()}
   * will use the parent station coordinate if not set, but this method will return
   * based on this instance; Hence the {@link #getCoordinate()} might return a coordinate,
   * while this method return {@code false}.
   */
  boolean isCoordinateSet() {
    return coordinate != null;
  }

  /**
   * Returns whether this station element is accessible for wheelchair users.
   */
  public WheelChairBoarding getWheelchairBoarding() {
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

  public void setParentStation(Station parentStation) {
    this.parentStation = parentStation;
  }
}
