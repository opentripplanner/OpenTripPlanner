package org.opentripplanner.framework.csv.parser;

import java.util.Objects;

class ValueOutsideRangeIssue extends AbstractIssue {

  private final Number value;
  private final String valueType;
  private final Object range;

  public ValueOutsideRangeIssue(
    String columnName,
    Number value,
    String valueType,
    Object range,
    String csvLine,
    String issueType
  ) {
    super(columnName, csvLine, issueType);
    this.value = value;
    this.valueType = valueType;
    this.range = range;
  }

  @Override
  public String errorCode() {
    return issueType() + "OutsideRange";
  }

  @Override
  public String messageTemplate() {
    return "The %s value '%s' for %s is outside expected range %s: %s";
  }

  @Override
  public Object[] messageArguments() {
    return new Object[] {
      valueType,
      Objects.toString(value),
      columnName(),
      Objects.toString(range),
      csvLine(),
    };
  }
}
