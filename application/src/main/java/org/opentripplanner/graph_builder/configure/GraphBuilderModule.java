package org.opentripplanner.graph_builder.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.cache.GraphBuildCacheManager;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;

@Module
public class GraphBuilderModule {

  @Provides
  @Singleton
  static GraphBuilder provideGraphBuilder(
    Graph baseGraph,
    DeduplicatorService deduplicator,
    TransitRepository transitRepository,
    DataImportIssueStore issueStore,
    GraphBuilderDataSources closeDataSourcesHandle,
    GraphBuildCacheManager cacheManager
  ) {
    return new GraphBuilder(
      baseGraph,
      deduplicator,
      transitRepository,
      issueStore,
      closeDataSourcesHandle,
      cacheManager
    );
  }
}
