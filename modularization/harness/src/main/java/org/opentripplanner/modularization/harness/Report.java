package org.opentripplanner.modularization.harness;

/** Formats the result of a comparison as text. */
final class Report {

  private static final int MAX_LISTED = 50;

  private Report() {}

  static String format(int totalDifferences, ApprovedDiscrepancies.Result result) {
    var sb = new StringBuilder();
    int approved = totalDifferences - result.unapproved().size();
    sb
      .append("Differences: ")
      .append(totalDifferences)
      .append(" (approved: ")
      .append(approved)
      .append(", not approved: ")
      .append(result.unapproved().size())
      .append(")\n");
    result.unapproved().stream().limit(MAX_LISTED).forEach(d -> sb.append(d).append('\n'));
    if (result.unapproved().size() > MAX_LISTED) {
      sb
        .append("... and ")
        .append(result.unapproved().size() - MAX_LISTED)
        .append(" more\n");
    }
    if (!result.unused().isEmpty()) {
      sb.append("Approved entries that no longer match anything (remove them):\n");
      result.unused().forEach(e -> sb.append("  ").append(e).append('\n'));
    }
    sb.append(result.isCompliant() ? "COMPLIANT\n" : "NOT COMPLIANT\n");
    return sb.toString();
  }
}
