package org.opentripplanner.generate.doc;

import static graphql.Assert.assertNotNull;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.DOCS_ROOT;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_ROOT;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;

import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.TemplateUtil;

@GeneratesDocumentation
public class GraphQLTutorialDocTest {

  private static final File TEMPLATE = new File(TEMPLATE_ROOT, "GraphQL-Tutorial.md");

  private static final File OUT_FILE = new File(DOCS_ROOT + "/apis", "GraphQL-Tutorial.md");

  /**
   * NOTE! This test updates the {@code docs/GraphQlTutorial.md} document based on the latest
   * version of the code.
   * This test fails if the document have changed. This make sure that this test fails in the
   * CI pipeline if config file changes is not committed. Manually inspect the changes in the
   * configuration, commit the configuration document, and run the test again to pass.
   */
  @Test
  public void updateTutorialDoc() throws IOException {
    // Read and close input file (same as output file)
    String doc = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    var routeQuery = getGraphQlQuery("routes.graphql");
    var planQuery = getGraphQlQuery("plan.graphql");

    doc = replaceSection(doc, "route-query", routeQuery);
    doc = replaceSection(doc, "plan-query", planQuery);
    writeFile(OUT_FILE, doc);

    assertFileEquals(original, OUT_FILE);
  }

  @Nonnull
  private static String getGraphQlQuery(String resourceName) throws IOException {
    var url = Resources.getResource("org/opentripplanner/apis/gtfs/queries/" + resourceName);
    var query = TemplateUtil.graphQlExample(Resources.toString(url, StandardCharsets.UTF_8));
    assertNotNull(query);
    return query;
  }
}
