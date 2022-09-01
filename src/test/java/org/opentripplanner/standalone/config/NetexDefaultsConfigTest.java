package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.feed.NetexDefaultsConfig;

class NetexDefaultsConfigTest {

  @Test
  void testDefaultPatternMatchers() {
    NetexDefaultsConfig subject = new NetexDefaultsConfig(
      new NodeAdapter(MissingNode.getInstance(), "NetexParametersTest")
    );

    assertTrue(subject.ignoreFilePattern.matcher("").matches());
    assertTrue(subject.sharedFilePattern.matcher("shared-data.xml").matches());
    assertTrue(subject.sharedGroupFilePattern.matcher("RUT-anything-shared.xml").matches());
    assertTrue(subject.groupFilePattern.matcher("RUT-anything.xml").matches());
    assertEquals("DefaultFeed", subject.netexFeedId);
  }

  @Test
  void testLoadingConfigAndPatternMatchers() throws IOException {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      "{\n" +
      "    'moduleFilePattern' : 'netex_.*\\\\.zip',\n" +
      "    'ignoreFilePattern' : '(__.*|\\\\..*)',\n" +
      "    'sharedFilePattern' : '_stops.xml',\n" +
      "    'sharedGroupFilePattern' : '_(\\\\w{3})_shared_data.xml',\n" +
      "    'groupFilePattern' : '(\\\\w{3})_.*\\\\.xml',\n" +
      "    'netexFeedId': 'RB'\n" +
      "}"
    );

    NetexDefaultsConfig subject = new NetexDefaultsConfig(nodeAdapter);

    assertTrue(subject.ignoreFilePattern.matcher(".ignore").matches());
    assertTrue(subject.ignoreFilePattern.matcher("__ignore").matches());
    assertTrue(subject.sharedFilePattern.matcher("_stops.xml").matches());
    assertTrue(subject.sharedGroupFilePattern.matcher("_RUT_shared_data.xml").matches());
    assertTrue(subject.groupFilePattern.matcher("RUT_anything.xml").matches());
    assertEquals("RB", subject.netexFeedId);
  }
}
