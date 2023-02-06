package org.opentripplanner.graph_builder.issue.report;

import java.util.Collection;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

record Bucket(BucketKey key, Collection<DataImportIssue> issues) implements Comparable<Bucket> {
  @Override
  public int compareTo(Bucket o) {
    return key.compareTo(o.key);
  }
}
