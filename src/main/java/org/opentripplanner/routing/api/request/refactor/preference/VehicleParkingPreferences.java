package org.opentripplanner.routing.api.request.refactor.preference;

public class VehicleParkingPreferences {

  /**
   * If true vehicle parking availability information will be used to plan park and ride trips where
   * it exists.
   */
  private boolean useAvailabilityInformation = false;

  public void setUseAvailabilityInformation(
    boolean useAvailabilityInformation
  ) {
    this.useAvailabilityInformation = useAvailabilityInformation;
  }

  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }
}
