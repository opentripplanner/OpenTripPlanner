package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;

/**
 * Builder for {@link CarpoolingRequest}. Can be constructed directly for testing, or from a
 * {@link org.opentripplanner.routing.api.request.RouteRequest} via
 * {@link CarpoolingRequest#of(org.opentripplanner.routing.api.request.RouteRequest)}.
 */
public class CarpoolingRequestBuilder {

  /**
   * Fallback search window applied when the {@link RouteRequest} carries none. Five hours wide
   * because carpool repositories are sparse: rejecting trips outside a narrow window would lose
   * matches the passenger would accept.
   */
  static final Duration DEFAULT_SEARCH_WINDOW = Duration.ofMinutes(300);

  private AccessEgressType accessOrEgress;
  private boolean isArriveByRequest;
  private WgsCoordinate passengerDropoff;
  private WgsCoordinate passengerPickup;
  private Instant requestedDateTime;
  private Duration maxWalkTime;
  private Duration searchWindow;

  public CarpoolingRequestBuilder() {}

  CarpoolingRequestBuilder(RouteRequest request) {
    this.isArriveByRequest = request.arriveBy();
    this.passengerPickup = new WgsCoordinate(request.from().getCoordinate());
    this.passengerDropoff = new WgsCoordinate(request.to().getCoordinate());
    this.requestedDateTime = request.dateTime();
    this.maxWalkTime = request
      .preferences()
      .street()
      .accessEgress()
      .maxDuration()
      .valueOf(StreetMode.WALK);
    this.searchWindow = request.searchWindow() == null
      ? DEFAULT_SEARCH_WINDOW
      : request.searchWindow();
  }

  public CarpoolingRequestBuilder withAccessOrEgress(AccessEgressType accessOrEgress) {
    this.accessOrEgress = accessOrEgress;
    return this;
  }

  public CarpoolingRequestBuilder withArriveBy(boolean isArriveByRequest) {
    this.isArriveByRequest = isArriveByRequest;
    return this;
  }

  public CarpoolingRequestBuilder withPassengerDropoff(WgsCoordinate passengerDropoff) {
    this.passengerDropoff = passengerDropoff;
    return this;
  }

  public CarpoolingRequestBuilder withPassengerPickup(WgsCoordinate passengerPickup) {
    this.passengerPickup = passengerPickup;
    return this;
  }

  public CarpoolingRequestBuilder withRequestedDateTime(Instant requestedDateTime) {
    this.requestedDateTime = requestedDateTime;
    return this;
  }

  public CarpoolingRequestBuilder withMaxWalkTime(Duration maxWalkTime) {
    this.maxWalkTime = maxWalkTime;
    return this;
  }

  public CarpoolingRequestBuilder withSearchWindow(Duration searchWindow) {
    this.searchWindow = searchWindow;
    return this;
  }

  public CarpoolingRequest build() {
    return new CarpoolingRequest(
      accessOrEgress,
      isArriveByRequest,
      passengerPickup,
      passengerDropoff,
      requestedDateTime,
      maxWalkTime,
      searchWindow
    );
  }
}
