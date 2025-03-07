package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceJsonExample;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceParametersDetails;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceParametersTable;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;
import static org.opentripplanner.utils.text.MarkdownFormatter.HEADER_3;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
public class RouteRequestDocTest {

  private static final File TEMPLATE = new File(TEMPLATE_PATH, "RouteRequest.md");
  private static final File OUT_FILE = new File(USER_DOC_PATH, "RouteRequest.md");
  private static final String ROUTER_CONFIG_PATH = "standalone/config/" + ROUTER_CONFIG_FILENAME;
  private static final SkipNodes SKIP_NODES = SkipNodes.of()
    .skip("vectorTileLayers", "sandbox/MapboxVectorTilesApi.md")
    .build();

  /**
   * NOTE! This test updates the {@code doc/user/RouteRequest.md} document based on the latest
   * version of the code. The following is auto generated:
   * <ul>
   *   <li>The configuration type table</li>
   * </ul>
   */
  @Test
  public void updateRouteRequestConfigurationDoc() {
    NodeAdapter node = readRoutingDefaults();

    // Read and close input file (same as output file)
    String doc = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    doc = replaceParametersTable(doc, getParameterSummaryTable(node));
    doc = replaceParametersDetails(doc, getParameterDetailsList(node));

    var example = TemplateUtil.jsonExampleBuilder(node.rawNode())
      .wrapInObject("routingDefaults")
      .build();
    doc = replaceJsonExample(doc, example, ROUTER_CONFIG_FILENAME);

    writeFile(OUT_FILE, doc);

    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readRoutingDefaults() {
    var json = jsonNodeFromResource(ROUTER_CONFIG_PATH);
    var conf = new RouterConfig(json, ROUTER_CONFIG_PATH, false);
    return conf.asNodeAdapter().child("routingDefaults");
  }

  private String getParameterSummaryTable(NodeAdapter node) {
    return new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable();
  }

  private String getParameterDetailsList(NodeAdapter node) {
    return ParameterDetailsList.listParametersWithDetails(node, SKIP_NODES, HEADER_3);
  }
}
