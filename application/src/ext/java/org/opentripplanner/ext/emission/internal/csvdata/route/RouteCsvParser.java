package org.opentripplanner.ext.emission.internal.csvdata.route;

import com.csvreader.CsvReader;
import java.util.List;
import org.opentripplanner.framework.csv.parser.AbstractCsvParser;
import org.opentripplanner.framework.csv.parser.HandledCsvParseException;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.utils.lang.DoubleRange;

class RouteCsvParser extends AbstractCsvParser<RouteRow> {

  private static final String ROUTE_ID = "route_id";
  private static final String AVG_CO_2_PER_VEHICLE_PER_KM = "avg_co2_per_vehicle_per_km";
  private static final String AVG_PASSENGER_COUNT = "avg_passenger_count";

  private static final DoubleRange AVG_CO_2_PER_VEHICLE_PER_KM_RANGE = DoubleRange.of(
    0.0,
    100_000.0
  );
  private static final DoubleRange AVG_PASSENGER_COUNT_RANGE = DoubleRange.of(0.001, 10_000.0);

  static final List<String> HEADERS = List.of(
    ROUTE_ID,
    AVG_CO_2_PER_VEHICLE_PER_KM,
    AVG_PASSENGER_COUNT
  );

  public RouteCsvParser(DataImportIssueStore issueStore, CsvReader reader) {
    super(issueStore, reader, "RouteEmission");
  }

  @Override
  protected List<String> headers() {
    return HEADERS;
  }

  @Override
  protected RouteRow createNextRow() throws HandledCsvParseException {
    return new RouteRow(
      getString(ROUTE_ID),
      getDouble(AVG_CO_2_PER_VEHICLE_PER_KM, AVG_CO_2_PER_VEHICLE_PER_KM_RANGE),
      getDouble(AVG_PASSENGER_COUNT, AVG_PASSENGER_COUNT_RANGE)
    );
  }
}
