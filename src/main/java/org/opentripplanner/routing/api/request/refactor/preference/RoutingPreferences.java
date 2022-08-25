package org.opentripplanner.routing.api.request.refactor.preference;

// * User/trip cost/time/slack/reluctance search config.
public class RoutingPreferences {

  private final TransitPreferences transit = new TransitPreferences();
  private final TransferPreferences transfer = new TransferPreferences();
  private final WalkPreferences walk = new WalkPreferences();
  private final StreetPreferences street = new StreetPreferences();
  private final WheelchairPreferences wheelchair = new WheelchairPreferences();
  private final BikePreferences bike = new BikePreferences();
  private final CarPreferences car = new CarPreferences();
  private final VehicleRentalPreferences rental = new VehicleRentalPreferences();
  private final VehicleParkingPreferences parking = new VehicleParkingPreferences();
  private final SystemPreferences system = new SystemPreferences();

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
}
