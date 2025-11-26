package org.opentripplanner.street.search.request;

import static org.opentripplanner.street.search.request.StreetSearchRequest.MAX_CLOSENESS_METERS;

import java.time.Instant;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;

public class StreetSearchRequestBuilder {

  Instant startTime;
  StreetMode mode;
  boolean arriveBy;
  boolean wheelchairEnabled;
  Envelope fromEnvelope;
  Envelope toEnvelope;
  boolean geoidElevation;
  double turnReluctance;
  WalkRequest walk;
  BikeRequest bike;
  CarRequest car;
  WheelchairRequest wheelchair;
  ScooterRequest scooter;
  ElevatorRequest elevator;

  StreetSearchRequestBuilder(StreetSearchRequest original) {
    this.startTime = original.startTime();
    this.mode = original.mode();
    this.arriveBy = original.arriveBy();
    this.wheelchairEnabled = original.wheelchairEnabled();
    this.fromEnvelope = original.fromEnvelope();
    this.toEnvelope = original.toEnvelope();
    this.geoidElevation = original.geoidElevation();
    this.turnReluctance = original.turnReluctance();
    this.walk = original.walk();
    this.bike = original.bike();
    this.car = original.car();
    this.scooter = original.scooter();
    this.wheelchair = original.wheelchair();
    this.elevator = original.elevator();
  }

  public StreetSearchRequestBuilder withStartTime(Instant startTime) {
    this.startTime = startTime;
    return this;
  }

  public StreetSearchRequestBuilder withMode(StreetMode mode) {
    this.mode = mode;
    return this;
  }

  public StreetSearchRequestBuilder withArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return this;
  }

  public StreetSearchRequestBuilder withWheelchairEnabled(boolean wheelchair) {
    this.wheelchairEnabled = wheelchair;
    return this;
  }

  public StreetSearchRequestBuilder withWheelchair(Consumer<WheelchairRequest.Builder> request) {
    this.wheelchair = this.wheelchair.copyOf().apply(request).build();
    return this;
  }

  public StreetSearchRequestBuilder withFrom(GenericLocation from) {
    this.fromEnvelope = createEnvelope(from);
    return this;
  }

  public StreetSearchRequestBuilder withTo(GenericLocation to) {
    this.toEnvelope = createEnvelope(to);
    return this;
  }

  public StreetSearchRequestBuilder withGeoidElevation(boolean value) {
    this.geoidElevation = value;
    return this;
  }

  public StreetSearchRequestBuilder withTurnReluctance(double v) {
    this.turnReluctance = v;
    return this;
  }

  public StreetSearchRequestBuilder withUseRentalAvailability(boolean b) {
    withCar(c -> c.withRental(r -> r.withUseAvailabilityInformation(b)));
    withBike(c -> c.withRental(r -> r.withUseAvailabilityInformation(b)));
    withScooter(c -> c.withRental(r -> r.withUseAvailabilityInformation(b)));
    return this;
  }

  public StreetSearchRequestBuilder withWalk(Consumer<WalkRequest.Builder> body) {
    this.walk = this.walk.copyOf().apply(body).build();
    return this;
  }

  public StreetSearchRequestBuilder withBike(Consumer<BikeRequest.Builder> body) {
    this.bike = this.bike.copyOf().apply(body).build();
    return this;
  }

  public StreetSearchRequestBuilder withCar(Consumer<CarRequest.Builder> body) {
    this.car = this.car.copyOf().apply(body).build();
    return this;
  }

  public StreetSearchRequestBuilder withScooter(Consumer<ScooterRequest.Builder> body) {
    this.scooter = this.scooter.copyOf().apply(body).build();
    return this;
  }

  public StreetSearchRequestBuilder withElevator(Consumer<ElevatorRequest.Builder> body) {
    this.elevator = this.elevator.copyOf().apply(body).build();
    return this;
  }

  Instant startTimeOrNow() {
    return startTime == null ? Instant.now() : startTime;
  }

  public StreetSearchRequest build() {
    return new StreetSearchRequest(this);
  }

  @Nullable
  private static Envelope createEnvelope(GenericLocation location) {
    if (location == null) {
      return null;
    }

    Coordinate coordinate = location.getCoordinate();
    if (coordinate == null) {
      return null;
    }

    double lat = SphericalDistanceLibrary.metersToDegrees(MAX_CLOSENESS_METERS);
    double lon = SphericalDistanceLibrary.metersToLonDegrees(MAX_CLOSENESS_METERS, coordinate.y);

    Envelope env = new Envelope(coordinate);
    env.expandBy(lon, lat);

    return env;
  }
}
