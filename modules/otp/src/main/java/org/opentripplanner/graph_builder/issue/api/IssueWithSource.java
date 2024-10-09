package org.opentripplanner.graph_builder.issue.api;

public class IssueWithSource implements DataImportIssue {

  private final DataImportIssue delegate;
  private final String source;

  public IssueWithSource(DataImportIssue delegate, String source) {
    this.delegate = delegate;
    this.source = source;
  }

  @Override
  public String getType() {
    return this.delegate.getType();
  }

  @Override
  public String getMessage() {
    return this.delegate.getMessage() + " - " + source;
  }
}
