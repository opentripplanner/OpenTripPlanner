package org.opentripplanner.ext.carpickupzone.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneRepository;
import org.opentripplanner.ext.carpickupzone.internal.graphbuilder.CarPickupZoneGraphBuilder;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

@Module
public class CarPickupZoneGraphBuilderModule {

  @Provides
  @Singleton
  @Nullable
  static CarPickupZoneGraphBuilder provideCarPickupZoneGraphBuilder(
    GraphBuilderDataSources dataSources,
    @Nullable CarPickupZoneRepository carPickupZoneRepository,
    DataImportIssueStore issueStore
  ) {
    if (OTPFeature.CarPickupZone.isOff() || carPickupZoneRepository == null) {
      return null;
    }
    return new CarPickupZoneGraphBuilder(
      dataSources.getCarPickupZoneConfiguredDataSource(),
      carPickupZoneRepository,
      issueStore
    );
  }
}
