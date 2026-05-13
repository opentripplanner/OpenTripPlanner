package org.opentripplanner.graph_builder.issue.service;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.error.OtpError;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe implementation of {@link DataImportIssueStore}. NeTEx bundles are loaded in
 * parallel, so {@link #add} and {@link #listIssues} synchronize on the internal list.
 */
@Singleton
public class DefaultDataImportIssueStore implements DataImportIssueStore {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger(ISSUES_LOG_NAME);
  private final List<DataImportIssue> issues = new ArrayList<>();

  public DefaultDataImportIssueStore() {}

  @Override
  public void add(DataImportIssue issue) {
    synchronized (issues) {
      ISSUE_LOG.debug("{} - {}", issue.getType(), issue.getMessage());
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
  public List<DataImportIssue> listIssues() {
    synchronized (issues) {
      return List.copyOf(this.issues);
    }
  }

  @Override
  public String toString() {
    throw new UnsupportedOperationException(
      "Printing the DefaultDataImportIssueStore is not a good idea!"
    );
  }
}
