package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLArrivalDeparture;
import org.opentripplanner.transit.service.ArrivalDeparture;

class ArrivalDepartureMapperTest {

  @Test
  void mapAllValues() {
    assertEquals(
      ArrivalDeparture.ARRIVALS,
      ArrivalDepartureMapper.map(GraphQLArrivalDeparture.ARRIVALS)
    );
    assertEquals(
      ArrivalDeparture.DEPARTURES,
      ArrivalDepartureMapper.map(GraphQLArrivalDeparture.DEPARTURES)
    );
    assertEquals(ArrivalDeparture.BOTH, ArrivalDepartureMapper.map(GraphQLArrivalDeparture.EITHER));
  }

  @Test
  void nullDefaultsToBoth() {
    assertThat(ArrivalDepartureMapper.map(null)).isEqualTo(ArrivalDeparture.BOTH);
  }
}
