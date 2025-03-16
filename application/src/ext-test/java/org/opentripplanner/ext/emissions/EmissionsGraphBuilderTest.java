package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.framework.application.OtpFileNames;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class EmissionsGraphBuilderTest {

  private final ResourceLoader RES = ResourceLoader.of(EmissionsGraphBuilderTest.class);
  private final File CO2_GTFS_ZIP = RES.file("emissions-test-gtfs.zip");
  private final File CO2_GTFS = RES.file("emissions-test-gtfs/");
  private final String CONFIG_PATH = "standalone/config/" + OtpFileNames.BUILD_CONFIG_FILENAME;
  private final BuildConfig buildConfig = new BuildConfig(
    jsonNodeFromResource(CONFIG_PATH),
    CONFIG_PATH,
    true
  );
  private final Multimap<FileType, DataSource> inputData = ArrayListMultimap.create();

  @Test
  void testMultipleGtfsDataReading() {
    inputData.put(GTFS, new ZipFileDataSource(CO2_GTFS_ZIP, GTFS));
    inputData.put(GTFS, new DirectoryDataSource(CO2_GTFS, GTFS));
    Iterable<ConfiguredCompositeDataSource<GtfsFeedParameters>> configuredDataSource =
      getGtfsConfiguredDatasource();
    EmissionsRepository emissionsRepository = new EmissionsRepository();
    EmissionsGraphBuilder emissionsGraphBuilder = new EmissionsGraphBuilder(
      configuredDataSource,
      buildConfig,
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

  private Iterable<
    ConfiguredCompositeDataSource<GtfsFeedParameters>
  > getGtfsConfiguredDatasource() {
    return inputData
      .get(GTFS)
      .stream()
      .map(it -> new ConfiguredCompositeDataSource<>(it, getGtfsFeedConfig(it)))
      .toList();
  }

  private GtfsFeedParameters getGtfsFeedConfig(DataSource dataSource) {
    return buildConfig.transitFeeds
      .gtfsFeeds()
      .stream()
      .findFirst()
      .orElse(buildConfig.gtfsDefaults.copyOf().withSource(dataSource.uri()).build());
  }
}
