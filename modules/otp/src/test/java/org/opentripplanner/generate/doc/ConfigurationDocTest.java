package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.generate.doc.support.ConfigTypeTable.configTypeTable;
import static org.opentripplanner.generate.doc.support.OTPFeatureTable.otpFeaturesTable;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;

@GeneratesDocumentation
public class ConfigurationDocTest {

  private static final File TEMPLATE = new File(TEMPLATE_PATH, "Configuration.md");

  private static final File OUT_FILE = new File(USER_DOC_PATH, "Configuration.md");

  private static final String CONFIG_TYPE_PLACEHOLDER = "CONFIGURATION-TYPES-TABLE";
  private static final String OTP_FEATURE_PLACEHOLDER = "OTP-FEATURE-TABLE";

  /**
   * NOTE! This test updates the {@code doc/user/Configuration.md} document based on the latest
   * version of the code. The following is auto generated:
   * <ul>
   *   <li>The configuration type table</li>
   *   <li>The list of OTP features</li>
   * </ul>
   * This test fails if the document has changed. This makes sure that this test fails in the
   * CI pipeline if config file changes are not committed. Manually inspect the changes in the
   * configuration, commit the configuration document, and run test again to pass.
   */
  @Test
  public void updateConfigurationDoc() {
    // Read and close input file (same as output file)
    String doc = readFile(TEMPLATE);
    String original = readFile(OUT_FILE);

    doc = replaceSection(doc, CONFIG_TYPE_PLACEHOLDER, configTypeTable());
    doc = replaceSection(doc, OTP_FEATURE_PLACEHOLDER, otpFeaturesTable());
    writeFile(OUT_FILE, doc);

    assertFileEquals(original, OUT_FILE);
  }
}
