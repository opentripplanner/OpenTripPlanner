package org.opentripplanner.generate.doc.framework;

/**
 * Replace a text in a file wrapped using HTML comments
 */
@SuppressWarnings("NewClassNamingConvention")
public class TemplateUtil {

  private static final String PARAMETERS_TABLE = "PARAMETERS-TABLE";
  private static final String PARAMETERS_DETAILS = "PARAMETERS-DETAILS";

  private static final String NEW_LINE = "\n";
  private static final String COMMENT_OPEN = "<!-- ";
  private static final String COMMENT_CLOSE = " -->";

  public static String replaceParametersTable(String doc, String replacement) {
    return replaceSection(doc, PARAMETERS_TABLE, replacement);
  }

  public static String replaceParametersDetails(String doc, String replacement) {
    return replaceSection(doc, PARAMETERS_DETAILS, replacement);
  }

  public static String replaceSection(String doc, String token, String replacement) {
    var replaceToken = replaceToken(token);

    if (!doc.contains(replaceToken)) {
      throw new IllegalStateException("Doc did not contain token: " + replaceToken);
    }
    var replacementText =
      """
      <!-- %s BEGIN -->
      <!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->
      
      %s
      <!-- %s END -->
      """.trim()
        .formatted(token, replacement, token);

    return doc.replace(replaceToken, replacementText);
  }

  private static String air(String section) {
    return NEW_LINE + section + NEW_LINE;
  }

  private static String replaceToken(String token) {
    return COMMENT_OPEN + "INSERT: " + token + COMMENT_CLOSE;
  }
}
