package org.opentripplanner.routing.core;

import java.time.Instant;
import java.util.function.Consumer;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;

public class AStarRequestBuilder {

  Instant startTime;
  StreetMode mode;
  RoutingPreferences preferences;
  boolean arriveBy;
  boolean wheelchair;
  VehicleParkingRequest parking;
  VehicleRentalRequest rental;
  GenericLocation from;
  GenericLocation to;

  AStarRequestBuilder(AStarRequest original) {
    this.startTime = original.startTime();
    this.mode = original.mode();
    this.preferences = original.preferences();
    this.arriveBy = original.arriveBy();
    this.wheelchair = original.wheelchair();
    this.parking = original.parking().clone();
    this.rental = original.rental();
    this.from = original.from();
    this.to = original.to();
  }

  public AStarRequestBuilder withStartTime(Instant startTime) {
    this.startTime = startTime;
    return this;
  }

  public AStarRequestBuilder withMode(StreetMode mode) {
    this.mode = mode;
    return this;
  }

  public AStarRequestBuilder withPreferences(RoutingPreferences preferences) {
    this.preferences = preferences;
    return this;
  }

  public AStarRequestBuilder withPreferences(Consumer<RoutingPreferences.Builder> body) {
    return withPreferences(preferences.copyOf().apply(body).build());
  }

  public AStarRequestBuilder withArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return this;
  }

  public AStarRequestBuilder withWheelchair(boolean wheelchair) {
    this.wheelchair = wheelchair;
    return this;
  }

  public AStarRequestBuilder withParking(VehicleParkingRequest parking) {
    this.parking = parking;
    return this;
  }

  public AStarRequestBuilder withRental(VehicleRentalRequest rental) {
    this.rental = rental;
    return this;
  }

  public AStarRequestBuilder withFrom(GenericLocation from) {
    this.from = from;
    return this;
  }

  public AStarRequestBuilder withTo(GenericLocation to) {
    this.to = to;
    return this;
  }

  public AStarRequest build() {
    return new AStarRequest(this);
  }
}
