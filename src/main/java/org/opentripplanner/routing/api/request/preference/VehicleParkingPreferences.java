package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;

// TODO VIA: Javadoc
public class VehicleParkingPreferences implements Cloneable, Serializable {

  private boolean useAvailabilityInformation = false;

  /**
   * If true vehicle parking availability information will be used to plan park and ride trips where
   * it exists.
   */
  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  public void setUseAvailabilityInformation(boolean useAvailabilityInformation) {
    this.useAvailabilityInformation = useAvailabilityInformation;
  }

  public VehicleParkingPreferences clone() {
    try {
      var clone = (VehicleParkingPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
