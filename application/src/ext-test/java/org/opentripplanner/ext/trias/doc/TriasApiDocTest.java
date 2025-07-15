package org.opentripplanner.ext.trias.doc;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.SANDBOX_TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.SANDBOX_USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromPath;
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
import org.opentripplanner.test.support.ResourceLoader;

@GeneratesDocumentation
public class TriasApiDocTest {

  private static final File TEMPLATE = new File(SANDBOX_TEMPLATE_PATH, "TriasApi.md");
  private static final File OUT_FILE = new File(SANDBOX_USER_DOC_PATH, "TriasApi.md");

  private static final File ROUTER_CONFIG_FILE = ResourceLoader.of(
    TriasApiDocTest.class
  ).extTestResourceFile(ROUTER_CONFIG_FILENAME);
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();
  public static final String CONFIG_PARAM = "triasApi";

  @Test
  public void update() {
    NodeAdapter node = readTriasConfig();

    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "config", updaterDoc(node));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readTriasConfig() {
    var json = jsonNodeFromPath(ROUTER_CONFIG_FILE.toPath());
    var conf = new RouterConfig(json, ROUTER_CONFIG_FILE.getName(), false);
    return conf.asNodeAdapter().child(CONFIG_PARAM);
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
    var root = TemplateUtil.jsonExampleBuilder(node.rawNode()).wrapInObject(CONFIG_PARAM).build();
    buf.header(3, "Example configuration", null).addExample(ROUTER_CONFIG_FILENAME, root);
  }
}
