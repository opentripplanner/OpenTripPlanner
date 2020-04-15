package org.opentripplanner.model;

/**
 * Acts as the supertype for all entities, except stations, created from the GTFS stops table.
 * Most of the fileds are shared between the types, and eg. in pathways the namespace any of them
 * can be used as from and to.
 * */
public abstract class StationElement extends TransitEntity<FeedScopedId>  {

  protected FeedScopedId id;

  private String name;

  private String code;

  private String description;

  protected WgsCoordinate coordinate;

  private WheelChairBoarding wheelchairBoarding;

  private String levelName;

  private double levelIndex;

  private Station parentStation;

  @Override public FeedScopedId getId() {
      return id;
  }

  @Override public void setId(FeedScopedId id) {
      this.id = id;
  }

  /**
   * Name of the station element if provided.
   */
  public String getName() {
      return name;
  }

  public void setName(String name) {
      this.name = name;
  }

  /**
   * Public facing stop code (short text or number).
   */
  public String getCode() {
      return code;
  }

  public void setCode(String code) {
      this.code = code;
  }

  /**
   * Additional information about the station element (if needed).
   */
  public String getDescription() {
      return description;
  }

  public void setDescription(String description) {
      this.description = description;
  }

  /**
   * Center point/location for the station element. Returns the coordinate of the parent station,
   * if the coordinate is not defined for this station element.
   */
  public WgsCoordinate getCoordinate() {
    return coordinate != null ? coordinate : parentStation.getCoordinate();
  }

  public void setCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
  }

  public double getLat() {
    return getCoordinate().latitude();
  }

  public double getLon() {
    return getCoordinate().longitude();
  }

  /**
   * Returns whether this station element is accessible for wheelchair users.
   * */
  public WheelChairBoarding getWheelchairBoarding() {
      return wheelchairBoarding;
  }

  public void setWheelchairBoarding(WheelChairBoarding wheelchairBoarding) {
      this.wheelchairBoarding = wheelchairBoarding;
  }

  /** Level name for elevator descriptions */
  public String getLevelName() {
      return levelName;
  }

  public void setLevelName(String levelName) {
      this.levelName = levelName;
  }

  /** Level index for hop counts in elevators */
  public double getLevelIndex() {
      return levelIndex;
  }

  public void setLevelIndex(double levelIndex) {
    this.levelIndex = levelIndex;
  }

  /** Parent station for the station element */
  public Station getParentStation() {
    return parentStation;
  }

  public void setParentStation(Station parentStation) {
    this.parentStation = parentStation;
  }
}
