package org.opentripplanner.ext.vehiclerentalservicedirectory.generatedoc;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.SANDBOX_TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.SANDBOX_USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceJsonExample;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceParametersDetails;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceParametersTable;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;
import static org.opentripplanner.utils.text.MarkdownFormatter.HEADER_4;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
public class VehicleRentalServiceDirectoryConfigDocTest {

  private static final String DOCUMENT = "VehicleRentalServiceDirectory.md";
  private static final File TEMPLATE = new File(SANDBOX_TEMPLATE_PATH, DOCUMENT);
  private static final File OUT_FILE = new File(SANDBOX_USER_DOC_PATH, DOCUMENT);
  private static final String CONFIG_PATH =
    "org/opentripplanner/ext/vehiclerentalservicedirectory/generatedoc/" + ROUTER_CONFIG_FILENAME;
  private static final String CONFIG_TAG = "vehicleRentalServiceDirectory";
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  @Test
  public void updateConfigurationDoc() {
    NodeAdapter node = readConfigDefaults();

    // Read and close input file (same as output file)
    String doc = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    doc = replaceParametersTable(doc, getParameterSummaryTable(node));
    doc = replaceParametersDetails(doc, getParameterDetailsList(node));

    var example = TemplateUtil.jsonExampleBuilder(node.rawNode()).wrapInObject(CONFIG_TAG).build();
    doc = replaceJsonExample(doc, example, ROUTER_CONFIG_FILENAME);

    writeFile(OUT_FILE, doc);

    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readConfigDefaults() {
    var json = jsonNodeFromResource(CONFIG_PATH);
    var conf = new RouterConfig(json, CONFIG_PATH, false);
    return conf.asNodeAdapter().child(CONFIG_TAG);
  }

  private String getParameterSummaryTable(NodeAdapter node) {
    return new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable();
  }

  private String getParameterDetailsList(NodeAdapter node) {
    return ParameterDetailsList.listParametersWithDetails(node, SKIP_NODES, HEADER_4);
  }
}
