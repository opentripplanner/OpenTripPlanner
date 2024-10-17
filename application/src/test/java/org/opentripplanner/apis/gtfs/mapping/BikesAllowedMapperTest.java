package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLBikesAllowed;
import org.opentripplanner.transit.model.network.BikeAccess;

class BikesAllowedMapperTest {

  @Test
  void mapping() {
    assertEquals(GraphQLBikesAllowed.NO_INFORMATION, BikesAllowedMapper.map(BikeAccess.UNKNOWN));
    assertEquals(GraphQLBikesAllowed.NOT_ALLOWED, BikesAllowedMapper.map(BikeAccess.NOT_ALLOWED));
    assertEquals(GraphQLBikesAllowed.ALLOWED, BikesAllowedMapper.map(BikeAccess.ALLOWED));
  }
}
