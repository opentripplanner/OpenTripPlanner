package org.opentripplanner.apis.vectortiles;

import java.util.List;
import org.opentripplanner.apis.vectortiles.model.LayerStyleBuilder;
import org.opentripplanner.apis.vectortiles.model.MapboxStyleJson;
import org.opentripplanner.apis.vectortiles.model.TileSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.RasterSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;

public class DebugStyleJson {

  private static final RasterSource BACKGROUND_SOURCE = new RasterSource(
    "background",
    List.of("https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"),
    256
  );

  static MapboxStyleJson build(String url) {
    var vectorSource = new VectorSource("debug", url);
    List<TileSource> sources = List.of(BACKGROUND_SOURCE, vectorSource);
    return new MapboxStyleJson(
      "OTP Debug Tiles",
      sources,
      List.of(
        LayerStyleBuilder
          .ofId("background")
          .typeRaster()
          .source(BACKGROUND_SOURCE)
          .minZoom(0)
          .maxZoom(22),
        LayerStyleBuilder
          .ofId("regular-stop")
          .typeCircle()
          .source(vectorSource)
          .sourceLayer("regularStops")
          .circleStroke("#140d0e", 1)
          .circleColor("#fcf9fa")
          .minZoom(13)
          .maxZoom(22)
      )
    );
  }
}
