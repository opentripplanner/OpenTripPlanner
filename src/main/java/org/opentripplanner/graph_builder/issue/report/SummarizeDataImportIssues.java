package org.opentripplanner.graph_builder.issue.report;

import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.ISSUES_LOG_NAME;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummarizeDataImportIssues {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger(ISSUES_LOG_NAME);

  private final Map<String, Long> summary;

  public SummarizeDataImportIssues(List<DataImportIssue> issues) {
    summary =
      issues
        .stream()
        .map(DataImportIssue::getType)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  public void printSummary() {
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
  public Map<String, Long> summary() {
    return summary;
  }
}
