package org.opentripplanner.framework.csv.parser;

import org.opentripplanner.framework.error.OtpError;

abstract class AbstractIssue implements OtpError {

  private final String columnName;
  private final String csvLine;
  private final String issueType;

  public AbstractIssue(String columnName, String csvLine, String issueType) {
    this.columnName = columnName;
    this.csvLine = csvLine;
    this.issueType = issueType;
  }

  String columnName() {
    return columnName;
  }

  String csvLine() {
    return csvLine;
  }

  String issueType() {
    return issueType;
  }
}
