package org.opentripplanner.ext.emission.internal.csvdata;

import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.csvdata.route.RouteDataReader;
import org.opentripplanner.ext.emission.internal.csvdata.trip.TripDataReader;
import org.opentripplanner.ext.emission.internal.csvdata.trip.TripHopMapper;
import org.opentripplanner.framework.csv.HeadersDoNotMatch;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles reading the CO₂ emissions data from the files in the GTFS package
 * and saving it in a map.
 */
public class EmissionDataReader {

  private static final Logger LOG = LoggerFactory.getLogger(EmissionDataReader.class);

  public static final String EMISSION_FILE_NAME = "emissions.txt";

  private final DataImportIssueStore issueStore;
  private final EmissionRepository emissionRepository;
  private final TripHopMapper tripHopMapper;

  public EmissionDataReader(
    EmissionRepository emissionRepository,
    TripHopMapper tripHopMapper,
    DataImportIssueStore issueStore
  ) {
    this.tripHopMapper = tripHopMapper;
    this.issueStore = issueStore;
    this.emissionRepository = emissionRepository;
  }

  public void read(CompositeDataSource catalog, String resolvedFeedId) {
    var emissionDataSource = catalog.entry(EMISSION_FILE_NAME);

    if (emissionDataSource == null || !emissionDataSource.exists()) {
      LOG.info("The {} does not contain any {} file.", catalog.detailedInfo(), EMISSION_FILE_NAME);
      return;
    }
    read(emissionDataSource, resolvedFeedId);
  }

  public void read(DataSource emissionDataSource, String resolvedFeedId) {
    LOG.info(
      "Reading EMISSION data: %s (feedId: %s)".formatted(
          emissionDataSource.detailedInfo(),
          resolvedFeedId
        )
    );

    if (!emissionDataSource.exists()) {
      LOG.info(
        "Emission datasource does not exist! DataSource: {}",
        emissionDataSource.detailedInfo()
      );
      return;
    }

    try {
      // Assume input CO₂ emission data is Route average data
      var routeReader = new RouteDataReader(emissionDataSource, issueStore);
      this.emissionRepository.addRouteEmissions(routeReader.read(resolvedFeedId, m -> LOG.info(m)));
      return;
    } catch (HeadersDoNotMatch ignore) {}

    try {
      // Assume input CO₂ emission data is per trip hop
      tripHopMapper.setCurrentFeedId(resolvedFeedId);
      var tripReader = new TripDataReader(emissionDataSource, issueStore);
      this.emissionRepository.addTripPatternEmissions(
          tripHopMapper.map(tripReader.read(m -> LOG.info(m)))
        );
      return;
    } catch (HeadersDoNotMatch ignore) {}

    LOG.error(
      "No emission data read from: " +
      emissionDataSource.detailedInfo() +
      ". Do the header columns match?"
    );
  }
}
