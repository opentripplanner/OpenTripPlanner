package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.JsonSupport.newNodeAdapterForTest;

import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.feed.NetexDefaultParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class NetexDefaultParametersTest {

  @Test
  void testDefaultPatternMatchers() {
    NetexDefaultParameters subject = new NetexDefaultParameters(
      new NodeAdapter(MissingNode.getInstance(), "NetexParametersTest")
    );

    assertTrue(subject.ignoreFilePattern().matcher("").matches());
    assertTrue(subject.sharedFilePattern().matcher("shared-data.xml").matches());
    assertTrue(subject.sharedGroupFilePattern().matcher("RUT-anything-shared.xml").matches());
    assertTrue(subject.groupFilePattern().matcher("RUT-anything.xml").matches());
    assertEquals("DefaultFeed", subject.feedId());
  }

  @Test
  void testLoadingConfigAndPatternMatchers() throws IOException {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        'feedId': 'EN',
        'moduleFilePattern' : 'netex_.*\\\\.zip',
        'sharedFilePattern' : '_stops.xml',
        'sharedGroupFilePattern' : '_(\\\\w{3})_shared_data.xml',
        'groupFilePattern' : '(\\\\w{3})_.*\\\\.xml',
        'ignoreFilePattern' : '(__.*|\\\\..*)'
      }
      """
    );

    NetexDefaultParameters subject = new NetexDefaultParameters(nodeAdapter);

    assertEquals("EN", subject.feedId());
    assertTrue(subject.sharedFilePattern().matcher("_stops.xml").matches());
    assertTrue(subject.sharedGroupFilePattern().matcher("_RUT_shared_data.xml").matches());
    assertTrue(subject.groupFilePattern().matcher("RUT_anything.xml").matches());
    assertTrue(subject.ignoreFilePattern().matcher(".ignore").matches());
    assertTrue(subject.ignoreFilePattern().matcher("__ignore").matches());
  }
}
