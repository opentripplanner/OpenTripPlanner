package org.opentripplanner.generate.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.framework.text.MarkdownFormatter.HEADER_4;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection2;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeFromResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.MarkDownDocWriter;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class UpdaterConfigDocTest {

  private static final File TEMPLATE = new File("doc-templates", "UpdaterConfig.md");
  private static final File FILE = new File("docs", "UpdaterConfig.md");

  private static final String BUILD_CONFIG_FILENAME = "standalone/config/router-config.json";
  private static final Set<String> SKIP_UPDATERS = Set.of("siri-azure-sx-updater");
  private static final SkipNodes SKIP_NODES = SkipNodes.of();

  /**
   * NOTE! This test updates the {@code docs/Configuration.md} document based on the latest
   * version of the code. The following is auto generated:
   * <ul>
   *   <li>The configuration type table</li>
   *   <li>The list of OTP features</li>
   * </ul>
   */
  @Test
  public void updateBuildConfigurationDoc() {
    NodeAdapter node = readBuildConfig();

    // Read and close inout file (same as output file)
    String doc = readFile(TEMPLATE);

    for (String childName : node.listChildrenByName()) {
      var child = node.child(childName);
      var type = child.peek("type").asText();

      if (!SKIP_UPDATERS.contains(type)) {
        doc = replaceSection2(doc, type, updaterDoc(child));
      }
    }

    writeFile(FILE, doc);
    assertEquals(doc, readFile(FILE));
  }

  private NodeAdapter readBuildConfig() {
    var json = jsonNodeFromResource(BUILD_CONFIG_FILENAME);
    var conf = new RouterConfig(json, BUILD_CONFIG_FILENAME, false);
    return conf.asNodeAdapter().child("updaters");
  }

  private String updaterDoc(NodeAdapter node) {
    StringBuilder buf = new StringBuilder();
    buf.append(getParameterSummaryTable(node));
    String details = getParameterDetailsTable(node);

    if (!details.isBlank()) {
      buf.append("\n\n#### Details\n\n").append(details);
    }
    buf.append("\n");
    buf.append(
      """
      ```JSON
      // router-config.json
      {
        "updaters": [
          %s
          ]
        }
      ```
        """.formatted(
          node.toPrettyString().indent(4).trim()
        )
    );

    return buf.toString();
  }

  private String getParameterSummaryTable(NodeAdapter node) {
    return new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable();
  }

  private String getParameterDetailsTable(NodeAdapter node) {
    var stream = new ByteArrayOutputStream();
    var out = new MarkDownDocWriter(new PrintStream(stream));
    ParameterDetailsList.listParametersWithDetails(node, out, SKIP_NODES, HEADER_4);
    return stream.toString(StandardCharsets.UTF_8);
  }
}
