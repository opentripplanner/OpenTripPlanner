package org.opentripplanner.routing.api.request.refactor.preference;

import java.io.Serializable;

public class VehicleParkingPreferences implements Cloneable, Serializable {

  /**
   * If true vehicle parking availability information will be used to plan park and ride trips where
   * it exists.
   */
  private boolean useAvailabilityInformation = false;

  public VehicleParkingPreferences clone() {
    try {
      var clone = (VehicleParkingPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public void setUseAvailabilityInformation(boolean useAvailabilityInformation) {
    this.useAvailabilityInformation = useAvailabilityInformation;
  }

  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }
}
