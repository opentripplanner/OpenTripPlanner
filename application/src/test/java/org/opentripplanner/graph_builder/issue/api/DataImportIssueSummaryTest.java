package org.opentripplanner.graph_builder.issue.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.collection.ListUtils;

class DataImportIssueSummaryTest {

  static final DataImportIssue ISSUE_1 = Issue.issue("issue1", "issue");
  static final DataImportIssue ISSUE_2 = Issue.issue("issue2", "issue");
  static final List<DataImportIssue> ISSUES = List.of(ISSUE_1, ISSUE_1, ISSUE_1);

  @Test
  void summarise() {
    var summary = new DataImportIssueSummary(ISSUES);

    assertEquals(Map.of(ISSUE_1.getType(), 3l), summary.asMap());
  }

  @Test
  void combine() {
    var summary1 = new DataImportIssueSummary(ISSUES);
    var summary2 = new DataImportIssueSummary(ListUtils.combine(ISSUES, List.of(ISSUE_2)));

    var combined = DataImportIssueSummary.combine(summary1, summary2);

    assertEquals(Map.of(ISSUE_2.getType(), 1l, ISSUE_1.getType(), 6l), combined.asMap());
  }
}
