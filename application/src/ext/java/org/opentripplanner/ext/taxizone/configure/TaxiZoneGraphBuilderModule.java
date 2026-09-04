package org.opentripplanner.ext.taxizone.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.ext.taxizone.internal.graphbuilder.TaxiZoneGraphBuilder;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.standalone.config.BuildConfig;

@Module
public class TaxiZoneGraphBuilderModule {

  @Provides
  @Singleton
  @Nullable
  static TaxiZoneGraphBuilder provideTaxiZoneGraphBuilder(
    GraphBuilderDataSources dataSources,
    @Nullable TaxiZoneRepository taxiZoneRepository,
    DataImportIssueStore issueStore,
    BuildConfig config
  ) {
    if (OTPFeature.TaxiZone.isOff() || taxiZoneRepository == null) {
      return null;
    }
    return new TaxiZoneGraphBuilder(
      dataSources.getTaxiZoneConfiguredDataSource(),
      taxiZoneRepository,
      issueStore,
      config.getTransitServicePeriod()
    );
  }
}
