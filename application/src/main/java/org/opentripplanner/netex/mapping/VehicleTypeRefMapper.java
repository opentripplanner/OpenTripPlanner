package org.opentripplanner.netex.mapping;

import jakarta.xml.bind.JAXBElement;
import javax.annotation.Nullable;
import org.rutebanken.netex.model.VehicleTypeRefStructure;

/**
 * Maps the NeTEx VehicleTypeRef field from the netex representation to a string.
 */
class VehicleTypeRefMapper {

  /** private constructor to prevent instantiation of utility class */
  private VehicleTypeRefMapper() {}

  /**
   * Return ref of the vehicle type as a string if it is given.
   */
  @Nullable
  static String mapVehicleTypeRef(
    @Nullable JAXBElement<? extends VehicleTypeRefStructure> vehicleTypeRef
  ) {
    if (vehicleTypeRef == null || vehicleTypeRef.getValue() == null) {
      return null;
    }
    return vehicleTypeRef.getValue().getRef();
  }
}
