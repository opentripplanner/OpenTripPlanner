package org.opentripplanner.ext.datastore.gs;

import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.utils.text.MarkdownFormatter.HEADER_4;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.DocsTestConstants;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.test.support.ResourceLoader;

@GeneratesDocumentation
public class GsConfigurationDocTest implements DocsTestConstants {

  private static final File TEMPLATE = new File(TEMPLATE_PATH, "GoogleCloudStorage.md");
  private static final File OUT_FILE = new File(SANDBOX_USER_DOC_PATH, "GoogleCloudStorage.md");
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  @Test
  public void updateMapGcStorageConfigDoc() {
    NodeAdapter node = readMapGcStorageConfigConfig();

    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "config", updaterDoc(node));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readMapGcStorageConfigConfig() {
    var buildConfigFile = ResourceLoader.of(GsConfigurationDocTest.class).extTestResourceFile(
      BUILD_CONFIG_FILENAME
    );

    var json = JsonSupport.jsonNodeFromPath(buildConfigFile.toPath());
    var conf = new BuildConfig(json, buildConfigFile.toString(), false);
    return conf.asNodeAdapter();
  }

  private String updaterDoc(NodeAdapter node) {
    DocBuilder buf = new DocBuilder();
    addExample(buf, node);
    addParameterSummaryTable(buf, node.child("gsConfig"));
    addDetailsSection(buf, node.child("gsConfig"));
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
    var root = TemplateUtil.jsonExampleBuilder(node.rawNode()).build();
    buf.header(3, "Example configuration", null).addExample("build-config.json", root);
  }
}
