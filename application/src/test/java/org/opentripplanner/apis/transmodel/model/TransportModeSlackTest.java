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

  private static final Duration D10m = Duration.ofMinutes(10);
  private static final Duration D30m = Duration.ofMinutes(30);
  private static final Duration D60m = Duration.ofMinutes(60);

  private static final int D10mSec = (int) D10m.toSeconds();
  private static final int D30mSec = (int) D30m.toSeconds();
  private static final int D60mSec = (int) D60m.toSeconds();

  @Test
  public void mapToApiList() {
    // Given
    DurationForEnum<TransitMode> domain = DurationForEnum.of(TransitMode.class)
      .with(TransitMode.FUNICULAR, D10m)
      .with(TransitMode.CABLE_CAR, D10m)
      .with(TransitMode.RAIL, D30m)
      .with(TransitMode.AIRPLANE, D60m)
      .build();

    // When
    List<TransportModeSlack> result = TransportModeSlack.mapToApiList(domain);

    assertEquals(D10m.toSeconds(), result.get(0).slack);
    assertTrue(result.get(0).modes.contains(TransitMode.CABLE_CAR));
    assertTrue(result.get(0).modes.contains(TransitMode.FUNICULAR));

    assertEquals(D30m.toSeconds(), result.get(1).slack);
    assertTrue(result.get(1).modes.contains(TransitMode.RAIL));

    assertEquals(D60m.toSeconds(), result.get(2).slack);
    assertTrue(result.get(2).modes.contains(TransitMode.AIRPLANE));
  }

  @Test
  public void mapToDomain() {
    // Given
    List<Object> apiSlackInput = List.of(
      Map.of("slack", D10mSec, "modes", List.of(TransitMode.FUNICULAR, TransitMode.CABLE_CAR)),
      Map.of("slack", D30mSec, "modes", List.of(TransitMode.RAIL)),
      Map.of("slack", D60mSec, "modes", List.of(TransitMode.AIRPLANE))
    );

    var builder = DurationForEnum.of(TransitMode.class);

    // When
    TransportModeSlack.mapIntoDomain(builder, apiSlackInput);

    var result = builder.build();

    // Then
    assertEquals(Duration.ZERO, result.valueOf(TransitMode.BUS));
    assertEquals(D10m, result.valueOf(TransitMode.FUNICULAR));
    assertEquals(D10m, result.valueOf(TransitMode.CABLE_CAR));
    assertEquals(D30m, result.valueOf(TransitMode.RAIL));
    assertEquals(D60m, result.valueOf(TransitMode.AIRPLANE));
  }
}
