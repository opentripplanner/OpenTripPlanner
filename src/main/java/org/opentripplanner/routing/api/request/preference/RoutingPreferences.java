package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.core.TraverseMode;

/** User/trip cost/time/slack/reluctance search config. */
public class RoutingPreferences implements Cloneable, Serializable {

  private TransitPreferences transit = new TransitPreferences();
  private TransferPreferences transfer = new TransferPreferences();
  private WalkPreferences walk = WalkPreferences.DEFAULT;
  private StreetPreferences street = new StreetPreferences();

  @Nonnull
  private WheelchairPreferences wheelchair = WheelchairPreferences.DEFAULT;

  private BikePreferences bike = BikePreferences.DEFAULT;
  private CarPreferences car = new CarPreferences();
  private VehicleRentalPreferences rental = new VehicleRentalPreferences();
  private VehicleParkingPreferences parking = new VehicleParkingPreferences();
  private SystemPreferences system = new SystemPreferences();

  /**
   * This set the reluctance for bike, walk, car and bikeWalking (x2.7) - all in one go. These
   * parameters can be set individually.
   */
  public void setAllStreetReluctance(double streetReluctance) {
    if (streetReluctance > 0) {
      withWalk(it -> it.setReluctance(streetReluctance));
      withBike(it -> it.setReluctance(streetReluctance).setWalkingReluctance(streetReluctance * 2.7)
      );
      this.car.setReluctance(streetReluctance);
    }
  }

  public TransitPreferences transit() {
    return transit;
  }

  public TransferPreferences transfer() {
    return transfer;
  }

  public WalkPreferences walk() {
    return walk;
  }

  public RoutingPreferences withWalk(Consumer<WalkPreferences.Builder> body) {
    var builder = walk.copyOf();
    body.accept(builder);
    this.walk = builder.build();
    return this;
  }

  public StreetPreferences street() {
    return street;
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
    var builder = bike.copyOf();
    body.accept(builder);
    this.bike = builder.build();
    return this;
  }

  public CarPreferences car() {
    return car;
  }

  public VehicleRentalPreferences rental() {
    return rental;
  }

  public VehicleParkingPreferences parking() {
    return parking;
  }

  public SystemPreferences system() {
    return system;
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

      clone.transit = transit.clone();
      clone.transfer = transfer.clone();
      clone.street = street.clone();
      clone.car = car.clone();
      clone.rental = rental.clone();
      clone.parking = parking.clone();
      clone.system = system.clone();

      // The following immutable types can be skipped:
      // - walk, bike, wheelchair

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
