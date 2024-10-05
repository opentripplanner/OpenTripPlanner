package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

/**
 * A test for the successful case is at {@link LegacyRouteRequestMapperTest#passThroughPoints()}
 */
class PassThroughLocationMapperTest {

  public static List<List<Map<?, ?>>> failureCases() {
    return List.of(
      List.of(Map.of("passThrough", Map.of("stopLocationIds", List.of("fantasy:id")))),
      List.of(Map.of("passThrough", Map.of())),
      List.of(Map.of("passThrough", Map.of()), Map.of("passThroughLocation", Map.of()))
    );
  }

  @ParameterizedTest
  @MethodSource("failureCases")
  void throwException(List<Map<String, Object>> params) {
    var service = new DefaultTransitService(new TransitModel());
    assertThrows(
      IllegalArgumentException.class,
      () -> PassThroughLocationMapper.toLocations(service, params)
    );
  }
}
