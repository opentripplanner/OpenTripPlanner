package org.opentripplanner.model.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.network.TripPatternBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * This test will create a Transit service builder with a limited service period. The services
 * defined is in period [D0, D3] with D1 and D2 inside that period. The builder's period limit is
 * [D2, D3], so calendar data outside of it is dropped as it is added.
 * <p>
 * All data related in the last part of the service should be removed after calling {@code build()}.
 */
public class TransitDataImportBuilderLimitPeriodTest {

  private static final LocalDate D0 = LocalDate.of(2020, 1, 1);
  private static final LocalDate D1 = LocalDate.of(2020, 1, 8);
  private static final LocalDate D2 = LocalDate.of(2020, 1, 15);
  private static final LocalDate D3 = LocalDate.of(2020, 1, 31);
  private static final FeedScopedId SERVICE_C_IN = FeedScopedIdForTestFactory.id("CalSrvIn");
  private static final FeedScopedId SERVICE_D_IN = FeedScopedIdForTestFactory.id("CalSrvDIn");
  private static final FeedScopedId SERVICE_C_OUT = FeedScopedIdForTestFactory.id("CalSrvOut");
  private static final FeedScopedId SERVICE_D_OUT = FeedScopedIdForTestFactory.id("CalSrvDOut");
  private static final Set<DayOfWeek> ALL_DAYS = EnumSet.allOf(DayOfWeek.class);
  private static final Deduplicator DEDUPLICATOR = new Deduplicator();
  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop STOP_1 = TEST_MODEL.stop("Stop-1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("Stop-2").build();
  private static final List<StopTime> STOP_TIMES = List.of(
    createStopTime(STOP_1, 0),
    createStopTime(STOP_2, 300)
  );
  private static final StopPattern STOP_PATTERN = new StopPattern(STOP_TIMES);
  private static int SEQ_NR = 0;
  private final Route route = TransitRepositoryForTest.route(newId().getId()).build();
  private final Trip tripCSIn = createTrip("TCalIn", SERVICE_C_IN);
  private final Trip tripCSOut = createTrip("TCalOut", SERVICE_C_OUT);
  private final Trip tripCSDIn = createTrip("TDateIn", SERVICE_D_IN);
  private final Trip tripCSDOut = createTrip("TDateOut", SERVICE_D_OUT);
  private TripPattern patternInT1;
  private TripPattern patternInT2;
  private TransitDataImportBuilder subject;

  @BeforeEach
  public void setUp() {
    subject = new TransitDataImportBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP,
      LocalDateRange.ofInclusiveEnd(D2, D3)
    );

    // Add a service calendar that overlap with the period limit
    subject
      .tripCalendars()
      .addWeeklyCalendar(SERVICE_C_IN, ALL_DAYS, LocalDateRange.ofInclusiveEnd(D1, D3));

    // Add a service calendar that is outside the period limit, expected deleted later
    subject
      .tripCalendars()
      .addWeeklyCalendar(SERVICE_C_OUT, ALL_DAYS, LocalDateRange.ofInclusiveEnd(D0, D1));

    // Add a service calendar date that is within the period limit
    subject.tripCalendars().addServiceDate(SERVICE_D_IN, D2);

    // Add a service calendar date that is OUTSIDE the period limit, expected deleted later
    subject.tripCalendars().addServiceDate(SERVICE_D_OUT, D1);

    // Add 2 stops
    subject.siteRepository().withRegularStop(STOP_1);
    subject.siteRepository().withRegularStop(STOP_2);

    // Add Route
    subject.getRoutes().add(route);

    // Add trips; one for each day and calendar
    subject.getTripsById().addAll(List.of(tripCSIn, tripCSOut, tripCSDIn, tripCSDOut));

    // Pattern with trips that is partially deleted later
    patternInT1 = createTripPattern(List.of(tripCSIn, tripCSOut));
    // Pattern with trip that is inside period
    patternInT2 = createTripPattern(List.of(tripCSDIn));
    // Pattern with trip outside limiting period - pattern is deleted later
    TripPattern patternOut = createTripPattern(List.of(tripCSDOut));

    subject.getTripPatterns().put(STOP_PATTERN, patternInT1);
    subject.getTripPatterns().put(STOP_PATTERN, patternInT2);
    subject.getTripPatterns().put(STOP_PATTERN, patternOut);
  }

  @Test
  public void testLimitPeriod() {
    // Calendar data is filtered as it is added: SERVICE_C_OUT's period does not overlap [D2, D3],
    // and SERVICE_D_OUT's only date (D1) falls outside it - neither was ever registered.
    assertEquals(Set.of(SERVICE_C_IN, SERVICE_D_IN), subject.tripCalendars().listServiceIds());

    // Trips/patterns referencing the dropped service ids are still present, until
    // removeEntitiesWithInvalidReferences() below removes entities with no remaining valid
    // service id (this is otherwise called by build(), but that also builds the SiteRepository,
    // which this test's minimal fixture isn't set up for).
    assertEquals(4, subject.getTripsById().size());
    assertEquals(3, subject.getTripPatterns().get(STOP_PATTERN).size());
    assertEquals(2, patternInT1.scheduledTripsAsStream().count());
    assertEquals(2, patternInT1.getScheduledTimetable().getTripTimes().size());
    assertEquals(1, patternInT2.scheduledTripsAsStream().count());
    assertEquals(1, patternInT2.getScheduledTimetable().getTripTimes().size());

    subject.removeEntitiesWithInvalidReferences();

    // Verify trips
    EntityById<Trip> trips = subject.getTripsById();
    assertEquals(2, trips.size(), trips.toString());
    assertTrue(trips.containsKey(tripCSIn.getId()), trips.toString());
    assertTrue(trips.containsKey(tripCSDIn.getId()), trips.toString());

    // Verify patterns
    Collection<TripPattern> patterns = subject.getTripPatterns().get(STOP_PATTERN);
    assertEquals(2, patterns.size());
    assertTrue(patterns.contains(patternInT1), patterns.toString());
    assertTrue(patterns.contains(patternInT2), patterns.toString());

    // Verify patternInT1 is replaced by a copy that contains one less trip
    TripPattern copyOfTripPattern1 = subject
      .getTripPatterns()
      .values()
      .stream()
      .filter(p -> p.getId().equals(patternInT1.getId()))
      .findFirst()
      .orElseThrow();
    assertNotSame(patternInT1, copyOfTripPattern1);
    assertEquals(1, copyOfTripPattern1.scheduledTripsAsStream().count());
    assertEquals(tripCSIn, copyOfTripPattern1.scheduledTripsAsStream().findFirst().orElseThrow());

    // Verify trips in patternInT2 is unchanged (one trip)
    assertEquals(1, patternInT2.scheduledTripsAsStream().count());

    // Verify scheduledTimetable trips (one trip is removed from the copy of patternInT1)
    assertEquals(1, copyOfTripPattern1.getScheduledTimetable().getTripTimes().size());
    assertEquals(
      tripCSIn,
      copyOfTripPattern1.getScheduledTimetable().getTripTimes().get(0).getTrip()
    );

    // Verify scheduledTimetable trips in pattern is unchanged (one trip)
    assertEquals(1, patternInT2.getScheduledTimetable().getTripTimes().size());
  }

  private static StopTime createStopTime(RegularStop stop, int time) {
    StopTime st = new StopTime();
    st.setStop(stop);
    st.setDepartureTime(time);
    st.setArrivalTime(time);
    st.setPickupType(PickDrop.NONE);
    st.setDropOffType(PickDrop.NONE);
    return st;
  }

  private static FeedScopedId newId() {
    return FeedScopedIdForTestFactory.id(Integer.toString(++SEQ_NR));
  }

  private TripPattern createTripPattern(Collection<Trip> trips) {
    FeedScopedId patternId = FeedScopedIdForTestFactory.id(
      trips
        .stream()
        .map(t -> t.getId().getId())
        .collect(Collectors.joining(":"))
    );
    TripPatternBuilder tpb = TripPattern.of(patternId)
      .withRoute(route)
      .withStopPattern(STOP_PATTERN);

    for (Trip trip : trips) {
      tpb.withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(TripTimesFactory.tripTimes(trip, STOP_TIMES, DEDUPLICATOR))
      );
    }
    return tpb.build();
  }

  private Trip createTrip(String id, FeedScopedId serviceId) {
    return TransitRepositoryForTest.trip(id)
      .withServiceId(serviceId)
      .withDirection(Direction.INBOUND)
      .withRoute(route)
      .build();
  }
}
