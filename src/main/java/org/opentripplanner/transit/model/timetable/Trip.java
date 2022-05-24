/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.timetable;

import static java.util.Objects.requireNonNullElse;
import static org.opentripplanner.util.lang.ObjectUtils.ifNotNull;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.opentripplanner.model.Direction;
import org.opentripplanner.model.TripAlteration;
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity2;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.util.lang.StringUtils;

public final class Trip extends TransitEntity2<Trip, TripBuilder> {

  private final Operator operator;
  private final Route route;
  private final FeedScopedId serviceId;
  private final String shortName;
  private final TransitMode mode;
  private final String netexSubmode;
  private final String headsign;

  // TODO RT - Fix this after the Transmodel is refactored
  // The shapeId is mutable to clear the reference in case the shape do not exist.
  // This happens late in the graph-build process, but we can not create a new object
  // because there are other objects witch may reference this particular instance.
  private FeedScopedId shapeId;

  private final Direction direction;
  private final BikeAccess bikesAllowed;
  private final WheelchairAccessibility wheelchairBoarding;

  private final String gtfsBlockId;
  private final String gtfsFareId;

  private final String netexInternalPlanningCode;
  private final TripAlteration netexAlteration;

  Trip(TripBuilder builder) {
    super(builder.getId());
    // Route is done first, it is used as fallback in many cases
    this.route = builder.getRoute();

    this.operator = ifNotNull(builder.getOperator(), route.getOperator());
    this.serviceId = builder.getServiceId();
    this.shortName = builder.getShortName();
    this.mode = requireNonNullElse(builder.getMode(), route.getMode());
    this.netexSubmode = ifNotNull(builder.getNetexSubmode(), route.getNetexSubmode());
    this.headsign = builder.getHeadsign();
    this.shapeId = builder.getShapeId();
    this.direction = requireNonNullElse(builder.getDirection(), Direction.UNKNOWN);
    this.bikesAllowed = requireNonNullElse(builder.getBikesAllowed(), route.getBikesAllowed());
    this.wheelchairBoarding =
      requireNonNullElse(builder.getWheelchairBoarding(), WheelchairAccessibility.NO_INFORMATION);
    this.gtfsBlockId = builder.getGtfsBlockId();
    this.gtfsFareId = builder.getGtfsFareId();
    this.netexInternalPlanningCode = builder.getNetexInternalPlanningCode();
    this.netexAlteration = requireNonNullElse(builder.getNetexAlteration(), TripAlteration.PLANNED);
  }

  public static TripBuilder of(FeedScopedId id) {
    return new TripBuilder(id);
  }

  /**
   * Operator running the trip. Returns operator of this trip, if it exist, or else the route
   * operator.
   */
  @Nonnull
  public Operator getOperator() {
    return operator;
  }

  @Nonnull
  public Route getRoute() {
    return route;
  }

  /**
   * At the moment this is probably null-safe, but we want to reduce this to GTFS serviceId for
   * matching trips in GTFS RT updates, if the datasource is NeTEx, then we do not have this and
   * would like to remove todays generated ID.
   */
  @Nullable
  public FeedScopedId getServiceId() {
    return serviceId;
  }

  /**
   * Public code or identifier for the journey. Equal to NeTEx PublicCode. GTFS and NeTEx have
   * additional constraints on this fields that are not enforced in OTP.
   */
  public String getShortName() {
    return shortName;
  }

  @Nonnull
  public TransitMode getMode() {
    return mode;
  }

  @Nullable
  public String getNetexSubmode() {
    return netexSubmode;
  }

  public String getHeadsign() {
    return headsign;
  }

  public FeedScopedId getShapeId() {
    return shapeId;
  }

  /**
   * Clear the shapeId if the shape is missing from the data. There is no way to set the
   * shape again, so use this carefully.
   */
  public void deleteShapeId() {
    shapeId = null;
  }

  /**
   * The direction for this Trip (and all other Trips in this TripPattern).
   */
  @NotNull
  public Direction getDirection() {
    return direction;
  }

  public String getGtfsDirectionIdAsString(String unknownValue) {
    return direction.equals(Direction.UNKNOWN)
      ? unknownValue
      : Integer.toString(direction.gtfsCode);
  }

  public BikeAccess getBikesAllowed() {
    return bikesAllowed;
  }

  public WheelchairAccessibility getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  public String getBlockId() {
    return gtfsBlockId;
  }

  /** Custom extension for KCM to specify a fare per-trip */
  public String getFareId() {
    return gtfsFareId;
  }

  /**
   * Internal code (non-public identifier) for the journey (e.g. train- or trip number from the
   * planners' tool). This is kept to ensure compatibility with legacy planning systems. In NeTEx
   * this maps to privateCode, there is no GTFS equivalent.
   */
  public String getNetexInternalPlanningCode() {
    return netexInternalPlanningCode;
  }

  /**
   * Default alteration for a trip.
   * <p>
   * This is planned, by default (e.g. GTFS and if not set explicit).
   */
  public TripAlteration getTripAlteration() {
    return netexAlteration;
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

  @Override
  public boolean sameValue(@Nonnull Trip other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(this.operator, other.operator) &&
      Objects.equals(this.route, other.route) &&
      Objects.equals(this.shortName, other.shortName) &&
      Objects.equals(this.mode, other.mode) &&
      Objects.equals(this.netexSubmode, other.netexSubmode) &&
      Objects.equals(this.serviceId, other.serviceId) &&
      Objects.equals(this.netexInternalPlanningCode, other.netexInternalPlanningCode) &&
      Objects.equals(this.headsign, other.headsign) &&
      Objects.equals(this.gtfsBlockId, other.gtfsBlockId) &&
      Objects.equals(this.shapeId, other.shapeId) &&
      Objects.equals(this.direction, other.direction) &&
      Objects.equals(this.bikesAllowed, other.bikesAllowed) &&
      Objects.equals(this.wheelchairBoarding, other.wheelchairBoarding) &&
      Objects.equals(this.netexAlteration, other.netexAlteration) &&
      Objects.equals(this.gtfsFareId, other.gtfsFareId)
    );
  }

  @Override
  public TripBuilder copy() {
    return new TripBuilder(this);
  }
}
