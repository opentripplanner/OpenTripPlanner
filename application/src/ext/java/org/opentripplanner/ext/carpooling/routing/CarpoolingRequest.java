package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * The parts of a {@link RouteRequest} that carpooling uses, created by the {@link #of} methods.
 */
public class CarpoolingRequest {

  private final AccessEgressType accessOrEgress;
  private final boolean isArriveByRequest;
  private final WgsCoordinate passengerPickup;
  private final WgsCoordinate passengerDropoff;
  private final Instant requestedDateTime;
  private final Duration maxWalkTime;
  private final Duration searchWindow;
  private final Duration stopDuration;
  private final Duration maxJourneyDuration;

  CarpoolingRequest(
    AccessEgressType accessOrEgress,
    boolean isArriveByRequest,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Instant requestedDateTime,
    Duration maxWalkTime,
    Duration searchWindow,
    Duration stopDuration,
    Duration maxJourneyDuration
  ) {
    this.accessOrEgress = accessOrEgress;
    this.isArriveByRequest = isArriveByRequest;
    this.passengerPickup = passengerPickup;
    this.passengerDropoff = passengerDropoff;
    this.requestedDateTime = requestedDateTime;
    this.maxWalkTime = Objects.requireNonNull(
      maxWalkTime,
      "maxWalkTime is required; populate it via the RouteRequest-based builder or withMaxWalkTime()"
    );
    this.searchWindow = Objects.requireNonNull(
      searchWindow,
      "searchWindow is required; populate it via the RouteRequest-based builder or withSearchWindow()"
    );
    this.stopDuration = Objects.requireNonNull(
      stopDuration,
      "stopDuration is required; populate it via the RouteRequest-based builder or withStopDuration()"
    );
    this.maxJourneyDuration = Objects.requireNonNull(
      maxJourneyDuration,
      "maxJourneyDuration is required; use the RouteRequest-based builder or withMaxJourneyDuration()"
    );
  }

  public static CarpoolingRequest of(RouteRequest request, Duration defaultSearchWindow) {
    return new CarpoolingRequestBuilder(request, defaultSearchWindow).build();
  }

  public static CarpoolingRequest of(
    RouteRequest request,
    AccessEgressType accessOrEgress,
    Duration defaultSearchWindow
  ) {
    return new CarpoolingRequestBuilder(request, defaultSearchWindow)
      .withAccessOrEgress(accessOrEgress)
      .build();
  }

  /** Whether the carpool leg connects to a transit stop; {@code false} for direct routing. */
  public boolean isAccessEgressRequest() {
    return accessOrEgress != null;
  }

  /** Whether the carpool leg runs from the passenger's origin to a transit stop. */
  public boolean isAccessRequest() {
    return accessOrEgress != null && accessOrEgress.isAccess();
  }

  /** Whether the carpool leg runs from a transit stop to the passenger's destination. */
  public boolean isEgressRequest() {
    return accessOrEgress != null && accessOrEgress.isEgress();
  }

  public boolean isArriveByRequest() {
    return isArriveByRequest;
  }

  public WgsCoordinate getPassengerPickup() {
    return passengerPickup;
  }

  public WgsCoordinate getPassengerDropoff() {
    return passengerDropoff;
  }

  public Instant getRequestedDateTime() {
    return requestedDateTime;
  }

  /** The longest walk between the passenger's origin or destination and the car. */
  public Duration getMaxWalkTime() {
    return maxWalkTime;
  }

  /**
   * The search window applied to {@link #getRequestedDateTime()}: after it for depart-after, before
   * it for arrive-by.
   */
  public Duration getSearchWindow() {
    return searchWindow;
  }

  /** The dwell the car makes at each stop, the passenger's pickup and dropoff included. */
  public Duration getStopDuration() {
    return stopDuration;
  }

  /** The longest journey the search accepts, from the origin to the destination. */
  public Duration getMaxJourneyDuration() {
    return maxJourneyDuration;
  }
}
