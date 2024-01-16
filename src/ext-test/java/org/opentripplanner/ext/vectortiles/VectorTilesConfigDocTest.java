package org.opentripplanner.ext.vectortiles;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
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
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
public class VectorTilesConfigDocTest {

  private static final String DOCUMENT = "sandbox/MapboxVectorTilesApi.md";
  private static final File TEMPLATE = new File(TEMPLATE_ROOT, DOCUMENT);
  private static final File OUT_FILE = new File(DOCS_ROOT, DOCUMENT);
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();
  private static final String ROUTER_CONFIG_PATH = "standalone/config/" + ROUTER_CONFIG_FILENAME;

  @Test
  public void updateDoc() {
    NodeAdapter node = readVectorTiles();

    // Read and close inout file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "parameters", updaterDoc(node));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readVectorTiles() {
    var json = jsonNodeFromResource(ROUTER_CONFIG_PATH);
    var conf = new RouterConfig(json, ROUTER_CONFIG_PATH, false);
    return conf.asNodeAdapter().child("vectorTiles");
  }

  private String updaterDoc(NodeAdapter node) {
    DocBuilder buf = new DocBuilder();
    addParameterSummaryTable(buf, node);
    addDetailsSection(buf, node);
    addExample(buf, node);
    return buf.toString();
  }

  private void addParameterSummaryTable(DocBuilder buf, NodeAdapter node) {
    buf.addSection(new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable());
  }

  private void addDetailsSection(DocBuilder buf, NodeAdapter node) {
    String details = getParameterDetailsTable(node);

    if (!details.isBlank()) {
      buf.header(4, "Details", null).addSection(details);
    }
  }

  private String getParameterDetailsTable(NodeAdapter node) {
    return ParameterDetailsList.listParametersWithDetails(node, SKIP_NODES, HEADER_4);
  }

  private void addExample(DocBuilder buf, NodeAdapter node) {
    buf.addSection("##### Example configuration");
    buf.addUpdaterExample(ROUTER_CONFIG_FILENAME, node.rawNode());
  }
}
