package org.opentripplanner.standalone.config.buildconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersList;
import org.opentripplanner.osm.tagmapping.OsmTagMapperSource;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class OsmConfigTest {

  @Test
  void mapOsmDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        osmDefaults: {
         'osmTagMapping': 'finland',
         'timeZone' : 'Europe/Helsinki'
       }
      }
      """
    );

    var subject = OsmConfig.mapOsmDefaults(nodeAdapter, "osmDefaults");

    assertNull(subject.source());
    assertEquals(OsmTagMapperSource.FINLAND, subject.osmTagMapper());
    assertEquals(ZoneIds.HELSINKI, subject.timeZone());
  }

  @Test
  void mapMissingOsmDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
      }
      """
    );

    var subject = OsmConfig.mapOsmDefaults(nodeAdapter, "osmDefaults");

    assertNull(subject.source());
    assertEquals(OsmTagMapperSource.DEFAULT, subject.osmTagMapper());
    assertNull(subject.timeZone());
  }

  @Test
  void mapOsmExtractWithDefaults() {
    NodeAdapter defaultsAdapter = newNodeAdapterForTest(
      """
      {
        osmDefaults: {
         'osmTagMapping': 'finland',
         'timeZone' : 'Europe/Helsinki'
       }
      }
      """
    );

    var defaults = OsmConfig.mapOsmDefaults(defaultsAdapter, "osmDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        'osm': [{
          'source': 'https://foo.bar/osm.pbf'
        }]
      }
      """
    );

    OsmExtractParametersList subject = OsmConfig.mapOsmConfig(nodeAdapter, "osm", defaults);
    var osmExtractParameters = subject.parameters.get(0);

    assertEquals("https://foo.bar/osm.pbf", osmExtractParameters.source().toString());
    assertEquals(OsmTagMapperSource.FINLAND, osmExtractParameters.osmTagMapper());
    assertEquals(ZoneIds.HELSINKI, osmExtractParameters.timeZone());
  }

  @Test
  void mapOsmExtractsWithConflictingDefaults() {
    NodeAdapter defaultsAdapter = newNodeAdapterForTest(
      """
      {
        osmDefaults: {
         'osmTagMapping': 'finland',
         'timeZone' : 'Europe/Helsinki'
       }
      }
      """
    );

    var defaults = OsmConfig.mapOsmDefaults(defaultsAdapter, "osmDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        'osm': [
          {
            'source': 'https://foo.bar/atlanta-osm.pbf',
            'osmTagMapping': 'atlanta',
            'timeZone' : 'US/Eastern'
          },
          {
            'source': 'https://foo.bar/houston-osm.pbf',
            'osmTagMapping': 'houston',
            'timeZone' : 'US/Central'
          }
        ]
      }
      """
    );

    OsmExtractParametersList subject = OsmConfig.mapOsmConfig(nodeAdapter, "osm", defaults);
    var atlantaOsmExtractParameters = subject.parameters.get(0);

    assertEquals(
      "https://foo.bar/atlanta-osm.pbf",
      atlantaOsmExtractParameters.source().toString()
    );
    assertEquals(OsmTagMapperSource.ATLANTA, atlantaOsmExtractParameters.osmTagMapper());
    assertEquals(ZoneIds.US_ESTERN, atlantaOsmExtractParameters.timeZone());

    var houstonOsmExtractParameters = subject.parameters.get(1);
    assertEquals(
      "https://foo.bar/houston-osm.pbf",
      houstonOsmExtractParameters.source().toString()
    );
    assertEquals(OsmTagMapperSource.HOUSTON, houstonOsmExtractParameters.osmTagMapper());
    assertEquals(ZoneIds.US_CENTRAL, houstonOsmExtractParameters.timeZone());
  }

  @Test
  void mapOsmExtractWithNoDefaults() {
    NodeAdapter noDefaultsAdapter = newNodeAdapterForTest(
      """
      {
      }
      """
    );

    var defaults = OsmConfig.mapOsmDefaults(noDefaultsAdapter, "osmDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        'osm': [
          {
            'source': 'https://foo.bar/atlanta-osm.pbf',
            'osmTagMapping': 'atlanta',
            'timeZone' : 'US/Eastern'
          }
        ]
      }
      """
    );

    OsmExtractParametersList subject = OsmConfig.mapOsmConfig(nodeAdapter, "osm", defaults);
    var atlantaOsmExtractParameters = subject.parameters.get(0);

    assertEquals(
      "https://foo.bar/atlanta-osm.pbf",
      atlantaOsmExtractParameters.source().toString()
    );
    assertEquals(OsmTagMapperSource.ATLANTA, atlantaOsmExtractParameters.osmTagMapper());
    assertEquals(ZoneIds.US_ESTERN, atlantaOsmExtractParameters.timeZone());
  }
}
