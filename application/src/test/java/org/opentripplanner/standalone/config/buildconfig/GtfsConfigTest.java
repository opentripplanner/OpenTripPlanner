package org.opentripplanner.standalone.config.buildconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.transit.model.site.StopTransferPriority;

class GtfsConfigTest {

  @Test
  void mapGtfsDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        gtfsDefaults: {
         'removeRepeatedStops': false,
         'stationTransferPreference' : 'preferred',
         'discardMinTransferTimes': true,
         'blockBasedInterlining': false,
         'maxInterlineDistance': 300
       }
      }
      """
    );

    var subject = GtfsConfig.mapGtfsDefaultParameters(nodeAdapter, "gtfsDefaults");

    assertNull(subject.source());
    assertNull(subject.feedId());
    assertFalse(subject.removeRepeatedStops());
    assertEquals(StopTransferPriority.PREFERRED, subject.stationTransferPreference());
    assertTrue(subject.discardMinTransferTimes());
    assertFalse(subject.blockBasedInterlining());
    assertEquals(300, subject.maxInterlineDistance());
  }

  @Test
  void mapMissingGtfsDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
      }
      """
    );

    var subject = GtfsConfig.mapGtfsDefaultParameters(nodeAdapter, "gtfsDefaults");

    assertNull(subject.source());
    assertNull(subject.feedId());
    assertTrue(subject.removeRepeatedStops());
    assertEquals(StopTransferPriority.ALLOWED, subject.stationTransferPreference());
    assertFalse(subject.discardMinTransferTimes());
    assertTrue(subject.blockBasedInterlining());
    assertEquals(200, subject.maxInterlineDistance());
  }

  @Test
  void mapGtfsFeedWithDefaults() {
    NodeAdapter defaultsAdapter = newNodeAdapterForTest(
      """
      {
        gtfsDefaults: {
         'removeRepeatedStops': false,
         'stationTransferPreference' : 'preferred',
         'discardMinTransferTimes': true,
         'blockBasedInterlining': false,
         'maxInterlineDistance': 300
       }
      }
      """
    );

    var defaults = GtfsConfig.mapGtfsDefaultParameters(defaultsAdapter, "gtfsDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
         'source': 'https://foo.bar/gtfs.zip',
         'feedId': 'test'
      }
      """
    );

    GtfsFeedParameters subject = GtfsConfig.mapGtfsFeed(nodeAdapter, defaults);

    assertEquals("https://foo.bar/gtfs.zip", subject.source().toASCIIString());
    assertEquals("test", subject.feedId());
    assertFalse(subject.removeRepeatedStops());
    assertEquals(StopTransferPriority.PREFERRED, subject.stationTransferPreference());
    assertTrue(subject.discardMinTransferTimes());
    assertFalse(subject.blockBasedInterlining());
    assertEquals(300, subject.maxInterlineDistance());
  }

  @Test
  void mapGtfsFeedWithConflictingDefaults() {
    NodeAdapter defaultsAdapter = newNodeAdapterForTest(
      """
      {
        gtfsDefaults: {
         'removeRepeatedStops': true,
         'stationTransferPreference' : 'allowed',
         'discardMinTransferTimes': true,
         'blockBasedInterlining': true,
         'maxInterlineDistance': 300
       }
      }
      """
    );

    var defaults = GtfsConfig.mapGtfsDefaultParameters(defaultsAdapter, "gtfsDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
         'source': 'https://foo.bar/gtfs.zip',
         'feedId': 'test',
         'removeRepeatedStops': false,
         'stationTransferPreference' : 'preferred',
         'discardMinTransferTimes': false,
         'blockBasedInterlining': false,
         'maxInterlineDistance': 400
      }
      """
    );

    GtfsFeedParameters subject = GtfsConfig.mapGtfsFeed(nodeAdapter, defaults);

    assertEquals("https://foo.bar/gtfs.zip", subject.source().toASCIIString());
    assertEquals("test", subject.feedId());
    assertFalse(subject.removeRepeatedStops());
    assertEquals(StopTransferPriority.PREFERRED, subject.stationTransferPreference());
    assertFalse(subject.discardMinTransferTimes());
    assertFalse(subject.blockBasedInterlining());
    assertEquals(400, subject.maxInterlineDistance());
  }

  @Test
  void mapGtfsFeedWithNoDefaults() {
    NodeAdapter noDefaultsAdapter = newNodeAdapterForTest(
      """
      {
      }
      """
    );

    var defaults = GtfsConfig.mapGtfsDefaultParameters(noDefaultsAdapter, "gtfsDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
         'source': 'https://foo.bar/gtfs.zip',
         'feedId': 'test',
         'removeRepeatedStops': 'false',
         'stationTransferPreference' : 'preferred',
         'discardMinTransferTimes': true,
         'blockBasedInterlining': false,
         'maxInterlineDistance': 300
      }
      """
    );

    GtfsFeedParameters subject = GtfsConfig.mapGtfsFeed(nodeAdapter, defaults);

    assertEquals("https://foo.bar/gtfs.zip", subject.source().toASCIIString());
    assertEquals("test", subject.feedId());
    assertFalse(subject.removeRepeatedStops());
    assertEquals(StopTransferPriority.PREFERRED, subject.stationTransferPreference());
    assertTrue(subject.discardMinTransferTimes());
    assertFalse(subject.blockBasedInterlining());
    assertEquals(300, subject.maxInterlineDistance());
  }
}
