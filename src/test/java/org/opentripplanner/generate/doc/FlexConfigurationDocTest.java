package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.framework.text.MarkdownFormatter.HEADER_4;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.DOCS_ROOT;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_ROOT;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

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
public class FlexConfigurationDocTest {

  private static final File TEMPLATE = new File(TEMPLATE_ROOT, "Flex.md");
  private static final File OUT_FILE = new File(DOCS_ROOT + "/sandbox", "Flex.md");

  private static final String ROUTER_CONFIG_FILENAME = "standalone/config/router-config.json";
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  @Test
  public void updateFlexDoc() {
    NodeAdapter node = readFlexConfig();

    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "config", updaterDoc(node));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readFlexConfig() {
    var json = jsonNodeFromResource(ROUTER_CONFIG_FILENAME);
    var conf = new RouterConfig(json, ROUTER_CONFIG_FILENAME, false);
    return conf.asNodeAdapter().child("flex");
  }

  private String updaterDoc(NodeAdapter node) {
    DocBuilder buf = new DocBuilder();
    addExample(buf, node);
    addParameterSummaryTable(buf, node);
    addDetailsSection(buf, node);
    return buf.toString();
  }

  private void addParameterSummaryTable(DocBuilder buf, NodeAdapter node) {
    buf
      .header(3, "Overview", null)
      .addSection(new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable());
  }

  private void addDetailsSection(DocBuilder buf, NodeAdapter node) {
    buf
      .header(3, "Details", null)
      .addSection(ParameterDetailsList.listParametersWithDetails(node, SKIP_NODES, HEADER_4));
  }

  private void addExample(DocBuilder buf, NodeAdapter node) {
    var root = TemplateUtil.jsonExampleBuilder(node.rawNode()).wrapInObject("flex").build();
    buf.header(3, "Example configuration", null).addExample("router-config.json", root);
  }
}
