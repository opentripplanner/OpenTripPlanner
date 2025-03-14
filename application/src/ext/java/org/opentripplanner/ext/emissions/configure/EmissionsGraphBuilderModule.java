package org.opentripplanner.ext.emissions.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsGraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.standalone.config.BuildConfig;

@Module
public class EmissionsGraphBuilderModule {

  @Provides
  @Singleton
  static EmissionsGraphBuilder provideEmissionsModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    @Nullable EmissionsDataModel emissionsDataModel,
    DataImportIssueStore issueStore
  ) {
    return new EmissionsGraphBuilder(
      dataSources.getGtfsConfiguredDatasource(),
      config,
      emissionsDataModel,
      issueStore
    );
  }
}
