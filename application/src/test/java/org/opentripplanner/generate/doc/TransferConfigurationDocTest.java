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
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Generates the configuration reference of {@code doc/user/TransferConfiguration.md}, from the
 * documentation of the parameters in the config code. The {@code transfers} section is read from
 * a build config with only the transfer configuration, which sets every parameter.
 */
@GeneratesDocumentation
public class TransferConfigurationDocTest {

  private static final String FILE_NAME = "TransferConfiguration.md";
  private static final File TEMPLATE = new File(TEMPLATE_PATH, FILE_NAME);
  private static final File OUT_FILE = new File(USER_DOC_PATH, FILE_NAME);

  private static final String CONFIG_PATH = "standalone/config/build-config-transfers.json";
  private static final SkipNodes SKIP_NODES = SkipNodes.of()
    .skipNestedElements("requests", "RouteRequest.md")
    .build();

  @Test
  public void updateTransferConfigurationDoc() {
    var json = jsonNodeFromResource(CONFIG_PATH);
    var node = readTransfersNode();

    // Read and close input file (same as output file)
    String doc = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    doc = replaceParametersTable(
      doc,
      new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable()
    );
    doc = replaceParametersDetails(
      doc,
      ParameterDetailsList.listParametersWithDetails(node, SKIP_NODES, HEADER_3)
    );
    doc = replaceJsonExample(doc, json, OtpFileNames.BUILD_CONFIG_FILENAME);

    writeFile(OUT_FILE, doc);

    assertFileEquals(original, OUT_FILE);
  }

  private static NodeAdapter readTransfersNode() {
    var json = jsonNodeFromResource(CONFIG_PATH);
    var conf = new BuildConfig(json, CONFIG_PATH, true);
    return conf.asNodeAdapter().child("transfers");
  }
}
