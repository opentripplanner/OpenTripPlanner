package org.opentripplanner.apis.vectortiles;

import java.util.List;
import org.opentripplanner.apis.vectortiles.model.LayerStyleBuilder;
import org.opentripplanner.apis.vectortiles.model.StyleSpec;
import org.opentripplanner.apis.vectortiles.model.TileSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.RasterSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;

/**
 *  A Mapbox/Mapblibre style specification for rendering debug information about transit and
 *  street data.
 */
public class DebugStyleSpec {

  private static final RasterSource BACKGROUND_SOURCE = new RasterSource(
    "background",
    List.of("https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"),
    256,
    "© OpenStreetMap Contributors"
  );

  public record VectorSourceLayer(VectorSource vectorSource, String vectorLayer) {}

  static StyleSpec build(VectorSource debugSource, VectorSourceLayer regularStops) {
    List<TileSource> sources = List.of(BACKGROUND_SOURCE, debugSource);
    return new StyleSpec(
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
          .vectorSourceLayer(regularStops)
          .circleStroke("#140d0e", 2)
          .circleColor("#fcf9fa")
          .minZoom(13)
          .maxZoom(22)
      )
    );
  }
}
