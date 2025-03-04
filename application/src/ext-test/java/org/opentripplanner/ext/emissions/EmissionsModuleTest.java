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
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.framework.application.OtpFileNames;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class EmissionsModuleTest {

  private final ResourceLoader RES = ResourceLoader.of(EmissionsModuleTest.class);
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
    inputData.put(GTFS, new FileDataSource(CO2_GTFS_ZIP, GTFS));
    inputData.put(GTFS, new FileDataSource(CO2_GTFS, GTFS));
    Iterable<ConfiguredDataSource<GtfsFeedParameters>> configuredDataSource =
      getGtfsConfiguredDatasource();
    EmissionsDataModel emissionsDataModel = new EmissionsDataModel();
    EmissionsModule emissionsModule = new EmissionsModule(
      configuredDataSource,
      buildConfig,
      emissionsDataModel,
      DataImportIssueStore.NOOP
    );
    emissionsModule.buildGraph();
    assertEquals(
      Optional.of(0.006),
      emissionsDataModel.getCO2EmissionsById(new FeedScopedId("emissionstest", "1001"))
    );
    assertEquals(
      Optional.of(0.041),
      emissionsDataModel.getCO2EmissionsById(new FeedScopedId("emissionstest1", "1002"))
    );
  }

  private Iterable<ConfiguredDataSource<GtfsFeedParameters>> getGtfsConfiguredDatasource() {
    return inputData
      .get(GTFS)
      .stream()
      .map(it -> new ConfiguredDataSource<>(it, getGtfsFeedConfig(it)))
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
