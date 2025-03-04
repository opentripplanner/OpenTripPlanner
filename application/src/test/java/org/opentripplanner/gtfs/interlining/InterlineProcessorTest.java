package org.opentripplanner.gtfs.interlining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.StaySeatedNotAllowed;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.transfer.DefaultTransferService;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class InterlineProcessorTest implements PlanTestConstants {

  private static TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  List<TripPattern> patterns = List.of(
    tripPattern("trip-1", "block-1", "service-1"),
    tripPattern("trip-2", "block-1", "service-1"),
    tripPattern("trip-3", "block-1", "service-2"),
    tripPattern("trip-4", "block-1", "service-3"),
    tripPattern("trip-5", "block-2", "service-4")
  );

  static Stream<Arguments> interlineTestCases() {
    return Stream.of(
      Arguments.of(
        List.of(
          new FeedScopedId("1", "service-1"),
          new FeedScopedId("1", "service-2"),
          new FeedScopedId("1", "service-3"),
          new FeedScopedId("1", "service-4")
        ),
        List.of(
          List.of(LocalDate.of(2023, Month.JANUARY, 1), LocalDate.of(2023, Month.JANUARY, 5)),
          List.of(LocalDate.of(2023, Month.JANUARY, 1)),
          List.of(LocalDate.of(2023, Month.JANUARY, 1)),
          List.of(LocalDate.of(2023, Month.JANUARY, 1))
        ),
        "[ConstrainedTransfer{from: TripTP{F:trip-2, stopPos 2}, to: TripTP{F:trip-3, stopPos 0}, " +
        "constraint: {staySeated}}, ConstrainedTransfer{from: TripTP{F:trip-1, stopPos 2}, " +
        "to: TripTP{F:trip-2, stopPos 0}, constraint: {staySeated}}, " +
        "ConstrainedTransfer{from: TripTP{F:trip-3, stopPos 2}, to: TripTP{F:trip-4, stopPos 0}, constraint: {staySeated}}]"
      ),
      Arguments.of(
        List.of(
          new FeedScopedId("1", "service-1"),
          new FeedScopedId("1", "service-2"),
          new FeedScopedId("1", "service-3"),
          new FeedScopedId("1", "service-4")
        ),
        List.of(
          List.of(LocalDate.of(2023, Month.JANUARY, 1), LocalDate.of(2023, Month.JANUARY, 5)),
          List.of(LocalDate.of(2023, Month.JANUARY, 1)),
          List.of(LocalDate.of(2023, Month.JANUARY, 5)),
          List.of(LocalDate.of(2023, Month.JANUARY, 1))
        ),
        "[ConstrainedTransfer{from: TripTP{F:trip-2, stopPos 2}, to: TripTP{F:trip-3, stopPos 0}, " +
        "constraint: {staySeated}}, ConstrainedTransfer{from: TripTP{F:trip-1, stopPos 2}, " +
        "to: TripTP{F:trip-2, stopPos 0}, constraint: {staySeated}}, " +
        "ConstrainedTransfer{from: TripTP{F:trip-2, stopPos 2}, to: TripTP{F:trip-4, stopPos 0}, constraint: {staySeated}}]"
      ),
      // No common days between services
      Arguments.of(
        List.of(
          new FeedScopedId("1", "service-1"),
          new FeedScopedId("1", "service-2"),
          new FeedScopedId("1", "service-3"),
          new FeedScopedId("1", "service-4")
        ),
        List.of(
          List.of(LocalDate.of(2023, Month.JANUARY, 1), LocalDate.of(2023, Month.JANUARY, 5)),
          List.of(LocalDate.of(2023, Month.JANUARY, 2)),
          List.of(LocalDate.of(2023, Month.JANUARY, 3)),
          List.of(LocalDate.of(2023, Month.JANUARY, 1))
        ),
        "[ConstrainedTransfer{from: TripTP{F:trip-1, stopPos 2}, to: TripTP{F:trip-2, stopPos 0}, constraint: {staySeated}}]"
      )
    );
  }

  @ParameterizedTest(name = "{0} services with {1} dates should generate transfers: {2}")
  @MethodSource("interlineTestCases")
  void testInterline(
    List<FeedScopedId> serviceIds,
    List<List<LocalDate>> serviceDates,
    String transfers
  ) {
    var transferService = new DefaultTransferService();
    var calendarServiceData = new CalendarServiceData();
    for (int i = 0; i < serviceIds.size(); i++) {
      calendarServiceData.putServiceDatesForServiceId(serviceIds.get(i), serviceDates.get(i));
    }
    var processor = new InterlineProcessor(
      transferService,
      List.of(),
      100,
      DataImportIssueStore.NOOP,
      calendarServiceData
    );

    var createdTransfers = processor.run(patterns);

    assertEquals(transferService.listAll(), createdTransfers);

    createdTransfers.forEach(t -> assertTrue(t.getTransferConstraint().isStaySeated()));

    assertEquals(transfers, createdTransfers.toString());
  }

  @Test
  void staySeatedNotAllowed() {
    var transferService = new DefaultTransferService();

    var fromTrip = patterns.get(0).getScheduledTimetable().getTripTimes().get(0).getTrip();
    var toTrip = patterns.get(1).getScheduledTimetable().getTripTimes().get(0).getTrip();
    var notAllowed = new StaySeatedNotAllowed(fromTrip, toTrip);

    var calendarService = new CalendarServiceData();
    calendarService.putServiceDatesForServiceId(
      new FeedScopedId("1", "service-1"),
      List.of(LocalDate.of(2023, Month.JANUARY, 1))
    );

    var processor = new InterlineProcessor(
      transferService,
      List.of(notAllowed),
      100,
      DataImportIssueStore.NOOP,
      calendarService
    );

    var createdTransfers = processor.run(patterns);
    assertEquals(0, createdTransfers.size());

    assertEquals(transferService.listAll(), createdTransfers);
  }

  private static TripPattern tripPattern(String tripId, String blockId, String serviceId) {
    var trip = TimetableRepositoryForTest.trip(tripId)
      .withGtfsBlockId(blockId)
      .withServiceId(new FeedScopedId("1", serviceId))
      .build();

    var stopTimes = List.of(
      TEST_MODEL.stopTime(trip, 0),
      TEST_MODEL.stopTime(trip, 1),
      TEST_MODEL.stopTime(trip, 2)
    );
    var stopPattern = new StopPattern(stopTimes);

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    return TripPattern.of(TimetableRepositoryForTest.id(tripId))
      .withRoute(trip.getRoute())
      .withStopPattern(stopPattern)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();
  }
}
