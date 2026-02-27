package org.opentripplanner.routing.linking.internal;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.linking.internal.VertexCreationService.LocationType.FROM;
import static org.opentripplanner.routing.linking.internal.VertexCreationService.LocationType.TO;
import static org.opentripplanner.routing.linking.internal.VertexCreationService.LocationType.VISIT_VIA_LOCATION;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.TraverseMode;

class VertexCreationServiceTest {

  private static final GenericLocation LOCATION = GenericLocation.fromCoordinate(1.0, 2.0);
  private static final List<TraverseMode> MODES = List.of(TraverseMode.CAR);
  private static final List<VertexCreationService.LocationType> ALL_TYPES = List.of(
    FROM,
    VISIT_VIA_LOCATION,
    TO
  );

  @Test
  void createVertexCreationRequestForFrom() {
    var request = VertexCreationService.createVertexCreationRequest(LOCATION, MODES, FROM);
    assertThat(request.incomingModes()).isEmpty();
    assertEquals(MODES, request.outgoingModes());
    assertEquals("Origin", request.label().toString(ENGLISH));
    assertEquals(LOCATION.getCoordinate(), request.coordinate());
  }

  @Test
  void createVertexCreationRequestForVia() {
    var request = VertexCreationService.createVertexCreationRequest(
      LOCATION,
      MODES,
      VertexCreationService.LocationType.VISIT_VIA_LOCATION
    );
    assertEquals(MODES, request.incomingModes());
    assertEquals(MODES, request.outgoingModes());
    assertEquals("Via location", request.label().toString(ENGLISH));
    assertEquals(LOCATION.getCoordinate(), request.coordinate());
  }

  @Test
  void createVertexCreationRequestForTo() {
    var request = VertexCreationService.createVertexCreationRequest(LOCATION, MODES, TO);
    assertEquals(MODES, request.incomingModes());
    assertThat(request.outgoingModes()).isEmpty();
    assertEquals("Destination", request.label().toString(ENGLISH));
    assertEquals(LOCATION.getCoordinate(), request.coordinate());
  }

  @Test
  void createVertexCreationRequestWithCustomLabel() {
    var label = "Label";
    var location = new GenericLocation(label, null, 1.0, 2.0);
    var modes = List.of(TraverseMode.WALK);
    var request = VertexCreationService.createVertexCreationRequest(
      location,
      modes,
      VertexCreationService.LocationType.VISIT_VIA_LOCATION
    );
    assertEquals(label, request.label().toString());
  }

  private static List<Arguments> getTraverseModeForLinkerTestCases() {
    return List.of(
      Arguments.of(
        List.of(
          StreetMode.WALK,
          StreetMode.BIKE,
          StreetMode.BIKE_TO_PARK,
          StreetMode.BIKE_RENTAL,
          StreetMode.CAR_RENTAL,
          StreetMode.SCOOTER_RENTAL,
          StreetMode.CARPOOL
        ),
        ALL_TYPES,
        List.of(TraverseMode.WALK)
      ),
      Arguments.of(List.of(StreetMode.CAR), ALL_TYPES, List.of(TraverseMode.CAR)),
      Arguments.of(
        List.of(StreetMode.FLEXIBLE, StreetMode.CAR_HAILING, StreetMode.CAR_PICKUP),
        ALL_TYPES,
        List.of(TraverseMode.WALK, TraverseMode.CAR)
      ),
      Arguments.of(List.of(StreetMode.CAR_TO_PARK), List.of(FROM), List.of(TraverseMode.CAR)),
      Arguments.of(
        List.of(StreetMode.CAR_TO_PARK),
        List.of(VISIT_VIA_LOCATION, TO),
        List.of(TraverseMode.WALK)
      )
    );
  }

  @ParameterizedTest
  @MethodSource("getTraverseModeForLinkerTestCases")
  void getTraverseModeForLinker(
    List<StreetMode> modes,
    List<VertexCreationService.LocationType> types,
    List<TraverseMode> expected
  ) {
    for (var mode : modes) {
      for (var type : types) {
        var resultModes = VertexCreationService.getTraverseModeForLinker(mode, type);
        assertThat(resultModes).containsExactlyElementsIn(expected);
      }
    }
  }
}
