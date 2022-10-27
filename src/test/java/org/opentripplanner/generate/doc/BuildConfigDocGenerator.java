package org.opentripplanner.generate.doc;

import static org.opentripplanner.generate.doc.framework.ParameterDetailsList.listParametersWithDetails;
import static org.opentripplanner.generate.doc.framework.ParameterSummaryTable.createParametersTable;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeFromResource;

import java.io.File;
import java.io.FileNotFoundException;
import org.opentripplanner.generate.doc.framework.MarkDownDocWriter;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class BuildConfigDocGenerator {

  private static final File OUT_FILE = new File("docs", "BuildConfiguration-generated.md");
  private static final String BUILD_CONFIG_FILENAME = "standalone/config/build-config.json";
  private static final SkipNodes SKIP_NODES = SkipNodes.of(
    "dataOverlay",
    "/docs/sandbox/DataOverlay.md",
    "transferRequests",
    "/docs/RoutingRequest.md"
  );

  private final MarkDownDocWriter out = MarkDownDocWriter.create(OUT_FILE);

  public static void main(String[] args) throws FileNotFoundException {
    var printer = new BuildConfigDocGenerator();
    var json = jsonNodeFromResource(BUILD_CONFIG_FILENAME);
    var conf = new BuildConfig(json, BUILD_CONFIG_FILENAME, false);
    var node = conf.asNodeAdapter();
    printer.write(node);
  }

  public void write(NodeAdapter node) {
    out.printDocTitle(BuildConfigTemplate.TITLE);
    out.printSection(BuildConfigTemplate.INTRODUCTION);

    out.printHeader1("Overview");
    out.printSection(BuildConfigTemplate.OVERVIEW);

    out.printHeader2("Parameters", null);
    out.printSection(BuildConfigTemplate.PARAMETERS);
    createParametersTable(node, out, SKIP_NODES);

    /* Add support for listing deprecated parameters (old, not in use parameters)
    out.printHeader2("Deprecated Parameters", null);
    out.printSection(template.parametersDeprecated());
    createDeprecatedParametersTable(node, out, this::skipObject);
    */

    out.printHeader1("Parameter Details");
    out.printSection(BuildConfigTemplate.PARAMETER_DETAILS);
    listParametersWithDetails(node, out, SKIP_NODES);
  }
}
