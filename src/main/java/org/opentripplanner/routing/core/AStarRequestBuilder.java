package org.opentripplanner.routing.core;

import java.time.Instant;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;

public class AStarRequestBuilder {

  private Instant startTime = Instant.now();
  private StreetMode mode = StreetMode.WALK;
  private RoutingPreferences preferences = new RoutingPreferences();
  private boolean arriveBy = false;
  private boolean wheelchair = false;
  private VehicleParkingRequest parking = new VehicleParkingRequest();
  private VehicleRentalRequest rental = new VehicleRentalRequest();
  private GenericLocation from = null;
  private GenericLocation to = null;

  AStarRequestBuilder() {}

  AStarRequestBuilder(AStarRequest original) {
    this.startTime = original.startTime();
    this.mode = original.mode();
    this.preferences = original.preferences().clone();
    this.arriveBy = original.arriveBy();
    this.wheelchair = original.wheelchair();
    this.parking = original.parking().clone();
    this.rental = original.rental();
    this.from = original.from();
    this.to = original.to();
  }

  public AStarRequestBuilder setStartTime(Instant startTime) {
    this.startTime = startTime;
    return this;
  }

  public Instant startTime() {
    return startTime;
  }

  public AStarRequestBuilder setMode(StreetMode mode) {
    this.mode = mode;
    return this;
  }

  public StreetMode mode() {
    return mode;
  }

  public AStarRequestBuilder setPreferences(RoutingPreferences preferences) {
    this.preferences = preferences;
    return this;
  }

  public RoutingPreferences preferences() {
    return preferences;
  }

  public AStarRequestBuilder setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return this;
  }

  public boolean arriveBy() {
    return arriveBy;
  }

  public AStarRequestBuilder setWheelchair(boolean wheelchair) {
    this.wheelchair = wheelchair;
    return this;
  }

  public boolean wheelchair() {
    return wheelchair;
  }

  public AStarRequestBuilder setParking(VehicleParkingRequest parking) {
    this.parking = parking;
    return this;
  }

  public VehicleParkingRequest parking() {
    return parking;
  }

  public AStarRequestBuilder setRental(VehicleRentalRequest rental) {
    this.rental = rental;
    return this;
  }

  public VehicleRentalRequest rental() {
    return rental;
  }

  public AStarRequestBuilder setFrom(GenericLocation from) {
    this.from = from;
    return this;
  }

  public GenericLocation from() {
    return from;
  }

  public AStarRequestBuilder setTo(GenericLocation to) {
    this.to = to;
    return this;
  }

  public GenericLocation to() {
    return to;
  }

  public AStarRequest build() {
    return new AStarRequest(this);
  }
}
