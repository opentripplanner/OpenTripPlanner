package org.opentripplanner.generate.doc.framework;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Replace a text in a file wrapped using HTML comments
 */
public class TemplateUtil {

  private static final String PARAMETERS_TABLE = "PARAMETERS-TABLE";
  private static final String PARAMETERS_DETAILS = "PARAMETERS-DETAILS";
  private static final String EXAMPLE = "JSON-EXAMPLE";
  private static final String COMMENT_OPEN = "<!-- ";
  private static final String COMMENT_CLOSE = " -->";

  public static String replaceParametersTable(String doc, String replacement) {
    return replaceSection(doc, PARAMETERS_TABLE, replacement);
  }

  public static String replaceParametersDetails(String doc, String replacement) {
    return replaceSection(doc, PARAMETERS_DETAILS, replacement);
  }

  public static String replaceJsonExample(String doc, NodeAdapter root, String source) {
    return replaceSection(doc, EXAMPLE, jsonExample(root, source));
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

  private static String replaceToken(String token) {
    return COMMENT_OPEN + "INSERT: " + token + COMMENT_CLOSE;
  }

  /**
   * Create a JSON example for the node. The given source  from the node
   */
  public static String jsonExample(NodeAdapter nodeAdapter, String source) {
    return """
      ```JSON
      // %s
      %s
      ```
      """.formatted(
        source,
        nodeAdapter.toPrettyString()
      );
  }
}
