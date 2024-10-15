package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.framework.text.MarkdownFormatter.HEADER_4;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.USER_DOC_PATH;
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
public class VehicleParkingDocTest {

  private static final File TEMPLATE = new File(TEMPLATE_PATH, "VehicleParking.md");
  private static final File OUT_FILE = new File(USER_DOC_PATH + "/sandbox", "VehicleParking.md");

  private static final String ROUTER_CONFIG_PATH = "standalone/config/" + ROUTER_CONFIG_FILENAME;
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();

  @Test
  public void updateVehicleParkingDoc() {
    NodeAdapter node = readVehicleUpdaters();

    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    for (String childName : node.listChildrenByName()) {
      var child = node.child(childName);
      var type = child.typeQualifier();
      if (type.equals("vehicle-parking")) {
        var sourceType = child.sourceTypeQualifier();
        template = replaceSection(template, sourceType, updaterDoc(child));
      }
    }

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readVehicleUpdaters() {
    var json = jsonNodeFromResource(ROUTER_CONFIG_PATH);
    var conf = new RouterConfig(json, ROUTER_CONFIG_PATH, false);
    return conf.asNodeAdapter().child("updaters");
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
