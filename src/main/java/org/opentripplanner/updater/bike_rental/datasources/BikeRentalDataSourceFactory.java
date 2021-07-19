package org.opentripplanner.updater.bike_rental.datasources;

import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;

public class BikeRentalDataSourceFactory {

  public static BikeRentalDataSource create(BikeRentalDataSourceParameters source) {
    switch (source.getSourceType()) {
      // There used to be a lot more updaters and corresponding config variables here, but since
      // the industry has largely moved towards GBFS, they were removed.
      // See: https://groups.google.com/g/opentripplanner-users/c/0P8UGBJG-zE/m/xaOZnL3OAgAJ
      // If you want to add your provider-specific updater, you can move them into a sandbox module
      // and become the point of contact for the community.
      case GBFS: return new GbfsBikeRentalDataSource((GbfsBikeRentalDataSourceParameters) source);
    }
    throw new IllegalArgumentException(
        "Unknown bike rental source type: " + source.getSourceType()
    );
  }
}
