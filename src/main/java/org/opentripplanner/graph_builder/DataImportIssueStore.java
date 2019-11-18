package org.opentripplanner.graph_builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataImportIssueStore {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger("DATA_IMPORT_ISSUES");

  private final List<DataImportIssue> issues = new ArrayList<>();

  private final boolean storeIssues;

  public DataImportIssueStore(boolean storeIssues) {
    this.storeIssues = storeIssues;
  }

  public void add(DataImportIssue issue) {
    ISSUE_LOG.debug("{} - {}", issue.getClass().getSimpleName(), issue.getMessage());
    if (storeIssues) {
      this.issues.add(issue);
    }
  }

  void summarize() {
    Map<String, Long> issueCounts = issues
        .stream()
        .map(Object::getClass)
        .collect(Collectors.groupingBy(Class::getSimpleName, Collectors.counting()));

    int maxLength = issueCounts.keySet().stream().mapToInt(String::length).max().orElse(10);
    final String FMT = "  - %-" + maxLength + "s  %,7d";

    ISSUE_LOG.info("Issue summary (number of each type):");

    issueCounts.keySet().stream().sorted().forEach(issueType ->
        ISSUE_LOG.info(String.format(FMT, issueType, issueCounts.get(issueType)))
    );
  }

  public List<DataImportIssue> getIssues() {
    return this.issues;
  }
}
