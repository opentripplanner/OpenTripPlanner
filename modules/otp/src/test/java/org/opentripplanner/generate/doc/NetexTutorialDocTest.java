package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;
import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
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
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

@GeneratesDocumentation
class NetexTutorialDocTest {

  private static final String NETEX_TUTORIAL_MD = "Netex-Tutorial.md";
  private static final File TEMPLATE = new File(TEMPLATE_PATH, NETEX_TUTORIAL_MD);
  private static final File OUT_FILE = new File(USER_DOC_PATH, NETEX_TUTORIAL_MD);

  private static final String TUTORIAL_PATH = "standalone/config/netex-tutorial/";

  @Test
  void updateTutorialDoc() {
    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    template = replaceSection(template, "build-config", toJson(BUILD_CONFIG_FILENAME));
    template = replaceSection(template, "router-config", toJson(ROUTER_CONFIG_FILENAME));

    writeFile(OUT_FILE, template);
    assertFileEquals(original, OUT_FILE);
  }

  private static String toJson(String fileName) {
    var path = TUTORIAL_PATH + fileName;
    var nodeAdapter = loadBuildConfig(path);
    var buf = new DocBuilder();
    buf.addExample(fileName, nodeAdapter.rawNode());
    return buf.toString();
  }

  private static NodeAdapter loadBuildConfig(String buildConfigPath) {
    var json = jsonNodeFromResource(buildConfigPath);
    var conf = new RouterConfig(json, buildConfigPath, false);
    return conf.asNodeAdapter();
  }
}
