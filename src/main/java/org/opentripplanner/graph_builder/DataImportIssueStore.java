package org.opentripplanner.graph_builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DataImportIssueStore {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger("DATA_IMPORT_ISSUES");
  private static final DataImportIssueStore NOOP = new DataImportIssueStore(false);

  private final List<DataImportIssue> issues = new ArrayList<>();

  private final boolean storeIssues;

  private DataImportIssueStore(boolean storeIssues) {
    this.storeIssues = storeIssues;
  }

  @Inject
  public DataImportIssueStore() {
    this.storeIssues = true;
  }

  public static DataImportIssueStore noopIssueStore() {
    return NOOP;
  }

  public void add(DataImportIssue issue) {
    ISSUE_LOG.debug("{} - {}", issue.getType(), issue.getMessage());
    if (storeIssues) {
      this.issues.add(issue);
    }
  }

  public void add(String type, String message) {
    add(Issue.issue(type, message));
  }

  public void add(String type, String message, Object... arguments) {
    add(Issue.issue(type, message, arguments));
  }

  public List<DataImportIssue> getIssues() {
    return this.issues;
  }

  void summarize() {
    Map<String, Long> issueCounts = issues
      .stream()
      .map(DataImportIssue::getType)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    int maxLength = issueCounts.keySet().stream().mapToInt(String::length).max().orElse(10);
    final String FMT = "  - %-" + maxLength + "s  %,7d";

    ISSUE_LOG.info("Issue summary (number of each type):");

    issueCounts
      .keySet()
      .stream()
      .sorted()
      .forEach(issueType ->
        ISSUE_LOG.info(String.format(FMT, issueType, issueCounts.get(issueType)))
      );
  }
}
