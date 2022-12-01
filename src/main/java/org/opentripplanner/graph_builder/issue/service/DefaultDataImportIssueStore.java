package org.opentripplanner.graph_builder.issue.service;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultDataImportIssueStore implements DataImportIssueStore {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger(ISSUES_LOG_NAME);

  private final List<DataImportIssue> issues = new ArrayList<>();

  public DefaultDataImportIssueStore() {}

  @Override
  public void add(DataImportIssue issue) {
    ISSUE_LOG.debug("{} - {}", issue.getType(), issue.getMessage());
    this.issues.add(issue);
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
    return this.issues;
  }
}
