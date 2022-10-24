package org.opentripplanner.generate.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.ConfigTypeTable.configTypeTable;
import static org.opentripplanner.generate.doc.framework.OTPFeatureTable.otpFeaturesTable;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;

import java.io.File;
import org.junit.jupiter.api.Test;

public class ConfigurationDocTest {

  private static final File FILE = new File("docs", "Configuration.md");

  private static final String CONFIG_TYPE_PLACEHOLDER = "CONFIGURATION-TYPES-TABLE";
  private static final String OTP_FEATURE_PLACEHOLDER = "OTP-FEATURE-TABLE";

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
    String doc = readFile(FILE);
    doc = replaceSection(doc, CONFIG_TYPE_PLACEHOLDER, configTypeTable());
    doc = replaceSection(doc, OTP_FEATURE_PLACEHOLDER, otpFeaturesTable());
    writeFile(FILE, doc);

    assertEquals(doc, readFile(FILE));
  }
}
