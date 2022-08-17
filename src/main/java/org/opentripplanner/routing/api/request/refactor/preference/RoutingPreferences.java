package org.opentripplanner.routing.api.request.refactor.preference;

// * User/trip cost/time/slack/reluctance search config.
public class RoutingPreferences {
  private TransitPreferences transit;
  private TransferPreferences transfer;
  private WalkPreferences walk;
  private StreetPreferences street;
  private WheelchairPreferences wheelchair;
  private BikePreferences bike;
  private CarPreferences car;
  private VehicleRentalPreferences rental;
  private SystemPreferences system;

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

  public SystemPreferences system() {
    return system;
  }
}
