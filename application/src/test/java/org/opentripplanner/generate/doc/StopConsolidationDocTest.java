package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.test.support.ResourceLoader;

@GeneratesDocumentation
public class StopConsolidationDocTest {

  private static final String FILE_NAME = "StopConsolidation.md";
  private static final File TEMPLATE = new File(TEMPLATE_PATH, FILE_NAME);
  private static final File OUT_FILE = new File(USER_DOC_PATH + "/sandbox", FILE_NAME);

  private static final String CONFIG_FILENAME = "standalone/config/build-config.json";

  @Test
  public void updateDoc() {
    NodeAdapter node = readConfig();

    var lines = ResourceLoader.of(this).lines(
      "/org/opentripplanner/ext/stopconsolidation/consolidated-stops.csv",
      6
    );

    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    var joined = String.join("\n", lines);

    var csvExample =
      """
      ```
      %s
      ```
      """.formatted(joined);

    template = replaceSection(template, "config", updaterDoc(node));
    template = replaceSection(template, "file", csvExample);

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private NodeAdapter readConfig() {
    var json = jsonNodeFromResource(CONFIG_FILENAME);
    final String propName = "stopConsolidationFile";
    var node = json.path(propName);
    var jsonNode = TemplateUtil.jsonExampleBuilder(node).wrapInObject(propName).build();
    var adapter = new NodeAdapter(jsonNode, "source");
    var conf = new BuildConfig(adapter, false);
    return conf.asNodeAdapter();
  }

  private String updaterDoc(NodeAdapter node) {
    DocBuilder buf = new DocBuilder();
    addExample(buf, node);
    return buf.toString();
  }

  private void addExample(DocBuilder buf, NodeAdapter node) {
    var root = TemplateUtil.jsonExampleBuilder(node.rawNode()).build();
    buf.addExample("build-config.json", root);
  }
}
