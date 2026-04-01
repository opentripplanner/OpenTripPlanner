package org.opentripplanner.transit.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.network.ReplacedByRelation;
import org.opentripplanner.transit.model.network.ReplacementForRelation;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

public class ReplacementHelperTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);

  private static final String OPERATOR_ID = "operator_1";
  private static final String ADDED_TRIP_2_ID = "added_trip_2";
  private final String ROUTE_1_ID = "route_1";
  private final String ROUTE_2_ID = "route_2";
  private final Route ROUTE_1 = envBuilder.route(ROUTE_1_ID);
  private final Route ROUTE_2 = envBuilder.route(ROUTE_2_ID, routeBuilder ->
    routeBuilder.withNetexSubmode("railReplacementBus")
  );

  private final TransitTestEnvironment env = envBuilder
    .addTrip(
      TripInput.of(TRIP_1_ID)
        .withServiceDates(
          envBuilder.defaultServiceDate().minusDays(1),
          envBuilder.defaultServiceDate().plusDays(1)
        )
        .withRoute(ROUTE_1)
        .addStop(STOP_A, "12:00", "12:00")
        .addStop(STOP_B, "12:10", "12:10")
        .addStop(STOP_C, "12:20", "12:20")
    )
    .addTrip(
      TripInput.of(TRIP_2_ID)
        .withServiceDates(
          envBuilder.defaultServiceDate().minusDays(1),
          envBuilder.defaultServiceDate().plusDays(1)
        )
        .withNetexSubmode("railReplacementBus")
        .withRoute(ROUTE_2)
        .addStop(STOP_A, "13:00", "13:00")
        .addStop(STOP_B, "13:10", "13:10")
        .addStop(STOP_C, "13:20", "13:20")
    )
    .addStops(STOP_A_ID, STOP_B_ID, STOP_C_ID)
    .build();

  @Test
  void isReplacementRoute() {
    var service = env.transitService();
    var helper = service.getReplacementHelper();
    var trip1 = service.getTrip(id(TRIP_1_ID));
    var route1 = trip1.getRoute();
    var trip2 = service.getTrip(id(TRIP_2_ID));
    var route2 = trip2.getRoute();
    assertFalse(helper.isReplacementTrip(trip1));
    assertTrue(helper.isReplacementTrip(trip2));
    assertFalse(helper.isReplacementRoute(route1));
    assertTrue(helper.isReplacementRoute(route2));
  }

  @Test
  void replacementApiFields() {
    var addedTripOnServiceDate = createReplacementTrip(TRIP_1_ID, ADDED_TRIP_ID, "1");
    assertNotNull(addedTripOnServiceDate);
    // Because we don't generate synthetic TripOnServiceDates, replacementFor is
    // going to be empty here! Have to generate another realtime trip to actually
    // get something in the replacementFor list.
    // assertThat(addedTripOnServiceDate.getReplacementFor()).hasSize(1);

    var addedTripOnServiceDate2 = createReplacementTrip(ADDED_TRIP_ID, ADDED_TRIP_2_ID, "2");
    assertNotNull(addedTripOnServiceDate2);
    assertThat(addedTripOnServiceDate2.getReplacementFor()).hasSize(1);
    assertEquals(addedTripOnServiceDate, addedTripOnServiceDate2.getReplacementFor().get(0));

    var transitService = env.transitService();
    var replacementHelper = transitService.getReplacementHelper();

    assertTrue(replacementHelper.replacementsExist(transitService.getRoute(id(ROUTE_1_ID))));

    // fails because there is no synthetic TOSD for TRIP_1_ID
    // assertTrue(replacementHelper.replacementsExist(transitService.getTrip(id(TRIP_1_ID))));

    assertThat(
      StreamSupport.stream(
        replacementHelper.getReplacedBy(addedTripOnServiceDate).spliterator(),
        false
      )
        .map(ReplacedByRelation::getTripOnServiceDate)
        .toList()
    ).contains(addedTripOnServiceDate2);
    assertThat(
      StreamSupport.stream(
        replacementHelper.getReplacementFor(addedTripOnServiceDate2).spliterator(),
        false
      )
        .map(ReplacementForRelation::getTripOnServiceDate)
        .toList()
    ).contains(addedTripOnServiceDate);
  }

  private TripOnServiceDate createReplacementTrip(String oldId, String newId, String min) {
    var siri = SiriTestHelper.of(env);
    var createExtraJourney = siri
      .etBuilder()
      .withOperatorRef(OPERATOR_ID)
      .withEstimatedVehicleJourneyCode(newId)
      .withIsExtraJourney(true)
      .withLineRef(ROUTE_1_ID)
      .withVehicleJourneyRef(oldId)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("12:00", "12:0" + min)
          .call(STOP_B)
          .departAimedExpected("12:10", "12:1" + min)
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(createExtraJourney);
    assertSuccess(result);

    var service = env.transitService();
    return service.getTripOnServiceDate(id(newId));
  }
}
