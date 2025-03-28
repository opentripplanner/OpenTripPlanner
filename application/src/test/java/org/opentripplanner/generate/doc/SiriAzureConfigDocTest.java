package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;
import static org.opentripplanner.utils.text.MarkdownFormatter.HEADER_4;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.ParameterDetailsList;
import org.opentripplanner.generate.doc.framework.ParameterSummaryTable;
import org.opentripplanner.generate.doc.framework.SkipNodes;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
public class SiriAzureConfigDocTest {

  private static final File TEMPLATE = new File(TEMPLATE_PATH, "sandbox/siri/SiriAzureUpdater.md");
  private static final File OUT_FILE = new File(USER_DOC_PATH, "sandbox/siri/SiriAzureUpdater.md");

  private static final String ROUTER_CONFIG_PATH = "standalone/config/" + ROUTER_CONFIG_FILENAME;
  private static final Set<String> INCLUDE_UPDATERS = Set.of(
    "siri-azure-et-updater",
    "siri-azure-sx-updater"
  );
  private static final SkipNodes SKIP_NODES = SkipNodes.of().build();
  public static final ObjectMapper mapper = new ObjectMapper();

  /**
   * NOTE! This test updates the {@code doc/user/sandbox/SIRI-Config.md} document based on the latest
   * version of the code.
   */
  @Test
  public void updateSiriDoc() {
    NodeAdapter node = readUpdaterConfig();

    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    for (String childName : node.listChildrenByName()) {
      var child = node.child(childName);
      var type = child.typeQualifier();

      if (INCLUDE_UPDATERS.contains(type)) {
        template = replaceSection(template, type, updaterDoc(child));
      }
    }

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readUpdaterConfig() {
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
      buf.header(5, "Parameter details", null).addSection(details);
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
