package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.core.TraverseMode;

/** User/trip cost/time/slack/reluctance search config. */
public class RoutingPreferences implements Cloneable, Serializable {

  private TransitPreferences transit = new TransitPreferences();
  private TransferPreferences transfer = new TransferPreferences();
  private WalkPreferences walk = new WalkPreferences();
  private StreetPreferences street = new StreetPreferences();

  @Nonnull
  private WheelchairAccessibilityPreferences wheelchairAccessibility =
    WheelchairAccessibilityPreferences.DEFAULT;

  private BikePreferences bike = new BikePreferences();
  private CarPreferences car = new CarPreferences();
  private VehicleRentalPreferences rental = new VehicleRentalPreferences();
  private VehicleParkingPreferences parking = new VehicleParkingPreferences();
  private SystemPreferences system = new SystemPreferences();

  // TODO VIA (Thomas): Rename to setStreetReluctance
  public void setNonTransitReluctance(double streetReluctance) {
    if (streetReluctance > 0) {
      this.bike.setReluctance(streetReluctance);
      this.walk.setReluctance(streetReluctance);
      this.car.setReluctance(streetReluctance);
      this.bike.setWalkingReluctance(streetReluctance * 2.7);
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

  public StreetPreferences street() {
    return street;
  }

  /**
   * Preferences for how strict wheel-accessibility settings are
   */
  @Nonnull
  public WheelchairAccessibilityPreferences wheelchairAccessibility() {
    return wheelchairAccessibility;
  }

  public void setWheelchairAccessibility(
    @Nonnull WheelchairAccessibilityPreferences wheelchairAccessibility
  ) {
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  public BikePreferences bike() {
    return bike;
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
      // TODO VIA: 2022-09-06 Skipping WheelchairAccessibilityRequest

      var clone = (RoutingPreferences) super.clone();

      clone.transit = transit.clone();
      clone.transfer = transfer.clone();
      clone.walk = walk.clone();
      clone.street = street.clone();
      clone.wheelchairAccessibility = wheelchairAccessibility;
      clone.bike = bike.clone();
      clone.car = car.clone();
      clone.rental = rental.clone();
      clone.parking = parking.clone();
      clone.system = system.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
