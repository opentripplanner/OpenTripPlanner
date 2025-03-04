package org.opentripplanner.model.projectinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class MavenProjectVersionTest {

  @Test
  public void parse() {
    List.of(
      new TC("1", 1, 0, 0, ""),
      new TC("1.2", 1, 2, 0, ""),
      new TC("1.2.3", 1, 2, 3, ""),
      new TC("1-567", 1, 0, 0, "567"),
      new TC("1.2-567", 1, 2, 0, "567"),
      new TC("1.2.3-567", 1, 2, 3, "567"),
      new TC("1-rc-1", 1, 0, 0, "rc-1"),
      new TC("1.2.3-entur-31", 1, 2, 3, "entur-31"),
      new TC("1.2.3-SNAPSHOT", 1, 2, 3, "SNAPSHOT")
    ).forEach(tc -> {
      MavenProjectVersion v = MavenProjectVersion.parse(tc.input);
      assertEquals(tc.major, v.major, tc.input);
      assertEquals(tc.minor, v.minor, tc.input);
      assertEquals(tc.patch, v.patch, tc.input);
      assertEquals(tc.qualifier, v.qualifier, tc.input);
    });
  }

  @Test
  public void unqualifiedVersion() {
    assertEquals("1.0.0", MavenProjectVersion.parse("1").unqualifiedVersion());
    assertEquals("1.2.0", MavenProjectVersion.parse("1.2").unqualifiedVersion());
    assertEquals("1.2.3", MavenProjectVersion.parse("1.2.3").unqualifiedVersion());
    assertEquals("1.0.0", MavenProjectVersion.parse("1-rc-2").unqualifiedVersion());
  }

  @Test
  public void toStringTest() {
    assertEquals("1", MavenProjectVersion.parse("1").toString());
    assertEquals("1.2.3", MavenProjectVersion.parse("1.2.3").toString());
    assertEquals("1-rc-2", MavenProjectVersion.parse("1-rc-2").toString());
    assertEquals("UNKNOWN", MavenProjectVersion.parse("UNKNOWN").toString());
  }

  @Test
  public void sameVersion() {
    MavenProjectVersion ver = MavenProjectVersion.parse("1.4.5-rc-32");
    MavenProjectVersion verSame = MavenProjectVersion.parse("1.4.5-rc-32");
    assertTrue(ver.sameVersion(verSame));
  }

  static class TC {

    final String input;
    final int major;
    final int minor;
    final int patch;
    final String qualifier;

    public TC(String input, int major, int minor, int patch, String qualifier) {
      this.input = input;
      this.major = major;
      this.minor = minor;
      this.patch = patch;
      this.qualifier = qualifier;
    }

    @Override
    public String toString() {
      return input;
    }
  }
}
