package org.opentripplanner.apis.vectortiles;

import java.util.List;
import org.opentripplanner.apis.vectortiles.model.LayerStyleBuilder;
import org.opentripplanner.apis.vectortiles.model.StyleSpec;
import org.opentripplanner.apis.vectortiles.model.TileSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.RasterSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;

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
  private static final String MAGENTA = "#f21d52";
  private static final String YELLOW = "#e2d40d";
  private static final String GREY = "#8e8e89";
  private static final int MAX_ZOOM = 23;

  public record VectorSourceLayer(VectorSource vectorSource, String vectorLayer) {}

  static StyleSpec build(
    VectorSource debugSource,
    VectorSourceLayer regularStops,
    VectorSourceLayer edges
  ) {
    List<TileSource> sources = List.of(BACKGROUND_SOURCE, debugSource);
    return new StyleSpec(
      "OTP Debug Tiles",
      sources,
      List.of(
        LayerStyleBuilder.ofId("background").typeRaster().source(BACKGROUND_SOURCE).minZoom(0),
        LayerStyleBuilder
          .ofId("edge-fallback")
          .typeLine()
          .vectorSourceLayer(edges)
          .lineColor(GREY)
          .lineWidth(3)
          .minZoom(15)
          .maxZoom(MAX_ZOOM),
        LayerStyleBuilder
          .ofId("edge")
          .typeLine()
          .vectorSourceLayer(edges)
          .lineColor(MAGENTA)
          .edgeFilter(
            StreetEdge.class,
            AreaEdge.class,
            EscalatorEdge.class,
            TemporaryPartialStreetEdge.class,
            TemporaryFreeEdge.class
          )
          .lineWidth(3)
          .minZoom(13)
          .maxZoom(MAX_ZOOM),
        LayerStyleBuilder
          .ofId("link")
          .typeLine()
          .vectorSourceLayer(edges)
          .lineColor(YELLOW)
          .edgeFilter(
            StreetTransitStopLink.class,
            StreetTransitEntranceLink.class,
            BoardingLocationToStopLink.class
          )
          .lineWidth(3)
          .minZoom(13)
          .maxZoom(MAX_ZOOM),
        LayerStyleBuilder
          .ofId("regular-stop")
          .typeCircle()
          .vectorSourceLayer(regularStops)
          .circleStroke("#140d0e", 2)
          .circleColor("#fcf9fa")
          .minZoom(13)
          .maxZoom(MAX_ZOOM)
      )
    );
  }
}
