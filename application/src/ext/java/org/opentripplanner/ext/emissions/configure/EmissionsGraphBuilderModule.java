package org.opentripplanner.ext.emissions.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emissions.EmissionsGraphBuilder;
import org.opentripplanner.ext.emissions.EmissionsRepository;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.standalone.config.BuildConfig;

@Module
public class EmissionsGraphBuilderModule {

  @Provides
  @Singleton
  @Nullable
  static EmissionsGraphBuilder provideEmissionsModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    @Nullable EmissionsRepository emissionsRepository,
    DataImportIssueStore issueStore
  ) {
    if (emissionsRepository == null) {
      return null;
    }

    return new EmissionsGraphBuilder(
      dataSources.getGtfsConfiguredDatasource(),
      dataSources.getEmissionConfiguredDatasource(),
      config.emissions,
      emissionsRepository,
      issueStore
    );
  }
}
