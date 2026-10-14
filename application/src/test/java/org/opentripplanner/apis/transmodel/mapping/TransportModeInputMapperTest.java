package org.opentripplanner.apis.transmodel.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.transit.model.basic.TransitMode;

class TransportModeInputMapperTest {

  @Test
  void mapTransitMode() {
    assertEquals(TransitMode.BUS, TransportModeInputMapper.mapTransitMode(TransitMode.BUS));
  }

  @Test
  void mapNullTransitMode() {
    assertNull(TransportModeInputMapper.mapTransitMode(null));
  }

  @Test
  void rejectUnknownTransitMode() {
    // The GraphQL TransportMode enum maps "unknown" to a String, not to a TransitMode
    assertThrows(InvalidInputException.class, () ->
      TransportModeInputMapper.mapTransitMode("unknown")
    );
  }

  @Test
  void mapTransitModes() {
    assertThat(TransportModeInputMapper.mapTransitModes(List.of(TransitMode.BUS, TransitMode.RAIL)))
      .containsExactly(TransitMode.BUS, TransitMode.RAIL)
      .inOrder();
  }

  @Test
  void mapNullTransitModes() {
    assertThat(TransportModeInputMapper.mapTransitModes(null)).isEmpty();
  }

  @Test
  void rejectUnknownInTransitModes() {
    assertThrows(InvalidInputException.class, () ->
      TransportModeInputMapper.mapTransitModes(List.of(TransitMode.BUS, "unknown"))
    );
  }
}
