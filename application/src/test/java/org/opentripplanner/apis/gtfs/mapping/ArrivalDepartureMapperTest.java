package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLArrivalDeparture;
import org.opentripplanner.transit.service.ArrivalDeparture;

class ArrivalDepartureMapperTest {

  @ParameterizedTest
  @EnumSource(GraphQLArrivalDeparture.class)
  void mapAllValues(GraphQLArrivalDeparture graphQLValue) {
    assertThat(ArrivalDepartureMapper.map(graphQLValue)).isEqualTo(
      ArrivalDeparture.valueOf(graphQLValue.name())
    );
  }

  @Test
  void nullDefaultsToBoth() {
    assertThat(ArrivalDepartureMapper.map(null)).isEqualTo(ArrivalDeparture.BOTH);
  }
}
