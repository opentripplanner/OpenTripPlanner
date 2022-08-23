package org.opentripplanner.routing.api.request.refactor.preference;

public class VehicleParkingPreferences {

  /**
   * If true vehicle parking availability information will be used to plan park and ride trips where
   * it exists.
   */
  private boolean useVehicleParkingAvailabilityInformation = false;

  public void setUseVehicleParkingAvailabilityInformation(
    boolean useVehicleParkingAvailabilityInformation
  ) {
    this.useVehicleParkingAvailabilityInformation = useVehicleParkingAvailabilityInformation;
  }

  public boolean useVehicleParkingAvailabilityInformation() {
    return useVehicleParkingAvailabilityInformation;
  }
}
