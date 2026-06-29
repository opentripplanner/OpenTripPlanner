package org.opentripplanner.ext.carpickupzone.internal.graphbuilder;

import java.io.IOException;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneRepository;
import org.opentripplanner.ext.carpickupzone.graphbuilder.CarPickupZoneBuilder;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.mapping.GTFSToTransitDataImportMapper;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.transit.service.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads car pickup zone data from a GTFS Flex feed and stores the zones in the
 * {@link CarPickupZoneRepository}.
 */
public class CarPickupZoneDataReader {

  private static final Logger LOG = LoggerFactory.getLogger(CarPickupZoneDataReader.class);

  private final CarPickupZoneRepository carPickupZoneRepository;
  private final DataImportIssueStore issueStore;

  public CarPickupZoneDataReader(
    CarPickupZoneRepository carPickupZoneRepository,
    DataImportIssueStore issueStore
  ) {
    this.carPickupZoneRepository = carPickupZoneRepository;
    this.issueStore = issueStore;
  }

  /**
   * Load car pickup zones from the given bundle and add them to the repository.
   * Uses an isolated {@link SiteRepository} to avoid advancing the main model's stop index counter.
   */
  public void read(GtfsBundle bundle) throws IOException {
    var dao = bundle.loadDao();
    var mapper = new GTFSToTransitDataImportMapper(
      new TransitDataImportBuilder(SiteRepository.of().build(), issueStore),
      bundle.getFeedId(),
      issueStore,
      bundle.parameters().discardMinTransferTimes(),
      bundle.parameters().stationTransferPreference()
    );
    mapper.mapStopTripAndRouteDataIntoBuilder(dao);
    var zones = CarPickupZoneBuilder.buildZones(
      FlexTripsMapper.createFlexTrips(mapper.getBuilder(), issueStore)
    );
    carPickupZoneRepository.addZones(zones);
    LOG.info("Loaded {} car pickup zone(s) from {}", zones.size(), bundle.feedInfo());
  }
}
