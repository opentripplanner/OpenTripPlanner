package org.opentripplanner.graph_builder.issue.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;

class DataImportIssueReporterTest {

  private static final int MAX_NUMBER_OF_ISSUES_PER_FILE = 10;

  @Test
  void partitionIssues() {
    List<DataImportIssue> issues = new ArrayList<>();

    // Just a bit more than max should be still contained on one page
    for (int i = 0; i < 11; i++) {
      issues.add(Issue.issue("TypeA", "a_" + i));
    }

    // This should be split equally on 20 pages
    for (int i = 0; i < 200; i++) {
      issues.add(Issue.issue("TypeB", "b_" + i));
    }

    var buckets = DataImportIssueReporter.partitionIssues(issues, MAX_NUMBER_OF_ISSUES_PER_FILE);

    assertEquals(21, buckets.size());

    var sortedBuckets = buckets.stream().sorted().toList();

    assertEquals(new BucketKey("TypeA", null), sortedBuckets.get(0).key());
    assertEquals(11, sortedBuckets.get(0).issues().size());

    for (int i = 1; i < 21; i++) {
      assertEquals(new BucketKey("TypeB", i), sortedBuckets.get(i).key());
      assertEquals(10, sortedBuckets.get(i).issues().size());
    }
  }
}
