package org.opentripplanner.graph_builder.issue.api;

public class IssueWithSource implements DataImportIssue {

  private final String type;
  private final String message;
  private final String source;

  private IssueWithSource(String type, String message, String source) {
    this.type = type;
    this.message = message;
    this.source = source;
  }

  public static IssueWithSource issue(String type, String message, String source) {
    return new IssueWithSource(type, message, source);
  }

  public static IssueWithSource issue(DataImportIssue issue, String source) {
    return new IssueWithSource(issue.getType(), issue.getMessage(), source);
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public String getMessage() {
    return message + " - " + source;
  }
}
