package org.opentripplanner.updater.trip.gtfs;

import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType.REQUIRED_NO_DATA;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.TripPatternCache;
import org.opentripplanner.updater.trip.TripPatternIdGenerator;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class GtfsRealTimeTripUpdateAdapterTest {

  private static final LocalDate SERVICE_DATE = LocalDate.parse("2009-02-01");
  private TimetableRepository timetableRepository;
  private TransitService transitService;

  private final GtfsRealtimeFuzzyTripMatcher TRIP_MATCHER_NOOP = null;

  private String feedId;
  private TimetableSnapshotManager snapshotManager;

  @BeforeEach
  public void setUp() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    timetableRepository = model.timetableRepository();
    transitService = new DefaultTransitService(timetableRepository);

    feedId = transitService.listFeedIds().stream().findFirst().get();
    snapshotManager = new TimetableSnapshotManager(null, TimetableSnapshotParameters.DEFAULT, () ->
      SERVICE_DATE
    );
  }

  @Test
  public void testHandleModifiedTrip() {
    // GIVEN

    String modifiedTripId = "10.1";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(modifiedTripId);
      tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.REPLACEMENT);
      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(SERVICE_DATE));

      final long midnightSecondsSinceEpoch = ServiceDateUtils.asStartOfService(
        SERVICE_DATE,
        transitService.getTimeZone()
      ).toEpochSecond();

      final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

      tripUpdateBuilder.setTrip(tripDescriptorBuilder);

      { // Stop O
        final StopTimeUpdate.Builder stopTimeUpdateBuilder =
          tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("O");
        stopTimeUpdateBuilder.setStopSequence(10);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (30 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder =
            stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (30 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop C
        final StopTimeUpdate.Builder stopTimeUpdateBuilder =
          tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("C");
        stopTimeUpdateBuilder.setStopSequence(30);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (40 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder =
            stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (45 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop D
        final StopTimeUpdate.Builder stopTimeUpdateBuilder =
          tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(SKIPPED);
        stopTimeUpdateBuilder.setStopId("D");
        stopTimeUpdateBuilder.setStopSequence(40);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (50 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder =
            stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (51 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop P
        final StopTimeUpdate.Builder stopTimeUpdateBuilder =
          tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("P");
        stopTimeUpdateBuilder.setStopSequence(50);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (55 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder =
            stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (55 * 60));
          departureBuilder.setDelay(0);
        }
      }

      tripUpdate = tripUpdateBuilder.build();
    }

    var updater = defaultUpdater();

    // WHEN
    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      DIFFERENTIAL,
      List.of(tripUpdate),
      feedId
    );
    snapshotManager.purgeAndCommit();

    // THEN
    final TimetableSnapshot snapshot = snapshotManager.getTimetableSnapshot();

    // Original trip pattern
    {
      final FeedScopedId tripId = new FeedScopedId(feedId, modifiedTripId);
      final Trip trip = transitService.getTrip(tripId);
      final TripPattern originalTripPattern = transitService.findPattern(trip);

      final Timetable originalTimetableForToday = snapshot.resolve(
        originalTripPattern,
        SERVICE_DATE
      );
      final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

      assertNotSame(originalTimetableForToday, originalTimetableScheduled);

      var original = originalTimetableScheduled.getTripTimes(
        new FeedScopedId(feedId, modifiedTripId)
      );
      assertNotNull(original, "Original trip should be found in scheduled time table");
      assertFalse(
        original.isCanceledOrDeleted(),
        "Original trip times should not be canceled in scheduled time table"
      );
      assertEquals(RealTimeState.SCHEDULED, original.getRealTimeState());

      var originalTT = originalTimetableForToday.getTripTimes(
        new FeedScopedId(feedId, modifiedTripId)
      );
      assertNotNull(originalTT, "Original trip should be found in time table for service date");
      assertTrue(
        originalTT.isDeleted(),
        "Original trip times should be deleted in time table for service date"
      );
      assertEquals(RealTimeState.DELETED, originalTT.getRealTimeState());
    }

    // New trip pattern
    {
      final TripPattern newTripPattern = snapshot.getNewTripPatternForModifiedTrip(
        new FeedScopedId(feedId, modifiedTripId),
        SERVICE_DATE
      );
      assertNotNull(newTripPattern, "New trip pattern should be found");

      final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, SERVICE_DATE);
      final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

      assertNotSame(newTimetableForToday, newTimetableScheduled);

      var tripTimes = newTimetableForToday.getTripTimes(new FeedScopedId(feedId, modifiedTripId));
      assertNotNull(tripTimes, "New trip should be found in time table for service date");
      assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());

      assertNull(
        newTimetableScheduled.getTripTimes(id(modifiedTripId)),
        "New trip should not be found in scheduled time table"
      );
    }
  }

  private GtfsRealTimeTripUpdateAdapter defaultUpdater() {
    return new GtfsRealTimeTripUpdateAdapter(
      timetableRepository,
      snapshotManager,
      new TripPatternCache(new TripPatternIdGenerator(), transitService::findPattern),
      () -> SERVICE_DATE
    );
  }
}
