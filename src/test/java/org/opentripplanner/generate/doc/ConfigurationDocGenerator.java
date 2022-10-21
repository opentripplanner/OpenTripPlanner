package org.opentripplanner.generate.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.ConfigTypeTable;
import org.opentripplanner.generate.doc.framework.TemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("NewClassNamingConvention")
public class ConfigurationDocGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationDocGenerator.class);

  private static final File FILE = new File("docs", "Configuration.md");

  private static final String CONFIG_TYPE_PLACEHOLDER = "CONFIGURATION-TYPES-TABLE";
  public static final String NEW_LINE = "\n";

  /**
   * NOTE! This test updates the {@code docs/Configuration.md} document based on the latest
   * version of the code. The following is auto generated:
   * <ul>
   *   <li>The configuration type table</li>
   * </ul>
   */
  @Test
  public void updateConfigurationDoc() {
    try (var is = new FileInputStream(FILE)) {
      String doc = new String(is.readAllBytes(), StandardCharsets.UTF_8);

      var configTypesTable = NEW_LINE + NEW_LINE + ConfigTypeTable.configTypeTable() + NEW_LINE;
      doc = TemplateUtil.replace(doc, CONFIG_TYPE_PLACEHOLDER, configTypesTable);

      var out = new PrintWriter(new FileOutputStream(FILE));

      out.write(doc);
      out.flush();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
