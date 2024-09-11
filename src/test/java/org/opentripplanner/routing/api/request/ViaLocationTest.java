package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class ViaLocationTest {

  private static final FeedScopedId ID = FeedScopedId.ofNullable("F", "1");

  public static final String LABEL = "AName";
  private static final ViaLocation passThroughLocation = ViaLocation.passThroughLocation(
    LABEL,
    List.of(ID)
  );

  // TODO add cases for none passThroughLocation here when added...

  @Test
  void allowAsPassThroughPoint() {
    assertTrue(passThroughLocation.allowAsPassThroughPoint());
  }

  @Test
  void minimumWaitTime() {
    assertEquals(Duration.ZERO, passThroughLocation.minimumWaitTime());
  }

  @Test
  void label() {
    assertEquals(LABEL, passThroughLocation.label());
  }

  @Test
  void connections() {
    assertEquals("[F:1]", passThroughLocation.connections().toString());
  }

  @Test
  void testToString() {
    assertEquals(
      "ViaLocation{AName, allowAsPassThroughPoint, connections: [F:1]}",
      passThroughLocation.toString()
    );
  }
}
