package org.opentripplanner.generate.doc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.generate.doc.framework.ConfigTypeTable.configTypeTable;
import static org.opentripplanner.generate.doc.framework.OTPFeatureTable.otpFeaturesTable;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("NewClassNamingConvention")
public class ConfigurationDocTest {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationDocTest.class);

  private static final File FILE = new File("docs", "Configuration.md");

  private static final String CONFIG_TYPE_PLACEHOLDER = "CONFIGURATION-TYPES-TABLE";
  private static final String OTP_FEATURE_PLACEHOLDER = "OTP-FEATURE-TABLE";
  public static final String NEW_LINE = "\n";

  /**
   * NOTE! This test updates the {@code docs/Configuration.md} document based on the latest
   * version of the code. The following is auto generated:
   * <ul>
   *   <li>The configuration type table</li>
   *   <li>The list of OTP features</li>
   * </ul>
   * This test fails if the document have changed. This make sure that this test fails in the
   * CI pipeline if config file changes is not committed. Manually inspect the changes in the
   * configuration, commit the configuration document, and run test again to pass.
   */
  @Test
  public void updateConfigurationDoc() {
    // Read and close inout file (same as output file)
    String doc = readInConfigurationFile();
    doc = replaceSection(doc, CONFIG_TYPE_PLACEHOLDER, air(configTypeTable()));
    doc = replaceSection(doc, OTP_FEATURE_PLACEHOLDER, air(otpFeaturesTable()));
    writeToConfigurationFile(doc);

    assertEquals(doc, readInConfigurationFile());
  }

  private String readInConfigurationFile() {
    try (var is = new FileInputStream(FILE)) {
      return new String(is.readAllBytes(), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void writeToConfigurationFile(String doc) {
    try (var fileOut = new FileOutputStream(FILE)) {
      var out = new PrintWriter(fileOut);
      out.write(doc);
      out.flush();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static String air(String section) {
    return NEW_LINE + NEW_LINE + section + NEW_LINE;
  }
}
