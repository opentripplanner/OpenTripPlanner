package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;

/**
 * Exposes the graph build issue counts as one gauge {@code graph_build_issues} per issue type,
 * tagged with {@code type}. The gauges hold their counts strongly, so a registry added later, like
 * the Prometheus registry, still gets them.
 */
class GraphBuildIssueMeterBinder implements MeterBinder {

  private static final String NAME = "graph_build_issues";

  private final DataImportIssueSummary issueSummary;

  GraphBuildIssueMeterBinder(DataImportIssueSummary issueSummary) {
    this.issueSummary = issueSummary;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    issueSummary.asMap().forEach((type, count) ->
      Gauge.builder(NAME, () -> count)
        .description("Number of data import issues of the given type found by the graph build")
        .tag("type", type)
        .register(registry)
    );
  }
}
