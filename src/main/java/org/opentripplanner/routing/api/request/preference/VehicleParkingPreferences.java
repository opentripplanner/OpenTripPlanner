package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * TODO VIA: Javadoc
 *
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class VehicleParkingPreferences implements Serializable {

  public static final VehicleParkingPreferences DEFAULT = new VehicleParkingPreferences(false);
  private static final VehicleParkingPreferences USE_AVAILABILITY = new VehicleParkingPreferences(
    true
  );

  private final boolean useAvailabilityInformation;

  private VehicleParkingPreferences(boolean useAvailabilityInformation) {
    this.useAvailabilityInformation = useAvailabilityInformation;
  }

  /**
   * Create a new instance of this class based on the given input.
   */
  public static VehicleParkingPreferences of(boolean useAvailabilityInformation) {
    // There are ony 2 possible instances of this class so we can simply the building process.
    return useAvailabilityInformation ? USE_AVAILABILITY : DEFAULT;
  }

  /**
   * If true vehicle parking availability information will be used to plan park and ride trips where
   * it exists.
   */
  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleParkingPreferences that = (VehicleParkingPreferences) o;
    return useAvailabilityInformation == that.useAvailabilityInformation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(useAvailabilityInformation);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(VehicleParkingPreferences.class)
      .addBoolIfTrue("useAvailabilityInformation", useAvailabilityInformation)
      .toString();
  }
}
