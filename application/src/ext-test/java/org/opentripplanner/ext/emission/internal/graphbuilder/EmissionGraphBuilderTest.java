package org.opentripplanner.ext.emission.internal.graphbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emission.EmissionTestData;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.emission.parameters.EmissionFeedParameters;
import org.opentripplanner.ext.emission.parameters.EmissionParameters;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.ConfiguredDataSource;
import org.opentripplanner.gtfs.config.GtfsDefaultParameters;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.service.TimetableRepository;

public class EmissionGraphBuilderTest implements EmissionTestData {

  @Test
  void testMultipleGtfsDataReading() {
    var gtfsDataSources = List.of(
      configuredDataSource(gtfsWithEmissionZip()),
      configuredDataSource(gtfsWithEmissionDir())
    );
    var feedDataSources = List.of(configuredDataSource(emissionOnRoutes()));
    var emissionRepository = new DefaultEmissionRepository();

    var subject = new EmissionGraphBuilder(
      gtfsDataSources,
      feedDataSources,
      EmissionParameters.DEFAULT,
      emissionRepository,
      new TimetableRepository(),
      DataImportIssueStore.NOOP
    );
    subject.buildGraph();
    assertEquals(
      Emission.co2_g(.006),
      emissionRepository.routePassengerEmissionsPerMeter(ROUTE_ID_GD_1001)
    );
    assertEquals(
      Emission.co2_g(0.041),
      emissionRepository.routePassengerEmissionsPerMeter(ROUTE_ID_GZ_1002)
    );
    assertEquals(
      Emission.co2_g(0.006),
      emissionRepository.routePassengerEmissionsPerMeter(ROUTE_ID_EM_R1)
    );
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
