package org.opentripplanner.ext.emission.internal.csvdata.csvparser;

import org.opentripplanner.framework.error.OtpError;

class ValueMissingIssue implements OtpError {

  private final String columnName;
  private final String csvLine;

  public ValueMissingIssue(String columnName, String csvLine) {
    this.columnName = columnName;
    this.csvLine = csvLine;
  }

  @Override
  public String errorCode() {
    return "EmissionValueMissing";
  }

  @Override
  public String messageTemplate() {
    return "Value for '%s' is missing: %s";
  }

  @Override
  public Object[] messageArguments() {
    return new Object[] { columnName, csvLine };
  }
}
