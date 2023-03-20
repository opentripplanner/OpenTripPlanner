package org.opentripplanner.ext.ridehailing;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TestItineraryBuilder;

class RideHailingFilterTest implements PlanTestConstants {

  @Test
  void noServices() {
    var i = TestItineraryBuilder.newItinerary(A).drive(T11_30, PlanTestConstants.T11_50, B).build();
    var filter = new RideHailingFilter(List.of());

    var filtered = filter.filter(List.of(i));

    assertSame(StreetLeg.class, filtered.get(0).getLegs().get(0).getClass());
  }
}
