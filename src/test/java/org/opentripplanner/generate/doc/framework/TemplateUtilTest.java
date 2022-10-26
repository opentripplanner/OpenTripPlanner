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
  public void test() {
    var body = """
      Expected line 1.
      Expected line 2.
      """;
    var text = """
      <!-- TEST BEGIN -->
      %s
      <!-- TEST END -->
      """;

    var expected = text.formatted(
      "<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->\n\n" +
      body
    );
    var doc = text.formatted("^ANY TEXT $1 - With special chars...");

    assertEquals(expected, replaceSection(doc, "TEST", body));
  }
}
