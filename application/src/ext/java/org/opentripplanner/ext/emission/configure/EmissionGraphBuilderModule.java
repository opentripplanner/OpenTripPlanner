package org.opentripplanner.ext.emission.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.graphbuilder.EmissionGraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TimetableRepository;

@Module
public class EmissionGraphBuilderModule {

  @Provides
  @Singleton
  @Nullable
  static EmissionGraphBuilder provideEmissionGraphBuilder(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    @Nullable EmissionRepository emissionRepository,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore
  ) {
    if (emissionRepository == null) {
      return null;
    }

    return new EmissionGraphBuilder(
      dataSources.getGtfsConfiguredDataSource(),
      dataSources.getEmissionConfiguredDataSource(),
      config.emission,
      emissionRepository,
      timetableRepository,
      issueStore
    );
  }
}
