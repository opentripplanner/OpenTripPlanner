package org.opentripplanner.updater.vehicle_rental.datasources;

import org.opentripplanner.ext.smoovebikerental.SmooveBikeRentalDataSource;
import org.opentripplanner.ext.smoovebikerental.SmooveBikeRentalDataSourceParameters;
import org.opentripplanner.ext.vilkkubikerental.VilkkuBikeRentalDataSource;
import org.opentripplanner.ext.vilkkubikerental.VilkkuBikeRentalDataSourceParameters;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.updater.DataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public class VehicleRentalDataSourceFactory {

  public static DataSource<VehicleRentalPlace> create(VehicleRentalDataSourceParameters source) {
    return switch (source.sourceType()) {
      // There used to be a lot more updaters and corresponding config variables here, but since
      // the industry has largely moved towards GBFS, they were removed.
      // See: https://groups.google.com/g/opentripplanner-users/c/0P8UGBJG-zE/m/xaOZnL3OAgAJ
      // If you want to add your provider-specific updater, you can move them into a sandbox module
      // and become the point of contact for the community.
      case GBFS -> new GbfsVehicleRentalDataSource((GbfsVehicleRentalDataSourceParameters) source);
      case SMOOVE -> new SmooveBikeRentalDataSource((SmooveBikeRentalDataSourceParameters) source);
      case VILKKU -> new VilkkuBikeRentalDataSource((VilkkuBikeRentalDataSourceParameters) source);
    };
  }
}
