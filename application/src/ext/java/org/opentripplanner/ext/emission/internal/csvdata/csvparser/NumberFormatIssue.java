package org.opentripplanner.ext.emission.internal.csvdata.csvparser;

import java.util.Objects;
import org.opentripplanner.framework.error.OtpError;

final class NumberFormatIssue implements OtpError {

  private final String columnName;
  private final Object value;
  private final String type;
  private final String csvLine;

  public NumberFormatIssue(String columnName, Object value, String type, String csvLine) {
    this.columnName = columnName;
    this.value = value;
    this.type = type;
    this.csvLine = csvLine;
  }

  @Override
  public String errorCode() {
    return "EmissionNumberFormat";
  }

  @Override
  public String messageTemplate() {
    return "Unable to parse value '%s' for '%s' of type %s: %s";
  }

  @Override
  public Object[] messageArguments() {
    return new Object[] { Objects.toString(value), columnName, type, csvLine };
  }
}
