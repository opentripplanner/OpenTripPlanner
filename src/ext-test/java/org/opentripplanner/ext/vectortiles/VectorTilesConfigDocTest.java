package org.opentripplanner.ext.vectortiles;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.framework.text.MarkdownFormatter.HEADER_4;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.DOCS_ROOT;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_ROOT;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromPath;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.test.support.ResourceLoader;

@GeneratesDocumentation
public class VectorTilesConfigDocTest {

  private static final String DOCUMENT = "sandbox/MapboxVectorTilesApi.md";
  private static final File TEMPLATE = new File(TEMPLATE_ROOT, DOCUMENT);
  private static final File OUT_FILE = new File(DOCS_ROOT, DOCUMENT);
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  @Test
  public void updateDoc() {
    NodeAdapter node = readVectorTiles();

    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "parameters", vectorTilesDoc(node));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readVectorTiles() {
    var path = ResourceLoader.of(this).file("router-config.json").toPath();
    var json = jsonNodeFromPath(path);
    var conf = new RouterConfig(json, path.toString(), false);
    return conf.asNodeAdapter().child("vectorTiles");
  }

  private String vectorTilesDoc(NodeAdapter node) {
    DocBuilder buf = new DocBuilder();
    addParameterSummaryTable(buf, node);
    addDetailsSection(buf, node);
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
}
