package org.opentripplanner.graph_builder.issue.service;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultDataImportIssueStore implements DataImportIssueStore {

  private static final Logger ISSUE_LOG = LoggerFactory.getLogger(ISSUES_LOG_NAME);

  private final List<DataImportIssue> issues = new ArrayList<>();
  private String fileName = null;

  public DefaultDataImportIssueStore() {}

  @Override
  public void add(DataImportIssue issue) {
    ISSUE_LOG.debug("{} - {}", issue.getType(), issue.getMessage());
    this.issues.add(issue);
  }

  @Override
  public void add(String type, String message) {
    if (fileName != null) {
      add(Issue.issue(type, message + " - %s", fileName));
    } else {
      add(Issue.issue(type, message));
    }
  }

  @Override
  public void add(String type, String message, Object... arguments) {
    if (fileName != null) {
      add(
        Issue.issue(
          type,
          message + " - %s",
          Stream.concat(Stream.of(arguments), Stream.of(fileName)).toArray()
        )
      );
    } else {
      add(Issue.issue(type, message, arguments));
    }
  }

  @Override
  public void setFilename(String filename) {
    this.fileName = filename;
  }

  @Override
  public List<DataImportIssue> listIssues() {
    return this.issues;
  }
}
