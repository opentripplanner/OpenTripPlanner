package org.opentripplanner.framework.csv.parser;

final class ValueMissingIssue extends AbstractIssue {

  public ValueMissingIssue(String columnName, String csvLine, String issueType) {
    super(columnName, csvLine, issueType);
  }

  @Override
  public String errorCode() {
    return issueType() + "ValueMissing";
  }

  @Override
  public String messageTemplate() {
    return "Value for '%s' is missing: %s";
  }

  @Override
  public Object[] messageArguments() {
    return new Object[] { columnName(), csvLine() };
  }
}
