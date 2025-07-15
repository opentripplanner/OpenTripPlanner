package org.opentripplanner.graph_builder.issue.service;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.error.OtpError;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.graph_builder.issue.api.IssueWithSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultDataImportIssueStore implements DataImportIssueStore {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger(ISSUES_LOG_NAME);

  private final List<DataImportIssue> issues = new ArrayList<>();
  private String currentSource = null;

  public DefaultDataImportIssueStore() {}

  @Override
  public void add(DataImportIssue issue) {
    ISSUE_LOG.debug("{} - {}", issue.getType(), issue.getMessage());
    if (currentSource != null) {
      this.issues.add(new IssueWithSource(issue, currentSource));
    } else {
      this.issues.add(issue);
    }
  }

  @Override
  public void add(OtpError issue) {
    add(issue.errorCode(), issue.messageTemplate(), issue.messageArguments());
  }

  @Override
  public void add(String type, String message) {
    add(Issue.issue(type, message));
  }

  @Override
  public void add(String type, String message, Object... arguments) {
    add(Issue.issue(type, message, arguments));
  }

  @Override
  public void startProcessingSource(String source) {
    this.currentSource = source;
  }

  @Override
  public void stopProcessingSource() {
    this.currentSource = null;
  }

  @Override
  public List<DataImportIssue> listIssues() {
    return this.issues;
  }

  @Override
  public String toString() {
    throw new UnsupportedOperationException(
      "Printing the DefaultDataImportIssueStore is not a good idea!"
    );
  }
}
