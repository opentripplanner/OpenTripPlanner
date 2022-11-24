package org.opentripplanner.graph_builder.issue.api;

import java.util.List;

/**
 * A no-op implementation of the issue store, convenient for unit testing. No issues are
 * added, and the {@link #listIssues()} will always return an empty list.
 */
class NoopDataImportIssueStore implements DataImportIssueStore {

  @Override
  public void add(DataImportIssue issue) {}

  @Override
  public void add(String type, String message) {}

  @Override
  public void add(String type, String message, Object... arguments) {}

  @Override
  public List<DataImportIssue> listIssues() {
    return List.of();
  }
}
