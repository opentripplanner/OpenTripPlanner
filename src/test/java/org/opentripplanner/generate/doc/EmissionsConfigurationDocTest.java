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
import org.opentripplanner.framework.application.OtpFileNames;
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
public class EmissionsConfigurationDocTest {

  private static final File TEMPLATE = new File(TEMPLATE_ROOT, "Emissions.md");
  private static final File OUT_FILE = new File(DOCS_ROOT + "/sandbox", "Emissions.md");
  private static final String CONFIG_JSON = OtpFileNames.BUILD_CONFIG_FILENAME;
  private static final String CONFIG_PATH = "standalone/config/" + CONFIG_JSON;
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  @Test
  public void updateEmissionsDoc() {
    NodeAdapter node = readEmissionsConfig();

    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "config", updaterDoc(node));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readEmissionsConfig() {
    var json = jsonNodeFromResource(CONFIG_PATH);
    var conf = new BuildConfig(json, CONFIG_PATH, false);
    return conf.asNodeAdapter().child("emissions");
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
    var root = TemplateUtil.jsonExampleBuilder(node.rawNode()).wrapInObject("emissions").build();
    buf.header(3, "Example configuration", null).addExample("build-config.json", root);
  }
}
