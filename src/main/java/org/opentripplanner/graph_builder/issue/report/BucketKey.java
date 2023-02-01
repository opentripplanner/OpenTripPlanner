package org.opentripplanner.graph_builder.issue.report;

record BucketKey(String issueType, Integer index) implements Comparable<BucketKey> {
  String key() {
    String bucketIndexString = this.index != null ? String.valueOf(this.index) : "";
    return this.issueType + bucketIndexString;
  }

  String label() {
    String bucketIndexString = this.index != null ? (" " + this.index) : "";
    return this.issueType + bucketIndexString;
  }

  @Override
  public int compareTo(BucketKey o) {
    return key().compareTo(o.key());
  }
}
