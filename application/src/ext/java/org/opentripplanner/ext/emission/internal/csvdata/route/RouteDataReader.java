package org.opentripplanner.ext.emission.internal.csvdata.route;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.framework.csv.HeadersDoNotMatch;
import org.opentripplanner.framework.csv.OtpCsvReader;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class handles reading the CO₂ emissions data from the files in the GTFS package
 * and saving it in a map.
 */
public class RouteDataReader {

  private final DataSource emissionDataSource;
  private final DataImportIssueStore issueStore;

  public RouteDataReader(DataSource emissionDataSource, DataImportIssueStore issueStore) {
    this.emissionDataSource = emissionDataSource;
    this.issueStore = issueStore;
  }

  /**
   * Read emission data for routes. The given resolvedFeedId is used to create route ids which map
   * to the internal OTP Routes entities.
   * @return a map of emissions for each route id in the data set
   * @throws HeadersDoNotMatch - if the headers does not match, this can be used to try another
   *                             reader, mainly to support multiple versions of the data.
   */
  public Map<FeedScopedId, Emission> read(
    String resolvedFeedId,
    @Nullable Consumer<String> progressLogger
  ) throws HeadersDoNotMatch {
    var emissionData = new HashMap<FeedScopedId, Emission>();

    OtpCsvReader.<RouteRow>of()
      .withDataSource(emissionDataSource)
      .withProgressLogger(progressLogger)
      .withParserFactory(r -> new RouteCsvParser(issueStore, r))
      .withRowHandler(row -> {
        emissionData.put(
          new FeedScopedId(resolvedFeedId, row.routeId()),
          Emission.of(row.calculatePassengerCo2PerMeter())
        );
      })
      .read();
    return emissionData;
  }
}
