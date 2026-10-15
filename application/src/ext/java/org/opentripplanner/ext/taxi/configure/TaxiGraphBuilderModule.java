package org.opentripplanner.ext.taxi.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.taxi.TaxiRepository;
import org.opentripplanner.ext.taxi.internal.graphbuilder.TaxiGraphBuilder;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

@Module
public class TaxiGraphBuilderModule {

  @Provides
  @Singleton
  @Nullable
  static TaxiGraphBuilder provideTaxiGraphBuilder(
    GraphBuilderDataSources dataSources,
    @Nullable TaxiRepository taxiRepository,
    DataImportIssueStore issueStore
  ) {
    if (OTPFeature.TaxiRouting.isOff() || taxiRepository == null) {
      return null;
    }
    return new TaxiGraphBuilder(
      dataSources.getTaxiConfiguredDataSource(),
      taxiRepository,
      issueStore
    );
  }
}
