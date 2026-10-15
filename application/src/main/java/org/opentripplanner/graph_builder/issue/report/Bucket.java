package org.opentripplanner.graph_builder.issue.report;

import java.util.Collection;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

/**
 * The issues written to one report file. All issues in a bucket are of the same type.
 *
 * @param reportedIssuesOfType the number of issues of this type written to the report, across all
 *                             the buckets of the type.
 * @param totalIssuesOfType the number of issues of this type found during the graph build. This is
 *                          larger than {@code reportedIssuesOfType} if the report is truncated.
 */
record Bucket(
  BucketKey key,
  Collection<DataImportIssue> issues,
  int reportedIssuesOfType,
  int totalIssuesOfType
) implements Comparable<Bucket> {
  boolean isTruncated() {
    return reportedIssuesOfType < totalIssuesOfType;
  }

  @Override
  public int compareTo(Bucket o) {
    return key.compareTo(o.key);
  }
}
