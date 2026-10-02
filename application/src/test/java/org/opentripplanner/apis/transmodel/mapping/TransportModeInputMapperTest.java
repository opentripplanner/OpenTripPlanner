package org.opentripplanner.apis.transmodel.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.transit.model.basic.TransitMode;

class TransportModeInputMapperTest {

  @Test
  void mapTransitMode() {
    assertThat(TransportModeInputMapper.mapTransitMode(TransitMode.BUS)).isEqualTo(TransitMode.BUS);
  }

  @Test
  void mapNullTransitMode() {
    assertThat(TransportModeInputMapper.mapTransitMode(null)).isNull();
  }

  @Test
  void rejectUnknownTransitMode() {
    // The GraphQL TransportMode enum maps "unknown" to a String, not to a TransitMode
    var ex = assertThrows(InvalidInputException.class, () ->
      TransportModeInputMapper.mapTransitMode("unknown")
    );
    assertThat(ex)
      .hasMessageThat()
      .isEqualTo("transportMode 'unknown' cannot be used as a filter.");
  }

  @Test
  void mapTransitModes() {
    assertThat(TransportModeInputMapper.mapTransitModes(List.of(TransitMode.BUS, TransitMode.RAIL)))
      .containsExactly(TransitMode.BUS, TransitMode.RAIL)
      .inOrder();
  }

  @Test
  void mapNullTransitModes() {
    assertThat(TransportModeInputMapper.mapTransitModes(null)).isNull();
  }

  @Test
  void rejectUnknownInTransitModes() {
    assertThrows(InvalidInputException.class, () ->
      TransportModeInputMapper.mapTransitModes(List.of(TransitMode.BUS, "unknown"))
    );
  }
}
