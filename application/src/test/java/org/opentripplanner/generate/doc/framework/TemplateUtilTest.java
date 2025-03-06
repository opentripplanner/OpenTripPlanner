package org.opentripplanner.generate.doc.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;

import org.junit.jupiter.api.Test;

/**
 * Replace a text in a file wrapped using HTML comments
 */
@SuppressWarnings("NewClassNamingConvention")
public class TemplateUtilTest {

  @Test
  public void replaceSectionTest() {
    var body =
      """
      Expected line 1.
      Expected line 2.
      """;
    var doc = "<!-- INSERT: TEST -->";

    assertEquals(
      """
      <!-- TEST BEGIN -->
      <!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

      Expected line 1.
      Expected line 2.

      <!-- TEST END -->
      """.trim(),
      replaceSection(doc, "TEST", body)
    );
  }
}
