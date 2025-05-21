package org.opentripplanner.ext.emission.internal.csvdata.route;

import com.csvreader.CsvReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.datastore.api.DataSource;
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
  private boolean dataProcessed = false;

  public RouteDataReader(DataSource emissionDataSource, DataImportIssueStore issueStore) {
    this.emissionDataSource = emissionDataSource;
    this.issueStore = issueStore;
  }

  public Map<FeedScopedId, Emission> read(String resolvedFeedId, Runnable logStepCallback) {
    if (!emissionDataSource.exists()) {
      return Map.of();
    }
    var emissionData = new HashMap<FeedScopedId, Emission>();
    var reader = new CsvReader(emissionDataSource.asInputStream(), StandardCharsets.UTF_8);
    var parser = new RouteCsvParser(issueStore, reader);

    if (!parser.headersMatch()) {
      return Map.of();
    }

    while (parser.hasNext()) {
      logStepCallback.run();
      var value = parser.next();
      emissionData.put(
        new FeedScopedId(resolvedFeedId, value.routeId()),
        Emission.of(value.calculatePassengerCo2PerMeter())
      );
      dataProcessed = true;
    }
    return emissionData;
  }

  public boolean isDataProcessed() {
    return dataProcessed;
  }
}
