package org.opentripplanner.ext.empiricaldelay.internal.csvinput.delay;

import com.csvreader.CsvReader;
import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.csv.parser.AbstractCsvParser;
import org.opentripplanner.framework.csv.parser.HandledCsvParseException;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.IntRange;

/**
 * <pre>
 * empirical_delay_service_id,trip_id,stop_id,stop_sequence,median
 * Friday,VYG:ServiceJourney:BUS-2280_374435-R,NSR:Quay:1,1,0
 * Friday,RUT:ServiceJourney:bc0092f0102605437e3e49cc3a88d0ba,NSR:Quay:10001,39,0
 * Friday,RUT:ServiceJourney:ccd08000dcdb315b1a3015b36f9602af,NSR:Quay:10001,39,0
 * </pre>
 */
public class TripTimeDelayCsvParser extends AbstractCsvParser<TripTimeDelayRow> {

  private static final String SERVICE_ID = "empirical_delay_service_id";
  private static final String TRIP_ID = "trip_id";
  private static final String STOP_ID = "stop_id";
  private static final String STOP_SEQUENCE = "stop_sequence";
  private static final String P50 = "p50";
  private static final String P90 = "p90";

  static final List<String> HEADERS = List.of(
    SERVICE_ID,
    TRIP_ID,
    STOP_ID,
    STOP_SEQUENCE,
    P50,
    P90
  );
  private static final IntRange PERCENTILE_RANGE = IntRange.ofInclusive(
    0,
    (int) Duration.ofHours(5).toSeconds()
  );
  private static final IntRange STOP_SEQUENCE_RANGE = IntRange.ofInclusive(0, 10_000);

  private final String feedId;

  public TripTimeDelayCsvParser(DataImportIssueStore issueStore, CsvReader reader, String feedId) {
    super(issueStore, reader, "EmpiricalDelayStopTimes");
    this.feedId = feedId;
  }

  @Override
  public List<String> headers() {
    return HEADERS;
  }

  @Nullable
  @Override
  protected TripTimeDelayRow createNextRow() throws HandledCsvParseException {
    return new TripTimeDelayRow(
      getString(SERVICE_ID),
      new FeedScopedId(feedId, getString(TRIP_ID)),
      new FeedScopedId(feedId, getString(STOP_ID)),
      getInt(STOP_SEQUENCE, STOP_SEQUENCE_RANGE),
      getInt(P50, PERCENTILE_RANGE),
      getInt(P90, PERCENTILE_RANGE)
    );
  }
}
