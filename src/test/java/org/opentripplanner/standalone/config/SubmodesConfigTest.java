package org.opentripplanner.standalone.config;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

public class SubmodesConfigTest {

  private static final String SUBMODES_TEST_FILE =
      "src/main/resources/org/opentripplanner/submodes/submodes.csv";

  @Test
  public void testLoadSubmodesConfig() throws FileNotFoundException {
    FileInputStream input = new FileInputStream(SUBMODES_TEST_FILE);
    SubmodesConfig submodesConfig = new SubmodesConfig(input);
    assertEquals(30, submodesConfig.getSubmodes().size());
  }
}
