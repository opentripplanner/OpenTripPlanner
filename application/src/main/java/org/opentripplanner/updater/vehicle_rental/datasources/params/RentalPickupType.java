package org.opentripplanner.updater.vehicle_rental.datasources.params;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.opentripplanner.framework.doc.DocumentedEnum;

/**
 * This is temporary and will be removed in a future version of OTP.
 *
 * Enum to specify the type of rental data that is allowed to be read from the data source.
 */
public enum RentalPickupType implements DocumentedEnum<RentalPickupType> {
  STATION("Stations are imported."),
  FREE_FLOATING("Free-floating vehicles are imported.");

  public static final Set<RentalPickupType> ALL = Collections.unmodifiableSet(
    EnumSet.allOf(RentalPickupType.class)
  );

  private final String description;

  RentalPickupType(String description) {
    this.description = description.stripIndent().trim();
  }

  @Override
  public String typeDescription() {
    return (
      "This is temporary and will be removed in a future version of OTP. Use this to specify the type of rental data that is allowed to be read from the data source."
    );
  }

  @Override
  public String enumValueDescription() {
    return description;
  }
}
