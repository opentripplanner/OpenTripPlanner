package org.opentripplanner.graph_builder.issue.report;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;

class DataImportIssueReporterTest {

  private static final int NO_LIMIT = -1;
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

    var buckets = DataImportIssueReporter.partitionIssues(
      issues,
      NO_LIMIT,
      MAX_NUMBER_OF_ISSUES_PER_FILE
    );

    assertThat(buckets).hasSize(21);

    var sortedBuckets = buckets.stream().sorted().toList();

    assertEquals(new BucketKey("TypeA", null), sortedBuckets.get(0).key());
    assertThat(sortedBuckets.get(0).issues()).hasSize(11);
    assertEquals(11, sortedBuckets.get(0).totalIssuesOfType());
    assertFalse(sortedBuckets.get(0).isTruncated());

    for (int i = 1; i < 21; i++) {
      assertEquals(new BucketKey("TypeB", i), sortedBuckets.get(i).key());
      assertThat(sortedBuckets.get(i).issues()).hasSize(10);
      assertEquals(200, sortedBuckets.get(i).reportedIssuesOfType());
      assertEquals(200, sortedBuckets.get(i).totalIssuesOfType());
    }
  }

  @Test
  void keepOnlyTheIssuesWithTheHighestPriority() {
    List<DataImportIssue> issues = new ArrayList<>();
    for (int priority : new int[] { 3, 7, 1, 9, 5, 8 }) {
      issues.add(new PrioritizedIssue(priority));
    }

    var buckets = DataImportIssueReporter.partitionIssues(issues, 3, MAX_NUMBER_OF_ISSUES_PER_FILE);

    assertThat(buckets).hasSize(1);
    var bucket = buckets.getFirst();
    assertThat(bucket.issues().stream().map(DataImportIssue::getPriority).toList())
      .containsExactly(9, 8, 7)
      .inOrder();
    assertEquals(3, bucket.reportedIssuesOfType());
    assertEquals(6, bucket.totalIssuesOfType());
    assertTrue(bucket.isTruncated());
  }

  @Test
  void truncateBeforeSplittingIntoFiles() {
    List<DataImportIssue> issues = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      issues.add(Issue.issue("TypeA", "a_" + i));
    }
    for (int i = 0; i < 5; i++) {
      issues.add(Issue.issue("TypeB", "b_" + i));
    }

    var buckets = DataImportIssueReporter.partitionIssues(
      issues,
      50,
      MAX_NUMBER_OF_ISSUES_PER_FILE
    );

    var typeA = buckets
      .stream()
      .filter(b -> b.key().issueType().equals("TypeA"))
      .toList();
    assertThat(typeA).hasSize(5);
    for (var bucket : typeA) {
      assertThat(bucket.issues()).hasSize(10);
      assertEquals(50, bucket.reportedIssuesOfType());
      assertEquals(1000, bucket.totalIssuesOfType());
    }

    var typeB = buckets
      .stream()
      .filter(b -> b.key().issueType().equals("TypeB"))
      .toList();
    assertThat(typeB).hasSize(1);
    assertThat(typeB.getFirst().issues()).hasSize(5);
    assertFalse(typeB.getFirst().isTruncated());
  }

  private record PrioritizedIssue(int priority) implements DataImportIssue {
    @Override
    public String getMessage() {
      return "Issue with priority " + priority;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }
}
