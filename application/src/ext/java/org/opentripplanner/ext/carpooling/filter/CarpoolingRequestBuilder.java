package org.opentripplanner.ext.carpooling.filter;

import java.time.Instant;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Builder for {@link CarpoolingRequest}. Can be constructed directly for testing, or from a
 * {@link org.opentripplanner.routing.api.request.RouteRequest} via
 * {@link CarpoolingRequest#of(org.opentripplanner.routing.api.request.RouteRequest)}.
 */
public class CarpoolingRequestBuilder {

  private AccessEgressType accessOrEgress;
  private boolean isArriveByRequest;
  private WgsCoordinate passengerDropoff;
  private WgsCoordinate passengerPickup;
  private Instant requestedDateTime;

  public CarpoolingRequestBuilder() {}

  CarpoolingRequestBuilder(RouteRequest request) {
    this.isArriveByRequest = request.arriveBy();
    this.passengerPickup = new WgsCoordinate(request.from().getCoordinate());
    this.passengerDropoff = new WgsCoordinate(request.to().getCoordinate());
    this.requestedDateTime = request.dateTime();
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

  public CarpoolingRequest build() {
    return new CarpoolingRequest(
      accessOrEgress,
      isArriveByRequest,
      passengerPickup,
      passengerDropoff,
      requestedDateTime
    );
  }
}
