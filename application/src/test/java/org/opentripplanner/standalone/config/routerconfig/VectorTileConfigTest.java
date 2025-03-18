package org.opentripplanner.standalone.config.routerconfig;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.ext.vectortiles.layers.LayerFilters;
import org.opentripplanner.inspector.vector.LayerParameters;

class VectorTileConfigTest {

  @Test
  void fallbackWhenEmpty() {
    assertEquals(LayerParameters.MIN_ZOOM, VectorTileConfig.DEFAULT.minZoom());
    assertEquals(LayerParameters.MAX_ZOOM, VectorTileConfig.DEFAULT.maxZoom());
  }

  @Test
  void computeZoomFromLayers() {
    final int maxZoom = 24;
    final int minZoom = 2;
    var config = new VectorTileConfig(
      List.of(
        new VectorTileConfig.Layer(
          "aaa",
          VectorTilesResource.LayerType.Stop,
          "a-mapper",
          maxZoom,
          minZoom,
          60,
          0.25,
          LayerFilters.FilterType.NONE
        )
      ),
      null,
      null
    );
    assertEquals(minZoom, config.minZoom());
    assertEquals(maxZoom, config.maxZoom());
  }
}
