package org.opentripplanner.standalone.config.buildconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class NetexConfigTest {

  @Test
  void mapNetexDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        netexDefaults: {
         'feedId': 'EN',
         'moduleFilePattern' : 'netex_.*\\\\.zip',
         'sharedFilePattern' : '_stops.xml',
         'sharedGroupFilePattern' : '_(\\\\w{3})_shared_data.xml',
         'groupFilePattern' : '(\\\\w{3})_.*\\\\.xml',
         'ignoreFilePattern' : '(__.*|\\\\..*)'
       }
      }
      """
    );

    var subject = NetexConfig.mapNetexDefaultParameters(nodeAdapter, "netexDefaults");

    assertCommonParameters(subject);
  }

  @Test
  void mapNetexFeed() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
         'source': 'https://my.source.no/netex.obj',
         'feedId': 'EN',
         'moduleFilePattern' : 'netex_.*\\\\.zip',
         'sharedFilePattern' : '_stops.xml',
         'sharedGroupFilePattern' : '_(\\\\w{3})_shared_data.xml',
         'groupFilePattern' : '(\\\\w{3})_.*\\\\.xml',
         'ignoreFilePattern' : '(__.*|\\\\..*)'
      }
      """
    );

    NetexFeedParameters subject = NetexConfig.mapNetexFeed(
      nodeAdapter,
      NetexFeedParameters.DEFAULT
    );

    assertEquals("https://my.source.no/netex.obj", subject.source().toASCIIString());
    assertCommonParameters(subject);
  }

  private void assertCommonParameters(NetexFeedParameters subject) {
    assertEquals("EN", subject.feedId());
    assertTrue(subject.sharedFilePattern().matcher("_stops.xml").matches());
    assertTrue(subject.sharedGroupFilePattern().matcher("_RUT_shared_data.xml").matches());
    assertTrue(subject.groupFilePattern().matcher("RUT_anything.xml").matches());
    assertTrue(subject.ignoreFilePattern().matcher(".ignore").matches());
    assertTrue(subject.ignoreFilePattern().matcher("__ignore").matches());
  }
}
