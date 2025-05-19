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

  private final DataImportIssueStore issueStore;
  private boolean dataProcessed = false;

  public TripDataReader(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  public List<TripLegsRow> read(DataSource emissionDataSource) {
    if (!emissionDataSource.exists()) {
      return List.of();
    }
    var emissionData = new ArrayList<TripLegsRow>();
    var reader = new CsvReader(emissionDataSource.asInputStream(), StandardCharsets.UTF_8);
    var parser = new TripLegsCsvParser(issueStore, reader);

    if (!parser.headersMatch()) {
      return List.of();
    }

    while (parser.hasNext()) {
      emissionData.add(parser.next());
      dataProcessed = true;
    }
    return emissionData;
  }

  public boolean isDataProcessed() {
    return dataProcessed;
  }
}
