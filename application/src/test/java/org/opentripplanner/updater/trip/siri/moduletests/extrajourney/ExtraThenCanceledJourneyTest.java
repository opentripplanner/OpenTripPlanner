package org.opentripplanner.updater.trip.siri.moduletests.extrajourney;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

class ExtraThenCanceledJourneyTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stop(STOP_B_ID);

  private final Operator OPERATOR = envBuilder.operator("operatorId");
  private final Route ROUTE = envBuilder.route("routeId", OPERATOR);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withRoute(ROUTE)
    .addStop(stopA, "10:00", "10:00")
    .addStop(stopB, "10:10", "10:10");

  @Test
  void extraThenCanceledJourney() {
    var env = envBuilder.addTrip(TRIP_1_INPUT).build();
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");
    var siri = SiriTestHelper.of(env);

    assertSuccess(siri.applyEstimatedTimetable(addedJourney(siri)));

    assertEquals(
      "ADDED | A [R] 11:00 11:00 | B 11:10 11:10",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );

    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:routeId::001:RT[ADDED]"
    );

    // cancel the added journey again, should add a cancelled trip to the raptor data
    assertSuccess(siri.applyEstimatedTimetable(cancelledJourney(siri)));
    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:routeId::001:RT[CANCELED]"
    );

    assertEquals(
      "CANCELED | A 11:00 11:00 | B 11:10 11:10",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
  }

  private List<EstimatedTimetableDeliveryStructure> addedJourney(SiriTestHelper siri) {
    return siriEtBuilder(siri).withIsExtraJourney(true).buildEstimatedTimetableDeliveries();
  }

  private List<EstimatedTimetableDeliveryStructure> cancelledJourney(SiriTestHelper siri) {
    return siriEtBuilder(siri).withCancellation(true).buildEstimatedTimetableDeliveries();
  }

  private SiriEtBuilder siriEtBuilder(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withOperatorRef("operatorId")
      .withLineRef("routeId")
      .withRecordedCalls(builder -> builder.call(stopA).departAimedActual("11:00", "11:00"))
      .withEstimatedCalls(builder -> builder.call(stopB).arriveAimedExpected("11:10", "11:10"));
  }
}
