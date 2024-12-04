package org.opentripplanner.updater.trip;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.updater.trip.BackwardsDelayPropagationType.REQUIRED_NO_DATA;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;

public class TimetableSnapshotSourceTest {

  private static final LocalDate SERVICE_DATE = LocalDate.parse("2009-02-01");
  private static final TripUpdate CANCELLATION = new TripUpdateBuilder(
    "1.1",
    SERVICE_DATE,
    CANCELED,
    ZoneIds.NEW_YORK
  )
    .build();
  private TimetableRepository timetableRepository;
  private TransitService transitService;

  private final GtfsRealtimeFuzzyTripMatcher TRIP_MATCHER_NOOP = null;

  private String feedId;

  @BeforeEach
  public void setUp() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    timetableRepository = model.timetableRepository();
    transitService = new DefaultTransitService(timetableRepository);

    feedId = transitService.listFeedIds().stream().findFirst().get();
  }

  @Test
  public void testGetSnapshot() {
    var updater = defaultUpdater();

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      DIFFERENTIAL,
      List.of(CANCELLATION),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    assertNotNull(snapshot);
    assertSame(snapshot, updater.getTimetableSnapshot());
  }

  private TimetableSnapshotSource defaultUpdater() {
    return new TimetableSnapshotSource(
      new TimetableSnapshotSourceParameters(Duration.ZERO, true),
      timetableRepository,
      () -> SERVICE_DATE
    );
  }
}
