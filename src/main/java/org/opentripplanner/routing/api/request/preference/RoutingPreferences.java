package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.core.TraverseMode;

/** User/trip cost/time/slack/reluctance search config. */
@SuppressWarnings("UnusedReturnValue")
public final class RoutingPreferences implements Cloneable, Serializable {

  private TransitPreferences transit = TransitPreferences.DEFAULT;
  private TransferPreferences transfer = TransferPreferences.DEFAULT;
  private WalkPreferences walk = WalkPreferences.DEFAULT;
  private StreetPreferences street = StreetPreferences.DEFAULT;

  @Nonnull
  private WheelchairPreferences wheelchair = WheelchairPreferences.DEFAULT;

  private BikePreferences bike = BikePreferences.DEFAULT;
  private CarPreferences car = CarPreferences.DEFAULT;
  private VehicleRentalPreferences rental = new VehicleRentalPreferences();
  private VehicleParkingPreferences parking = VehicleParkingPreferences.DEFAULT;
  private SystemPreferences system = new SystemPreferences();

  @Nonnull
  private ItineraryFilterPreferences itineraryFilter = ItineraryFilterPreferences.DEFAULT;

  /**
   * This set the reluctance for bike, walk, car and bikeWalking (x2.7) - all in one go. These
   * parameters can be set individually.
   */
  public void setAllStreetReluctance(double streetReluctance) {
    if (streetReluctance > 0) {
      withWalk(it -> it.withReluctance(streetReluctance));
      withBike(it -> it.setReluctance(streetReluctance).setWalkingReluctance(streetReluctance * 2.7)
      );
      withCar(it -> it.withReluctance(streetReluctance));
    }
  }

  public TransitPreferences transit() {
    return transit;
  }

  public RoutingPreferences withTransit(Consumer<TransitPreferences.Builder> body) {
    transit = transit.copyOf().apply(body).build();
    return this;
  }

  public TransferPreferences transfer() {
    return transfer;
  }

  public RoutingPreferences withTransfer(Consumer<TransferPreferences.Builder> body) {
    this.transfer = transfer.copyOf().apply(body).build();
    return this;
  }

  public WalkPreferences walk() {
    return walk;
  }

  public RoutingPreferences withWalk(Consumer<WalkPreferences.Builder> body) {
    this.walk = walk.copyOf().apply(body).build();
    return this;
  }

  public StreetPreferences street() {
    return street;
  }

  public RoutingPreferences withStreet(Consumer<StreetPreferences.Builder> body) {
    this.street = street.copyOf().apply(body).build();
    return this;
  }

  /**
   * Preferences for how strict wheel-accessibility settings are
   */
  @Nonnull
  public WheelchairPreferences wheelchair() {
    return wheelchair;
  }

  public void setWheelchair(@Nonnull WheelchairPreferences wheelchair) {
    this.wheelchair = wheelchair;
  }

  public BikePreferences bike() {
    return bike;
  }

  public RoutingPreferences withBike(Consumer<BikePreferences.Builder> body) {
    this.bike = bike.copyOf().apply(body).build();
    return this;
  }

  public CarPreferences car() {
    return car;
  }

  public RoutingPreferences withCar(Consumer<CarPreferences.Builder> body) {
    this.car = car.copyOf().apply(body).build();
    return this;
  }

  public VehicleRentalPreferences rental() {
    return rental;
  }

  public VehicleParkingPreferences parking() {
    return parking;
  }

  public RoutingPreferences withParking(VehicleParkingPreferences parking) {
    this.parking = parking;
    return this;
  }

  public SystemPreferences system() {
    return system;
  }

  @Nonnull
  public ItineraryFilterPreferences itineraryFilter() {
    return itineraryFilter;
  }

  public RoutingPreferences withItineraryFilter(Consumer<ItineraryFilterPreferences.Builder> body) {
    this.itineraryFilter = itineraryFilter.copyOf().apply(body).build();
    return this;
  }

  public void withItineraryFilter(@Nonnull ItineraryFilterPreferences itineraryFilter) {
    this.itineraryFilter = itineraryFilter;
  }

  /**
   * The road speed for a specific traverse mode.
   */
  public double getSpeed(TraverseMode mode, boolean walkingBike) {
    return switch (mode) {
      case WALK -> walkingBike ? bike.walkingSpeed() : walk.speed();
      case BICYCLE -> bike.speed();
      case CAR -> car.speed();
      default -> throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
    };
  }

  public RoutingPreferences clone() {
    try {
      var clone = (RoutingPreferences) super.clone();

      clone.rental = rental.clone();
      clone.system = system.clone();

      // The following immutable types can be skipped:
      // - walk, bike, car, street, transfer, transit, parking, wheelchair

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
