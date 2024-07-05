package org.opentripplanner.graph_builder.issue.api;

import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.ISSUES_LOG_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A summarised version of the {@see DataImportIssueStore} which doesn't contain all the issues
 * instances but only the names and their counts.
 */
public class DataImportIssueSummary implements Serializable {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger(ISSUES_LOG_NAME);
  private final Map<String, Long> summary;

  public DataImportIssueSummary(List<DataImportIssue> issues) {
    this(
      issues
        .stream()
        .map(DataImportIssue::getType)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
    );
  }

  private DataImportIssueSummary(Map<String, Long> summary) {
    this.summary = Map.copyOf(summary);
  }

  /**
   * Takes two summaries and combine them into a single one. If there are types that
   * are in both summaries their counts are added.
   */
  public static DataImportIssueSummary combine(
    DataImportIssueSummary first,
    DataImportIssueSummary second
  ) {
    var combined = new HashMap<>(first.asMap());
    second
      .asMap()
      .forEach((type, count) -> {
        if (combined.containsKey(type)) {
          var countSoFar = combined.get(type);
          combined.put(type, count + countSoFar);
        } else {
          combined.put(type, count);
        }
      });

    return new DataImportIssueSummary(combined);
  }

  public static DataImportIssueSummary empty() {
    return new DataImportIssueSummary(List.of());
  }

  public void logSummary() {
    int maxLength = summary.keySet().stream().mapToInt(String::length).max().orElse(10);
    final String FMT = "  - %-" + maxLength + "s  %,7d";

    ISSUE_LOG.info("Issue summary (number of each type):");

    summary
      .keySet()
      .stream()
      .sorted()
      .forEach(issueType -> ISSUE_LOG.info(String.format(FMT, issueType, summary.get(issueType))));
  }

  @Nonnull
  public Map<String, Long> asMap() {
    return summary;
  }
}
