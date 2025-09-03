package org.opentripplanner.ext.empiricaldelay.internal.csvinput;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.empiricaldelay.internal.csvinput.calendar.CalendarCsvParser;
import org.opentripplanner.ext.empiricaldelay.internal.csvinput.calendar.CalendarRow;
import org.opentripplanner.ext.empiricaldelay.internal.csvinput.delay.TripTimeDelayCsvParser;
import org.opentripplanner.ext.empiricaldelay.internal.csvinput.delay.TripTimeDelayRow;
import org.opentripplanner.ext.empiricaldelay.internal.model.TripDelaysDto;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.ext.empiricaldelay.model.calendar.EmpiricalDelayCalendar;
import org.opentripplanner.framework.csv.HeadersDoNotMatch;
import org.opentripplanner.framework.csv.OtpCsvReader;
import org.opentripplanner.framework.error.OtpError;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading empirical delays from a set of csv files.
 */
public class EmpiricalDelayCsvDataReader {

  private static final Logger LOG = LoggerFactory.getLogger(EmpiricalDelayCsvDataReader.class);

  public static final String CALENDAR_FILE_NAME = "empirical_delay_calendar.csv";
  public static final String TRIP_TIME_DELAY_FILE_NAME = "empirical_delay_trip_times.csv";

  private final DataImportIssueStore issueStore;
  private EmpiricalDelayCalendar calendar;
  /** The serviceIds is cashed, so we can validate the delay trip time rows more efficient. */
  private final Set<String> serviceIds = new HashSet<>();
  private final Map<FeedScopedId, TripDelaysDto> trips = new HashMap<>();

  public EmpiricalDelayCsvDataReader(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  public EmpiricalDelayCalendar calendar() {
    return calendar;
  }

  public Collection<TripDelaysDto> trips() {
    return trips.values();
  }

  public void read(CompositeDataSource directory, String feedId) {
    var calendarDataSource = directory.entry(CALENDAR_FILE_NAME);
    var delayDataSource = directory.entry(TRIP_TIME_DELAY_FILE_NAME);

    if (!dataSourcesExist(directory, feedId, calendarDataSource, delayDataSource)) {
      return;
    }
    try {
      readCalendarFile(calendarDataSource);
      readTripDelays(delayDataSource, feedId);
    } catch (HeadersDoNotMatch e) {
      LOG.error(e.getMessage());
    }
  }

  /* private methods */

  private void readCalendarFile(DataSource dataSource) throws HeadersDoNotMatch {
    var calBuilder = EmpiricalDelayCalendar.of();

    OtpCsvReader.<CalendarRow>of()
      .withDataSource(dataSource)
      .withProgressLogger(msg -> LOG.info(msg))
      .withParserFactory(reader -> new CalendarCsvParser(issueStore, reader))
      .withRowHandler(row ->
        calBuilder.with(row.serviceId(), row.asDayOfWeekSet(), row.startDate(), row.endDate())
      )
      .read();

    this.calendar = calBuilder.build();
    this.serviceIds.addAll(calendar.listServiceIds());
  }

  private void readTripDelays(DataSource dataSource, String feedId) throws HeadersDoNotMatch {
    OtpCsvReader.<TripTimeDelayRow>of()
      .withDataSource(dataSource)
      .withProgressLogger(msg -> LOG.info(msg))
      .withParserFactory(reader -> new TripTimeDelayCsvParser(issueStore, reader, feedId))
      .withRowHandler(this::handleEmpiricalDelayRow)
      .read();
  }

  private void handleEmpiricalDelayRow(TripTimeDelayRow row) {
    String serviceId = row.empiricalDelayServiceId();
    if (!serviceIds.contains(serviceId)) {
      issueStore.add(
        OtpError.of(
          "EmpiricalDelayMissingServiceId",
          "Empirical delay serviceId(%s) does not exist in service calendar. Row is skipped: %s",
          serviceId,
          row
        )
      );
      return;
    }

    var trip = trips.computeIfAbsent(row.tripId(), id -> new TripDelaysDto(id));
    trip.addDelay(
      row.empiricalDelayServiceId(),
      row.stopSequence(),
      row.stopId(),
      new EmpiricalDelay(row.p50(), row.p90())
    );
  }

  private boolean dataSourcesExist(
    CompositeDataSource parent,
    String feedId,
    DataSource... dataSources
  ) {
    for (var dataSource : dataSources) {
      if (dataSource == null || !dataSource.exists()) {
        LOG.info("The {} does not contain any {} file.", parent.detailedInfo(), dataSource.path());
        return false;
      }
    }
    LOG.info(
      "Reading empirical delay data: %s (feedId: %s)".formatted(parent.detailedInfo(), feedId)
    );
    return true;
  }
}
