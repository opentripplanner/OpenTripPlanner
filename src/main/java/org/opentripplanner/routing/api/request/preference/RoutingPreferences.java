package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;

// * User/trip cost/time/slack/reluctance search config.
public class RoutingPreferences implements Cloneable, Serializable {

  private TransitPreferences transit = new TransitPreferences();
  private TransferPreferences transfer = new TransferPreferences();
  private WalkPreferences walk = new WalkPreferences();
  private StreetPreferences street = new StreetPreferences();
  private WheelchairPreferences wheelchair = new WheelchairPreferences();
  private BikePreferences bike = new BikePreferences();
  private CarPreferences car = new CarPreferences();
  private VehicleRentalPreferences rental = new VehicleRentalPreferences();
  private VehicleParkingPreferences parking = new VehicleParkingPreferences();
  private SystemPreferences system = new SystemPreferences();

  public void setNonTransitReluctance(double nonTransitReluctance) {
    if (nonTransitReluctance > 0) {
      this.bike.setReluctance(nonTransitReluctance);
      this.walk.setReluctance(nonTransitReluctance);
      this.car.setReluctance(nonTransitReluctance);
      this.bike.setWalkingReluctance(nonTransitReluctance * 2.7);
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
