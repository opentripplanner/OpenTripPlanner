package org.opentripplanner.ext.vehiclerentalgraphbuilder.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.vehiclerentalgraphbuilder.internal.graphbuilder.VehicleRentalGraphBuilder;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;

@Module
public class VehicleRentalGraphBuilderModule {

  /**
   * The sandbox is activated by the presence of a manifest url in the build config; returning
   * {@code null} here leaves the module out of the graph build.
   */
  @Provides
  @Singleton
  @Nullable
  static VehicleRentalGraphBuilder provideVehicleRentalGraphBuilder(
    BuildConfig config,
    GbfsNetworkOverrides overrides,
    Graph graph,
    VehicleRentalRepository rentalRepository
  ) {
    if (!config.vehicleRentalGraphBuilder.hasUrl()) {
      return null;
    }

    return new VehicleRentalGraphBuilder(
      config.vehicleRentalGraphBuilder,
      overrides,
      graph,
      (DefaultVehicleRentalRepository) rentalRepository
    );
  }
}
