package org.opentripplanner.graph_builder.issue.api;

import java.util.List;

/**
 * This service is used to store issued during data import. When the import is complete
 * all issues are written to a report. For test there is a NO-OP implementation, see the constant
 * {@link #NOOP}.
 *
 * When creating issues try to avoid creating the sting message, this will take a lot of memory
 * during graph build. Instead, keep references to values you want to include in the message and
 * construct the string when generating the report(when {@link DataImportIssue#getMessage()} or
 * {@link DataImportIssue#getHTMLMessage()} is called).
 */
public interface DataImportIssueStore {
  String ISSUES_LOG_NAME = "DATA_IMPORT_ISSUES";
  DataImportIssueStore NOOP = new NoopDataImportIssueStore();

  /** Add an issue to the issue report. */
  void add(DataImportIssue issue);

  /** Add an issue to the issue report without the need of creating a issue class. */
  void add(String type, String message);

  /**
   * Add an issue to the issue report without the need of creating a issue class.
   * The given list of {@code arguments} is injected into the message using
   * {@link String#format(String, Object...)}.
   */
  void add(String type, String message, Object... arguments);

  /** List all issues added */
  List<DataImportIssue> listIssues();
}
