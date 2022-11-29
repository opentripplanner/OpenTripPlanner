package org.opentripplanner.standalone.config.buildconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.standalone.config.framework.JsonSupport.newNodeAdapterForTest;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersList;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;
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
    assertEquals(OsmTagMapper.Source.FINLAND, subject.osmTagMapper());
    assertEquals(ZoneId.of("Europe/Helsinki"), subject.timeZone());
  }

  @Test
  void mapMissingOsmDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest("""
      {
      }
      """);

    var subject = OsmConfig.mapOsmDefaults(nodeAdapter, "osmDefaults");

    assertNull(subject.source());
    assertEquals(OsmTagMapper.Source.DEFAULT, subject.osmTagMapper());
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
    assertEquals(OsmTagMapper.Source.FINLAND, osmExtractParameters.osmTagMapper());
    assertEquals(ZoneId.of("Europe/Helsinki"), osmExtractParameters.timeZone());
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
    assertEquals(OsmTagMapper.Source.ATLANTA, atlantaOsmExtractParameters.osmTagMapper());
    assertEquals(ZoneId.of("US/Eastern"), atlantaOsmExtractParameters.timeZone());

    var houstonOsmExtractParameters = subject.parameters.get(1);
    assertEquals(
      "https://foo.bar/houston-osm.pbf",
      houstonOsmExtractParameters.source().toString()
    );
    assertEquals(OsmTagMapper.Source.HOUSTON, houstonOsmExtractParameters.osmTagMapper());
    assertEquals(ZoneId.of("US/Central"), houstonOsmExtractParameters.timeZone());
  }

  @Test
  void mapOsmExtractWithNoDefaults() {
    NodeAdapter noDefaultsAdapter = newNodeAdapterForTest("""
      {
      }
      """);

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
    assertEquals(OsmTagMapper.Source.ATLANTA, atlantaOsmExtractParameters.osmTagMapper());
    assertEquals(ZoneId.of("US/Eastern"), atlantaOsmExtractParameters.timeZone());
  }
}
