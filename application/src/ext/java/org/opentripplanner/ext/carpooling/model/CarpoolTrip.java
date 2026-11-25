package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.organization.ContactInfo;

/**
 * A driver's journey that passengers can join: the stops of the planned route, the capacity of the
 * car and, per stop, how much later than scheduled the driver may arrive.
 * <p>
 * Trips are created from SIRI-ET messages, see
 * {@link org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdater}. They are immutable:
 * an update is a new instance, upserted into the
 * {@link org.opentripplanner.ext.carpooling.CarpoolingRepository}.
 */
public class CarpoolTrip
  extends AbstractTransitEntity<CarpoolTrip, CarpoolTripBuilder>
  implements LogInfo
{

  /** Default total capacity (including driver) when no capacity information is provided. */
  public static final int DEFAULT_TOTAL_CAPACITY = 5;

  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final String provider;
  private final int totalCapacity;

  // Ordered list of stops along the carpool route where passengers can be picked up or dropped off
  private final List<CarpoolStop> stops;

  private final Duration[] legDeviationBudgets;

  @Nullable
  private final ContactInfo publicContactInformation;

  public CarpoolTrip(CarpoolTripBuilder builder) {
    super(builder.getId());
    this.startTime = Objects.requireNonNull(builder.startTime());
    this.endTime = Objects.requireNonNull(builder.endTime());
    this.provider = builder.provider();
    this.totalCapacity = builder.totalCapacity();
    this.stops = Collections.unmodifiableList(builder.stops());
    if (stops.size() < 2) {
      throw new IllegalArgumentException(
        "Carpool trip " + getId() + " must contain at least an origin and a destination stop"
      );
    }
    this.legDeviationBudgets = new Duration[stops.size() - 1];
    var budget = stops.getLast().getDeviationBudget();
    for (int leg = legDeviationBudgets.length - 1; leg >= 0; leg--) {
      var stopBudget = stops.get(leg + 1).getDeviationBudget();
      if (stopBudget.compareTo(budget) < 0) {
        budget = stopBudget;
      }
      legDeviationBudgets[leg] = budget;
    }
    this.publicContactInformation = builder.publicContactInformation();
  }

  /**
   * Returns the destination stop (last stop in the trip).
   */
  public CarpoolStop getDestination() {
    return stops.getLast();
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  /**
   * Returns the latest expected arrival time of the destination stop if available, otherwise falls
   * back to {@link #endTime()}.
   *
   * @return the latest expected end time of the trip
   */
  public ZonedDateTime latestEndTime() {
    var latest = getDestination().getLatestExpectedArrivalTime();
    return latest != null ? latest : endTime;
  }

  public String provider() {
    return provider;
  }

  /**
   * @return Total number of seats in the vehicle, including the driver seat
   */
  public int totalCapacity() {
    return totalCapacity;
  }

  /**
   * The stops in visit order, the origin first and the destination last, with the stops of booked
   * passengers in between.
   */
  public List<CarpoolStop> stops() {
    return stops;
  }

  @Nullable
  public ContactInfo publicContactInformation() {
    return publicContactInformation;
  }

  /** The coordinates of the stops, in visit order. */
  public List<WgsCoordinate> routePoints() {
    return stops.stream().map(CarpoolStop::getCoordinate).toList();
  }

  /**
   * Returns the number of passengers onboard the vehicle when departing the given stop.
   *
   * @param stopIndex The 0-based index of the stop in the stop list
   * @return Number of passengers onboard when departing from this stop
   * @throws IllegalArgumentException if stopIndex is out of bounds
   */
  public int getPassengerCountAtDepartureOfStop(int stopIndex) {
    if (stopIndex < 0 || stopIndex >= stops.size()) {
      throw new IllegalArgumentException(
        "Stop index " + stopIndex + " is out of bounds (0 to " + (stops.size() - 1) + ")"
      );
    }

    return stops.get(stopIndex).getOnboardCount();
  }

  /** The number of legs: one from each stop to the next. */
  public int legCount() {
    return stops.size() - 1;
  }

  /** Whether one more passenger fits on the leg from stop {@code leg} to the next. */
  public boolean hasFreeSeat(int leg) {
    return getPassengerCountAtDepartureOfStop(leg) < totalCapacity;
  }

  /**
   * The deviation budget of the leg from stop {@code leg}: the budget of the stop it ends at, or of
   * a later stop if that is smaller, since a delay on the leg delays every later stop too.
   */
  public Duration legDeviationBudget(int leg) {
    return legDeviationBudgets[leg];
  }

  @Nullable
  @Override
  public String logName() {
    return getId().toString();
  }

  /** Value comparison: same id and content, the stops compared by value too. */
  @Override
  public boolean sameAs(CarpoolTrip other) {
    return (
      getId().equals(other.getId()) &&
      startTime.equals(other.startTime) &&
      endTime.equals(other.endTime) &&
      Objects.equals(provider, other.provider) &&
      totalCapacity == other.totalCapacity &&
      sameStops(other.stops) &&
      Objects.equals(publicContactInformation, other.publicContactInformation)
    );
  }

  private boolean sameStops(List<CarpoolStop> otherStops) {
    if (stops.size() != otherStops.size()) {
      return false;
    }
    for (int i = 0; i < stops.size(); i++) {
      if (!stops.get(i).sameAs(otherStops.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public TransitBuilder<CarpoolTrip, CarpoolTripBuilder> copy() {
    return new CarpoolTripBuilder(this);
  }
}
