package org.opentripplanner.transit.model.timetable;

import javax.annotation.Nonnull;
import org.opentripplanner.model.Direction;
import org.opentripplanner.model.TripAlteration;
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntityBuilder;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Operator;

public class TripBuilder extends TransitEntityBuilder<Trip, TripBuilder> {

  private Operator operator;
  private Route route;
  private FeedScopedId serviceId;
  private String shortName;
  private TransitMode mode;
  private String netexSubmode;
  private String headsign;
  private FeedScopedId shapeId;
  private Direction direction;
  private BikeAccess bikesAllowed;
  private WheelchairAccessibility wheelchairBoarding;
  private String gtfsBlockId;
  private String gtfsFareId;
  private String netexInternalPlanningCode;
  private TripAlteration netexAlteration = TripAlteration.PLANNED;

  TripBuilder(FeedScopedId id) {
    super(id);
  }

  TripBuilder(Trip original) {
    super(original);
  }

  public Operator getOperator() {
    return operator;
  }

  public TripBuilder setOperator(Operator operator) {
    this.operator = operator;
    return this;
  }

  public Route getRoute() {
    return route;
  }

  public TripBuilder setRoute(Route route) {
    this.route = route;
    return this;
  }

  public FeedScopedId getServiceId() {
    return serviceId;
  }

  public TripBuilder setServiceId(FeedScopedId serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  public String getShortName() {
    return shortName;
  }

  public TripBuilder setShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  public TransitMode getMode() {
    return mode;
  }

  public TripBuilder setMode(TransitMode mode) {
    this.mode = mode;
    return this;
  }

  public String getNetexSubmode() {
    return netexSubmode;
  }

  public TripBuilder setNetexSubmode(String netexSubmode) {
    this.netexSubmode = netexSubmode;
    return this;
  }

  public String getNetexInternalPlanningCode() {
    return netexInternalPlanningCode;
  }

  public TripBuilder setNetexInternalPlanningCode(String netexInternalPlanningCode) {
    this.netexInternalPlanningCode = netexInternalPlanningCode;
    return this;
  }

  public String getHeadsign() {
    return headsign;
  }

  public TripBuilder setHeadsign(String headsign) {
    this.headsign = headsign;
    return this;
  }

  public String getGtfsBlockId() {
    return gtfsBlockId;
  }

  public TripBuilder setGtfsBlockId(String gtfsBlockId) {
    this.gtfsBlockId = gtfsBlockId;
    return this;
  }

  public FeedScopedId getShapeId() {
    return shapeId;
  }

  public TripBuilder setShapeId(FeedScopedId shapeId) {
    this.shapeId = shapeId;
    return this;
  }

  public Direction getDirection() {
    return direction;
  }

  public TripBuilder setDirection(Direction direction) {
    this.direction = direction;
    return this;
  }

  public BikeAccess getBikesAllowed() {
    return bikesAllowed;
  }

  public TripBuilder setBikesAllowed(BikeAccess bikesAllowed) {
    this.bikesAllowed = bikesAllowed;
    return this;
  }

  public WheelchairAccessibility getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  public TripBuilder setWheelchairBoarding(WheelchairAccessibility wheelchairBoarding) {
    this.wheelchairBoarding = wheelchairBoarding;
    return this;
  }

  public TripAlteration getNetexAlteration() {
    return netexAlteration;
  }

  public TripBuilder setNetexAlteration(TripAlteration netexAlteration) {
    this.netexAlteration = netexAlteration;
    return this;
  }

  public String getGtfsFareId() {
    return gtfsFareId;
  }

  public TripBuilder setGtfsFareId(String gtfsFareId) {
    this.gtfsFareId = gtfsFareId;
    return this;
  }

  @Override
  protected Trip buildFromValues() {
    return new Trip(this);
  }

  @Override
  protected void updateLocal(@Nonnull Trip original) {
    this.route = original.getRoute();
    this.operator = original.getOperator();
    this.serviceId = original.getServiceId();
    this.mode = original.getMode();
    this.netexSubmode = original.getNetexSubmode();
    this.shortName = original.getShortName();
    this.headsign = original.getHeadsign();
    this.gtfsBlockId = original.getBlockId();
    this.shapeId = original.getShapeId();
    this.direction = original.getDirection();
    this.bikesAllowed = original.getBikesAllowed();
    this.wheelchairBoarding = original.getWheelchairBoarding();
    this.netexInternalPlanningCode = original.getNetexInternalPlanningCode();
    this.gtfsFareId = original.getFareId();
  }
}
