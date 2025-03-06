/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.timetable;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.utils.lang.StringUtils;

/**
 * A Trip represents the movement of a public transport vehicle on a given {@link Route}, using a
 * given {@link TransitMode}, on a given sequence of stops served at given passing times.
 * <p>A scheduled Trip can run at most once per service date,
 * while a frequency-based Trip runs several times on a given service date.
 * <p>A Trip can run on multiple service dates.
 * <p>The service dates on which a trip is running are identified
 * by its service id and can be looked up with
 * {@link org.opentripplanner.model.calendar.CalendarService}.
 * <p>Trips that follow the same sequence of stops are grouped under a {@link org.opentripplanner.transit.model.network.TripPattern}
 * via a {@link org.opentripplanner.model.Timetable}
 * <p>A Trip is equivalent to the TransModel concept of SERVICE JOURNEY.
 */
public final class Trip extends AbstractTransitEntity<Trip, TripBuilder> implements LogInfo {

  private final Route route;
  private final TransitMode mode;
  private final Direction direction;
  private final BikeAccess bikesAllowed;
  private final CarAccess carsAllowed;
  private final Accessibility wheelchairBoarding;

  private final SubMode netexSubmode;
  private final TripAlteration netexAlteration;

  @Nullable
  private final Operator operator;

  @Nullable
  private final FeedScopedId serviceId;

  @Nullable
  private final String shortName;

  @Nullable
  private final I18NString headsign;

  @Nullable
  private final FeedScopedId shapeId;

  @Nullable
  private final String gtfsBlockId;

  @Nullable
  private final String netexInternalPlanningCode;

  Trip(TripBuilder builder) {
    super(builder.getId());
    // Required fields
    // Route is done first, it is used as a fallback for some fields
    this.route = requireNonNull(builder.getRoute());
    this.mode = requireNonNullElse(builder.getMode(), route.getMode());
    this.netexSubmode = builder.getNetexSubmode() != null
      ? SubMode.getOrBuildAndCacheForever(builder.getNetexSubmode())
      : route.getNetexSubmode();
    this.direction = requireNonNullElse(builder.getDirection(), Direction.UNKNOWN);
    this.bikesAllowed = requireNonNullElse(builder.getBikesAllowed(), route.getBikesAllowed());
    this.carsAllowed = requireNonNullElse(builder.getCarsAllowed(), CarAccess.UNKNOWN);
    this.wheelchairBoarding = requireNonNullElse(
      builder.getWheelchairBoarding(),
      Accessibility.NO_INFORMATION
    );
    this.netexAlteration = requireNonNullElse(builder.getNetexAlteration(), TripAlteration.PLANNED);

    // Optional fields

    this.operator = ifNotNull(builder.getOperator(), route.getOperator());
    this.serviceId = builder.getServiceId();
    this.shortName = builder.getShortName();
    this.headsign = builder.getHeadsign();
    this.shapeId = builder.getShapeId();
    this.gtfsBlockId = builder.getGtfsBlockId();
    this.netexInternalPlanningCode = builder.getNetexInternalPlanningCode();
  }

  public static TripBuilder of(FeedScopedId id) {
    return new TripBuilder(id);
  }

  /**
   * Operator running the trip. Returns operator of this trip, if it exist, or else the route
   * operator.
   */
  @Nullable
  public Operator getOperator() {
    return operator;
  }

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
  @Nullable
  public String getShortName() {
    return shortName;
  }

  public TransitMode getMode() {
    return mode;
  }

  public SubMode getNetexSubMode() {
    return netexSubmode;
  }

  @Nullable
  public I18NString getHeadsign() {
    return headsign;
  }

  @Nullable
  public FeedScopedId getShapeId() {
    return shapeId;
  }

  /**
   * The direction for this Trip (and all other Trips in this TripPattern).
   */
  public Direction getDirection() {
    return direction;
  }

  public BikeAccess getBikesAllowed() {
    return bikesAllowed;
  }

  public CarAccess getCarsAllowed() {
    return carsAllowed;
  }

  public Accessibility getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  @Nullable
  public String getGtfsBlockId() {
    return gtfsBlockId;
  }

  /**
   * Internal code (non-public identifier) for the journey (e.g. train- or trip number from the
   * planners' tool). This is kept to ensure compatibility with legacy planning systems. In NeTEx
   * this maps to privateCode, there is no GTFS equivalent.
   */
  @Nullable
  public String getNetexInternalPlanningCode() {
    return netexInternalPlanningCode;
  }

  /**
   * Default alteration for a trip.
   * <p>
   * This is planned, by default (e.g. GTFS and if not set explicit).
   */
  public TripAlteration getNetexAlteration() {
    return netexAlteration;
  }

  /**
   * Return human friendly name to identify the trip when mode, from/to stop and times are
   * known. This method is meant for debug/logging, and should not be exposed in any API.
   */
  public String logName() {
    if (StringUtils.hasValue(shortName)) {
      return shortName;
    }
    if (StringUtils.hasValue(route.getName())) {
      return route.getName();
    }
    if (I18NString.hasValue(headsign)) {
      return headsign.toString();
    }
    return mode.name();
  }

  @Override
  public boolean sameAs(Trip other) {
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
      Objects.equals(this.carsAllowed, other.carsAllowed) &&
      Objects.equals(this.wheelchairBoarding, other.wheelchairBoarding) &&
      Objects.equals(this.netexAlteration, other.netexAlteration)
    );
  }

  @Override
  public TripBuilder copy() {
    return new TripBuilder(this);
  }
}
