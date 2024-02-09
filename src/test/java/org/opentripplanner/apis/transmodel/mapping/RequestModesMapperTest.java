package org.opentripplanner.apis.transmodel.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

class RequestModesMapperTest {

  @Test
  void testMapRequestModesEmptyMapReturnsDefaults() {
    Map<String, StreetMode> inputModes = Map.of();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(RequestModes.of().build(), mappedModes);
  }

  @Test
  void testMapRequestModesAccessSetReturnsDefaultsForOthers() {
    Map<String, StreetMode> inputModes = Map.of("accessMode", StreetMode.BIKE);

    RequestModes wantModes = RequestModes
      .of()
      .withAccessMode(StreetMode.BIKE)
      .withTransferMode(StreetMode.BIKE)
      .build();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(wantModes, mappedModes);
  }

  @Test
  void testMapRequestModesEgressSetReturnsDefaultsForOthers() {
    Map<String, StreetMode> inputModes = Map.of("egressMode", StreetMode.CAR);

    RequestModes wantModes = RequestModes.of().withEgressMode(StreetMode.CAR).build();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(wantModes, mappedModes);
  }

  @Test
  void testMapRequestModesDirectSetReturnsDefaultsForOthers() {
    Map<String, StreetMode> inputModes = Map.of("directMode", StreetMode.CAR);

    RequestModes wantModes = RequestModes.of().withDirectMode(StreetMode.CAR).build();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(wantModes, mappedModes);
  }
}
