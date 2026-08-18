package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.dataFetchingEnvironment;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.model.RealTimeTripStateModel;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.calendar.DefaultTripCalendars;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

class TripOnServiceDateImplTest {

  private static final String TRIP_ID = "Trip1";
  private static final String TOSD_ID = "Tosd1";
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 15);

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");
  private final RegularStop STOP_C = envBuilder.stop("C");

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_ID)
    .withWithTripOnServiceDate(TOSD_ID)
    .addStop(STOP_A, "10:00:00", "10:00:00")
    .addStop(STOP_B, "10:30:00", "10:30:00")
    .addStop(STOP_C, "11:00:00", "11:00:00");

  private static final TripOnServiceDateImpl SUBJECT = new TripOnServiceDateImpl();

  // ---------------------------------------------------------------------------
  // realTimeTripState
  // ---------------------------------------------------------------------------

  @Test
  void scheduledTrip_allFlagsFalse() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var transitService = env.transitService();
    var tosd = transitService.getTripOnServiceDate(id(TOSD_ID));
    var dfEnv = dataFetchingEnvironment(tosd, Map.of(), transitService);
    var state = SUBJECT.realTimeTripState().get(dfEnv);
    assertEquals(new RealTimeTripStateModel(false, false, false, false, false), state);
  }

  @Test
  void canceledTrip_canceledAndUpdatedFlagsTrue() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var canceledTripTimes = tripData
      .scheduledTripTimes()
      .createRealTimeFromScheduledTimes()
      .withCanceled()
      .build();
    var transitService = transitServiceWithUpdate(
      env,
      RealTimeTripUpdate.of(tripData.tripPattern(), canceledTripTimes, SERVICE_DATE).build()
    );
    var tosd = transitService.getTripOnServiceDate(id(TOSD_ID));
    var dfEnv = dataFetchingEnvironment(tosd, Map.of(), transitService);
    var state = SUBJECT.realTimeTripState().get(dfEnv);
    assertTrue(state.canceled());
    assertTrue(state.updated());
    assertFalse(state.added());
    assertFalse(state.timesModified());
    assertFalse(state.tripPatternModified());
  }

  @Test
  void delayedTrip_timesModifiedAndUpdatedFlagsTrue() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var delayedTripTimes = tripData
      .scheduledTripTimes()
      .createRealTimeFromScheduledTimes()
      .withRealTimeUpdated()
      .build();
    var transitService = transitServiceWithUpdate(
      env,
      RealTimeTripUpdate.of(tripData.tripPattern(), delayedTripTimes, SERVICE_DATE).build()
    );
    var tosd = transitService.getTripOnServiceDate(id(TOSD_ID));
    var dfEnv = dataFetchingEnvironment(tosd, Map.of(), transitService);
    var state = SUBJECT.realTimeTripState().get(dfEnv);
    assertTrue(state.timesModified());
    assertTrue(state.updated());
    assertFalse(state.canceled());
    assertFalse(state.added());
    assertFalse(state.tripPatternModified());
  }

  @Test
  void tripWithNoMatchingPattern_returnsNull() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var transitService = env.transitService();
    // A TripOnServiceDate whose trip is unknown to the transit service → no pattern found.
    // The trip is built standalone (not added via addTrip), so it is not registered with any
    // pattern and findPattern returns null.
    var unknownTrip = Trip.of(id("unknown")).withRoute(envBuilder.route("Runknown")).build();
    var tosd = TripOnServiceDate.of(id("unknown-tosd"))
      .withTrip(unknownTrip)
      .withServiceDate(SERVICE_DATE)
      .build();
    var dfEnv = dataFetchingEnvironment(tosd, Map.of(), transitService);
    assertNull(SUBJECT.realTimeTripState().get(dfEnv));
  }

  private static TransitService transitServiceWithUpdate(
    TransitTestEnvironment env,
    RealTimeTripUpdate update
  ) {
    var repo = env.transitRepository();
    var snapshot = new DefaultTimetableRepository(
      repo.getRaptorTransitData(),
      new DefaultTripCalendars()
    );
    snapshot.update(update);
    return new DefaultTransitService(repo, snapshot.commit());
  }
}
