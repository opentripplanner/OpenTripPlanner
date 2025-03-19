package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.datastore.api.FileType.GTFS;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.ext.emissions.internal.DefaultEmissionsRepository;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.gtfs.config.GtfsDefaultParameters;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class EmissionsGraphBuilderTest {

  private final ResourceLoader RES = ResourceLoader.of(EmissionsGraphBuilderTest.class);
  private final CompositeDataSource CO2_GTFS_ZIP = RES.catalogDataSource(
    "emissions-test-gtfs.zip",
    GTFS
  );
  private final CompositeDataSource CO2_GTFS = RES.catalogDataSource("emissions-test-gtfs/", GTFS);

  @Test
  void testMultipleGtfsDataReading() {
    var configuredDataSources = List.of(
      configuredDataSource(CO2_GTFS_ZIP),
      configuredDataSource(CO2_GTFS)
    );

    var emissionsRepository = new DefaultEmissionsRepository();
    var emissionsGraphBuilder = new EmissionsGraphBuilder(
      configuredDataSources,
      BuildConfig.DEFAULT.emissions,
      emissionsRepository,
      DataImportIssueStore.NOOP
    );
    emissionsGraphBuilder.buildGraph();
    assertEquals(
      Optional.of(0.006),
      emissionsRepository.getCO2EmissionsById(new FeedScopedId("emissionstest", "1001"))
    );
    assertEquals(
      Optional.of(0.041),
      emissionsRepository.getCO2EmissionsById(new FeedScopedId("emissionstest1", "1002"))
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
}
