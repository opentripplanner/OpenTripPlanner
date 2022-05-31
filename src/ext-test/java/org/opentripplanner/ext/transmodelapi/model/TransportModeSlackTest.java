package org.opentripplanner.ext.transmodelapi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.TransitMode;

public class TransportModeSlackTest {

  @Test
  public void mapToApiList() {
    // Given
    Map<TransitMode, Integer> domain = Map.of(
      TransitMode.FUNICULAR,
      600,
      TransitMode.CABLE_CAR,
      600,
      TransitMode.RAIL,
      1800,
      TransitMode.AIRPLANE,
      3600
    );

    // When
    List<TransportModeSlack> result = TransportModeSlack.mapToApiList(domain);

    assertEquals(600, result.get(0).slack);
    assertTrue(result.get(0).modes.contains(TransitMode.CABLE_CAR));
    assertTrue(result.get(0).modes.contains(TransitMode.FUNICULAR));

    assertEquals(1800, result.get(1).slack);
    assertTrue(result.get(1).modes.contains(TransitMode.RAIL));

    assertEquals(3600, result.get(2).slack);
    assertTrue(result.get(2).modes.contains(TransitMode.AIRPLANE));
  }

  @Test
  public void mapToDomain() {
    // Given
    List<Object> apiSlackInput = List.of(
      Map.of("slack", 600, "modes", List.of(TransitMode.FUNICULAR, TransitMode.CABLE_CAR)),
      Map.of("slack", 1800, "modes", List.of(TransitMode.RAIL)),
      Map.of("slack", 3600, "modes", List.of(TransitMode.AIRPLANE))
    );

    Map<TransitMode, Integer> result;

    // When
    result = TransportModeSlack.mapToDomain(apiSlackInput);

    // Then
    assertNull(result.get(TransitMode.BUS));
    assertEquals(Integer.valueOf(600), result.get(TransitMode.FUNICULAR));
    assertEquals(Integer.valueOf(600), result.get(TransitMode.CABLE_CAR));
    assertEquals(Integer.valueOf(1800), result.get(TransitMode.RAIL));
    assertEquals(Integer.valueOf(3600), result.get(TransitMode.AIRPLANE));
  }
}
