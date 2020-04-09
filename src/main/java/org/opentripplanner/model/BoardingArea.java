/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

/**
 * A place along a platform, where the vehicle van be boarded. Equivalent to GTFS stop location.
 */
public final class BoardingArea extends TransitEntity<FeedScopedId> implements StationElement {

  private static final long serialVersionUID = 1L;

  private FeedScopedId id;

  /**
   * Name of the node.
   */
  private String name;

  private double lat;

  private double lon;

  /**
   * Public facing stop code (short text or number).
   */
  private String code;

  /**
   * Additional information about the node (if needed).
   */
  private String description;

  /**
   * URL to a web page containing information about this particular node.
   */
  private String url;

  private Stop parentStop;

  private WheelChairBoarding wheelchairBoarding;

  private String levelName;

  private double levelIndex;

  public BoardingArea() {}

  public BoardingArea(FeedScopedId id) {
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
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
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

  public Stop getParentStop() {
    return parentStop;
  }

  public void setParentStop(Stop parentStop) {
    this.parentStop = parentStop;
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
