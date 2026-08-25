package org.opentripplanner.generate.doc;

import static graphql.Assert.assertNotNull;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.test.support.ResourceLoader;

@GeneratesDocumentation
public class GraphQLTutorialDocTest {

  private static final ResourceLoader RESOURCES = ResourceLoader.of(GraphQLTutorialDocTest.class);

  private static final File TEMPLATE = new File(TEMPLATE_PATH, "GraphQL-Tutorial.md");

  private static final File OUT_FILE = new File(USER_DOC_PATH + "/apis", "GraphQL-Tutorial.md");

  /**
   * NOTE! This test updates the {@code doc/user/GraphQlTutorial.md} document based on the latest
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

    var routeQuery = getGraphQlQuery("routes-tutorial.graphql");
    var planQuery = getGraphQlQuery("planConnection-tutorial.graphql");

    doc = replaceSection(doc, "route-query", routeQuery);
    doc = replaceSection(doc, "plan-query", planQuery);
    writeFile(OUT_FILE, doc);

    assertFileEquals(original, OUT_FILE);
  }

  private static String getGraphQlQuery(String resourceName) {
    var content = RESOURCES.fileToString("/org/opentripplanner/apis/gtfs/queries/" + resourceName);
    var query = TemplateUtil.graphQlExample(content);
    assertNotNull(query);
    return query;
  }
}
