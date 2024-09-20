package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLCarsAllowed;
import org.opentripplanner.transit.model.network.CarAccess;

class CarsAllowedMapperTest {

  @Test
  void mapping() {
    assertEquals(GraphQLCarsAllowed.NO_INFORMATION, CarsAllowedMapper.map(CarAccess.UNKNOWN));
    assertEquals(GraphQLCarsAllowed.NOT_ALLOWED, CarsAllowedMapper.map(CarAccess.NOT_ALLOWED));
    assertEquals(GraphQLCarsAllowed.ALLOWED, CarsAllowedMapper.map(CarAccess.ALLOWED));
  }
}
