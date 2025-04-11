package org.opentripplanner.ext.emission.internal.csvdata;

import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.csvdata.route.RouteDataReader;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

/**
 * This class handles reading the CO₂ emissions data from the files in the GTFS package
 * and saving it in a map.
 */
public class EmissionDataReader {

  public static final String EMISSION_FILE_NAME = "emissions.txt";

  private final DataImportIssueStore issueStore;
  private final EmissionRepository emissionRepository;

  public EmissionDataReader(
    EmissionRepository emissionRepository,
    DataImportIssueStore issueStore
  ) {
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
    }
  }
}
