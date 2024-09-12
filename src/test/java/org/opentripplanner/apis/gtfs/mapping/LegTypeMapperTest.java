package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLLegType;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;

class LegTypeMapperTest implements PlanTestConstants {

  @Test
  void flex() {
    var flexLeg = TestItineraryBuilder.newItinerary(A).flex(T11_00, T11_30, B).build().firstLeg();
    assertEquals(GraphQLLegType.FLEX, LegTypeMapper.map(flexLeg));
  }

  @Test
  void transit() {
    var busLeg = TestItineraryBuilder.newItinerary(A).bus(1, T11_00, T11_30, B).build().firstLeg();
    assertEquals(GraphQLLegType.SCHEDULED_TRANSIT, LegTypeMapper.map(busLeg));
  }
}
