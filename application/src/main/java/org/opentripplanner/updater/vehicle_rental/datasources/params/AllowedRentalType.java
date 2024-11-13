package org.opentripplanner.updater.vehicle_rental.datasources.params;

import org.opentripplanner.framework.doc.DocumentedEnum;

/**
 * This is temporary and will be removed in a future version of OTP.
 *
 * Enum to specify the type of rental data that is allowed to be read from the data source.
 */
public enum AllowedRentalType implements DocumentedEnum<AllowedRentalType> {
  STATIONS("Only station data is allowed."),
  VEHICLES("Only vehicle data is allowed."),
  ALL("All types of rental data are allowed.");

  private final String description;

  AllowedRentalType(String description) {
    this.description = description.stripIndent().trim();
  }

  @Override
  public String typeDescription() {
    return (
      "Temporary parameter. Use this to specify the type of rental data that is allowed to be read from the data source."
    );
  }

  @Override
  public String enumValueDescription() {
    return description;
  }
}
