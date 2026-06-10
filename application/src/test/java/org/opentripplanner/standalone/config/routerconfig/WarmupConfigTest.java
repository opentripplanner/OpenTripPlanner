package org.opentripplanner.standalone.config.routerconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.street.model.StreetMode;

class WarmupConfigTest {

  @Test
  void defaultModesWhenAbsent() {
    var root = createNodeAdapter(
      """
      {
        warmup: {
          from: { lat: 59.91, lon: 10.75 },
          to: { lat: 59.95, lon: 10.76 }
        }
      }
      """
    );
    var config = WarmupConfig.mapWarmupConfig("warmup", root);
    assertNotNull(config);
    assertEquals(List.of(StreetMode.WALK, StreetMode.CAR_TO_PARK), config.accessModes());
    assertEquals(List.of(StreetMode.WALK, StreetMode.WALK), config.egressModes());
  }

  @Test
  void mismatchedModeListSizesThrows() {
    var root = createNodeAdapter(
      """
      {
        warmup: {
          from: { lat: 59.91, lon: 10.75 },
          to: { lat: 59.95, lon: 10.76 },
          accessModes: ["WALK", "BIKE", "CAR_TO_PARK"],
          egressModes: ["WALK", "BIKE"]
        }
      }
      """
    );
    assertThrows(IllegalArgumentException.class, () ->
      WarmupConfig.mapWarmupConfig("warmup", root)
    );
  }

  private static NodeAdapter createNodeAdapter(String jsonText) {
    return new NodeAdapter(jsonNodeForTest(jsonText), "Test");
  }
}
