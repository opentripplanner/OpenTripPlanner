package org.opentripplanner.ext.emission.internal.csvdata.trip;

import com.csvreader.CsvReader;
import java.util.List;
import org.opentripplanner.framework.csv.parser.AbstractCsvParser;
import org.opentripplanner.framework.csv.parser.HandledCsvParseException;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.utils.lang.DoubleRange;
import org.opentripplanner.utils.lang.IntRange;

class TripHopsCsvParser extends AbstractCsvParser<TripHopsRow> {

  public static final String TRIP_ID = "trip_id";
  public static final String START_STOP_ID = "from_stop_id";
  public static final String START_STOP_SEQ_NR = "from_stop_sequence";
  private static final String CO2 = "co2";
  private static final List<String> HEADERS = List.of(
    TRIP_ID,
    START_STOP_ID,
    START_STOP_SEQ_NR,
    CO2
  );
  private static final IntRange STOP_SEQ_NR_RANGE = IntRange.ofInclusive(0, 10_000);

  /**
   * Electrical vehicles can charge while going downhill, hence the negative range.
   */
  private static final DoubleRange CO2_RANGE = DoubleRange.of(-1_000_000.0, 1_000_000_000.0);

  public TripHopsCsvParser(DataImportIssueStore issueStore, CsvReader reader) {
    super(issueStore, reader, "TripEmission");
  }

  @Override
  public List<String> headers() {
    return HEADERS;
  }

  @Override
  protected TripHopsRow createNextRow() throws HandledCsvParseException {
    return new TripHopsRow(
      getString(TRIP_ID),
      getString(START_STOP_ID),
      getInt(START_STOP_SEQ_NR, STOP_SEQ_NR_RANGE),
      Gram.of(getDouble(CO2, CO2_RANGE))
    );
  }
}
