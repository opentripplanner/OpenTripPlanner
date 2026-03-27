package org.opentripplanner.ext.carpooling.filter;

import java.time.Instant;
import java.util.Optional;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;

public class CarpoolingRequestBuilder {

  private boolean isArriveByRequest;
  private WgsCoordinate passengerDropoff;
  private WgsCoordinate passengerPickup;
  private Instant requestedTimeDate;

  public CarpoolingRequestBuilder() {}

  CarpoolingRequestBuilder(RouteRequest request) {
    this.isArriveByRequest = request.arriveBy();
    this.passengerDropoff = Optional.of(request)
      .map(RouteRequest::to)
      .map(GenericLocation::getCoordinate)
      .map(WgsCoordinate::new)
      .orElseThrow();
    this.passengerPickup = Optional.of(request)
      .map(RouteRequest::from)
      .map(GenericLocation::getCoordinate)
      .map(WgsCoordinate::new)
      .orElseThrow();
    this.requestedTimeDate = request.dateTime();
  }

  public CarpoolingRequestBuilder withIsArriveByRequest(boolean isArriveByRequest) {
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
    this.requestedTimeDate = requestedDateTime;
    return this;
  }

  public CarpoolingRequest build() {
    return new CarpoolingRequest(
      isArriveByRequest,
      passengerPickup,
      passengerDropoff,
      requestedTimeDate
    );
  }
}
