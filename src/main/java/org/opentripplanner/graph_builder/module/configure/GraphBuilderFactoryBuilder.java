package org.opentripplanner.graph_builder.module.configure;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;

public class GraphBuilderFactoryBuilder {

  private BuildConfig config;
  private Graph graph;
  private TransitModel transitModel;
  private DataImportIssueStore issueStore;

  BuildConfig config() {
    return config;
  }

  public GraphBuilderFactoryBuilder withConfig(BuildConfig config) {
    this.config = config;
    return this;
  }

  Graph graph() {
    return graph;
  }

  public GraphBuilderFactoryBuilder withGraph(Graph graph) {
    this.graph = graph;
    return this;
  }

  TransitModel transitModel() {
    return transitModel;
  }

  public GraphBuilderFactoryBuilder withTransitModel(TransitModel transitModel) {
    this.transitModel = transitModel;
    return this;
  }

  /**
   * Supplies an issue store, if no issue store is provided a noop is used. This comes in handy in
   * unit tests.
   */
  DataImportIssueStore issueStore() {
    return issueStore == null ? DataImportIssueStore.noopIssueStore() : issueStore;
  }

  public GraphBuilderFactoryBuilder withIssueStore() {
    this.issueStore = new DataImportIssueStore();
    return this;
  }

  public GraphBuilderFactory build() {
    return new GraphBuilderFactory(this);
  }
}
