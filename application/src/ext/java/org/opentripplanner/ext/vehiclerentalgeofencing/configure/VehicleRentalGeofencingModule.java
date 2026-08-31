package org.opentripplanner.ext.vehiclerentalgeofencing.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.vehiclerentalgeofencing.internal.graphbuilder.VehicleRentalGeofencingGraphBuilder;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;

@Module
public class VehicleRentalGeofencingModule {

  /**
   * The sandbox is activated by the presence of a manifest url in the build config; returning
   * {@code null} here leaves the module out of the graph build.
   */
  @Provides
  @Singleton
  @Nullable
  static VehicleRentalGeofencingGraphBuilder provideVehicleRentalGeofencingGraphBuilder(
    BuildConfig config,
    GbfsNetworkOverrides overrides,
    Graph graph,
    VehicleRentalRepository rentalRepository
  ) {
    if (!config.vehicleRentalGeofencing.hasUrl()) {
      return null;
    }

    return new VehicleRentalGeofencingGraphBuilder(
      config.vehicleRentalGeofencing,
      overrides,
      graph,
      rentalRepository
    );
  }
}
