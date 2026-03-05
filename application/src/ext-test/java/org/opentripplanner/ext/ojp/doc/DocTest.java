package org.opentripplanner.ext.ojp.doc;

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
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.test.support.ResourceLoader;

class DocTest {

  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  private final String configParam;

  private final File template;
  private final File outFile;

  private final File routerConfigFile;

  DocTest(String markdownFile, String configFile, String configParam) {
    this.configParam = configParam;
    this.template = new File(SANDBOX_TEMPLATE_PATH, markdownFile);
    this.outFile = new File(SANDBOX_USER_DOC_PATH, markdownFile);
    this.routerConfigFile = ResourceLoader.of(DocTest.class).extTestResourceFile(configFile);
  }

  void build() {
    NodeAdapter node = readConfig();

    // Read and close input file (same as output file)
    String template = readFile(this.template);
    String original = readFile(this.outFile);

    template = replaceSection(template, "config", updaterDoc(node));

    writeFile(outFile, template);
    assertFileEquals(original, outFile);
  }

  private NodeAdapter readConfig() {
    var json = jsonNodeFromPath(routerConfigFile.toPath());
    var conf = new RouterConfig(json, routerConfigFile.getName(), false);
    return conf.asNodeAdapter().child(configParam);
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
    var root = TemplateUtil.jsonExampleBuilder(node.rawNode()).wrapInObject(configParam).build();
    buf.header(3, "Example configuration", null).addExample(ROUTER_CONFIG_FILENAME, root);
  }
}
