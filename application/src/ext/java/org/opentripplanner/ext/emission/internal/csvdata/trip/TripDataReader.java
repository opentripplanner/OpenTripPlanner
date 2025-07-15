package org.opentripplanner.ext.emission.internal.csvdata.trip;

import com.csvreader.CsvReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

/**
 * This class handles reading the CO₂ emissions data from the files in the GTFS package
 * and saving it in a map.
 */
public class TripDataReader {

  private final DataSource emissionDataSource;
  private final DataImportIssueStore issueStore;
  private boolean dataProcessed = false;

  public TripDataReader(DataSource emissionDataSource, DataImportIssueStore issueStore) {
    this.emissionDataSource = emissionDataSource;
    this.issueStore = issueStore;
  }

  public List<TripHopsRow> read(Runnable logStepCallback) {
    if (!emissionDataSource.exists()) {
      return List.of();
    }
    var emissionData = new ArrayList<TripHopsRow>();
    var reader = new CsvReader(emissionDataSource.asInputStream(), StandardCharsets.UTF_8);
    var parser = new TripHopsCsvParser(issueStore, reader);

    if (!parser.headersMatch()) {
      return List.of();
    }

    while (parser.hasNext()) {
      logStepCallback.run();
      emissionData.add(parser.next());
      dataProcessed = true;
    }
    return emissionData;
  }

  public boolean isDataProcessed() {
    return dataProcessed;
  }
}
