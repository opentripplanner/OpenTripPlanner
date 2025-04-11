package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emissions.internal.DefaultEmissionsRepository;
import org.opentripplanner.ext.emissions.model.EmissionFeedParameters;
import org.opentripplanner.ext.emissions.model.EmissionParameters;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.ConfiguredDataSource;
import org.opentripplanner.gtfs.config.GtfsDefaultParameters;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.test.support.ResourceLoader;

public class EmissionsGraphBuilderTest implements EmissionTestData {

  private final ResourceLoader RES = ResourceLoader.of(EmissionsGraphBuilderTest.class);

  @Test
  void testMultipleGtfsDataReading() {
    var gtfsDataSources = List.of(
      configuredDataSource(gtfsWithEmissionZip()),
      configuredDataSource(gtfsWithEmissionDir())
    );
    var feedDataSources = List.of(configuredDataSource(emissionFeed()));

    var emissionsRepository = new DefaultEmissionsRepository();
    var emissionsGraphBuilder = new EmissionsGraphBuilder(
      gtfsDataSources,
      feedDataSources,
      EmissionParameters.DEFAULT,
      emissionsRepository,
      DataImportIssueStore.NOOP
    );
    emissionsGraphBuilder.buildGraph();
    assertEquals(Optional.of(0.006), emissionsRepository.getCO2EmissionsById(ROUTE_ID_GD_1001));
    assertEquals(Optional.of(0.041), emissionsRepository.getCO2EmissionsById(ROUTE_ID_GZ_1002));
    assertEquals(Optional.of(0.006), emissionsRepository.getCO2EmissionsById(ROUTE_ID_EM_R1));
  }

  private static ConfiguredCompositeDataSource<GtfsFeedParameters> configuredDataSource(
    CompositeDataSource dataSource
  ) {
    return new ConfiguredCompositeDataSource<>(
      dataSource,
      GtfsDefaultParameters.DEFAULT.withFeedInfo().withSource(dataSource.uri()).build()
    );
  }

  private static ConfiguredDataSource<EmissionFeedParameters> configuredDataSource(
    DataSource dataSource
  ) {
    return new ConfiguredDataSource<>(
      dataSource,
      new EmissionFeedParameters(FEED_FEED_ID, dataSource.uri())
    );
  }
}
