package org.opentripplanner.framework.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;
import static org.opentripplanner.framework.application.OtpFileNames.OTP_CONFIG_FILENAME;
import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.framework.application.OtpFileNames.isConfigFile;

import org.junit.jupiter.api.Test;

class OtpFileNamesTest {

  @Test
  public void testIsConfigFile() {
    assertTrue(isConfigFile(OTP_CONFIG_FILENAME));
    assertTrue(isConfigFile(BUILD_CONFIG_FILENAME));
    assertTrue(isConfigFile(ROUTER_CONFIG_FILENAME));
    assertFalse(isConfigFile("not-config.json"));
  }
}
