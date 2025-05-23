package org.opentripplanner.ext.emission.internal.csvdata.csvparser;

import java.util.Objects;
import org.opentripplanner.framework.error.OtpError;

class ValueOutsideRangeIssue implements OtpError {

  private final String columnName;
  private final Number value;
  private final String type;
  private final Object range;
  private final String csvLine;

  public ValueOutsideRangeIssue(
    String columnName,
    Number value,
    String type,
    Object range,
    String csvLine
  ) {
    this.columnName = columnName;
    this.value = value;
    this.type = type;
    this.range = range;
    this.csvLine = csvLine;
  }

  @Override
  public String errorCode() {
    return "EmissionOutsideRange";
  }

  @Override
  public String messageTemplate() {
    return "The %s value '%s' for %s is outside expected range %s: %s";
  }

  @Override
  public Object[] messageArguments() {
    return new Object[] {
      type,
      Objects.toString(value),
      columnName,
      Objects.toString(range),
      csvLine,
    };
  }
}
