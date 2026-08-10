package org.opentripplanner.updater.trip.siri.moduletests.extrajourney;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

/// This test adds an extra journey on a service date where the original pattern is not running.
/// It correctly ends up in the API queries but the RAPTOR transit data drops it.
@Disabled("Not supported right now but should be fixed")
class ExtraJourneyOnServiceDateTest implements RealtimeTestConstants {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 23);
  private static final LocalDate NEXT_DAY = SERVICE_DATE.plusDays(1);

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(NEXT_DAY);
  private final Route route = envBuilder.route("route", builder ->
    builder.withOperator(Operator.of(id("operator")).withName("OP").build())
  );
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(7))
    .withRoute(route)
    .addStop(STOP_A, "12:00")
    .addStop(STOP_B, "12:10");

  @Test
  void addJourneyOnServiceDateWherePatternIsNotRunning() {
    var env = envBuilder.addTrip(TRIP_1_INPUT).build();
    assertThat(env.raptorData(SERVICE_DATE).summarizePatterns()).containsExactly("F:Pattern1[S]");
    assertThat(env.raptorData(NEXT_DAY).summarizePatterns()).isEmpty();

    var siri = SiriTestHelper.of(env);
    var updates = addedJourney(siri);

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "A U | A 13:00 13:00 | B 13:10 13:10",
      env.tripData(ADDED_TRIP_ID, NEXT_DAY).showTimetable()
    );
    assertEquals(
      "S | A 13:00 13:00 | B 13:10 13:10",
      env.tripData(ADDED_TRIP_ID, NEXT_DAY).showScheduledTimetable()
    );
    assertThat(env.raptorData(NEXT_DAY).summarizePatterns()).containsExactly(
      "F:route::001:RT[A U]"
    );
  }

  private List<EstimatedTimetableDeliveryStructure> addedJourney(SiriTestHelper siri) {
    return siriEtBuilder(siri).withIsExtraJourney(true).buildEstimatedTimetableDeliveries();
  }

  private SiriEtBuilder siriEtBuilder(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withOperatorRef(route.getOperator().getId().getId())
      .withLineRef(route.getId().getId())
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(NEXT_DAY).withVehicleJourneyRef("XXX")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("13:00", "13:00")
          .call(STOP_B)
          .departAimedExpected("13:10", "13:10")
      );
  }
}
