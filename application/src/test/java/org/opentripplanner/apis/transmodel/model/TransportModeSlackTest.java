package org.opentripplanner.apis.transmodel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.transit.model.basic.TransitMode;

public class TransportModeSlackTest {

  private static final Duration D10_m = Duration.ofMinutes(10);
  private static final Duration D30_m = Duration.ofMinutes(30);
  private static final Duration D60_m = Duration.ofMinutes(60);

  private static final int D10_m_SECONDS = (int) D10_m.toSeconds();
  private static final int D30_m_SECONDS = (int) D30_m.toSeconds();
  private static final int D60_m_SECONDS = (int) D60_m.toSeconds();

  @Test
  public void mapToApiList() {
    // Given
    DurationForEnum<TransitMode> domain = DurationForEnum.of(TransitMode.class)
      .with(TransitMode.FUNICULAR, D10_m)
      .with(TransitMode.CABLE_CAR, D10_m)
      .with(TransitMode.RAIL, D30_m)
      .with(TransitMode.AIRPLANE, D60_m)
      .build();

    // When
    List<TransportModeSlack> result = TransportModeSlack.mapToApiList(domain);

    assertEquals(D10_m.toSeconds(), result.get(0).slack);
    assertTrue(result.get(0).modes.contains(TransitMode.CABLE_CAR));
    assertTrue(result.get(0).modes.contains(TransitMode.FUNICULAR));

    assertEquals(D30_m.toSeconds(), result.get(1).slack);
    assertTrue(result.get(1).modes.contains(TransitMode.RAIL));

    assertEquals(D60_m.toSeconds(), result.get(2).slack);
    assertTrue(result.get(2).modes.contains(TransitMode.AIRPLANE));
  }

  @Test
  public void mapToDomain() {
    // Given
    List<Object> apiSlackInput = List.of(
      Map.of(
        "slack",
        D10_m_SECONDS,
        "modes",
        List.of(TransitMode.FUNICULAR, TransitMode.CABLE_CAR)
      ),
      Map.of("slack", D30_m_SECONDS, "modes", List.of(TransitMode.RAIL)),
      Map.of("slack", D60_m_SECONDS, "modes", List.of(TransitMode.AIRPLANE))
    );

    var builder = DurationForEnum.of(TransitMode.class);

    // When
    TransportModeSlack.mapIntoDomain(builder, apiSlackInput);

    var result = builder.build();

    // Then
    assertEquals(Duration.ZERO, result.valueOf(TransitMode.BUS));
    assertEquals(D10_m, result.valueOf(TransitMode.FUNICULAR));
    assertEquals(D10_m, result.valueOf(TransitMode.CABLE_CAR));
    assertEquals(D30_m, result.valueOf(TransitMode.RAIL));
    assertEquals(D60_m, result.valueOf(TransitMode.AIRPLANE));
  }
}
