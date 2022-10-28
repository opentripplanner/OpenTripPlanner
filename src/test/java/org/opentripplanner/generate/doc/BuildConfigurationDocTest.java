package org.opentripplanner.generate.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeFromResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.MarkDownDocWriter;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class BuildConfigurationDocTest {

  private static final File FILE = new File("docs", "BuildConfiguration-poc.md");

  private static final String PARAMETERS_TABLE = "PARAMETERS-TABLE";
  private static final String PARAMETERS_DETAILS = "PARAMETERS-DETAILS";

  private static final String BUILD_CONFIG_FILENAME = "standalone/config/build-config.json";
  private static final SkipNodes SKIP_NODES = SkipNodes.of(
    "dataOverlay",
    "/docs/sandbox/DataOverlay.md",
    "transferRequests",
    "/docs/RoutingRequest.md"
  );

  /**
   * NOTE! This test updates the {@code docs/Configuration.md} document based on the latest
   * version of the code. The following is auto generated:
   * <ul>
   *   <li>The configuration type table</li>
   *   <li>The list of OTP features</li>
   * </ul>
   */
  @Test
  public void updateBuildConfigurationDoc() {
    NodeAdapter node = readBuildConfig();

    // Read and close inout file (same as output file)
    String doc = readFile(FILE);

    doc = replaceSection(doc, PARAMETERS_TABLE, getParameterSummaryTable(node));
    doc = replaceSection(doc, PARAMETERS_DETAILS, getParameterDetailsTable(node));

    writeFile(FILE, doc);

    assertEquals(doc, readFile(FILE));
  }

  private NodeAdapter readBuildConfig() {
    var json = jsonNodeFromResource(BUILD_CONFIG_FILENAME);
    var conf = new BuildConfig(json, BUILD_CONFIG_FILENAME, false);
    return conf.asNodeAdapter();
  }

  private String getParameterSummaryTable(NodeAdapter node) {
    return new ParameterSummaryTable(SKIP_NODES).createTable(node).toMarkdownTable();
  }

  private String getParameterDetailsTable(NodeAdapter node) {
    var stream = new ByteArrayOutputStream();
    var out = new MarkDownDocWriter(new PrintStream(stream));
    ParameterDetailsList.listParametersWithDetails(node, out, SKIP_NODES);
    return stream.toString(StandardCharsets.UTF_8);
  }
}
