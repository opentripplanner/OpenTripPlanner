package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;
import static org.opentripplanner.model.plan.PlanTestConstants.C;
import static org.opentripplanner.model.plan.PlanTestConstants.D;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_01;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_15;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_30;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_50;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;

class LegacyGraphQLItineraryImplTest {

  static final Instant ALERT_START_TIME = OffsetDateTime
    .parse("2023-02-15T12:03:28+01:00")
    .toInstant();
  static final Instant ALERT_END_TIME = ALERT_START_TIME.plus(1, ChronoUnit.DAYS);

  @Test
  void legs() throws Exception {
    Itinerary i1 = newItinerary(A, T11_00)
      .walk(20, B)
      .bus(122, T11_01, T11_15, C)
      .rail(439, T11_30, T11_50, D)
      .build();

    var railLeg = i1.getTransitLeg(2);
    var alert = TransitAlert
      .of(id("an-alert"))
      .withHeaderText(new NonLocalizedString("A header"))
      .withDescriptionText(new NonLocalizedString("A description"))
      .addTimePeriod(
        new TimePeriod(ALERT_START_TIME.getEpochSecond(), ALERT_END_TIME.getEpochSecond())
      )
      .build();
    railLeg.addAlert(alert);
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().source(i1).build();

    var fetcher = new LegacyGraphQLItineraryImpl();

    assertEquals(3000, fetcher.duration().get(env));
    var legs = ListUtils.ofIterable(fetcher.legs().get(env));
    assertEquals(3, legs.size());
    assertEquals(1, legs.get(2).getTransitAlerts().size());
  }
}
