package org.opentripplanner.apis.transmodel.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

class RequestModesMapperTest {

  private static final RequestModes MODES_NOT_SET = RequestModes.of()
    .withAccessMode(StreetMode.NOT_SET)
    .withEgressMode(StreetMode.NOT_SET)
    .withDirectMode(StreetMode.NOT_SET)
    .build();

  @Test
  void testMapRequestModesEmptyMapReturnsDefaults() {
    Map<String, StreetMode> inputModes = Map.of();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(MODES_NOT_SET, mappedModes);
  }

  @Test
  void testMapRequestModesScooterRentalAccessSetReturnsDefaultsForOthers() {
    Map<String, StreetMode> inputModes = Map.of("accessMode", StreetMode.SCOOTER_RENTAL);

    RequestModes wantModes = MODES_NOT_SET.copyOf()
      .withAccessMode(StreetMode.SCOOTER_RENTAL)
      .withTransferMode(StreetMode.WALK)
      .withDirectMode(null)
      .build();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(wantModes, mappedModes);
  }

  @Test
  void testMapRequestModesBikeAccessSetReturnsDefaultsForOthers() {
    Map<String, StreetMode> inputModes = Map.of("accessMode", StreetMode.BIKE);

    RequestModes wantModes = MODES_NOT_SET.copyOf()
      .withAccessMode(StreetMode.BIKE)
      .withTransferMode(StreetMode.BIKE)
      .withDirectMode(null)
      .build();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(wantModes, mappedModes);
  }

  @Test
  void testMapRequestModesEgressSetReturnsDefaultsForOthers() {
    Map<String, StreetMode> inputModes = Map.of("egressMode", StreetMode.CAR);

    RequestModes wantModes = MODES_NOT_SET.copyOf()
      .withEgressMode(StreetMode.CAR)
      .withDirectMode(null)
      .build();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(wantModes, mappedModes);
  }

  @Test
  void testMapRequestModesDirectSetReturnsDefaultsForOthers() {
    Map<String, StreetMode> inputModes = Map.of("directMode", StreetMode.CAR);

    RequestModes wantModes = MODES_NOT_SET.copyOf().withDirectMode(StreetMode.CAR).build();

    RequestModes mappedModes = RequestModesMapper.mapRequestModes(inputModes);

    assertEquals(wantModes, mappedModes);
  }
}
