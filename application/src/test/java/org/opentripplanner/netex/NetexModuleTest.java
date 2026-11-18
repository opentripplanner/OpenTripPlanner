package org.opentripplanner.netex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.netex.configure.NetexConfigure;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;

class NetexModuleTest {

  private static final File NETEX_NORDIC_DIR = new File("src/test/resources/netex/nordic");

  @Test
  void dataSourceIsClosedWhenTheBundleIsLoaded() {
    var dataSource = new CloseTrackingDataSource(new File(NETEX_NORDIC_DIR, "netex_minimal"));
    var subject = createNetexModule(dataSource);

    subject.buildGraph();

    assertTrue(dataSource.closed);
  }

  @Test
  void dataSourceIsNotClosedBeforeTheGraphIsBuilt() {
    var dataSource = new CloseTrackingDataSource(new File(NETEX_NORDIC_DIR, "netex_minimal"));
    var subject = createNetexModule(dataSource);

    subject.checkInputs();

    assertFalse(dataSource.closed);
  }

  private static NetexModule createNetexModule(DirectoryDataSource dataSource) {
    var buildConfig = new OtpConfigLoader(NETEX_NORDIC_DIR).loadBuildConfig();
    var netexConfig = buildConfig.netexDefaults.copyOf().withSource(dataSource.uri()).build();
    var transitRepository = new TransitRepository(new SiteRepository());

    return new NetexConfigure(buildConfig).createNetexModule(
      List.of(new ConfiguredCompositeDataSource<>(dataSource, netexConfig)),
      transitRepository,
      new TimetableDataImport(),
      new DefaultVehicleParkingRepository(),
      new DefaultStreetDetailsRepository(),
      new Graph(),
      new Deduplicator(),
      DataImportIssueStore.NOOP
    );
  }

  private static class CloseTrackingDataSource extends DirectoryDataSource {

    private boolean closed = false;

    CloseTrackingDataSource(File path) {
      super(path, FileType.NETEX);
    }

    @Override
    public void close() {
      closed = true;
      super.close();
    }
  }
}
