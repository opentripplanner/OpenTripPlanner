package org.opentripplanner.ext.empiricaldelay.internal.csvinput.calendar;

import com.csvreader.CsvReader;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.csv.parser.AbstractCsvParser;
import org.opentripplanner.framework.csv.parser.HandledCsvParseException;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

/**
 * <pre>
 * empirical_delay_calendar.txt
 *
 * empirical_delay_service_id, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start_date, end_date
 * MONDAY   ,1,0,0,0,0,0,0,2025-01-01, 2030-12-31
 * TUESDAY  ,0,1,0,0,0,0,0,2025-01-01, 2030-12-31
 * WEDNESDAY,0,0,1,0,0,0,0,2025-01-01, 2030-12-31
 * THURDAY  ,0,0,0,1,0,0,0,2025-01-01, 2030-12-31
 * FRIDAY   ,0,0,0,0,1,0,0,2025-01-01, 2030-12-31
 * SATURDAY ,0,0,0,0,0,1,0,2025-01-01, 2030-12-31
 * SUNDAY   ,0,0,0,0,0,0,1,2025-01-01, 2030-12-31
 */
public class CalendarCsvParser extends AbstractCsvParser<CalendarRow> {

  private static final String SERVICE_ID = "empirical_delay_service_id";
  private static final String MONDAY = "monday";
  private static final String TUESDAY = "tuesday";
  private static final String WEDNESDAY = "wednesday";
  private static final String THURSDAY = "thursday";
  private static final String FRIDAY = "friday";
  private static final String SATURDAY = "saturday";
  private static final String SUNDAY = "sunday";
  private static final String START_DATE = "start_date";
  private static final String END_DATE = "end_date";
  protected static final List<String> HEADERS = List.of(
    SERVICE_ID,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    START_DATE,
    END_DATE
  );

  public CalendarCsvParser(DataImportIssueStore issueStore, CsvReader reader) {
    super(issueStore, reader, "EmpiricalDelayServiceCalendar");
  }

  @Override
  public List<String> headers() {
    return HEADERS;
  }

  @Nullable
  @Override
  protected CalendarRow createNextRow() throws HandledCsvParseException {
    return new CalendarRow(
      getString(SERVICE_ID),
      getGtfsBoolean(MONDAY),
      getGtfsBoolean(TUESDAY),
      getGtfsBoolean(WEDNESDAY),
      getGtfsBoolean(THURSDAY),
      getGtfsBoolean(FRIDAY),
      getGtfsBoolean(SATURDAY),
      getGtfsBoolean(SUNDAY),
      getLocalDate(START_DATE),
      getLocalDate(END_DATE)
    );
  }
}
