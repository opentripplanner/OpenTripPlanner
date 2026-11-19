package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.SANDBOX_TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.SANDBOX_USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;
import static org.opentripplanner.utils.text.MarkdownFormatter.HEADER_4;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
public class CarpoolingConfigurationDocTest {

  private static final File TEMPLATE = new File(SANDBOX_TEMPLATE_PATH, "Carpooling.md");
  private static final File OUT_FILE = new File(SANDBOX_USER_DOC_PATH, "Carpooling.md");

  private static final String ROUTER_CONFIG_FILENAME = "standalone/config/router-config.json";
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  @Test
  public void updateCarpoolingDoc() {
    NodeAdapter node = readCarpoolingConfig();

    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "config", configDoc(node));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readCarpoolingConfig() {
    var json = jsonNodeFromResource(ROUTER_CONFIG_FILENAME);
    var conf = new RouterConfig(json, ROUTER_CONFIG_FILENAME, false);
    return conf.asNodeAdapter().child("carpooling");
  }

  private String configDoc(NodeAdapter node) {
    var example = TemplateUtil.jsonExampleBuilder(node.rawNode())
      .wrapInObject("carpooling")
      .build();
    var buf = new DocBuilder();
    buf.addExample("router-config.json", example);
    buf.newLine();
    buf.addSection(new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable());
    buf.addSection(ParameterDetailsList.listParametersWithDetails(node, SKIP_NODES, HEADER_4));
    return buf.toString();
  }
}
