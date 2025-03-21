package org.opentripplanner.standalone.config.routerconfig;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.ext.vectortiles.layers.LayerFilters;
import org.opentripplanner.inspector.vector.LayerParameters;

class VectorTileConfigTest {

  @Test
  void fallbackWhenEmpty() {
    assertEquals(LayerParameters.MIN_ZOOM, VectorTileConfig.DEFAULT.minZoom(Set.of()));
    assertEquals(LayerParameters.MAX_ZOOM, VectorTileConfig.DEFAULT.maxZoom(Set.of()));
  }

  @Test
  void fallbackWhenNotFound() {
    assertEquals(LayerParameters.MIN_ZOOM, VectorTileConfig.DEFAULT.minZoom(Set.of("x")));
    assertEquals(LayerParameters.MAX_ZOOM, VectorTileConfig.DEFAULT.maxZoom(Set.of("x")));
  }

  @Test
  void computeZoomFromLayers() {
    final int maxZoom = 24;
    final int minZoom = 2;
    var config = new VectorTileConfig(
      List.of(layerConfig("a", minZoom, maxZoom), layerConfig("b", minZoom + 1, maxZoom - 1)),
      null,
      null
    );
    assertEquals(minZoom, config.minZoom(Set.of("a", "b")));
    assertEquals(maxZoom, config.maxZoom(Set.of("a", "b")));

    assertEquals(minZoom, config.minZoom(Set.of("a")));
    assertEquals(maxZoom, config.maxZoom(Set.of("a")));
  }

  private static VectorTileConfig.Layer layerConfig(String name, int minZoom, int maxZoom) {
    return new VectorTileConfig.Layer(
      name,
      VectorTilesResource.LayerType.Stop,
      "a-mapper",
      maxZoom,
      minZoom,
      60,
      0.25,
      LayerFilters.FilterType.NONE
    );
  }
}
