package org.opentripplanner.model.projectinfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProjectInfoTest {

  @Test
  public void projectInfo() {
    ProjectInfo p = ProjectInfo.projectInfo();

    p.otpConfigVersion = "1";
    assertEquals("1", p.otpConfigVersion);
    assertNull(p.buildConfigVersion);
    assertNull(p.routerConfigVersion);

    assertEquals(2, p.version.major);
    assertTrue(p.version.minor >= 0);
    assertTrue(p.version.patch >= 0);

    if (!"UNKNOWN".equals(p.versionControl.branch)) {
      assertTrue(p.versionControl.commit, p.versionControl.commit.matches("[a-f0-9]{8,}"));
      assertNotNull(p.versionControl.branch);
      assertNotNull(p.versionControl.buildTime);
      assertNotNull(p.versionControl.commitTime);
    }
    else {
      assertEquals(p.versionControl.commit, "UNKNOWN");
      assertEquals(p.versionControl.branch, "UNKNOWN");
      assertEquals(p.versionControl.buildTime, "UNKNOWN");
      assertEquals(p.versionControl.commitTime, "UNKNOWN");
    }
  }
}