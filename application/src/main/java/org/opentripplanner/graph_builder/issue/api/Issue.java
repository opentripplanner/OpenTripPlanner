package org.opentripplanner.graph_builder.issue.api;

import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Generic issue type, which can be used to create issues.
 */
public class Issue implements DataImportIssue {

  private final String type;
  private final String message;
  private final Object[] arguments;

  private Issue(String type, String message, Object... arguments) {
    this.type = type;
    this.message = message;
    this.arguments = arguments;
  }

  public static Issue issue(String type, String message) {
    return new Issue(type, message);
  }

  public static Issue issue(String type, String message, Object... arguments) {
    return new Issue(type, message, arguments);
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getMessage() {
    return String.format(message, arguments);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addStr("type", type)
      .addStr("message", getMessage())
      .toString();
  }
}
