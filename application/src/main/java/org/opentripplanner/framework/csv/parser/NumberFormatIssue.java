package org.opentripplanner.framework.csv.parser;

import java.util.Objects;

final class NumberFormatIssue extends AbstractIssue {

  private final Object value;
  private final String valueType;

  public NumberFormatIssue(
    String columnName,
    Object value,
    String valueType,
    String csvLine,
    String type
  ) {
    super(columnName, csvLine, type);
    this.value = value;
    this.valueType = valueType;
  }

  @Override
  public String errorCode() {
    return issueType() + "NumberFormat";
  }

  @Override
  public String messageTemplate() {
    return "Unable to parse value '%s' for '%s' of type %s: %s";
  }

  @Override
  public Object[] messageArguments() {
    return new Object[] { Objects.toString(value), columnName(), valueType, csvLine() };
  }
}
