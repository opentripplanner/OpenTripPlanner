/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class Entrance extends TransitEntity<FeedScopedId> implements StationElement {

  private static final long serialVersionUID = 1L;

  private FeedScopedId id;

  /**
   * Name of the entrance.
   */
  private String name;

  /** Center point/location for the entrance. */
  private WgsCoordinate coordinate;

  /**
   * Public facing stop code (short text or number).
   */
  private String code;

  /**
   * Additional information about the entrance (if needed).
   */
  private String description;

  /**
   * URL to a web page containing information about this particular entrance.
   */
  private String url;

  private Station parentStation;

  private WheelChairBoarding wheelchairBoarding;

  private String levelName;

  private double levelIndex;

  public Entrance() {}

  public Entrance(FeedScopedId id) {
    this.id = id;
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }

  @Override
  public void setId(FeedScopedId id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getLat() {
    return coordinate == null ? 0 : coordinate.latitude();
  }

  public double getLon() {
    return coordinate == null ? 0 : coordinate.longitude();
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  public void setCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Station getParentStation() {
    return parentStation;
  }

  public void setParentStation(Station parentStation) {
    this.parentStation = parentStation;
  }

  public WheelChairBoarding getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  public void setWheelchairBoarding(WheelChairBoarding wheelchairBoarding) {
    this.wheelchairBoarding = wheelchairBoarding;
  }

  public String getLevelName() {
    return levelName;
  }

  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }

  public double getLevelIndex() {
    return levelIndex;
  }

  public void setLevelIndex(double levelIndex) {
    this.levelIndex = levelIndex;
  }

  @Override
  public String toString() {
    return "<Entrance " + this.id + ">";
  }
}
