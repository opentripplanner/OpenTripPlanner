package org.opentripplanner.model;

/**
 * Acts as the supertype for all entities, except stations, created from the GTFS stops table.
 * Most of the fileds are shared between the types, and eg. in pathways the namespace any of them
 * can be used as from and to.
 * */
public abstract class StationElement extends TransitEntity<FeedScopedId>  {

  protected FeedScopedId id;

  /**
   * Name of the station element if provided.
   */
  private String name;

  /**
   * Public facing stop code (short text or number).
   */
  private String code;

  /**
   * Additional information about the station element (if needed).
   */
  private String description;

  /** Center point/location for the station element. */
  protected WgsCoordinate coordinate;

  private WheelChairBoarding wheelchairBoarding;

  /** Level name for elevator descriptions */
  private String levelName;

  /** Level index for hop counts in elevators */
  private double levelIndex;

  /** Parent station for the station element*/
  private Station parentStation;

  @Override public FeedScopedId getId() {
      return id;
  }

  @Override public void setId(FeedScopedId id) {
      this.id = id;
  }

  public String getName() {
      return name;
  }

  public void setName(String name) {
      this.name = name;
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

  public Station getParentStation() {
    return parentStation;
  }

  public void setParentStation(Station parentStation) {
    this.parentStation = parentStation;
  }
}
