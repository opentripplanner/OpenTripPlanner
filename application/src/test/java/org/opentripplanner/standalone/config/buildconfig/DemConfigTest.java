package org.opentripplanner.standalone.config.buildconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParametersList;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class DemConfigTest {

  @Test
  void mapDemDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        demDefaults: {
         'elevationUnitMultiplier': 0.1
       }
      }
      """
    );

    var subject = DemConfig.mapDemDefaultsConfig(nodeAdapter, "demDefaults");

    assertNull(subject.source());
    assertEquals(0.1, subject.elevationUnitMultiplier());
  }

  @Test
  void mapMissingDemDefaultParameters() {
    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
      }
      """
    );

    var subject = DemConfig.mapDemDefaultsConfig(nodeAdapter, "demDefaults");

    assertNull(subject.source());
    assertEquals(1, subject.elevationUnitMultiplier());
  }

  @Test
  void mapDemExtractWithDefaults() {
    NodeAdapter defaultsAdapter = newNodeAdapterForTest(
      """
      {
        demDefaults: {
         'elevationUnitMultiplier': 0.1
       }
      }
      """
    );

    var defaults = DemConfig.mapDemDefaultsConfig(defaultsAdapter, "demDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        'dem': [{
          'source': 'https://foo.bar/dem.tif'
        }]
      }
      """
    );

    DemExtractParametersList subject = DemConfig.mapDemConfig(nodeAdapter, "dem", defaults);
    var demExtractParameters = subject.demExtracts().get(0);

    assertEquals("https://foo.bar/dem.tif", demExtractParameters.source().toString());
    assertEquals(0.1, demExtractParameters.elevationUnitMultiplier());
  }

  @Test
  void mapDemExtractsWithConflictingDefaults() {
    NodeAdapter defaultsAdapter = newNodeAdapterForTest(
      """
      {
        demDefaults: {
         'elevationUnitMultiplier': 1.5
       }
      }
      """
    );

    var defaults = DemConfig.mapDemDefaultsConfig(defaultsAdapter, "demDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        'dem': [
          {
            'source': 'https://foo.bar/atlanta-dem.tif',
            'elevationUnitMultiplier': 1.0
          },
          {
            'source': 'https://foo.bar/houston-dem.tif',
            'elevationUnitMultiplier': 0.1
          }
        ]
      }
      """
    );

    DemExtractParametersList subject = DemConfig.mapDemConfig(nodeAdapter, "dem", defaults);
    var atlantaDemExtractParameters = subject.demExtracts().get(0);

    assertEquals(
      "https://foo.bar/atlanta-dem.tif",
      atlantaDemExtractParameters.source().toString()
    );
    assertEquals(1, atlantaDemExtractParameters.elevationUnitMultiplier());

    var houstonDemExtractParameters = subject.demExtracts().get(1);
    assertEquals(
      "https://foo.bar/houston-dem.tif",
      houstonDemExtractParameters.source().toString()
    );
    assertEquals(0.1, houstonDemExtractParameters.elevationUnitMultiplier());
  }

  @Test
  void mapDemExtractWithNoDefaults() {
    NodeAdapter noDefaultsAdapter = newNodeAdapterForTest(
      """
      {
      }
      """
    );

    var defaults = DemConfig.mapDemDefaultsConfig(noDefaultsAdapter, "demDefaults");

    NodeAdapter nodeAdapter = newNodeAdapterForTest(
      """
      {
        'dem': [
           {
            'source': 'https://foo.bar/atlanta-dem.tif',
            'elevationUnitMultiplier': 1.5
          }
        ]
      }
      """
    );

    DemExtractParametersList subject = DemConfig.mapDemConfig(nodeAdapter, "dem", defaults);
    var atlantaDemExtractParameters = subject.demExtracts().get(0);

    assertEquals(
      "https://foo.bar/atlanta-dem.tif",
      atlantaDemExtractParameters.source().toString()
    );
    assertEquals(1.5, atlantaDemExtractParameters.elevationUnitMultiplier());
  }
}
