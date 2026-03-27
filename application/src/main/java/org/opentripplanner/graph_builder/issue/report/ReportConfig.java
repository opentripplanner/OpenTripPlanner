package org.opentripplanner.graph_builder.issue.report;

/**
 * White-label configuration for the data import issue report. Every field has a sensible default
 * so that the report works out-of-the-box for plain OTP deployments.
 *
 * <p>Pass a custom instance to {@link DataImportIssueReporter} to customise the look and feel.
 * Values are injected as CSS custom properties and JS constants in the generated HTML.
 *
 * @param appName      Title shown in the report header (e.g. {@code "My Transit Agency"})
 * @param logoUrl      URL to a logo image; use empty string to hide the logo
 * @param primaryColor CSS colour value for the main accent colour (e.g. {@code "#1a73e8"})
 * @param fontFamily   CSS font-family string (e.g. {@code "'Inter', sans-serif"})
 * @param pageSize     Number of issue messages shown per page inside each expanded group
 */
public record ReportConfig(
  String appName,
  String logoUrl,
  String primaryColor,
  String fontFamily,
  int pageSize
) {
  /** Sensible defaults for a plain OTP deployment. */
  public static final ReportConfig DEFAULT = new ReportConfig(
    "OpenTripPlanner",
    "",
    "#2563eb",
    "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif",
    50
  );

  /** Compact constructor — replaces blank/null fields with defaults. */
  public ReportConfig {
    if (appName == null || appName.isBlank()) {
      appName = DEFAULT.appName();
    }
    if (logoUrl == null) {
      logoUrl = "";
    }
    if (primaryColor == null || primaryColor.isBlank()) {
      primaryColor = DEFAULT.primaryColor();
    }
    if (fontFamily == null || fontFamily.isBlank()) {
      fontFamily = DEFAULT.fontFamily();
    }
    if (pageSize <= 0) {
      pageSize = DEFAULT.pageSize();
    }
  }
}
