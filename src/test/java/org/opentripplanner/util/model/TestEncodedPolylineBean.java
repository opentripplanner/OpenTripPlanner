package org.opentripplanner.util.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestEncodedPolylineBean {

  @Test
  public void testEncodedPolylineBean() {
    // test taken from an example usage
    EncodedPolylineBean eplb = new EncodedPolylineBean(
      "wovwF|`pbMgPzg@CHEHCFEFCFEFEFEDGDEDEDGBEBGDG@",
      17
    );
    assertEquals("wovwF|`pbMgPzg@CHEHCFEFCFEFEFEDGDEDEDGBEBGDG@", eplb.getPoints());
    assertEquals(17, eplb.getLength());
  }
}
