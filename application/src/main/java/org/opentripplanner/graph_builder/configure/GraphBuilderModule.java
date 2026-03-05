package org.opentripplanner.graph_builder.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TimetableRepository;

@Module
public class GraphBuilderModule {

  @Provides
  @Singleton
  static GraphBuilder provideGraphBuilder(
    Graph baseGraph,
    DeduplicatorService deduplicator,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore,
    GraphBuilderDataSources closeDataSourcesHandle
  ) {
    return new GraphBuilder(
      baseGraph,
      deduplicator,
      timetableRepository,
      issueStore,
      closeDataSourcesHandle
    );
  }
}
