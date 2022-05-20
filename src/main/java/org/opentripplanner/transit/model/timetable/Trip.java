/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.timetable;

import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.opentripplanner.model.Direction;
import org.opentripplanner.model.TripAlteration;
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.util.lang.StringUtils;

public final class Trip extends TransitEntity {

  private Operator operator;
  private Route route;
  private String shortName;
  private TransitMode mode;
  private String netexSubmode;
  private FeedScopedId serviceId;
  private String netexInternalPlanningCode;
  private String headsign;
  private String blockId;
  private FeedScopedId shapeId;

  @NotNull
  private Direction direction = Direction.UNKNOWN;

  private WheelchairAccessibility wheelchairBoarding = WheelchairAccessibility.NO_INFORMATION;
  private BikeAccess bikesAllowed = BikeAccess.UNKNOWN;
  private TripAlteration alteration = TripAlteration.PLANNED;

  private String fareId;

  public Trip(FeedScopedId id) {
    super(id);
  }

  public Trip(Trip obj) {
    this(obj.getId());
    this.route = obj.route;
    this.operator = obj.operator;
    this.serviceId = obj.serviceId;
    this.mode = obj.mode;
    this.netexSubmode = obj.netexSubmode;
    this.shortName = obj.shortName;
    this.headsign = obj.headsign;
    this.direction = obj.direction;
    this.blockId = obj.blockId;
    this.shapeId = obj.shapeId;
    this.wheelchairBoarding = obj.wheelchairBoarding;
    this.bikesAllowed = obj.bikesAllowed;
    this.fareId = obj.fareId;
  }

  /**
   * Operator running the trip. Returns operator of this trip, if it exist, or else the route
   * operator.
   */
  public Operator getOperator() {
    return operator != null ? operator : route.getOperator();
  }

  /**
   * This method return the operator associated with the trip. If the Trip have no Operator set
   * {@code null} is returned. Note! this method do not consider the {@link Route} that the trip is
   * part of.
   *
   * @see #getOperator()
   */
  public Operator getTripOperator() {
    return operator;
  }

  public void setTripOperator(Operator operator) {
    this.operator = operator;
  }

  public Route getRoute() {
    return route;
  }

  public void setRoute(Route route) {
    this.route = route;
  }

  public FeedScopedId getServiceId() {
    return serviceId;
  }

  public void setServiceId(FeedScopedId serviceId) {
    this.serviceId = serviceId;
  }

  public TransitMode getMode() {
    return mode == null ? getRoute().getMode() : mode;
  }

  public void setMode(TransitMode mode) {
    this.mode = mode.equals(getRoute().getMode()) ? null : mode;
  }

  public String getNetexSubmode() {
    return netexSubmode == null ? getRoute().getNetexSubmode() : netexSubmode;
  }

  public void setNetexSubmode(String netexSubmode) {
    this.netexSubmode =
      netexSubmode == null || netexSubmode.equals(getRoute().getNetexSubmode())
        ? null
        : netexSubmode;
  }

  /**
   * Public code or identifier for the journey. Equal to NeTEx PublicCode. GTFS and NeTEx have
   * additional constraints on this fields that are not enforced in OTP.
   */
  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  /**
   * Return human friendly short info to identify the trip when mode, from/to stop and times are
   * known. This method is meant for debug/logging, and should not be exposed in any API.
   */
  public String logInfo() {
    if (StringUtils.hasValue(shortName)) {
      return shortName;
    }
    if (route != null && StringUtils.hasValue(route.getName())) {
      return route.getName();
    }
    if (StringUtils.hasValue(headsign)) {
      return headsign;
    }
    return getId().getId();
  }

  /**
   * Internal code (non-public identifier) for the journey (e.g. train- or trip number from the
   * planners' tool). This is kept to ensure compatibility with legacy planning systems. In NeTEx
   * this maps to privateCode, there is no GTFS equivalent.
   */
  public String getNetexInternalPlanningCode() {
    return netexInternalPlanningCode;
  }

  public void setNetexInternalPlanningCode(String netexInternalPlanningCode) {
    this.netexInternalPlanningCode = netexInternalPlanningCode;
  }

  public String getHeadsign() {
    return headsign;
  }

  public void setHeadsign(String headsign) {
    this.headsign = headsign;
  }

  // TODO Consider moving this to the TripPattern class once we have refactored the transit model

  /**
   * The direction for this Trip (and all other Trips in this TripPattern).
   */
  @NotNull
  public Direction getDirection() {
    return direction;
  }

  public void setDirection(Direction direction) {
    // Enforce non-null
    this.direction = direction != null ? direction : Direction.UNKNOWN;
  }

  public String getGtfsDirectionIdAsString(String unknownValue) {
    return direction.equals(Direction.UNKNOWN)
      ? unknownValue
      : Integer.toString(direction.gtfsCode);
  }

  public String getBlockId() {
    return blockId;
  }

  public void setBlockId(String blockId) {
    this.blockId = blockId;
  }

  public FeedScopedId getShapeId() {
    return shapeId;
  }

  public void setShapeId(FeedScopedId shapeId) {
    this.shapeId = shapeId;
  }

  public WheelchairAccessibility getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  public void setWheelchairBoarding(WheelchairAccessibility boarding) {
    this.wheelchairBoarding =
      Objects.requireNonNullElse(boarding, WheelchairAccessibility.NO_INFORMATION);
  }

  public BikeAccess getBikesAllowed() {
    return bikesAllowed;
  }

  public void setBikesAllowed(BikeAccess bikesAllowed) {
    this.bikesAllowed = bikesAllowed;
  }

  public String toString() {
    return "<Trip " + getId() + ">";
  }

  /** Custom extension for KCM to specify a fare per-trip */
  public String getFareId() {
    return fareId;
  }

  public void setFareId(String fareId) {
    this.fareId = fareId;
  }

  /**
   * Default alteration for a trip.
   * <p>
   * This is planned, by default (e.g. GTFS and if not set explicit).
   */
  public TripAlteration getTripAlteration() {
    return alteration;
  }

  public void setAlteration(TripAlteration tripAlteration) {
    if (tripAlteration != null) {
      this.alteration = tripAlteration;
    }
  }
}
