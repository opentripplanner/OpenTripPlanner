package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import org.opentripplanner.routing.core.TraverseMode;

// TODO VIA: Javadoc
// * User/trip cost/time/slack/reluctance search config.
public class RoutingPreferences implements Cloneable, Serializable {

  private TransitPreferences transit = new TransitPreferences();
  private TransferPreferences transfer = new TransferPreferences();
  private WalkPreferences walk = new WalkPreferences();
  private StreetPreferences street = new StreetPreferences();

  // TODO VIA - To enable wheelchair we need a flag in the request, not relay on
  //          - wheelchair preferences to be set.
  private WheelchairPreferences wheelchair = new WheelchairPreferences();
  private BikePreferences bike = new BikePreferences();
  private CarPreferences car = new CarPreferences();
  private VehicleRentalPreferences rental = new VehicleRentalPreferences();
  private VehicleParkingPreferences parking = new VehicleParkingPreferences();
  private SystemPreferences system = new SystemPreferences();

  // TODO VIA: Rename to setStreetReluctance and move to StreetPreferences
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

  public WheelchairPreferences wheelchair() {
    return wheelchair;
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
      var clone = (RoutingPreferences) super.clone();

      clone.transit = transit.clone();
      clone.transfer = transfer.clone();
      clone.walk = walk.clone();
      clone.street = street.clone();
      clone.wheelchair = wheelchair.clone();
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
