package org.opentripplanner.model.projectinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class OtpProjectInfoTest {

  @Test
  public void projectInfo() {
    OtpProjectInfo p = OtpProjectInfo.projectInfo();

    System.out.println(p);

    p.otpConfigVersion = "1";
    assertEquals("1", p.otpConfigVersion);
    assertNull(p.buildConfigVersion);
    assertNull(p.routerConfigVersion);

    assertEquals(2, p.version.major);
    assertTrue(p.version.minor >= 0);
    assertTrue(p.version.patch >= 0);

    if (!"UNKNOWN".equals(p.versionControl.branch)) {
      assertTrue(p.versionControl.commit.matches("[a-f0-9]{8,}"), p.versionControl.commit);
      assertNotNull(p.versionControl.branch);
      assertNotNull(p.versionControl.buildTime);
      assertNotNull(p.versionControl.commitTime);
    } else {
      assertEquals("UNKNOWN", p.graphFileHeaderInfo.otpSerializationVersionIdPadded());
      assertEquals("UNKNOWN", p.versionControl.commit);
      assertEquals("UNKNOWN", p.versionControl.branch);
      assertEquals("UNKNOWN", p.versionControl.buildTime);
      assertEquals("UNKNOWN", p.versionControl.commitTime);
    }
  }

  @Test
  public void matchesRunningOTPInstance() {
    VersionControlInfo vci = new VersionControlInfo();
    GraphFileHeader unknown = new GraphFileHeader();
    GraphFileHeader ver1 = new GraphFileHeader("1");
    GraphFileHeader verX = new GraphFileHeader("X");

    // Given: project info with known version: 1
    OtpProjectInfo p = new OtpProjectInfo("1.1.1", ver1, vci);

    // Match same version and unknown
    assertTrue(p.matchesRunningOTPInstance(ver1));
    assertTrue(p.matchesRunningOTPInstance(unknown));
    // Fail to match other version
    assertFalse(p.matchesRunningOTPInstance(verX));

    // Given: project info with unknown version
    p = new OtpProjectInfo("1.1.1", unknown, vci);
    // Match any version
    assertTrue(p.matchesRunningOTPInstance(verX));
    assertTrue(p.matchesRunningOTPInstance(unknown));
  }
}
