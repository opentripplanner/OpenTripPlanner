package org.opentripplanner.standalone.server;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.graph_builder.issue.api.Issue;

class GraphBuildIssueMeterBinderTest {

  private static final DataImportIssue ISSUE_A = Issue.issue("IssueA", "a");
  private static final DataImportIssue ISSUE_B = Issue.issue("IssueB", "b");

  @Test
  void exposesOneGaugePerIssueType() {
    var registry = new SimpleMeterRegistry();
    new GraphBuildIssueMeterBinder(summary()).bindTo(registry);

    assertThat(
      registry
        .getMeters()
        .stream()
        .map(m -> m.getId().getName())
        .distinct()
        .toList()
    ).containsExactly("graph_build_issues");
    assertEquals(1.0, registry.get("graph_build_issues").tag("type", "IssueA").gauge().value());
    assertEquals(2.0, registry.get("graph_build_issues").tag("type", "IssueB").gauge().value());
  }

  /** As in OTP, the Prometheus registry is added after the gauges are bound. */
  @Test
  void prometheusRegistryAddedAfterBindingExportsEveryIssueType() {
    var composite = new CompositeMeterRegistry();
    new GraphBuildIssueMeterBinder(summary()).bindTo(composite);

    var prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    List<Meter.Id> failures = new ArrayList<>();
    prometheus.config().onMeterRegistrationFailed((id, reason) -> failures.add(id));
    composite.add(prometheus);

    assertThat(failures).isEmpty();
    var scrape = prometheus.scrape();
    assertThat(scrape).contains("graph_build_issues{type=\"IssueA\"} 1.0");
    assertThat(scrape).contains("graph_build_issues{type=\"IssueB\"} 2.0");
  }

  @Test
  void anEmptySummaryHasNoMeters() {
    var registry = new SimpleMeterRegistry();
    new GraphBuildIssueMeterBinder(DataImportIssueSummary.empty()).bindTo(registry);

    assertThat(registry.getMeters()).isEmpty();
  }

  private static DataImportIssueSummary summary() {
    return new DataImportIssueSummary(List.of(ISSUE_A, ISSUE_B, ISSUE_B));
  }
}
