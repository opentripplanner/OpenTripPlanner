package org.opentripplanner.ext.taxizone.internal.graphbuilder;

import java.io.IOException;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.ext.taxizone.graphbuilder.TaxiZoneBuilder;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.mapping.GTFSToTransitDataImportMapper;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.transit.service.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads taxi zone data from a GTFS Flex feed and stores the zones in the
 * {@link TaxiZoneRepository}.
 */
public class TaxiZoneDataReader {

  private static final Logger LOG = LoggerFactory.getLogger(TaxiZoneDataReader.class);

  private final TaxiZoneRepository taxiZoneRepository;
  private final DataImportIssueStore issueStore;
  private final LocalDateRange transitPeriodLimit;

  public TaxiZoneDataReader(
    TaxiZoneRepository taxiZoneRepository,
    DataImportIssueStore issueStore,
    LocalDateRange transitPeriodLimit
  ) {
    this.taxiZoneRepository = taxiZoneRepository;
    this.issueStore = issueStore;
    this.transitPeriodLimit = transitPeriodLimit;
  }

  /**
   * Load taxi zones from the given bundle and add them to the repository.
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
    var builder = mapper.getBuilder();
    // Trim calendar data to the same transitServiceStart/transitServiceEnd window used for the
    // rest of the transit model, so taxi zone service dates stay consistent with it.
    builder.limitServiceDays(transitPeriodLimit);
    var calendarServiceData = builder.buildCalendarServiceData();
    var zones = TaxiZoneBuilder.buildZones(
      FlexTripsMapper.createFlexTrips(builder, issueStore),
      calendarServiceData
    );
    taxiZoneRepository.addZones(zones);
    LOG.info("Loaded {} taxi zone(s) from {}", zones.size(), bundle.feedInfo());
  }
}
