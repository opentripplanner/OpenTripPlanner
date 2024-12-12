package org.opentripplanner.generate.doc;

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
import org.opentripplanner.framework.application.OtpFileNames;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
public class DebugUiConfigurationDocTest {

  private static final String CONFIG_JSON = OtpFileNames.DEBUG_UI_CONFIG_FILENAME;
  private static final File TEMPLATE = new File(TEMPLATE_PATH, "DebugUiConfiguration.md");
  private static final File OUT_FILE = new File(USER_DOC_PATH, "DebugUiConfiguration.md");

  private static final String CONFIG_PATH = "standalone/config/" + CONFIG_JSON;

  /**
   * NOTE! This test updates the {@code doc/user/Configuration.md} document based on the latest
   * version of the code.
   */
  @Test
  public void updateDoc() {
    NodeAdapter node = readConfig();

    // Read and close input file (same as output file)
    String doc = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    doc = replaceParametersTable(doc, getParameterSummaryTable(node));
    doc = replaceParametersDetails(doc, getParameterDetailsTable(node));
    doc = replaceJsonExample(doc, node, CONFIG_JSON);

    writeFile(OUT_FILE, doc);

    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readConfig() {
    var json = jsonNodeFromResource(CONFIG_PATH);
    var conf = new DebugUiConfig(json, CONFIG_PATH, true);
    return conf.asNodeAdapter();
  }

  private String getParameterSummaryTable(NodeAdapter node) {
    return new ParameterSummaryTable(SkipNodes.of().build()).createTable(node).toMarkdownTable();
  }

  private String getParameterDetailsTable(NodeAdapter node) {
    return ParameterDetailsList.listParametersWithDetails(node, SkipNodes.of().build(), HEADER_3);
  }
}
