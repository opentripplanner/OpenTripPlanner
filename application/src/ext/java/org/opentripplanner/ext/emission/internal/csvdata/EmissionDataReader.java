package org.opentripplanner.ext.emission.internal.csvdata;

import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.csvdata.route.RouteDataReader;
import org.opentripplanner.ext.emission.internal.csvdata.trip.TripDataReader;
import org.opentripplanner.ext.emission.internal.csvdata.trip.TripLegMapper;
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
  private final TripLegMapper tripLegMapper;

  public EmissionDataReader(
    EmissionRepository emissionRepository,
    TripLegMapper tripLegMapper,
    DataImportIssueStore issueStore
  ) {
    this.tripLegMapper = tripLegMapper;
    this.issueStore = issueStore;
    this.emissionRepository = emissionRepository;
  }

  public void read(CompositeDataSource catalog, String resolvedFeedId) {
    read(catalog.entry(EMISSION_FILE_NAME), resolvedFeedId);
  }

  public void read(DataSource emissionDataSource, String resolvedFeedId) {
    if (emissionDataSource.exists()) {
      // Assume input CO₂ emission data is Route avarage data
      var routeReader = new RouteDataReader(issueStore);
      this.emissionRepository.addRouteEmissions(
          routeReader.read(emissionDataSource, resolvedFeedId)
        );

      // Assume input CO₂ emission data is per trip leg
      tripLegMapper.setCurrentFeedId(resolvedFeedId);
      var tripReader = new TripDataReader(issueStore);
      this.emissionRepository.addTripPatternEmissions(
          tripLegMapper.map(tripReader.read(emissionDataSource))
        );

      if (!(routeReader.isDataProcessed() || tripReader.isDataProcessed())) {
        LOG.error("No emission data read from: " + emissionDataSource.detailedInfo());
      }
    }
  }
}
